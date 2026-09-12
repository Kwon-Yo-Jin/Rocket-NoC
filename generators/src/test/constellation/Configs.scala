package rocketnoc.test

import constellation.channel.{FlowParams, UserChannelParams, UserEgressParams, UserIngressParams, UserVirtualChannelParams}
import constellation.noc.NoCParams
import constellation.routing.{NonblockingVirtualSubnetworksRouting, RucheMesh2DDimensionOrderedRouting}
import constellation.test._
import constellation.topology.RucheMesh2D

/** Parameters shared by the raw, TileLink, AXI4, and evaluation regressions. */
object CustomRucheNoC {
  val nX = 4
  val nY = 4
  val nNodes = nX * nY

  val ingressNodes = 0 until nNodes
  val egressNodes = 0 until nNodes
  val flows = Seq.tabulate(nNodes, nNodes) { (source, destination) =>
    FlowParams(source, destination, vNetId = 0)
  }.flatten

  val params = NoCParams(
    topology = RucheMesh2D(
      nX = nX,
      nY = nY,
      xRucheFactor = 2,
      yRucheFactor = 2),
    channelParamGen = (_, _) =>
      UserChannelParams(Seq.fill(5)(UserVirtualChannelParams(bufferSize = 4))),
    ingresses = ingressNodes.map(UserIngressParams(_)),
    egresses = egressNodes.map(UserEgressParams(_)),
    flows = flows,
    vNetBlocking = (_, _) => true,
    routingRelation = NonblockingVirtualSubnetworksRouting(
      RucheMesh2DDimensionOrderedRouting(firstDim = 0),
      n = 5,
      nDedicatedChannels = 1)
  )

  val protocolIngressNodes = Seq(0, 3, 12, 15)
  val protocolEgressNodes = Seq(5, 6, 9, 10)
}

/** Random-flit functional test using the shared NoC parameters directly. */
class CustomRucheNoCConfig extends NoCTesterConfig(NoCTesterParams(
  nocParams = CustomRucheNoC.params,
  totalTxs = 2000,
  inputFlitStallProbability = 0.1,
  inputPacketStallProbability = 0.1,
  outputStallProbability = 0.1
))

/** TileLink transport test using the shared physical NoC parameters. */
class CustomRucheTLNoCConfig extends TLNoCTesterConfig(TLNoCTesterParams(
  inNodeMapping = CustomRucheNoC.protocolIngressNodes,
  outNodeMapping = CustomRucheNoC.protocolEgressNodes,
  txns = 200,
  nocParams = CustomRucheNoC.params
))

/** AXI4 transport test using the shared physical NoC parameters. */
class CustomRucheAXI4NoCConfig extends AXI4NoCTesterConfig(AXI4NoCTesterParams(
  inNodeMapping = CustomRucheNoC.protocolIngressNodes,
  outNodeMapping = CustomRucheNoC.protocolEgressNodes,
  txns = 200,
  nocParams = CustomRucheNoC.params
))

/** Throughput evaluation using the shared NoC parameters and all-to-all flows. */
class CustomRucheEvalNoCConfig extends NoCEvalConfig(NoCEvalParams(
  nocParams = CustomRucheNoC.params,
  warmupCycles = 200,
  measurementCycles = 20000,
  drainTimeoutCycles = 10000,
  flitsPerPacket = 4,
  flows = (_, _) => 0.05 / CustomRucheNoC.nNodes,
  requiredThroughput = 0.9
))
