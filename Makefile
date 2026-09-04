SHELL := /bin/bash

base_dir=$(abspath ./)

CHISEL_VERSION=3.6.0
MODEL ?= TestHarness
TOP ?= rocketnoc.system.RocketSystem
PROJECT ?= rocketnoc
CFG_PROJECT ?= $(PROJECT).config
CONFIG ?= DefaultConfig
CONFIG_LONG ?= $(CFG_PROJECT).$(CONFIG)
MILL ?= mill
SBT_LOG_DIR ?= $(base_dir)/log
NOC ?= TLNoC

verilog:
	cd $(base_dir) && $(MILL) emulator[$(TOP),$(CONFIG_LONG)].mfccompiler.compile

test_all:
	@set -o pipefail; \
	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch test | tee $(SBT_LOG_DIR)/AllNocTest.log

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

vis:
	find . -name '*.noc.adjlist' -o -name '*.noc.xy' -o -name '*.noc.edgeprops'
	MPLBACKEND=Agg python3 constellation/scripts/vis.py \
	./out/emulator/$(TOP)/$(CONFIG_LONG)/generatorRun/elaborate.dest/$(CONFIG_LONG).$(NOC).noc. \
	--area-per-node 4 \
	--no-show

clean_verilog:
	rm -rf out/
	rm -rf */out/

clean_test:
	rm -rf log/
	rm -rf target/
	rm -rf test_run_dir/
	rm -rf .bsp/

clean_all: clean_test clean_verilog
	rm -rf *.svg
