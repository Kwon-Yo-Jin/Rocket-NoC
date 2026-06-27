package tilenet.config

import org.chipsalliance.cde.config.Config
import freechips.rocketchip.rocket.{WithNBigCores, WithNHugeCores}
import freechips.rocketchip.subsystem.{SystemBusKey, WithCoherentBusTopology, WithNBanks, WithNMemoryChannels}
import freechips.rocketchip.system.BaseConfig

import constellation.channel._
import constellation.routing._
import constellation.router._
import constellation.topology._
import constellation.noc._
import constellation.protocol.{DiplomaticNetworkNodeMapping, GlobalTLNoCParams, SimpleTLNoCParams, SplitACDxBETLNoCParams}
import constellation.soc.{GlobalNoCParams, WithCbusNoC, WithGlobalNoC, WithMbusNoC, WithSbusNoC}

import scala.collection.immutable.ListMap

class WithSystemBusWidth(nBits: Int) extends Config((site, here, up) => {
  case SystemBusKey =>
    up(SystemBusKey, site).copy(beatBytes = nBits / 8)
})

/*
 * This config demonstrates a SoC architecture with three different
 * independent interconnects. The SBus interconnect is the main
 * backbone, handling core traffic to the L2s and peripherals.
 * The MBus handles requests to the DRAM and external memory.
 * The CBus handles requests to control devices.
 *
 * Topologies:
 *
 * 12 - 13 - 14 - 15
 *  |    |    |    |
 *  8 -- 9 - 10 - 11    0 -- 1 -- 2 -- 3
 *  |    |    |    |    |              |
 *  4 -- 5 -- 6 -- 7    7 -- 6 -- 5 -- 4
 *  |    |    |    |
 *  0 -- 1 -- 2 -- 3
 *
 * This table describes the mappings of each edge onto the network
 *
 * SI/SO: Inward/outward names into sbus
 * MI/MO: Inward/outward names into mbus
 *
 *   |(12)__________|(13)__________|(14)__________|(15)__________|
 *   |              | Core 6       | Core 7       |              |
 *   |              | SI:Core 6    | SI:Core 7    |              |
 *   |(8)___________|(9)___________|(10)__________|(11)__________|
 *   | Core 4       | L2 2         | L2 3         | Core 5       |
 *   | SI:Core 4    | S0:system[2] | SO:system[3] | SI:Core 5    |
 *   |(4)___________|(5)___________|(6)___________|(7)___________|
 *   | Core 2       | L2 0         | L2 1         | Core 3       |
 *   | SI:Core 2    | SO:system[0] | SO:system[1] | SI:Core 3    |
 *   |(0)___________|(1)___________|(2)___________|(3)___________|
 *   | FBus         | Core 0       | Core 1       | Pbus         |
 *   | SI:serial_tl | SI:Core 0    | SI:Core 1    | SO:pbus      |
 *   |______________|______________|______________|______________|
 *
 *   |(0)___________|(1)___________|(2)___________|(3)___________|
 *   | DRAM 0       | L2 0         | L2 1         | DRAM 1       |
 *   | M0:system[0] | MI:L2[0]     | MI:L2[1]     | M0:system[1] |
 *   | M0:serdesser |              |              |              |
 *   |______________|______________|______________|______________|
 *    ||||||||||||||                               ||||||||||||||
 *   |(7)___________|(6)___________|(5)___________|(4)___________|
 *   | DRAM 2       | L2 2         | L2 3         | DRAM 3       |
 *   | M0:system[2] | MI:L2[2]     | MI:L2[3]     | M0:system[3] |
 *   |              |              |              |              |
 *   |______________|______________|______________|______________|
 */
// DOC include start: MultiNoCConfig
class MultiNoCConfig extends Config(new SingleRocketSbusRingNoCConfig)
// DOC include end: MultiNoCConfig

/*
 * 10 - 11 - 12 - 13 - 14
 *            |
 *      0 --- 1 --- 2 --- 3
 *      |                 |
 *      9                 4
 *      |                 |
 *      8 --- 7 --- 6 --- 5
 *            |
 * 15 - 16 - 17 - 18 - 19
 *
 * SI/SO: Inward/outward names into sbus
 * MI/MO: Inward/outward names into mbus
 *
 * Agent  | Bus | String     | node
 * ================================
 * Core 0 | SI  | Core 0     |    2
 * Core 1 | SI  | Core 1     |   10
 * Core 2 | SI  | Core 2     |   11
 * Core 3 | SI  | Core 3     |   13
 * Core 4 | SI  | Core 4     |   14
 * Core 5 | SI  | Core 5     |   15
 * Core 6 | SI  | Core 6     |   16
 * Core 7 | SI  | Core 7     |   18
 * Core 8 | SI  | Core 8     |   19
 * fbus   | SI  | serial_tl  |    9
 * pbus   | SO  | pbus       |    4
 * L2 0   | SO  | system[0]  |    0
 * L2 1   | SO  | system[1]  |    2
 * L2 2   | SO  | system[2]  |    8
 * L2 3   | SO  | system[3]  |    6
 * L2 0   | MI  | Cache[0]   |    0
 * L2 1   | MI  | Cache[1]   |    2
 * L2 2   | MI  | Cache[2]   |    8
 * L2 3   | MI  | Cache[3]   |    6
 * DRAM 0 | MO  | system[0]  |    3
 * DRAM 1 | MO  | system[1]  |    5
 * spad   | MO  | ram[0]     |    9
 */
// DOC include start: SharedNoCConfig
class SharedNoCConfig extends Config(new SingleRocketSbusRingNoCConfig)
// DOC include end: SharedNoCConfig

// This Config implements a simple ring interconnect for the system bus
class SbusRingNoCConfig extends Config(
  new WithSbusNoC(SplitACDxBETLNoCParams(
    DiplomaticNetworkNodeMapping(
      inNodeMapping = ListMap(
        "Core 0 DCache[0]" -> 0,
        "Core 1 DCache[0]" -> 1,
        "Core 2 DCache[0]" -> 2,
        "Core 3 DCache[0]" -> 3,
        "Core 4 DCache[0]" -> 4,
        "Core 5 DCache[0]" -> 5,
        "Core 6 DCache[0]" -> 6,
        "Core 7 DCache[0]" -> 7,
        "slave-port-axi4" -> 8,
        "serial_tl" -> 8),
      outNodeMapping = ListMap(
        "system[0]" -> 9,
        "system[1]" -> 10,
        "system[2]" -> 11,
        "system[3]" -> 12,
        "ldut[0]" -> 9,
        "ldut[1]" -> 10,
        "ldut[2]" -> 11,
        "ldut[3]" -> 12,
        "ldut[4]" -> 8,
        "error" -> 8,
        "pbus" -> 8)), // TSI is on the pbus, so serial-tl and pbus should be on the same node
    acdNoCParams = NoCParams(
      topology        = UnidirectionalTorus1D(13),
      channelParamGen = (a, b) => UserChannelParams(Seq.fill(6) { UserVirtualChannelParams(4) }),
      routingRelation = NonblockingVirtualSubnetworksRouting(UnidirectionalTorus1DDatelineRouting(), 3, 2)),
    beNoCParams = NoCParams(
      topology        = UnidirectionalTorus1D(13),
      channelParamGen = (a, b) => UserChannelParams(Seq.fill(4) { UserVirtualChannelParams(1) }),
      routingRelation = NonblockingVirtualSubnetworksRouting(UnidirectionalTorus1DDatelineRouting(), 2, 2))
  )) ++
  new WithNHugeCores(8) ++
  new WithNBanks(4) ++
  new WithCoherentBusTopology ++
  new BaseConfig
)

// This config integrates a mesh interconnect for the system bus, and divides the system bus
// tilelink messages across two isolated interconnects
class SbusMeshNoCConfig extends Config(
  new WithSbusNoC(SplitACDxBETLNoCParams(
    DiplomaticNetworkNodeMapping(
      inNodeMapping = ListMap(
        "Core 0 DCache[0]" -> 0,
        "Core 1 DCache[0]" -> 1,
        "Core 2 DCache[0]" -> 2,
        "Core 3 DCache[0]" -> 3,
        "Core 4 DCache[0]" -> 4,
        "Core 5 DCache[0]" -> 7,
        "Core 6 DCache[0]" -> 8,
        "Core 7 DCache[0]" -> 11,
        "Core 8 DCache[0]" -> 12,
        "Core 9 DCache[0]" -> 13,
        "Core 10 DCache[0]" -> 14,
        "Core 11 DCache[0]" -> 15,
        "slave-port-axi4" -> 0,
        "serial_tl" -> 0),
      outNodeMapping = ListMap(
        "system[0]" -> 5,
        "system[1]" -> 6,
        "system[2]" -> 9,
        "system[3]" -> 10,
        "ldut[0]" -> 5,
        "ldut[1]" -> 6,
        "ldut[2]" -> 9,
        "ldut[3]" -> 10,
        "ldut[4]" -> 0,
        "error" -> 0,
        "pbus" -> 0)), // TSI is on the pbus, so serial-tl and pbus should be on the same node
    acdNoCParams = NoCParams(
      topology        = Mesh2D(4, 4),
      channelParamGen = (a, b) => UserChannelParams(Seq.fill(3) { UserVirtualChannelParams(3) }, unifiedBuffer = false),
      routerParams    = (i) => UserRouterParams(combineRCVA=true, combineSAST=true),
      routingRelation = NonblockingVirtualSubnetworksRouting(Mesh2DDimensionOrderedRouting(), 3, 1),
    ),
    beNoCParams = NoCParams(
      topology        = Mesh2D(4, 4),
      channelParamGen = (a, b) => UserChannelParams(Seq.fill(2) { UserVirtualChannelParams(3) }, unifiedBuffer = false),
      routerParams    = (i) => UserRouterParams(combineRCVA=true, combineSAST=true),
      routingRelation = NonblockingVirtualSubnetworksRouting(Mesh2DDimensionOrderedRouting(), 2, 1),
    ),
    beDivision = 4
  ), inlineNoC = true) ++
  new WithNHugeCores(12) ++
  new WithNBanks(4) ++
  new WithSystemBusWidth(128) ++
  new WithCoherentBusTopology ++
  new BaseConfig
)


class SingleRocketSbusRingNoCConfig extends Config(
  new WithSbusNoC(SimpleTLNoCParams(
    DiplomaticNetworkNodeMapping(
      inNodeMapping = ListMap(
        "Core 0" -> 0,
        "slave-port-axi4" -> 1),
      outNodeMapping = ListMap(
        "error" -> 1,
        "ldut" -> 2)),
    nocParams = NoCParams(
      topology = TerminalRouter(BidirectionalLine(3)),
      channelParamGen = { (_, _) =>
        UserChannelParams(Seq.fill(5) { UserVirtualChannelParams(4) })
      },
      routingRelation =
        NonblockingVirtualSubnetworksRouting(TerminalRouterRouting(BidirectionalLineRouting()), 5, 1))
  )) ++
  new WithNBigCores(1) ++
  new WithNBanks(1) ++
  new WithCoherentBusTopology ++
  new BaseConfig
)

class QuadRocketSbusRingNoCConfig extends Config(
  new WithSbusNoC(SimpleTLNoCParams(
    DiplomaticNetworkNodeMapping(
      inNodeMapping = ListMap(
        "Core 0" -> 0,
        "Core 1" -> 1,
        "Core 2" -> 2,
        "Core 3" -> 3,
        "slave-port-axi4" -> 4),
      outNodeMapping = ListMap(
        "error" -> 4,
        "ldut" -> 5)),
    nocParams = NoCParams(
      topology        = TerminalRouter(BidirectionalLine(9)),
      channelParamGen = (a, b) => UserChannelParams(Seq.fill(10) { UserVirtualChannelParams(4) }),
      routingRelation = NonblockingVirtualSubnetworksRouting(TerminalRouterRouting(BidirectionalLineRouting()), 5, 2))
  )) ++
  new WithNHugeCores(4) ++
  new WithNBanks(4) ++
  new WithCoherentBusTopology ++
  new BaseConfig)
