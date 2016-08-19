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


package com.twitter.graphjet.algorithms.randommultigraphneighbors;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;

/**
 * This class encapsulates a random multi-graph neighbor request.
 */
public class RandomMultiGraphNeighborsRequest {
  private final Long2DoubleMap leftSeedNodesWithWeight;
  private final int maxNumSamples;
  private final int maxNumResults;

  /**
   * Create a new request.
   *
   * @param leftSeedNodesWithWeight          is the seed set to use to supplement the query node
   * @param maxNumSamples                    is the maximum number of samples
   * @param maxResults                       is the maximum number of results to return
   */
  public RandomMultiGraphNeighborsRequest(
      Long2DoubleMap leftSeedNodesWithWeight,
      int maxNumSamples,
      int maxResults) {
    this.leftSeedNodesWithWeight = leftSeedNodesWithWeight;
    this.maxNumSamples = maxNumSamples;
    this.maxNumResults = maxResults;
  }

  public Long2DoubleMap getLeftSeedNodesWithWeight() {
    return leftSeedNodesWithWeight;
  }

  public int getMaxNumSamples() {
    return maxNumSamples;
  }

  public int getMaxNumResults() {
    return maxNumResults;
  }

}
