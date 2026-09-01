SHELL := /bin/bash

base_dir=$(abspath ./)

CHISEL_VERSION=3.6.0
MODEL ?= TestHarness
TOP ?= freechips.rocketchip.system.TestHarness
PROJECT ?= freechips.rocketchip.system
CFG_PROJECT ?= $(PROJECT)
CONFIG ?= $(CFG_PROJECT).DefaultConfig
MILL ?= mill
SBT_LOG_DIR ?= $(base_dir)/log

verilog:
	cd $(base_dir) && $(MILL) emulator[$(TOP),$(CONFIG)].mfccompiler.compile

test_all:
	@set -o pipefail; \
	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch test | tee $(SBT_LOG_DIR)/log.log

test:
	@mkdir -p log
	@set -o pipefail; \
	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly constellation.NoCTestEval00' | tee $(SBT_LOG_DIR)/NoCTestEval00.log
	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly constellation.NoCTestEval01' | tee $(SBT_LOG_DIR)/NoCTestEval01.log
	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly constellation.NoCTestEval02' | tee $(SBT_LOG_DIR)/NoCTestEval02.log
	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly constellation.NoCTestEval03' | tee $(SBT_LOG_DIR)/NoCTestEval03.log
	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly constellation.NoCTestEval04' | tee $(SBT_LOG_DIR)/NoCTestEval04.log
	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly constellation.NoCTestEval05' | tee $(SBT_LOG_DIR)/NoCTestEval05.log
	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly constellation.NoCTestEval06' | tee $(SBT_LOG_DIR)/NoCTestEval06.log
	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly constellation.NoCTestEval07' | tee $(SBT_LOG_DIR)/NoCTestEval07.log
	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly constellation.NoCTestEval08' | tee $(SBT_LOG_DIR)/NoCTestEval08.log

clean:
	rm -rf out/
	rm -rf */out/
	rm -rf target/
	rm -rf test_run_dir/
	rm -rf .bsp/
