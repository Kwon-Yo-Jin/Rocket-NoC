import mill._

trait Emulator extends Cross.Module2[String, String] {
  val top: String = crossValue
  val config: String = crossValue2

  object mfccompiler extends Module {
    def compile = T {
      val rocketChipDir = os.pwd / "rocket-chip"
      val rocketChipBuild = rocketChipDir / "build.sc"

      if (!os.exists(rocketChipDir)) {
        throw new RuntimeException(s"Missing Rocket Chip submodule: $rocketChipDir")
      }
      if (!os.exists(rocketChipBuild)) {
        throw new RuntimeException(s"Missing Rocket Chip Mill build: $rocketChipBuild")
      }

      val delegatedTarget = s"emulator[$top,$config].mfccompiler.compile"
      os.proc("mill", "-i", delegatedTarget).call(cwd = rocketChipDir)

      val delegatedCompileDest =
        rocketChipDir / "out" / "emulator" / top / config / "mfccompiler" / "compile.dest"
      if (!os.exists(delegatedCompileDest)) {
        throw new RuntimeException(
          s"Rocket Chip build completed but did not create: $delegatedCompileDest"
        )
      }

      for (path <- os.list(delegatedCompileDest)) {
        os.copy.over(path, T.dest / path.last, createFolders = true)
      }
      PathRef(T.dest)
    }
  }
}

object emulator extends Cross[Emulator](
  ("freechips.rocketchip.system.TestHarness", "freechips.rocketchip.system.DefaultConfig")
)
