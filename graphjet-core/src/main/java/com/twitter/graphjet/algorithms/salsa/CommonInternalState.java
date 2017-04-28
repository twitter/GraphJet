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


package com.twitter.graphjet.algorithms.salsa;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.bipartite.api.LeftIndexedBipartiteGraph;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This class encapsulates the common internal state needed to run both kinds of SALSA.
 */
public abstract class CommonInternalState<T extends LeftIndexedBipartiteGraph> {
  protected final SalsaStats salsaStats;
  protected final LongSet nonZeroSeedSet;
  protected final Long2IntMap currentLeftNodes;
  protected final Long2IntMap currentRightNodes;
  protected final Long2ObjectMap<NodeInfo> visitedRightNodes;
  protected SalsaRequest salsaRequest;

  /**
   * Get a new instance of a fresh internal state.
   *
   * @param salsaStats          is the stats object to use
   * @param expectedNodesToHit  is the number of nodes the random walk is expected to hit
   */
  public CommonInternalState(
      SalsaStats salsaStats,
      int expectedNodesToHit) {
    this.salsaStats = salsaStats;
    this.currentLeftNodes = new Long2IntOpenHashMap(expectedNodesToHit);
    this.currentRightNodes = new Long2IntOpenHashMap(expectedNodesToHit);
    this.visitedRightNodes = new Long2ObjectOpenHashMap<NodeInfo>(expectedNodesToHit);
    this.nonZeroSeedSet = new LongOpenHashSet(expectedNodesToHit);
  }

  public SalsaRequest getSalsaRequest() {
    return salsaRequest;
  }

  public SalsaStats getSalsaStats() {
    return salsaStats;
  }

  public LongSet getNonZeroSeedSet() {
    return nonZeroSeedSet;
  }

  public Long2IntMap getCurrentLeftNodes() {
    return currentLeftNodes;
  }

  public Long2IntMap getCurrentRightNodes() {
    return currentRightNodes;
  }

  public Long2ObjectMap<NodeInfo> getVisitedRightNodes() {
    return visitedRightNodes;
  }

  public void resetCurrentLeftNodes(long queryNode, int resetWeight) {
    currentLeftNodes.clear();
    currentLeftNodes.put(queryNode, resetWeight);
  }

  public void clearCurrentRightNodes() {
    currentRightNodes.clear();
  }

  /**
   * Adds the leftNode to the set of left nodes that the random walk will continue from in the next
   * iteration
   *
   * @param leftNode  is the left node to add
   */
  public void addNodeToCurrentLeftNodes(long leftNode) {
    if (currentLeftNodes.containsKey(leftNode)) {
      currentLeftNodes.put(leftNode, currentLeftNodes.get(leftNode) + 1);
    } else {
      currentLeftNodes.put(leftNode, 1);
    }
  }

  /**
   * Adds the rightNode to the set of right nodes that the random walk will continue from in the
   * next iteration
   *
   * @param rightNode  is the right node to add
   */
  public void addNodeToCurrentRightNodes(long rightNode) {
    if (currentRightNodes.containsKey(rightNode)) {
      currentRightNodes.put(rightNode, currentRightNodes.get(rightNode) + 1);
    } else {
      currentRightNodes.put(rightNode, 1);
    }
  }

  /**
   * Visits the given rightNode, updating the internal state about visits.
   *
   * @param nodeVisitor  is the node visitor to use
   * @param leftNode     is the left node that visits the rightNode
   * @param rightNode    is the right node being visited
   * @param edgeType     is the edge type between left node and right node
   * @param weight       is the weight of the edge
   * @return the number of visits to this node so far, including the current one
   */
  public int visitRightNode(
      SalsaNodeVisitor.NodeVisitor nodeVisitor,
      long leftNode,
      long rightNode,
      byte edgeType,
      long edgeMetadata,
      double weight) {
    return nodeVisitor.visitRightNode(leftNode, rightNode, edgeType, edgeMetadata, weight);
  }

  /**
   * Resets all internal state kept for SALSA to enable reuse.
   *
   * @param incomingSalsaRequest  is the new incoming request
   */
  public void resetWithRequest(SalsaRequest incomingSalsaRequest) {
    this.salsaRequest = incomingSalsaRequest;
    nonZeroSeedSet.clear();
    currentLeftNodes.clear();
    currentRightNodes.clear();
    visitedRightNodes.clear();
    salsaStats.reset();
    incomingSalsaRequest.resetFilters();
  }

  /**
   * We need to return a left-indexed graph at least for operations such as seeding
   *
   * @return a graph of type <code>T</code>
   */
  public abstract T getBipartiteGraph();
}
