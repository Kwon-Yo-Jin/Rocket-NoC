package rocketnoc.config.fragment

import org.chipsalliance.cde.config.Config
import org.chipsalliance.diplomacy.lazymodule.LazyModule

import freechips.rocketchip.resources.{Resource, ResourceBinding, ResourceString}
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink.{TLCacheCork, TLFilter}
import sifive.blocks.inclusivecache._

private object HierarchicalInclusiveCache {
  private case class Settings(
    level: Int,
    nBanks: Int,
    nWays: Int,
    capacityKB: Int,
    outerLatencyCycles: Int,
    subBankingFactor: Int,
    hintsSkipProbe: Boolean,
    ctrlAddr: Option[Int],
    writeBytes: Int,
    innerBus: TLBusWrapperLocation,
    controlBus: TLBusWrapperLocation,
    corkOuter: Boolean,
    deviceName: String)

  private def sets(settings: Settings, blockBytes: Int): Int = {
    require(settings.nBanks > 0, "An inclusive cache requires at least one bank")
    val denominator = blockBytes * settings.nWays * settings.nBanks
    require(
      settings.capacityKB * 1024 % denominator == 0,
      s"${settings.capacityKB} KiB cannot be divided into ${settings.nBanks} banks, " +
        s"${settings.nWays} ways, and ${blockBytes}-byte blocks")
    settings.capacityKB * 1024 / denominator
  }

  private def manager(settings: Settings, cacheSets: Int): CoherenceManagerWrapper.CoherenceManagerInstantiationFn = {
    context => {
      implicit val p = context.p
      val innerBus = context.locateTLBusWrapper(settings.innerBus)
      val controlBus = context.locateTLBusWrapper(settings.controlBus)

      val control = settings.ctrlAddr.map { address =>
        InclusiveCacheControlParameters(
          address = address,
          beatBytes = controlBus.beatBytes,
          bankedControl = false)
      }

      val cache = LazyModule(new InclusiveCache(
        CacheParameters(
          level = settings.level,
          ways = settings.nWays,
          sets = cacheSets,
          blockBytes = innerBus.blockBytes,
          beatBytes = innerBus.beatBytes,
          hintsSkipProbe = settings.hintsSkipProbe),
        InclusiveCacheMicroParameters(
          writeBytes = settings.writeBytes,
          portFactor = settings.subBankingFactor,
          memCycles = settings.outerLatencyCycles),
        control,
        settings.deviceName))

      if (!settings.corkOuter) {
        ResourceBinding { Resource(cache.device, "exists").bind(ResourceString("yes")) }
      }

      // DCache MMIO traffic must bypass the coherent cache datapath.
      def skipMMIO(client: freechips.rocketchip.tilelink.TLClientParameters) = {
        val dcacheMMIO =
          client.requestFifo &&
          client.sourceId.start % 2 == 1 &&
          client.nodePath.last.name == "dcache.node"
        if (dcacheMMIO) None else Some(client)
      }

      val filter = LazyModule(new TLFilter(cfilter = skipMMIO))
      val innerBuffer = InclusiveCachePortParameters.flowAD()
      val outerBuffer = InclusiveCachePortParameters.none()

      innerBuffer.suggestName(s"L${settings.level}_InclusiveCache_inner_TLBuffer")
      outerBuffer.suggestName(s"L${settings.level}_InclusiveCache_outer_TLBuffer")

      innerBuffer.node :*= filter.node
      cache.node :*= innerBuffer.node
      outerBuffer.node :*= cache.node

      cache.ctrls.foreach {
        _.ctrlnode := controlBus.coupleTo(s"l${settings.level}_ctrl") {
          freechips.rocketchip.tilelink.TLBuffer(1) :=
            freechips.rocketchip.tilelink.TLFragmenter(controlBus, Some(s"L${settings.level}Ctrl")) := _
        }
      }

      if (settings.corkOuter) {
        val cork = LazyModule(new TLCacheCork)
        cork.node :*= outerBuffer.node
        (filter.node, cork.node, None)
      } else {
        // Preserve TL-C across the cluster boundary so the global cache sees
        // this whole cluster cache as one coherent, probe-capable client.
        (filter.node, outerBuffer.node, None)
      }
    }
  }

  def cluster(
    clusterId: Int,
    nBanks: Int,
    nWays: Int,
    capacityKB: Int,
    outerLatencyCycles: Int,
    subBankingFactor: Int,
    hintsSkipProbe: Boolean,
    ctrlAddr: Option[Int],
    writeBytes: Int) = new Config((site, _, up) => {
      case ClusterBankedCoherenceKey(`clusterId`) => {
        val settings = Settings(
          level = 2,
          nBanks = nBanks,
          nWays = nWays,
          capacityKB = capacityKB,
          outerLatencyCycles = outerLatencyCycles,
          subBankingFactor = subBankingFactor,
          hintsSkipProbe = hintsSkipProbe,
          ctrlAddr = ctrlAddr,
          writeBytes = writeBytes,
          innerBus = CSBUS(clusterId),
          controlBus = CCBUS(clusterId),
          corkOuter = false,
          deviceName = s"cluster-l2-$clusterId")
        up(ClusterBankedCoherenceKey(clusterId), site).copy(
          nBanks = nBanks,
          coherenceManager = manager(settings, sets(settings, site(CacheBlockBytes))))
      }
    })

  def global(
    nBanks: Int,
    nWays: Int,
    capacityKB: Int,
    outerLatencyCycles: Int,
    subBankingFactor: Int,
    hintsSkipProbe: Boolean,
    ctrlAddr: Option[Int],
    writeBytes: Int) = new Config((site, _, up) => {
      case SubsystemBankedCoherenceKey => {
        val settings = Settings(
          level = 3,
          nBanks = nBanks,
          nWays = nWays,
          capacityKB = capacityKB,
          outerLatencyCycles = outerLatencyCycles,
          subBankingFactor = subBankingFactor,
          hintsSkipProbe = hintsSkipProbe,
          ctrlAddr = ctrlAddr,
          writeBytes = writeBytes,
          innerBus = SBUS,
          controlBus = CBUS,
          corkOuter = true,
          deviceName = "global-l3")
        up(SubsystemBankedCoherenceKey, site).copy(
          nBanks = nBanks,
          coherenceManager = manager(settings, sets(settings, site(CacheBlockBytes))))
      }
    })
}

/** Add a coherent inclusive L2 shared by the cores in one Rocket Chip cluster. */
class WithClusterInclusiveCache(
  clusterId: Int,
  nBanks: Int = 1,
  nWays: Int = 8,
  capacityKB: Int = 512,
  outerLatencyCycles: Int = 40,
  subBankingFactor: Int = 4,
  hintsSkipProbe: Boolean = false,
  ctrlAddr: Option[Int] = None,
  writeBytes: Int = 8)
    extends Config(HierarchicalInclusiveCache.cluster(
      clusterId,
      nBanks,
      nWays,
      capacityKB,
      outerLatencyCycles,
      subBankingFactor,
      hintsSkipProbe,
      ctrlAddr,
      writeBytes))

/** Add the last-level inclusive L3 that maintains coherence among clusters. */
class WithGlobalInclusiveCache(
  nBanks: Int = 32,
  nWays: Int = 8,
  capacityKB: Int = 16384,
  outerLatencyCycles: Int = 40,
  subBankingFactor: Int = 4,
  hintsSkipProbe: Boolean = false,
  ctrlAddr: Option[Int] = None,
  writeBytes: Int = 8)
    extends Config(HierarchicalInclusiveCache.global(
      nBanks,
      nWays,
      capacityKB,
      outerLatencyCycles,
      subBankingFactor,
      hintsSkipProbe,
      ctrlAddr,
      writeBytes))
