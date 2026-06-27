import mill._
import mill.scalalib._
import coursier.maven.MavenRepository

object v {
  val scala = "2.13.12"
  val chisel = ivy"org.chipsalliance::chisel:6.7.0"
  val chiselPlugin = ivy"org.chipsalliance:::chisel-plugin:6.7.0"
  val mainargs = ivy"com.lihaoyi::mainargs:0.5.0"
  val json4sJackson = ivy"org.json4s::json4s-jackson:4.0.5"
  val scalaReflect = ivy"org.scala-lang:scala-reflect:${scala}"
  val sourcecode = ivy"com.lihaoyi::sourcecode:0.3.1"
  val sonatypeSnapshots = Seq(
    MavenRepository("https://s01.oss.sonatype.org/content/repositories/snapshots")
  )
}

trait ChiselIvyModule extends ScalaModule {
  override def scalaVersion: T[String] = T(v.scala)
  override def ivyDeps = T(super.ivyDeps() ++ Agg(v.chisel))
  override def scalacPluginIvyDeps = T(super.scalacPluginIvyDeps() ++ Agg(v.chiselPlugin))
  override def repositoriesTask = T.task(super.repositoriesTask() ++ v.sonatypeSnapshots)
}

object cde extends ScalaModule {
  override def scalaVersion: T[String] = T(v.scala)
  override def millSourcePath = os.pwd / "rocket-chip" / "dependencies" / "cde" / "cde"
  override def sources = T.sources { Seq(PathRef(millSourcePath / "src")) }
}

object diplomacy extends ChiselIvyModule {
  override def millSourcePath = os.pwd / "rocket-chip" / "dependencies" / "diplomacy" / "diplomacy"
  override def sources = T.sources { Seq(PathRef(millSourcePath / "src")) }
  override def moduleDeps = super.moduleDeps ++ Seq(cde)
  override def ivyDeps = T(super.ivyDeps() ++ Agg(v.sourcecode))
  override def scalacOptions = T(super.scalacOptions() ++ Seq("-Wunused"))
}

object hardfloat extends ChiselIvyModule {
  override def millSourcePath = os.pwd / "rocket-chip" / "dependencies" / "hardfloat" / "hardfloat"
}

object macros extends ScalaModule {
  override def scalaVersion: T[String] = T(v.scala)
  override def millSourcePath = os.pwd / "rocket-chip" / "macros"
  override def ivyDeps = T(super.ivyDeps() ++ Agg(v.scalaReflect))
}

object generator extends ChiselIvyModule {
  override def mainClass = T(Some("freechips.rocketchip.diplomacy.Main"))
  override def millSourcePath = os.pwd / "rocket-chip"
  override def moduleDeps = super.moduleDeps ++ Seq(macros, hardfloat, diplomacy)
  override def ivyDeps = T(super.ivyDeps() ++ Agg(v.mainargs, v.json4sJackson))
  override def resources = T.sources {
    Seq(PathRef(os.pwd / "rocket-chip" / "src" / "main" / "resources"))
  }

  override def sources = T.sources {
    Seq(
      PathRef(os.pwd / "rocket-chip" / "src" / "main" / "scala"),
      PathRef(os.pwd / "constellation" / "src" / "main" / "scala" / "channel"),
      PathRef(os.pwd / "constellation" / "src" / "main" / "scala" / "noc"),
      PathRef(os.pwd / "constellation" / "src" / "main" / "scala" / "protocol"),
      PathRef(os.pwd / "constellation" / "src" / "main" / "scala" / "router"),
      PathRef(os.pwd / "constellation" / "src" / "main" / "scala" / "routing"),
      PathRef(os.pwd / "constellation" / "src" / "main" / "scala" / "soc"),
      PathRef(os.pwd / "constellation" / "src" / "main" / "scala" / "topology"),
      PathRef(os.pwd / "constellation" / "src" / "main" / "scala" / "util"),
      PathRef(os.pwd / "generators" / "src" / "main" / "scala")
    )
  }
}

trait Emulator extends Cross.Module2[String, String] {
  val top: String = crossValue
  val config: String = crossValue2

  object generatorRun extends Module {
    def elaborate = T {
      val dest = T.dest
      os.proc(
        mill.util.Jvm.javaExe,
        "-jar",
        generator.assembly().path,
        "--dir", dest.toString,
        "--top", top,
        config.split('_').flatMap(c => Seq("--config", c)),
      ).call(cwd = os.pwd / "rocket-chip")
      PathRef(dest)
    }

    def chiselAnno = T {
      os.walk(elaborate().path).collectFirst { case p if p.last.endsWith("anno.json") => p }
        .map(PathRef(_))
        .getOrElse(throw new RuntimeException(s"No annotation JSON generated for $top / $config"))
    }

    def chirrtl = T {
      os.walk(elaborate().path).collectFirst { case p if p.last.endsWith("fir") => p }
        .map(PathRef(_))
        .getOrElse(throw new RuntimeException(s"No FIRRTL generated for $top / $config"))
    }
  }

  object mfccompiler extends Module {
    def compile = T {
      os.proc(
        "firtool",
        generatorRun.chirrtl().path,
        s"--annotation-file=${generatorRun.chiselAnno().path}",
        "--disable-annotation-unknown",
        "-dedup",
        "-O=debug",
        "--split-verilog",
        "--preserve-values=named",
        "--output-annotation-file=mfc.anno.json",
        s"-o=${T.dest}"
      ).call(T.dest)
      PathRef(T.dest)
    }
  }
}

object emulator extends Cross[Emulator](
  ("freechips.rocketchip.system.TestHarness", "freechips.rocketchip.system.DefaultConfig"),
  ("freechips.rocketchip.system.TestHarness", "tilenet.config.SingleRocketSbusRingNoCConfig"),
  ("freechips.rocketchip.system.TestHarness", "tilenet.config.QuadRocketSbusRingNoCConfig"),
  ("freechips.rocketchip.system.TestHarness", "tilenet.config.MultiNoCConfig"),
  ("freechips.rocketchip.system.TestHarness", "tilenet.config.SbusRingNoCConfig"),
  ("freechips.rocketchip.system.TestHarness", "tilenet.config.SbusMeshNoCConfig"),
  ("freechips.rocketchip.system.TestHarness", "tilenet.config.SharedNoCConfig")
)
