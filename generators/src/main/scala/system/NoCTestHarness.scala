package tilenet.config

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

class NoCTestHarness()(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val success = Output(Bool())
  })

  val ldut = LazyModule(new ExampleRocketNoCSystem)
  val dut = Module(ldut.module)

  ldut.io_clocks.get.elements.values.foreach(_.clock := clock)
  val dutReset = (reset.asBool | ldut.debug.map { debug =>
    AsyncResetReg(debug.ndreset)
  }.getOrElse(false.B)).asBool
  ldut.io_clocks.get.elements.values.foreach(_.reset := dutReset)

  dut.dontTouchPorts()
  dut.tieOffInterrupts()
  SimAXIMem.connectMem(ldut)
  SimAXIMem.connectMMIO(ldut)

  ldut.l2_frontend_bus_axi4.foreach { a =>
    a.ar.valid := false.B
    a.ar.bits := DontCare
    a.aw.valid := false.B
    a.aw.bits := DontCare
    a.w.valid := false.B
    a.w.bits := DontCare
    a.r.ready := false.B
    a.b.ready := false.B
  }

  Debug.connectDebug(ldut.debug, ldut.resetctrl, ldut.psd, clock, reset.asBool, io.success)
}
