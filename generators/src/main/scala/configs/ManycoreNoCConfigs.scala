package rocketnoc.config

import org.chipsalliance.cde.config.{Config}
import freechips.rocketchip.subsystem.{SBUS, MBUS}

import constellation.channel._
import constellation.routing._
import constellation.router._
import constellation.topology._
import constellation.noc._
import constellation.soc.{GlobalNoCParams}

import scala.collection.immutable.ListMap


class SbusMesh64BigCoreNoCConfig extends Config(
  new constellation.soc.WithSbusNoC(constellation.protocol.SimpleTLNoCParams(
    constellation.protocol.DiplomaticNetworkNodeMapping(
      inNodeMapping = ListMap(
        ((0 until 64).map(i => s"Core $i " -> (i + 9 + 1 + i / 8)) :+
          ("debug" -> 18)): _*),
      outNodeMapping = ListMap(
        ((0 until 8).map(i => s"L2 InclusiveCache[$i]" -> (i + 1)) :+
          ("bootrom" -> 9) :+
          ("gen[0]" -> 0)): _*)), // TSI is on the pbus, so serial-tl and pbus should be on the same node
    NoCParams(
      topology        = Mesh2D(9, 9),
      channelParamGen = (a, b) => UserChannelParams(Seq.fill(5) { UserVirtualChannelParams(3) }, unifiedBuffer = false),
      routerParams    = (i) => UserRouterParams(combineRCVA=true, combineSAST=true),
      routingRelation = NonblockingVirtualSubnetworksRouting(Mesh2DDimensionOrderedRouting(), 5, 1),
    )), inlineNoC = true) ++
  new rocketnoc.config.fragment.WithRV32IMAFC ++
  new freechips.rocketchip.rocket.WithNBigCores(64) ++
  new freechips.rocketchip.subsystem.WithNBanks(8) ++
  new rocketnoc.config.fragment.WithSystemBusWidth(128) ++
  new rocketnoc.config.NoCBaseConfig
)

class GlobalMesh64BigCoreNoCConfig extends Config(
    new constellation.soc.WithGlobalNoC(GlobalNoCParams(
    NoCParams(
      topology        = Mesh2D(9, 9),
      channelParamGen = (a, b) => UserChannelParams(Seq.fill(16) { UserVirtualChannelParams(3) }, unifiedBuffer = false),
      routerParams    = (i) => UserRouterParams(combineRCVA=true, combineSAST=true),
      routingRelation = NonblockingVirtualSubnetworksRouting(Mesh2DEscapeRouting(), 15, 1),
    )
  )) ++
  new constellation.soc.WithMbusNoC(constellation.protocol.GlobalTLNoCParams(
    constellation.protocol.DiplomaticNetworkNodeMapping(
      inNodeMapping = ListMap(
        ((0 until 8).map(i => s"L2 InclusiveCache[$i]" -> (i + 1))): _*),
      outNodeMapping = ListMap(
        "gen[0]" -> 0))
  )) ++
  new constellation.soc.WithSbusNoC(constellation.protocol.GlobalTLNoCParams(
    constellation.protocol.DiplomaticNetworkNodeMapping(
      inNodeMapping = ListMap(
        ((0 until 64).map(i => s"Core $i " -> (i + 9 + 1 + i / 8)) :+
          ("debug" -> 27)): _*),
      outNodeMapping = ListMap(
        ((0 until 8).map(i => s"L2 InclusiveCache[$i]" -> (i + 1)) :+
          ("bootrom" -> 18) :+
          ("gen[0]" -> 9)): _*))
  )) ++
  new constellation.soc.WithCbusNoC(constellation.protocol.GlobalTLNoCParams(
    constellation.protocol.DiplomaticNetworkNodeMapping(
      inNodeMapping = ListMap(
        "debug" -> 27),
      outNodeMapping = ListMap(
        "error" -> 36, "ctrls[0]" -> 45, "pbus" -> 54, "plic" -> 63,
        "clint" -> 72, "dmInner" -> 72, "bootrom" -> 18)),
  )) ++
  new rocketnoc.config.fragment.WithRV32IMAFC ++
  new freechips.rocketchip.rocket.WithNBigCores(64) ++
  new freechips.rocketchip.subsystem.WithNBanks(8) ++
  new rocketnoc.config.fragment.WithSystemBusWidth(128) ++
  new rocketnoc.config.NoCBaseConfig
)

class DefaultConfig extends Config(
  new rocketnoc.config.fragment.WithRV32IMAFC ++
  new freechips.rocketchip.rocket.WithNBigCores(128) ++
  new freechips.rocketchip.subsystem.WithNBanks(16) ++
  new rocketnoc.config.fragment.WithSystemBusWidth(512) ++
  new rocketnoc.config.NoCBaseConfig
)

private object ClusterConfigFragments {
  val eightCoreClusters = (0 until 32).foldLeft(org.chipsalliance.cde.config.Parameters.empty) {
    case (tail, clusterId) =>
      new freechips.rocketchip.rocket.WithNBigCores(
        8,
        freechips.rocketchip.subsystem.InCluster(clusterId)) ++
      new rocketnoc.config.fragment.WithClusterInclusiveCache(clusterId) ++
      new freechips.rocketchip.subsystem.WithCluster(clusterId) ++
      tail
  }
}

class ClusterConfig extends Config(
  new rocketnoc.config.fragment.WithRV32IMAFC ++
  ClusterConfigFragments.eightCoreClusters ++
  new rocketnoc.config.fragment.WithGlobalInclusiveCache(
    nBanks = 32,
    capacityKB = 16384) ++
  new rocketnoc.config.fragment.WithSystemBusWidth(512) ++
  new rocketnoc.config.NoCBaseConfig
)
