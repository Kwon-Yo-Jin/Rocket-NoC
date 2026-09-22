package rocketnoc.config

import org.chipsalliance.cde.config.{Config}
import freechips.rocketchip.subsystem._
import freechips.rocketchip.rocket._

import constellation.channel._
import constellation.routing._
import constellation.router._
import constellation.topology._
import constellation.noc._
import constellation.soc.{GlobalNoCParams}

import scala.collection.immutable.ListMap

class FPGADefaultNCoreConfig extends Config({
  val core_num = 16
  new freechips.rocketchip.rocket.WithRV32 ++
  new freechips.rocketchip.subsystem.WithEdgeDataBits(32) ++
  new freechips.rocketchip.rocket.WithNBigCores(core_num) ++
  new NoCBaseConfig
})

class FPGASbusSplitRingNCoreConfig extends Config({
  val core_num = 32
  new constellation.soc.WithSbusNoC(constellation.protocol.SplitACDxBETLNoCParams(
    constellation.protocol.DiplomaticNetworkNodeMapping(
      inNodeMapping = ListMap(
        ((0 until core_num).map(i => s"Core $i " -> (i)) :+
          ("debug" -> (core_num))): _*),
      outNodeMapping = ListMap(
        "L2 InclusiveCache[0]" -> (core_num + 1),
        "gen[0]" -> (core_num + 2),
        "bootrom" -> (core_num + 3))), // TSI is on the pbus, so serial-tl and pbus should be on the same node
    acdNoCParams = NoCParams(
      topology        = UnidirectionalTorus1D(core_num + 4),
      channelParamGen = (a, b) => UserChannelParams(Seq.fill(6) { UserVirtualChannelParams(4) }),
      routingRelation = NonblockingVirtualSubnetworksRouting(UnidirectionalTorus1DDatelineRouting(), 3, 2)),
    beNoCParams = NoCParams(
      topology        = UnidirectionalTorus1D(core_num + 4),
      channelParamGen = (a, b) => UserChannelParams(Seq.fill(4) { UserVirtualChannelParams(1) }),
      routingRelation = NonblockingVirtualSubnetworksRouting(UnidirectionalTorus1DDatelineRouting(), 2, 2))
  )) ++
  new freechips.rocketchip.rocket.WithRV32 ++
  new freechips.rocketchip.subsystem.WithEdgeDataBits(32) ++
  new freechips.rocketchip.rocket.WithNBigCores(core_num) ++
  new rocketnoc.config.NoCBaseConfig
})

class FPGASbusRingNCoreConfig extends Config({
  val core_num = 4
  new constellation.soc.WithSbusNoC(constellation.protocol.SimpleTLNoCParams(
    constellation.protocol.DiplomaticNetworkNodeMapping(
      inNodeMapping = ListMap(
        ((0 until core_num).map(i => s"Core $i " -> (i)) :+
          ("debug" -> (core_num))): _*),
      outNodeMapping = ListMap(
        "L2 InclusiveCache[0]" -> (core_num + 1),
        "gen[0]" -> (core_num + 2),
        "bootrom" -> (core_num + 3))), // TSI is on the pbus, so serial-tl and pbus should be on the same node
    NoCParams(
      // topology        = UnidirectionalTorus1D(core_num + 4),
      topology        = BidirectionalTorus1D(core_num + 4),
      channelParamGen = (a, b) => UserChannelParams(Seq.fill(10) { UserVirtualChannelParams(4) }),
      //routingRelation = NonblockingVirtualSubnetworksRouting(UnidirectionalTorus1DDatelineRouting(), 5, 2))
      routingRelation = NonblockingVirtualSubnetworksRouting(BidirectionalTorus1DShortestRouting(), 5, 2))
  )) ++
  new freechips.rocketchip.rocket.WithRV32 ++
  new freechips.rocketchip.subsystem.WithEdgeDataBits(32) ++
  new freechips.rocketchip.rocket.WithNBigCores(core_num) ++
  new rocketnoc.config.NoCBaseConfig
})

class FPGASbusMeshNCoreConfig extends Config({
  val core_num = 32
  val mesh_x_len = math.ceil(math.sqrt(core_num.toDouble)).toInt
  val mesh_y_len = math.ceil((core_num + 4).toDouble / mesh_x_len).toInt
  new constellation.soc.WithSbusNoC(constellation.protocol.SimpleTLNoCParams(
    constellation.protocol.DiplomaticNetworkNodeMapping(
      inNodeMapping = ListMap(
        ((0 until core_num).map(i => s"Core $i " -> (i)) :+
          ("debug" -> (core_num))): _*),
      outNodeMapping = ListMap(
        "L2 InclusiveCache[0]" -> (core_num + 1),
        "bootrom" -> (core_num + 2),
        "gen[0]" -> (core_num + 3))), // TSI is on the pbus, so serial-tl and pbus should be on the same node
    NoCParams(
      topology        = Mesh2D(mesh_x_len, mesh_y_len),
      channelParamGen = (a, b) => UserChannelParams(Seq.fill(5) { UserVirtualChannelParams(4) }),//, unifiedBuffer = false),
      routerParams    = (i) => UserRouterParams(combineRCVA=true, combineSAST=true),
      routingRelation = NonblockingVirtualSubnetworksRouting(Mesh2DEscapeRouting(), 5, 1))
      //routingRelation = NonblockingVirtualSubnetworksRouting(Mesh2DDimensionOrderedRouting(), 5, 1))
  )) ++
  new freechips.rocketchip.rocket.WithRV32 ++
  new freechips.rocketchip.subsystem.WithEdgeDataBits(32) ++
  new freechips.rocketchip.rocket.WithNBigCores(core_num) ++
  new rocketnoc.config.NoCBaseConfig
})

class FPGASbus2DTorusNCoreConfig extends Config({
  val core_num = 32
  val mesh_x_len = math.ceil(math.sqrt(core_num.toDouble)).toInt
  val mesh_y_len = math.ceil((core_num + 4).toDouble / mesh_x_len).toInt
  new constellation.soc.WithSbusNoC(constellation.protocol.SimpleTLNoCParams(
    constellation.protocol.DiplomaticNetworkNodeMapping(
      inNodeMapping = ListMap(
        ((0 until core_num).map(i => s"Core $i " -> (i)) :+
          ("debug" -> (core_num))): _*),
      outNodeMapping = ListMap(
        "L2 InclusiveCache[0]" -> (core_num + 1),
        "bootrom" -> (core_num + 2),
        "gen[0]" -> (core_num + 3))), // TSI is on the pbus, so serial-tl and pbus should be on the same node
    NoCParams(
      topology        = BidirectionalTorus2D(mesh_x_len, mesh_y_len),
      channelParamGen = (a, b) => UserChannelParams(Seq.fill(10) { UserVirtualChannelParams(4) }),//, unifiedBuffer = false),
      routerParams    = (i) => UserRouterParams(combineRCVA=true, combineSAST=true),
      routingRelation = NonblockingVirtualSubnetworksRouting(DimensionOrderedBidirectionalTorus2DDatelineRouting(), 5, 2))
      //routingRelation = NonblockingVirtualSubnetworksRouting(Mesh2DDimensionOrderedRouting(), 5, 1))
  )) ++
  new freechips.rocketchip.rocket.WithRV32 ++
  new freechips.rocketchip.subsystem.WithEdgeDataBits(32) ++
  new freechips.rocketchip.rocket.WithNBigCores(core_num) ++
  new rocketnoc.config.NoCBaseConfig
})

class FPGASbusRucheNCoreConfig extends Config({
  val core_num = 32
  val mesh_x_len = math.ceil(math.sqrt(core_num.toDouble)).toInt
  val mesh_y_len = math.ceil((core_num + 4).toDouble / mesh_x_len).toInt
  val ruche_x_factor = 3
  val ruche_y_factor = 4
  new constellation.soc.WithSbusNoC(constellation.protocol.SimpleTLNoCParams(
    constellation.protocol.DiplomaticNetworkNodeMapping(
      inNodeMapping = ListMap(
        ((0 until core_num).map(i => s"Core $i " -> (i)) :+
          ("debug" -> (core_num))): _*),
      outNodeMapping = ListMap(
        "L2 InclusiveCache[0]" -> (core_num + 1),
        "bootrom" -> (core_num + 2),
        "gen[0]" -> (core_num + 3))), // TSI is on the pbus, so serial-tl and pbus should be on the same node
    NoCParams(
      topology        = RucheMesh2D(mesh_x_len, mesh_y_len, ruche_x_factor, ruche_y_factor),
      channelParamGen = (a, b) => UserChannelParams(Seq.fill(9) { UserVirtualChannelParams(4) }),//, unifiedBuffer = false),
      routerParams    = (i) => UserRouterParams(
        combineRCVA=true,
        combineSAST=true,
        vcAllocator=(vP) => (p) => new PrioritizingSingleVCAllocator(vP)(p)),
      //routingRelation = NonblockingVirtualSubnetworksRouting(ShortestPathRouting(), 5, 1))
      //routingRelation = NonblockingVirtualSubnetworksRouting(RucheMesh2DDimensionOrderedRouting(), 5, 1))
      routingRelation = NonblockingVirtualSubnetworksRouting(RucheMesh2DEscapeRouting(), 5, 1))
  )) ++
  new freechips.rocketchip.rocket.WithRV32 ++
  new freechips.rocketchip.subsystem.WithEdgeDataBits(32) ++
  new freechips.rocketchip.rocket.WithNBigCores(core_num) ++
  new rocketnoc.config.NoCBaseConfig
})