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


package com.twitter.graphjet.algorithms;

import com.google.common.base.Objects;

public class RecommendationStats {
  protected int numDirectNeighbors;
  protected int numRightNodesReached;
  protected int numRHSVisits;
  protected int minVisitsPerRightNode;
  protected int maxVisitsPerRightNode;
  protected int numRightNodesFiltered;

  /**
   * Convenience constructor that initializes all stats to 0
   */
  public RecommendationStats() {
    this(0, 0, 0, Integer.MAX_VALUE, 0, 0);
  }

  /**
   * This constructor sets the stats for testing purpose. We document the stats use here.
   *
   * @param numDirectNeighbors     is the number of direct neighbors of the query node
   * @param numRightNodesReached   is the number of unique right nodes that were reached by SALSA
   * @param numRHSVisits           is the total number of visits done by SALSA to nodes on the RHS
   *                               of the bipartite graph
   * @param minVisitsPerRightNode  is the minimum number of visits to any RHS node in SALSA
   * @param maxVisitsPerRightNode  is the maximum number of visits to any RHS node in SALSA
   * @param numRightNodesFiltered  is the number of RHS nodes filtered from the final output
   */
  public RecommendationStats(
    int numDirectNeighbors,
    int numRightNodesReached,
    int numRHSVisits,
    int minVisitsPerRightNode,
    int maxVisitsPerRightNode,
    int numRightNodesFiltered) {
    this.numDirectNeighbors = numDirectNeighbors;
    this.numRightNodesReached = numRightNodesReached;
    this.numRHSVisits = numRHSVisits;
    this.minVisitsPerRightNode = minVisitsPerRightNode;
    this.maxVisitsPerRightNode = maxVisitsPerRightNode;
    this.numRightNodesFiltered = numRightNodesFiltered;
  }

  public int getNumDirectNeighbors() {
    return numDirectNeighbors;
  }

  public void setNumDirectNeighbors(int numDirectNeighbors) {
    this.numDirectNeighbors = numDirectNeighbors;
  }

  public int getNumRightNodesReached() {
    return numRightNodesReached;
  }

  public void setNumRightNodesReached(int numRightNodesReached) {
    this.numRightNodesReached = numRightNodesReached;
  }

  public void setNumRHSVisits(int numRHSVisits) {
    this.numRHSVisits = numRHSVisits;
  }

  public int getNumRHSVisits() {
    return numRHSVisits;
  }

  public void addToNumRHSVisits(int numVisits) {
    this.numRHSVisits += numVisits;
  }

  public void setMinVisitsPerRightNode(int minVisitsPerRightNode) {
    this.minVisitsPerRightNode = minVisitsPerRightNode;
  }

  public int getMinVisitsPerRightNode() {
    return minVisitsPerRightNode;
  }

  public void updateMinVisitsPerRightNode(int numVisits) {
    this.minVisitsPerRightNode = Math.min(minVisitsPerRightNode, numVisits);
  }

  public void setMaxVisitsPerRightNode(int maxVisitsPerRightNode) {
    this.maxVisitsPerRightNode = maxVisitsPerRightNode;
  }

  public int getMaxVisitsPerRightNode() {
    return maxVisitsPerRightNode;
  }

  public void updateMaxVisitsPerRightNode(int numVisits) {
    this.maxVisitsPerRightNode = Math.max(maxVisitsPerRightNode, numVisits);
  }

  public void updateVisitStatsPerRightNode(int numVisits) {
    updateMinVisitsPerRightNode(numVisits);
    updateMaxVisitsPerRightNode(numVisits);
  }

  public int getNumRightNodesFiltered() {
    return numRightNodesFiltered;
  }

  public void setNumRightNodesFiltered(int numRightNodesFiltered) {
    this.numRightNodesFiltered = numRightNodesFiltered;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
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

    RecommendationStats other = (RecommendationStats) obj;

    return
        Objects.equal(numDirectNeighbors, other.numDirectNeighbors)
        && Objects.equal(numRightNodesReached, other.numRightNodesReached)
        && Objects.equal(numRHSVisits, other.numRHSVisits)
        && Objects.equal(minVisitsPerRightNode, other.minVisitsPerRightNode)
        && Objects.equal(maxVisitsPerRightNode, other.maxVisitsPerRightNode)
        && Objects.equal(numRightNodesFiltered, other.numRightNodesFiltered);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
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
    numDirectNeighbors = 0;
    numRightNodesReached = 0;
    numRHSVisits = 0;
    minVisitsPerRightNode = Integer.MAX_VALUE;
    maxVisitsPerRightNode = 0;
    numRightNodesFiltered = 0;
  }
}
