package tilenet.system

import chisel3._
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.lazymodule.LazyModule

import constellation.soc.CanHaveGlobalNoC
import freechips.rocketchip.devices.debug.Debug
import freechips.rocketchip.system.{ExampleRocketSystem, ExampleRocketSystemModuleImp, SimAXIMem}
import freechips.rocketchip.util.AsyncResetReg
import testchipip.soc.CanHaveSubsystemInjectors

class ExampleRocketNoCSystem(implicit p: Parameters)
    extends ExampleRocketSystem
    with CanHaveGlobalNoC 
    with CanHaveSubsystemInjectors {
  override lazy val module = new ExampleRocketSystemModuleImp(this)
}