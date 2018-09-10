/**
 * Copyright 2016 Twitter. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.graphjet.algorithms.counting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.RecommendationAlgorithm;
import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.RecommendationStats;
import com.twitter.graphjet.bipartite.LeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.longs.Long2ByteArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * Generate recommended RHS nodes by calculating aggregated weights.
 * Weights are accumulated by weights of LHS nodes whose edges are incident to RHS nodes.
 */
public abstract class TopSecondDegreeByCount<Request extends TopSecondDegreeByCountRequest,
  Response extends TopSecondDegreeByCountResponse>
  implements RecommendationAlgorithm<Request, Response> {

  protected static final Logger LOG = LoggerFactory.getLogger("graph");

  // Static variables for better memory reuse. Avoids re-allocation on every request
  private final LeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph;
  private final Long2ByteMap seenEdgesPerNode;
  protected final Long2ObjectMap<NodeInfo> visitedRightNodes;
  protected final List<NodeInfo> nodeInfosAfterFiltering;
  protected final RecommendationStats topSecondDegreeByCountStats;
  protected final StatsReceiver statsReceiver;
  protected final Counter numRequestsCounter;

  /**
   * @param leftIndexedBipartiteGraph is the
   *                                  {@link LeftIndexedMultiSegmentBipartiteGraph}
   *                                  to run TopSecondDegreeByCountForTweet on
   * @param expectedNodesToHit        is an estimate of how many nodes can be hit in
   *                                  TopSecondDegreeByCountForTweet. This is purely for allocating needed
   *                                  memory right up front to make requests fast.
   * @param statsReceiver             tracks the internal stats
   */
  public TopSecondDegreeByCount(
    LeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
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
   * Return whether the edge is within the age limit which is specified in the request.
   * It is used to filter information of unwanted edges from being aggregated.
   * @param edgeEngagementTime is the timestamp of the engagement edge
   * @param edgeAgeLimit is the time to live for the edge
   * @return true if this edge is within the max age limit, false otherwise.
   */
  protected boolean isEdgeEngagementWithinAgeLimit(long edgeEngagementTime, long edgeAgeLimit) {
    return (edgeEngagementTime >= System.currentTimeMillis() - edgeAgeLimit);
  }

  /**
   * Return whether we should proceed with updating an edge's info based on the criteria specified in the request
   * @param request the request object containing the criteria
   * @param rightNode is the RHS node
   * @param edgeType  is the edge type
   * @param edgeMetadata is the edge metadata
   * @return true if the edge's information should be collected, false if it should be skipped
   */
  protected abstract boolean isEdgeUpdateValid(
    Request request,
    long rightNode,
    byte edgeType,
    long edgeMetadata
  );

  /**
   * Update node information gathered about each RHS node, such as metadata and weights.
   * This method update nodes in {@link TopSecondDegreeByCount#visitedRightNodes}.
   * @param leftNode                is the LHS node from which traversal initialized
   * @param rightNode               is the RHS node at which traversal arrived
   * @param edgeType                is the edge type from which LHS and RHS nodes are connected
   * @param edgeMetadata            is the edge metadata from which LHS and RHS nodes are connected
   * @param weight                  is the weight contributed to a RHS node in this traversal
   * @param edgeIterator            is the iterator for traversing edges from LHS node
   */
  protected abstract void updateNodeInfo(
    long leftNode,
    long rightNode,
    byte edgeType,
    long edgeMetadata,
    double weight,
    EdgeIterator edgeIterator,
    int maxSocialProofTypeSize);

  /**
   * Generate and return recommendation response. As the last step in the calculation,
   * this method should utilize filtered information gathered in
   * {@link TopSecondDegreeByCount#nodeInfosAfterFiltering}.
   * @param request                 is the original request object
   * @return                        is the recommendations
   */
  protected abstract Response generateRecommendationFromNodeInfo(Request request);

  /**
   * Compute recommendations using the TopSecondDegreeByCount algorithm.
   * @param request  is the request for the algorithm
   * @param random   is used for all random choices within the algorithm
   * @return         Right hand side nodes with largest weights
   */
  @Override
  public Response computeRecommendations(Request request, Random random) {
    numRequestsCounter.incr();
    reset(request);

    collectRightNodeInfo(request);
    updateAlgorithmStats(request.getQueryNode());
    filterNodeInfo(request);
    return generateRecommendationFromNodeInfo(request);
  }

  private void reset(Request request) {
    request.resetFilters();
    visitedRightNodes.clear();
    nodeInfosAfterFiltering.clear();
    seenEdgesPerNode.clear();
    topSecondDegreeByCountStats.reset();
  }

  private void collectRightNodeInfo(Request request) {
    for (Long2DoubleMap.Entry entry: request.getLeftSeedNodesWithWeight().long2DoubleEntrySet()) {
      long leftNode = entry.getLongKey();
      EdgeIterator edgeIterator = leftIndexedBipartiteGraph.getLeftNodeEdges(leftNode);
      if (edgeIterator == null) {
        continue;
      }

      int numEdgesPerNode = 0;
      double weight = entry.getDoubleValue();
      seenEdgesPerNode.clear();
      // Sequentially iterating through the latest MAX_EDGES_PER_NODE edges per node
      while (edgeIterator.hasNext() && numEdgesPerNode++ < RecommendationRequest.MAX_EDGES_PER_NODE) {
        long rightNode = edgeIterator.nextLong();
        byte edgeType = edgeIterator.currentEdgeType();
        long edgeMetadata = edgeIterator.currentMetadata();

        boolean hasSeenRightNodeFromEdge =
          seenEdgesPerNode.containsKey(rightNode) && seenEdgesPerNode.get(rightNode) == edgeType;

        if (!hasSeenRightNodeFromEdge
          && isEdgeUpdateValid(request, rightNode, edgeType, edgeMetadata)) {
          seenEdgesPerNode.put(rightNode, edgeType);
          updateNodeInfo(
            leftNode,
            rightNode,
            edgeType,
            edgeMetadata,
            weight,
            edgeIterator,
            request.getMaxSocialProofTypeSize());
        }
      }
    }
  }

  private void updateAlgorithmStats(long queryNode) {
    topSecondDegreeByCountStats.setNumDirectNeighbors(
      leftIndexedBipartiteGraph.getLeftNodeDegree(queryNode)
    );

    int minVisitsPerRightNode = Integer.MAX_VALUE;
    int maxVisitsPerRightNode = 0;
    int numRHSVisits = 0;

    for (Long2ObjectMap.Entry<NodeInfo> entry: visitedRightNodes.long2ObjectEntrySet()) {
      NodeInfo nodeInfo = entry.getValue();
      int numVisits = nodeInfo.getNumVisits();

      minVisitsPerRightNode = Math.min(minVisitsPerRightNode, numVisits);
      maxVisitsPerRightNode = Math.max(maxVisitsPerRightNode, numVisits);
      numRHSVisits += numVisits;
    }

    topSecondDegreeByCountStats.setMinVisitsPerRightNode(minVisitsPerRightNode);
    topSecondDegreeByCountStats.setMaxVisitsPerRightNode(maxVisitsPerRightNode);
    topSecondDegreeByCountStats.setNumRHSVisits(numRHSVisits);
    topSecondDegreeByCountStats.setNumRightNodesReached(visitedRightNodes.size());
  }

  private void filterNodeInfo(Request request) {
    int numFilteredNodes = 0;
    for (NodeInfo nodeInfo : visitedRightNodes.values()) {
      if (request.filterResult(nodeInfo.getNodeId(), nodeInfo.getSocialProofs())) {
        numFilteredNodes++;
        continue;
      }
      nodeInfosAfterFiltering.add(nodeInfo);
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
