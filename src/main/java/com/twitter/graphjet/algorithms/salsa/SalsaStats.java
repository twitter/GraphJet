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

import com.google.common.base.Objects;

import com.twitter.graphjet.algorithms.RecommendationStats;

/**
 * This class encapsulates statistics captured from the SALSA execution. The stats are used
 * internally in SALSA, but the output of this is meant for external consumption.
 */
public final class SalsaStats extends RecommendationStats {
  private int numSeedNodes;

  /**
   * Convenience constructor that initializes all stats to 0
   */
  public SalsaStats() {
    super(0, 0, 0, Integer.MAX_VALUE, 0, 0);
    numSeedNodes = 0;
  }

  /**
   * This constructor sets the stats for testing purpose. We document the stats use here.
   *
   * @param numSeedNodes           is the number of seed nodes that SALSA starts with. Since this
   *                               number includes the query node, it is >= 1
   * @param numDirectNeighbors     is the number of direct neighbors of the query node
   * @param numRightNodesReached   is the number of unique right nodes that were reached by SALSA
   * @param numRHSVisits           is the total number of visits done by SALSA to nodes on the RHS
   *                               of the bipartite graph
   * @param minVisitsPerRightNode  is the minimum number of visits to any RHS node in SALSA
   * @param maxVisitsPerRightNode  is the maximum number of visits to any RHS node in SALSA
   * @param numRightNodesFiltered  is the number of RHS nodes filtered from the final output
   */
  protected SalsaStats(
      int numSeedNodes,
      int numDirectNeighbors,
      int numRightNodesReached,
      int numRHSVisits,
      int minVisitsPerRightNode,
      int maxVisitsPerRightNode,
      int numRightNodesFiltered) {
    super(
      numDirectNeighbors,
      numRightNodesReached,
      numRHSVisits,
      minVisitsPerRightNode,
      maxVisitsPerRightNode,
      numRightNodesFiltered
    );
    this.numSeedNodes = numSeedNodes;
  }

  public int getNumSeedNodes() {
    return numSeedNodes;
  }

  public void setNumSeedNodes(int numSeedNodes) {
    this.numSeedNodes = numSeedNodes;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        numSeedNodes,
        numDirectNeighbors,
        numRightNodesReached,
        numRHSVisits,
        minVisitsPerRightNode,
        maxVisitsPerRightNode,
        numRightNodesFiltered);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }

    SalsaStats other = (SalsaStats) obj;

    return
        Objects.equal(numSeedNodes, other.numSeedNodes)
        && Objects.equal(numDirectNeighbors, other.numDirectNeighbors)
        && Objects.equal(numRightNodesReached, other.numRightNodesReached)
        && Objects.equal(numRHSVisits, other.numRHSVisits)
        && Objects.equal(minVisitsPerRightNode, other.minVisitsPerRightNode)
        && Objects.equal(maxVisitsPerRightNode, other.maxVisitsPerRightNode)
        && Objects.equal(numRightNodesFiltered, other.numRightNodesFiltered);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("numSeedNodes", numSeedNodes)
        .add("numDirectNeighbors", numDirectNeighbors)
        .add("numRightNodesReached", numRightNodesReached)
        .add("numRHSVisits", numRHSVisits)
        .add("minVisitsPerRightNode", minVisitsPerRightNode)
        .add("maxVisitsPerRightNode", maxVisitsPerRightNode)
        .add("numRightNodesFiltered", numRightNodesFiltered)
        .toString();
  }

  /**
   * Resets all internal state kept for stats to enable reuse.
   */
  public void reset() {
    numSeedNodes = 0;
    numDirectNeighbors = 0;
    numRightNodesReached = 0;
    numRHSVisits = 0;
    minVisitsPerRightNode = Integer.MAX_VALUE;
    maxVisitsPerRightNode = 0;
    numRightNodesFiltered = 0;
  }
}
