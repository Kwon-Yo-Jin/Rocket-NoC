package rocketnoc.config.fragment

import freechips.rocketchip.rocket.{RocketCoreConfig}

class WithRV32IMAF extends RocketCoreConfig(c => c.copy(
  xLen = 32,
  pgLevels = 2, // sv32
  fpu = c.fpu.map(_.copy(fLen = 32)),
  useCompressed = false
))
class WithRV32IMAFC extends RocketCoreConfig(c => c.copy(
  xLen = 32,
  pgLevels = 2, // sv32
  fpu = c.fpu.map(_.copy(fLen = 32)),
  useCompressed = true
))
class WithRV32IMAFD extends RocketCoreConfig(c => c.copy(
  xLen = 32,
  pgLevels = 2, // sv32
  fpu = c.fpu.map(_.copy(fLen = 64)),
  useCompressed = false
))
class WithRV32IMAFDC extends RocketCoreConfig(c => c.copy(
  xLen = 32,
  pgLevels = 2, // sv32
  fpu = c.fpu.map(_.copy(fLen = 64)),
  useCompressed = true
))