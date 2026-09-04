package rocketnoc.system

import chisel3._
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.lazymodule.LazyModule

import constellation.soc.CanHaveGlobalNoC
import freechips.rocketchip.devices.debug.Debug
import freechips.rocketchip.subsystem.SubsystemBankedCoherenceKey
import freechips.rocketchip.system.{ExampleRocketSystem, ExampleRocketSystemModuleImp, SimAXIMem}
import freechips.rocketchip.tilelink.TLAdapterNode
import freechips.rocketchip.util.AsyncResetReg
import testchipip.soc.CanHaveSubsystemInjectors

object RocketSystem {
  private def nameInclusiveCacheManagers(p: Parameters): Parameters = {
    val coherence = p(SubsystemBankedCoherenceKey)
    p.alterPartial { case SubsystemBankedCoherenceKey =>
      coherence.copy(coherenceManager = { context =>
        implicit val contextParameters: Parameters = context.p
        val (inwardNode, outwardNode, halt) = coherence.coherenceManager(context)
        val managerRenamer = TLAdapterNode(managerFn = { port =>
          port.v1copy(managers = port.managers.map(
            _.v2copy(name = Some("L2 InclusiveCache"))))
        })
        inwardNode :*= managerRenamer
        (managerRenamer, outwardNode, halt)
      })
    }
  }
}

class RocketSystem(implicit p: Parameters)
    extends ExampleRocketSystem()(RocketSystem.nameInclusiveCacheManagers(p))
    with CanHaveGlobalNoC 
    with CanHaveSubsystemInjectors {
  override lazy val module = new RocketSystemModuleImp(this)
}

class RocketSystemModuleImp[+L <: RocketSystem](_outer: L) extends ExampleRocketSystemModuleImp(_outer)
