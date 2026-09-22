package rocketnoc.test

import chisel3._
import chiseltest._
import chiseltest.simulator.{
  SimulatorDebugAnnotation,
  VerilatorCFlags,
  VerilatorFlags,
  VerilatorLinkFlags
}
import constellation.test._
import org.chipsalliance.cde.config.{Config, Parameters}
import org.scalatest.flatspec.AnyFlatSpec

class NoCChiselTester(implicit val p: Parameters) extends Module {
  private val harness = Module(new TestHarness)
  when(harness.io.success) { stop() }
}

class TLNoCChiselTester(implicit val p: Parameters) extends Module {
  private val harness = Module(new TLTestHarness)
  when(harness.io.success) { stop() }
}

class AXI4NoCChiselTester(implicit val p: Parameters) extends Module {
  private val harness = Module(new AXI4TestHarness)
  when(harness.io.success) { stop() }
}

class EvalNoCChiselTester(implicit val p: Parameters) extends Module {
  private val harness = Module(new EvalHarness)
  when(harness.io.success) { stop() }
}

abstract class BaseNoCTest(
  gen: Parameters => Module,
  configs: Seq[Config],
  extraVerilatorFlags: Seq[String] = Nil)
    extends AnyFlatSpec
    with ChiselScalatestTester {

  behavior of "NoC"

  configs.foreach { config =>
    it should s"pass test with config ${config.getClass.getName}" in {
      implicit val p: Parameters = config
      test(gen(p))
        .withAnnotations(Seq(
          SimulatorDebugAnnotation,
          VerilatorBackendAnnotation,
          VerilatorFlags(extraVerilatorFlags),
          VerilatorLinkFlags(Seq(
            "-Wl,--allow-multiple-definition",
            "-fcommon")),
          VerilatorCFlags(Seq(
            "-DNO_VPI",
            "-fcommon",
            "-fpermissive"))
        ))
        .runUntilStop(timeout = 1000 * 1000)
    }
  }
}

abstract class NoCTest(configs: Seq[Config]) extends BaseNoCTest(p => new NoCChiselTester()(p), configs)

abstract class TLNoCTest(configs: Seq[Config]) extends BaseNoCTest(p => new TLNoCChiselTester()(p), configs)

abstract class AXI4NoCTest(configs: Seq[Config]) extends BaseNoCTest(p => new AXI4NoCChiselTester()(p), configs)

abstract class EvalNoCTest(configs: Seq[Config]) extends BaseNoCTest(p => new EvalNoCChiselTester()(p), configs, Seq("../../../constellation/src/main/resources/csrc/netrace/netrace.o"))

/** The four suites selected by the root build.sbt regression filter. */
class CustomDUTNoCTest extends NoCTest(Seq(new CustomDUTNoCConfig))
class CustomDUTTLNoCTest extends TLNoCTest(Seq(new CustomDUTTLNoCConfig))
class CustomDUTAXI4NoCTest extends AXI4NoCTest(Seq(new CustomDUTAXI4NoCConfig))
class CustomDUTEvalNoCTest extends EvalNoCTest(Seq(new CustomDUTEvalNoCConfig))

class BaselineMeshNoCTest extends NoCTest(Seq(new BaselineMeshNoCConfig))
class BaselineMeshTLNoCTest extends TLNoCTest(Seq(new BaselineMeshTLNoCConfig))
class BaselineMeshAXI4NoCTest extends AXI4NoCTest(Seq(new BaselineMeshAXI4NoCConfig))
class BaselineMeshEvalNoCTest extends EvalNoCTest(Seq(new BaselineMeshEvalNoCConfig))
