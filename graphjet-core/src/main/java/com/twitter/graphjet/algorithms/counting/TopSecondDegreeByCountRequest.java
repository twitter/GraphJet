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

import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.filters.ResultFilterChain;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Parent class of all requests using TopSecondDegreeByCount algorithm.
 */
public abstract class TopSecondDegreeByCountRequest extends RecommendationRequest {

  private final Long2DoubleMap leftSeedNodesWithWeight;
  private final int maxSocialProofTypeSize;
  private final ResultFilterChain resultFilterChain;
  private final long maxRightNodeAgeInMillis;
  private final long maxEdgeAgeInMillis;

  /**
   * @param queryNode                 is the query node for running TopSecondDegreeByCount
   * @param leftSeedNodesWithWeight   is the set of seed nodes and their weights to use for
   *                                  TopSecondDegreeByCount
   * @param toBeFiltered              is the set of RHS nodes to be filtered from the output
   * @param maxSocialProofTypeSize    is the number of social proof types in the graph
   * @param socialProofTypes          Social proof types, masked into a byte array
   * @param maxRightNodeAgeInMillis   Max right node age in millisecond, such as tweet age
   * @param maxEdgeAgeInMillis        Max edge age in millisecond such as reply edge age
   * @param resultFilterChain         Filter chain to be applied after recommendation computation
   */
  public TopSecondDegreeByCountRequest(
    long queryNode,
    Long2DoubleMap leftSeedNodesWithWeight,
    LongSet toBeFiltered,
    int maxSocialProofTypeSize,
    byte[] socialProofTypes,
    long maxRightNodeAgeInMillis,
    long maxEdgeAgeInMillis,
    ResultFilterChain resultFilterChain) {
    super(queryNode, toBeFiltered, socialProofTypes);
    this.leftSeedNodesWithWeight = leftSeedNodesWithWeight;
    this.maxSocialProofTypeSize = maxSocialProofTypeSize;
    this.maxRightNodeAgeInMillis = maxRightNodeAgeInMillis;
    this.maxEdgeAgeInMillis = maxEdgeAgeInMillis;
    this.resultFilterChain = resultFilterChain;
  }

  public Long2DoubleMap getLeftSeedNodesWithWeight() {
    return leftSeedNodesWithWeight;
  }

  public int getMaxSocialProofTypeSize() {
    return maxSocialProofTypeSize;
  }

  public long getMaxRightNodeAgeInMillis() {
    return maxRightNodeAgeInMillis;
  }

  public long getMaxEdgeAgeInMillis() {
    return maxEdgeAgeInMillis;
  }

  public void resetFilters() {
    if (resultFilterChain != null) {
      resultFilterChain.resetFilters(this);
    }
  }

  /**
   * Filter the given result using the filter chain
   * @param result is the node to check for filtering
   * @param socialProofs is the socialProofs of different types associated with the node
   * @return true if the node should be discarded, false otherwise
   */
  public boolean filterResult(Long result, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    return resultFilterChain != null && resultFilterChain.filterResult(result, socialProofs);
  }
}
