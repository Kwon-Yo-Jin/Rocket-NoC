base_dir=$(abspath ./)

CHISEL_VERSION=3.6.0
MODEL ?= TestHarness
TOP ?= freechips.rocketchip.system.TestHarness
PROJECT ?= freechips.rocketchip.system
CFG_PROJECT ?= $(PROJECT)
CONFIG ?= $(CFG_PROJECT).DefaultConfig
MILL ?= mill

verilog:
	cd $(base_dir) && $(MILL) emulator[$(TOP),$(CONFIG)].mfccompiler.compile

clean:
	rm -rf out/
	rm -rf */out/
