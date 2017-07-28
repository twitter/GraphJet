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

import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.filters.ResultFilterChain;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This class encapsulates a SALSA request. This is meant to be used only via the
 * {@link SalsaRequestBuilder}.
 */
public class SalsaRequest extends RecommendationRequest {
  private final Long2DoubleMap leftSeedNodesWithWeight;
  private final int numRandomWalks;
  private final int maxRandomWalkLength;
  private final double resetProbability;
  private final int maxNumResults;
  private final int maxSocialProofSize;
  private final int maxSocialProofTypeSize;
  private final double queryNodeWeightFraction;
  private final boolean removeCustomizedBitsNodes;
  private final ResultFilterChain resultFilterChain;

  /**
   * The constructor should only be called via {@link SalsaRequestBuilder}.
   * @param queryNode                 is the query node for running SALSA
   * @param leftSeedNodesWithWeight   is the set of seed nodes to use for SALSA, with weights being
   *                                  the proportion of random walks to start here. We do NOT assume
   *                                  that the queryNode is added to this.
   * @param toBeFiltered              is the set of RHS nodes to be filtered from the output
   * @param numRandomWalks            is the total number of random walks to run
   * @param maxRandomWalkLength       is the maximum length of a random walk
   * @param resetProbability          is the probability of reset in SALSA. Note that reset is only
   *                                  done on backward iterations.
   * @param maxNumResults             is the maximum number of results that SALSA will return
   * @param maxSocialProofSize        is the maximum size of social proof per type to return. Set
   *                                  this to 0 to return no social proof
   * @param maxSocialProofTypeSize    is the maximum size of social proof types in the graph.
   * @param socialProofTypes          is the social proof types to return
   * @param queryNodeWeightFraction   is the relative proportion of random walks to start at the
   *                                  queryNode in the first iteration. This parameter is only used
   * @param removeCustomizedBitsNodes removes tweets with metadata information embedded into top
   *                                  four bits
   * @param resultFilterChain         is the chain of filters to be applied
   */
  protected SalsaRequest(
      long queryNode,
      Long2DoubleMap leftSeedNodesWithWeight,
      LongSet toBeFiltered,
      int numRandomWalks,
      int maxRandomWalkLength,
      double resetProbability,
      int maxNumResults,
      int maxSocialProofSize,
      int maxSocialProofTypeSize,
      byte[] socialProofTypes,
      double queryNodeWeightFraction,
      boolean removeCustomizedBitsNodes,
      ResultFilterChain resultFilterChain) {
    super(queryNode, toBeFiltered, socialProofTypes);
    this.leftSeedNodesWithWeight = leftSeedNodesWithWeight;
    this.numRandomWalks = numRandomWalks;
    this.maxRandomWalkLength = maxRandomWalkLength;
    this.resetProbability = resetProbability;
    this.maxNumResults = maxNumResults;
    this.maxSocialProofSize = maxSocialProofSize;
    this.maxSocialProofTypeSize = maxSocialProofTypeSize;
    this.queryNodeWeightFraction = queryNodeWeightFraction;
    this.removeCustomizedBitsNodes = removeCustomizedBitsNodes;
    this.resultFilterChain = resultFilterChain;
  }

  public  Long2DoubleMap getLeftSeedNodesWithWeight() {
    return leftSeedNodesWithWeight;
  }

  public int getNumRandomWalks() {
    return numRandomWalks;
  }

  public int getMaxRandomWalkLength() {
    return maxRandomWalkLength;
  }

  public double getResetProbability() {
    return resetProbability;
  }

  public int getMaxNumResults() {
    return maxNumResults;
  }

  public int getMaxSocialProofSize() {
    return maxSocialProofSize;
  }

  public int getMaxSocialProofTypeSize() {
    return maxSocialProofTypeSize;
  }

  public double getQueryNodeWeightFraction() {
    return queryNodeWeightFraction;
  }

  public boolean removeCustomizedBitsNodes() {
    return removeCustomizedBitsNodes;
  }

  public void resetFilters() {
    if (resultFilterChain != null) {
      resultFilterChain.resetFilters(this);
    }
  }

  /**
   * filter the given result
   * @param result is the node to check for filtering
   * @param socialProofs is the socialProofs of different types associated with the node
   * @return true if the node should be discarded, false otherwise
   */
  public boolean filterResult(Long result, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    return resultFilterChain != null && resultFilterChain.filterResult(result, socialProofs);
  }
}
