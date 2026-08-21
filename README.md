make clean
make verilog
make verilog TOP=tilenet.config.NoCTestHarness CONFIG=tilenet.config.QuadRocketSbusRingNoCConfig
make verilog TOP=tilenet.config.NoCTestHarness CONFIG=tilenet.config.MultiNoCConfig
make verilog TOP=tilenet.config.NoCTestHarness CONFIG=tilenet.config.SbusRingNoCConfig
make verilog TOP=tilenet.config.NoCTestHarness CONFIG=tilenet.config.SbusMeshNoCConfig
make verilog TOP=tilenet.config.NoCTestHarness CONFIG=tilenet.config.SharedNoCConfig