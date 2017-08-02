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

import com.google.common.base.Preconditions;

import com.twitter.graphjet.algorithms.filters.ResultFilterChain;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This class allows building a {@link SalsaRequest}.
 */
public class SalsaRequestBuilder {
  private final long queryNode;
  private Long2DoubleMap leftSeedNodesWithWeight;
  private LongSet toBeFiltered;
  private int numRandomWalks = 1;
  private int maxRandomWalkLength = 1;
  private double resetProbability = 0.3;
  private int maxNumResults = 10;
  private int maxSocialProofSize = 0;
  private int maxSocialProofTypeSize = 1;
  private byte[] validSocialProofTypes = {0};
  private double queryNodeWeightFraction = 0.9;
  private boolean removeNegativeNodes = false;
  private ResultFilterChain resultFilterChain;

  public SalsaRequestBuilder(long queryNode) {
    this.queryNode = queryNode;
  }

  public SalsaRequestBuilder withLeftSeedNodes(Long2DoubleMap inputLeftSeedNodes) {
    this.leftSeedNodesWithWeight = inputLeftSeedNodes;
    return this;
  }

  public SalsaRequestBuilder withToBeFiltered(LongSet inputToBeFiltered) {
    this.toBeFiltered = inputToBeFiltered;
    return this;
  }

  public SalsaRequestBuilder withNumRandomWalks(int inputNumRandomWalks) {
    this.numRandomWalks = inputNumRandomWalks;
    return this;
  }

  /**
   * This function attempts to set the maximum length of a random walk, and will check that this
   * number is odd.
   *
   * @param inputMaxRandomWalkLength  is the maximum length of a random walk in SALSA
   * @return the object itself to allow chaining
   */
  public SalsaRequestBuilder withMaxRandomWalkLength(int inputMaxRandomWalkLength) {
    Preconditions.checkArgument(inputMaxRandomWalkLength % 2 == 1,
                                "Maximum random walk length must be odd.");
    this.maxRandomWalkLength = inputMaxRandomWalkLength;
    return this;
  }

  public SalsaRequestBuilder withResetProbability(double inputResetProbability) {
    this.resetProbability = inputResetProbability;
    return this;
  }

  public SalsaRequestBuilder withMaxNumResults(int inputMaxNumResults) {
    this.maxNumResults = inputMaxNumResults;
    return this;
  }

  public SalsaRequestBuilder withMaxSocialProofSize(int inputMaxSocialProofSize) {
    this.maxSocialProofSize = inputMaxSocialProofSize;
    return this;
  }

  public SalsaRequestBuilder withMaxSocialProofTypeSize(int inputMaxSocialProofTypeSize) {
    this.maxSocialProofTypeSize = inputMaxSocialProofTypeSize;
    return this;
  }

  public SalsaRequestBuilder withValidSocialProofTypes(byte[] inputValidSocialProofTypes) {
    this.validSocialProofTypes = inputValidSocialProofTypes;
    return this;
  }

  public SalsaRequestBuilder withQueryNodeWeightFraction(
      double inputQueryNodeWeightFraction) {
    this.queryNodeWeightFraction = inputQueryNodeWeightFraction;
    return this;
  }

  public SalsaRequestBuilder removeNegativeNodes(boolean inputRemoveNegativeNodes) {
    this.removeNegativeNodes = inputRemoveNegativeNodes;
    return this;
  }

  public SalsaRequestBuilder withResultFilterChain(ResultFilterChain inputResultFilterChain) {
    this.resultFilterChain = inputResultFilterChain;
    return this;
  }
  /**
   * Builds and returns a new {@link SalsaRequest}
   *
   * @return a new {@link SalsaRequest} instance
   */
  public SalsaRequest build() {
    if (toBeFiltered == null) {
      toBeFiltered = new LongOpenHashSet(1);
    }

    if (leftSeedNodesWithWeight == null || leftSeedNodesWithWeight.isEmpty()) {
      leftSeedNodesWithWeight = new Long2DoubleOpenHashMap(1);
      leftSeedNodesWithWeight.put(queryNode, 1.0);
    }

    return new SalsaRequest(
        queryNode,
        leftSeedNodesWithWeight,
        toBeFiltered,
        numRandomWalks,
        maxRandomWalkLength,
        resetProbability,
        maxNumResults,
        maxSocialProofSize,
        maxSocialProofTypeSize,
        validSocialProofTypes,
        queryNodeWeightFraction,
        removeNegativeNodes,
        resultFilterChain);
  }
}
