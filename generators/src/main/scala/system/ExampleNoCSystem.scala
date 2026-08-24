package tilenet.system

import chisel3._
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.lazymodule.LazyModule

import constellation.protocol.{GlobalTLNoCParams, SimpleTLNoCParams, SplitACDxBETLNoCParams, TLGlobalNoC, TLNoC, TLSplitACDxBENoC}
import constellation.soc.{CanHaveGlobalNoC, ConstellationMemoryBusParams}
import freechips.rocketchip.devices.debug.Debug
import freechips.rocketchip.devices.tilelink.BuiltInDevices
import freechips.rocketchip.subsystem.{COH, CoherenceManagerWrapperParams, HasTileLinkLocations, InSubsystem, MBUS, SubsystemBankedCoherenceKey, TLNetworkTopologyLocated}
import freechips.rocketchip.system.{ExampleRocketSystem, ExampleRocketSystemModuleImp, SimAXIMem}
import freechips.rocketchip.tilelink.{ProbePicker, RegionReplicator, TLAdapterNode, TLBusWrapper, TLBusWrapperInstantiationLike, TLBusWrapperTopology, TLFIFOFixer}
import freechips.rocketchip.util.{AsyncResetReg, Location}
import testchipip.soc.CanHaveSubsystemInjectors

object ExampleRocketNoCSystem {
  private case class NamedMemoryBusParams(params: ConstellationMemoryBusParams)
      extends TLBusWrapperInstantiationLike {
    def instantiate(context: HasTileLinkLocations, loc: Location[TLBusWrapper])
        (implicit p: Parameters): TLBusWrapper = {
      val bus = LazyModule(new NamedMemoryBus(params, loc.name, context))
      bus.suggestName(loc.name)
      context.tlBusWrapperLocationMap += (loc -> bus)
      bus
    }
  }

  private class NamedMemoryBus(
    params: ConstellationMemoryBusParams,
    name: String,
    context: HasTileLinkLocations
  )(implicit p: Parameters) extends TLBusWrapper(params.mbusParams, name) {
    private val replicator = params.mbusParams.replication.map(
      r => LazyModule(new RegionReplicator(r)))
    val prefixNode = replicator.map { r =>
      r.prefix := addressPrefixNexusNode
      addressPrefixNexusNode
    }

    private val memoryBusNoC = params.tlNoCParams match {
      case nocParams: GlobalTLNoCParams => context.asInstanceOf[CanHaveGlobalNoC].globalNoCDomain {
        LazyModule(new TLGlobalNoC(nocParams, name))
      }
      case nocParams: SimpleTLNoCParams =>
        LazyModule(new TLNoC(nocParams, name, params.inlineNoC))
      case nocParams: SplitACDxBETLNoCParams =>
        LazyModule(new TLSplitACDxBENoC(nocParams, name, params.inlineNoC))
    }

    val inwardNode = replicator
      .map(memoryBusNoC.node :*=* TLFIFOFixer(TLFIFOFixer.all) :*=* _.node)
      .getOrElse(memoryBusNoC.node :*=* TLFIFOFixer(TLFIFOFixer.all))

    private val subsystem = context.asInstanceOf[LazyModule]
    private val managerRenamer = TLAdapterNode(managerFn = { port =>
      port.v1copy(managers = port.managers.map { manager =>
        if (manager.nodePath.lastOption.exists(_.lazyModule eq subsystem))
          manager.v2copy(name = Some("Memory Channel"))
        else manager
      })
    })
    val outwardNode = managerRenamer :*= ProbePicker() :*= memoryBusNoC.node
    def busView = memoryBusNoC.node.edges.in.head

    val builtInDevices = BuiltInDevices.attach(params.mbusParams, outwardNode)
  }

  private def nameNoCEndpoints(p: Parameters): Parameters = {
    val coherence = p(SubsystemBankedCoherenceKey)
    val namedCoherence = coherence.copy(coherenceManager = { context =>
      implicit val contextParameters: Parameters = context.p
      val (inwardNode, outwardNode, halt) = coherence.coherenceManager(context)
      val managerRenamer = TLAdapterNode(managerFn = { port =>
        port.v1copy(managers = port.managers.map(
          _.v2copy(name = Some("L2 InclusiveCache"))))
      })
      inwardNode :*= managerRenamer
      (managerRenamer, outwardNode, halt)
    })
    val topology = p(TLNetworkTopologyLocated(InSubsystem))
    p.alterPartial {
      case SubsystemBankedCoherenceKey => namedCoherence
      case TLNetworkTopologyLocated(InSubsystem) => topology.map {
        case busTopology: TLBusWrapperTopology =>
          new TLBusWrapperTopology(busTopology.instantiations.map {
            case (MBUS, params: ConstellationMemoryBusParams) =>
              (MBUS, NamedMemoryBusParams(params))
            case (COH, params: CoherenceManagerWrapperParams) =>
              (COH, CoherenceManagerWrapperParams(
                blockBytes = params.blockBytes,
                beatBytes = params.beatBytes,
                nBanks = params.nBanks,
                name = params.name,
                dtsFrequency = params.dtsFrequency)(namedCoherence.coherenceManager))
            case instantiation => instantiation
          }, busTopology.connections)
        case other => other
      }
    }
  }
}

class ExampleRocketNoCSystem(implicit p: Parameters)
    extends ExampleRocketSystem()(ExampleRocketNoCSystem.nameNoCEndpoints(p))
    with CanHaveGlobalNoC 
    with CanHaveSubsystemInjectors {
  override lazy val module = new ExampleRocketNoCSystemModuleImp(this)
}

class ExampleRocketNoCSystemModuleImp[+L <: ExampleRocketNoCSystem](_outer: L) extends ExampleRocketSystemModuleImp(_outer)
