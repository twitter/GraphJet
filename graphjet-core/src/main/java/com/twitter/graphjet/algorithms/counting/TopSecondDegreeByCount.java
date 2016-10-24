package com.twitter.graphjet.algorithms.counting;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.RecommendationAlgorithm;
import com.twitter.graphjet.algorithms.RecommendationStats;
import com.twitter.graphjet.algorithms.counting.request.TopSecondDegreeByCountRequest;
import com.twitter.graphjet.algorithms.counting.response.TopSecondDegreeByCountResponse;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.NodeMetadataMultiSegmentIterator;
import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;
import it.unimi.dsi.fastutil.longs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates recommended RHS nodes by calculating aggregated weights.
 * Weights are accumulated by weights of LHS nodes whose edges point to RHS nodes
 */
public abstract class TopSecondDegreeByCount<Request extends TopSecondDegreeByCountRequest,
                                        Response extends TopSecondDegreeByCountResponse>
    implements RecommendationAlgorithm<Request, Response> {

  protected static final Logger LOG = LoggerFactory.getLogger("graph");
  protected static final int MAX_EDGES_PER_NODE = 500;

  // Static variables for better memory reuse. Avoids re-allocation on every request
  private final NodeMetadataLeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph;
  private final Long2ObjectMap<NodeInfo> visitedRightNodes;
  private final List<NodeInfo> nodeInfosAfterFiltering;
  private final Long2ByteMap seenEdgesPerNode;
  protected final RecommendationStats topSecondDegreeByCountStats;
  protected final StatsReceiver statsReceiver;
  protected final Counter numRequestsCounter;

  /**
   * This initializes all the state needed to run TopSecondDegreeByCount. Note that the object can
   * be reused for answering many different queries on the same graph, which allows for
   * optimizations such as reusing internally allocated maps etc.
   *
   * @param leftIndexedBipartiteGraph is the
   *                                  {@link NodeMetadataLeftIndexedMultiSegmentBipartiteGraph}
   *                                  to run TopSecondDegreeByCountForTweet on
   * @param expectedNodesToHit        is an estimate of how many nodes can be hit in
   *                                  TopSecondDegreeByCountForTweet. This is purely for allocating needed
   *                                  memory right up front to make requests fast.
   * @param statsReceiver             tracks the internal stats
   */
  public TopSecondDegreeByCount(
      NodeMetadataLeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
      int expectedNodesToHit,
      StatsReceiver statsReceiver) {
    this.leftIndexedBipartiteGraph = leftIndexedBipartiteGraph;
    this.visitedRightNodes = new Long2ObjectOpenHashMap<>(expectedNodesToHit);
    this.nodeInfosAfterFiltering = new ArrayList<>();
    this.seenEdgesPerNode = new Long2ByteArrayMap();
    this.topSecondDegreeByCountStats = new RecommendationStats();
    this.statsReceiver = statsReceiver.scope("TopSecondDegreeByCount");
    this.numRequestsCounter = this.statsReceiver.counter("numRequests");
  }

  /**
   * Interface method for updating information gathered about each RHS node.
   * @param leftNode                is the LHS node from which traversal initialized
   * @param rightNode               is the RHS node at which traversal arrived
   * @param edgeType                is the edge from which LHS and RHS nodes are connected
   * @param weight                  is the weight contributed to a RHS node in this traversal
   * @param edgeIterator            is the iterator for traversing edges from LHS node
   * @param maxSocialProofTypeSize  is the maximum social proof types to keep
   * @param collectedRightNodeInfo  is a map of visited nodes, containing their info such as weights
   */
  protected abstract void updateNodeInfo(
      long leftNode, long rightNode, byte edgeType, double weight,
      NodeMetadataMultiSegmentIterator edgeIterator, int maxSocialProofTypeSize,
      Long2ObjectMap<NodeInfo> collectedRightNodeInfo);

  protected abstract Response generateRecommendationFromNodeInfo(Request request, List<NodeInfo> filteredNodeInfos);

  /**
   * Computes recommendations using TopSecondDegreeByCount algorithm.
   * @param request  is the request for the algorithm
   * @param random   is used for all random choices within the algorithm
   * @return         Right hand side nodes with largest weights
   */
  @Override
  public Response computeRecommendations(Request request, Random random) {
    numRequestsCounter.incr();
    reset(request);

    collectRightNodeInfo(request, visitedRightNodes);
    recordIntermediateStats(request.getQueryNode(), visitedRightNodes);
    filterNodeInfo(request, visitedRightNodes, nodeInfosAfterFiltering);
    return generateRecommendationFromNodeInfo(request, nodeInfosAfterFiltering);
  }

  private void reset(Request request) {
    request.resetFilters();
    visitedRightNodes.clear();
    nodeInfosAfterFiltering.clear();
    seenEdgesPerNode.clear();
    topSecondDegreeByCountStats.reset();
  }

  private void collectRightNodeInfo(Request request, Long2ObjectMap<NodeInfo> collectedNodeInfo) {
    for (Long2DoubleMap.Entry entry: request.getLeftSeedNodesWithWeight().long2DoubleEntrySet()) {
      long leftNode = entry.getLongKey();
      double weight = entry.getDoubleValue();
      int numEdgesPerNode = 0;
      NodeMetadataMultiSegmentIterator edgeIterator =
          (NodeMetadataMultiSegmentIterator) leftIndexedBipartiteGraph.getLeftNodeEdges(leftNode);
      seenEdgesPerNode.clear();

      if (edgeIterator == null) {
        continue;
      }
      // Sequentially iterating through the latest MAX_EDGES_PER_NODE edges per node
      while (edgeIterator.hasNext() && numEdgesPerNode++ < MAX_EDGES_PER_NODE) {
        long rightNode = edgeIterator.nextLong();
        byte edgeType = edgeIterator.currentEdgeType();

        boolean hasSeenRightNodeFromEdge =
            seenEdgesPerNode.containsKey(rightNode) && seenEdgesPerNode.get(rightNode) == edgeType;

        if (!hasSeenRightNodeFromEdge) {
          seenEdgesPerNode.put(rightNode, edgeType);
          int maxSocialProofTypeSize = request.getMaxSocialProofTypeSize();
          updateNodeInfo(
              leftNode, rightNode,
              edgeType, weight,
              edgeIterator, maxSocialProofTypeSize, collectedNodeInfo);
        }
      }
    }
  }

  private void recordIntermediateStats(long queryNode, Long2ObjectMap<NodeInfo> collectedNodeInfo) {
    topSecondDegreeByCountStats.setNumDirectNeighbors(
        leftIndexedBipartiteGraph.getLeftNodeDegree(queryNode)
    );

    int minVisitsPerRightNode = Integer.MAX_VALUE;
    int maxVisitsPerRightNode = 0;
    int numRHSVisits = 0;

    for (Long2ObjectMap.Entry<NodeInfo> entry: collectedNodeInfo.long2ObjectEntrySet()) {
      NodeInfo nodeInfo = entry.getValue();
      int numVisits = nodeInfo.getNumVisits();

      minVisitsPerRightNode = Math.min(minVisitsPerRightNode, numVisits);
      maxVisitsPerRightNode = Math.max(maxVisitsPerRightNode, numVisits);
      numRHSVisits += numVisits;
    }

    topSecondDegreeByCountStats.setMinVisitsPerRightNode(minVisitsPerRightNode);
    topSecondDegreeByCountStats.setMaxVisitsPerRightNode(maxVisitsPerRightNode);
    topSecondDegreeByCountStats.setNumRHSVisits(numRHSVisits);
    topSecondDegreeByCountStats.setNumRightNodesReached(collectedNodeInfo.size());
  }

  private void filterNodeInfo(Request request, Long2ObjectMap<NodeInfo> collectedNodeInfo, List<NodeInfo> resultNodes) {
    int numFilteredNodes = 0;
    for (NodeInfo nodeInfo : collectedNodeInfo.values()) {
      if (request.filterResult(nodeInfo.getValue(), nodeInfo.getSocialProofs())) {
        numFilteredNodes++;
        continue;
      }
      resultNodes.add(nodeInfo);
    }
    topSecondDegreeByCountStats.setNumRightNodesFiltered(numFilteredNodes);
  }

  protected String getResultLogMessage(Request request) {
    return "TopSecondDegreeByCount: after running algorithm for request_id = "
        + request.getQueryNode()
        + ", we get numDirectNeighbors = " + topSecondDegreeByCountStats.getNumDirectNeighbors()
        + ", numRHSVisits = " + topSecondDegreeByCountStats.getNumRHSVisits()
        + ", numRightNodesReached = " + topSecondDegreeByCountStats.getNumRightNodesReached()
        + ", numRightNodesFiltered = " + topSecondDegreeByCountStats.getNumRightNodesFiltered()
        + ", minVisitsPerRightNode = " + topSecondDegreeByCountStats.getMinVisitsPerRightNode()
        + ", maxVisitsPerRightNode = " + topSecondDegreeByCountStats.getMaxVisitsPerRightNode();
  }
}
