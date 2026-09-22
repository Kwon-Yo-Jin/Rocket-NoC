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

.PHONY: verilog constellation test vis_list vis clean_all clean_verilog clean_test

verilog:
	cd $(base_dir) && $(MILL) emulator[$(TOP),$(CONFIG_LONG)].mfccompiler.compile

constellation:
	@mkdir -p $(SBT_LOG_DIR)
	(cd constellation && MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch test) | tee $(SBT_LOG_DIR)/Constellation_Test.log

test:
	@mkdir -p $(SBT_LOG_DIR)
	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly rocketnoc.test.CustomDUTEvalNoCTest' 2>&1    | tee $(SBT_LOG_DIR)/Custom_Test.log
	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly rocketnoc.test.BaselineMeshEvalNoCTest' 2>&1 | tee $(SBT_LOG_DIR)/Baseline_Test.log
#	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly rocketnoc.test.CustomDUTNoCTest' 2>&1        | tee $(SBT_LOG_DIR)/Custom_Test.log
#	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly rocketnoc.test.CustomDUTTLNoCTest' 2>&1      | tee $(SBT_LOG_DIR)/Custom_TLTest.log
#	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly rocketnoc.test.CustomDUTAXI4NoCTest' 2>&1    | tee $(SBT_LOG_DIR)/Custom_AXI4Test.log
#	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly rocketnoc.test.CustomDUTEvalNoCTest' 2>&1    | tee $(SBT_LOG_DIR)/Custom_EvalTest.log
#	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly rocketnoc.test.BaselineMeshNoCTest' 2>&1     | tee $(SBT_LOG_DIR)/Baseline_Test.log
#	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly rocketnoc.test.BaselineMeshTLNoCTest' 2>&1   | tee $(SBT_LOG_DIR)/Baseline_TLTest.log
#	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly rocketnoc.test.BaselineMeshAXI4NoCTest' 2>&1 | tee $(SBT_LOG_DIR)/Baseline_AXI4Test.log
#	MAKEFLAGS='VM_PARALLEL_BUILDS=0' sbt -sbt-version 1.10.2 -batch 'testOnly rocketnoc.test.BaselineMeshEvalNoCTest' 2>&1 | tee $(SBT_LOG_DIR)/Baseline_EvalTest.log

vis_list:
	find . -name '*.noc.adjlist' -o -name '*.noc.xy' -o -name '*.noc.edgeprops'

vis: vis_list
	MPLBACKEND=Agg python3 constellation/scripts/vis.py $(VIS_PATH) \
	--area-per-node 2 \
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
	rm -rf *.png
