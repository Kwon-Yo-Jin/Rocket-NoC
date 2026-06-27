make clean
make verilog
make verilog CONFIG=tilenet.config.SingleRocketSbusRingNoCConfig
make verilog CONFIG=tilenet.config.QuadRocketSbusRingNoCConfig
make verilog CONFIG=tilenet.config.MultiNoCConfig
make verilog CONFIG=tilenet.config.SbusRingNoCConfig
make verilog CONFIG=tilenet.config.SbusMeshNoCConfig
make verilog TOP=tilenet.config.NoCTestHarness CONFIG=tilenet.config.SharedNoCConfig