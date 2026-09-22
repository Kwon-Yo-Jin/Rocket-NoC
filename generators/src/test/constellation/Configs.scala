package rocketnoc.test

import constellation.channel.{FlowParams, UserChannelParams, UserEgressParams, UserIngressParams, UserVirtualChannelParams}
import constellation.noc.NoCParams
import constellation.router.{PrioritizingSingleVCAllocator, UserRouterParams}
import constellation.routing._
import constellation.test._
import constellation.topology._

/** Parameters shared by the raw, TileLink, AXI4, and evaluation regressions. */
object CustomDUTNoC {
  val nX = 6
  val nY = 6
  val nNodes = nX * nY

  val ingressNodes = 0 until 32
  val egressNodes = 33 until 34
  val flows = Seq.tabulate(ingressNodes.size, egressNodes.size) { (source, destination) =>
    FlowParams(source, destination, vNetId = 0)
  }.flatten
  val protocolIngressNodes = 0 until nX
  val protocolEgressNodes = (nNodes - nX) until nNodes

  //val offeredLoad = 0.015
  val offeredLoad = 0.4

  val params = NoCParams(
    topology = constellation.topology.RucheMesh2D(
      nX = nX,
      nY = nY,
      xRucheFactor = 2,
      yRucheFactor = 2),
    routerParams = _ => UserRouterParams(vcAllocator = (vP) => (p) => new PrioritizingSingleVCAllocator(vP)(p)),
    channelParamGen = (_, _) =>
      UserChannelParams(Seq.fill(9)(UserVirtualChannelParams(bufferSize = 4))),
    ingresses = ingressNodes.map(UserIngressParams(_)),
    egresses = egressNodes.map(UserEgressParams(_)),
    flows = flows,
    vNetBlocking = (_, _) => true,
    routingRelation = NonblockingVirtualSubnetworksRouting(
      RucheMesh2DEscapeRouting(),
      n = 1,
      nDedicatedChannels = 9)
  )
}

object BaselineMeshNoC {
  val nX = 6
  val nY = 6
  val nNodes = nX * nY

  val ingressNodes = 0 until 32
  val egressNodes = 33 until 34
  val flows = Seq.tabulate(ingressNodes.size, egressNodes.size) { (source, destination) =>
    FlowParams(source, destination, vNetId = 0)
  }.flatten
  val protocolIngressNodes = 0 until nX
  val protocolEgressNodes = (nNodes - nX) until nNodes

  //val offeredLoad = 0.015
  val offeredLoad = 0.4

  val params = NoCParams(
    topology = constellation.topology.Mesh2D(nX = nX, nY = nY),
    channelParamGen = (_, _) =>
      UserChannelParams(Seq.fill(5)(UserVirtualChannelParams(bufferSize = 4))),
    ingresses = ingressNodes.map(UserIngressParams(_)),
    egresses = egressNodes.map(UserEgressParams(_)),
    flows = flows,
    vNetBlocking = (_, _) => true,
    routingRelation = NonblockingVirtualSubnetworksRouting(
      Mesh2DEscapeRouting(),
      n = 1,
      nDedicatedChannels = 5)
  )
}

/** Random-flit functional test using the shared NoC parameters directly. */
class CustomDUTNoCConfig extends NoCTesterConfig(NoCTesterParams(
  nocParams = CustomDUTNoC.params,
  totalTxs = 20000,
  inputFlitStallProbability = 0.1,
  inputPacketStallProbability = 0.1,
  outputStallProbability = 0.1
))

/** TileLink transport test using the shared physical NoC parameters. */
class CustomDUTTLNoCConfig extends TLNoCTesterConfig(TLNoCTesterParams(
  inNodeMapping = CustomDUTNoC.protocolIngressNodes,
  outNodeMapping = CustomDUTNoC.protocolEgressNodes,
  txns = 200,
  nocParams = CustomDUTNoC.params
))

/** AXI4 transport test using the shared physical NoC parameters. */
class CustomDUTAXI4NoCConfig extends AXI4NoCTesterConfig(AXI4NoCTesterParams(
  inNodeMapping = CustomDUTNoC.protocolIngressNodes,
  outNodeMapping = CustomDUTNoC.protocolEgressNodes,
  txns = 200,
  nocParams = CustomDUTNoC.params
))

/** Throughput evaluation using the shared NoC parameters and all-to-all flows. */
class CustomDUTEvalNoCConfig extends NoCEvalConfig(NoCEvalParams(
  nocParams = CustomDUTNoC.params,
  warmupCycles = 200,
  measurementCycles = 200000,
  drainTimeoutCycles = 100000,
  flitsPerPacket = 4,
  flows = (_, _) => CustomDUTNoC.offeredLoad / CustomDUTNoC.ingressNodes.size, // * CustomDUTNoC.egressNodes.size,
  requiredThroughput = 0.9
))

/** Random-flit functional test using the shared NoC parameters directly. */
class BaselineMeshNoCConfig extends NoCTesterConfig(NoCTesterParams(
  nocParams = BaselineMeshNoC.params,
  totalTxs = 20000,
  inputFlitStallProbability = 0.1,
  inputPacketStallProbability = 0.1,
  outputStallProbability = 0.1
))

/** TileLink transport test using the shared physical NoC parameters. */
class BaselineMeshTLNoCConfig extends TLNoCTesterConfig(TLNoCTesterParams(
  inNodeMapping = BaselineMeshNoC.protocolIngressNodes,
  outNodeMapping = BaselineMeshNoC.protocolEgressNodes,
  txns = 200,
  nocParams = BaselineMeshNoC.params
))

/** AXI4 transport test using the shared physical NoC parameters. */
class BaselineMeshAXI4NoCConfig extends AXI4NoCTesterConfig(AXI4NoCTesterParams(
  inNodeMapping = BaselineMeshNoC.protocolIngressNodes,
  outNodeMapping = BaselineMeshNoC.protocolEgressNodes,
  txns = 200,
  nocParams = BaselineMeshNoC.params
))

/** Throughput evaluation using the shared NoC parameters and all-to-all flows. */
class BaselineMeshEvalNoCConfig extends NoCEvalConfig(NoCEvalParams(
  nocParams = BaselineMeshNoC.params,
  warmupCycles = 200,
  measurementCycles = 200000,
  drainTimeoutCycles = 100000,
  flitsPerPacket = 4,
  latencyHistogramStep = 5,
//  flows = (_, _) => 0.05 / BaselineMeshNoC.egressNodes.size,
  flows = (_, _) => BaselineMeshNoC.offeredLoad / BaselineMeshNoC.ingressNodes.size, // * BaselineMeshNoC.egressNodes.size,
  requiredThroughput = 0.9
))
