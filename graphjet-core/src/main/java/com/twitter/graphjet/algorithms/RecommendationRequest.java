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

import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This interface specifies a request received by a {@link RecommendationAlgorithm}.
 */
public abstract class RecommendationRequest {
  private final long queryNode;
  private final LongSet toBeFiltered;
  private final byte[] socialProofTypes;
  public static final int MAX_RECOMMENDATION_RESULTS = 1000;
  public static final int DEFAULT_RECOMMENDATION_RESULTS = 100;

  protected RecommendationRequest(
    long queryNode,
    LongSet toBeFiltered,
    byte[] socialProofTypes
  ) {
    this.queryNode = queryNode;
    this.toBeFiltered = toBeFiltered;
    this.socialProofTypes = socialProofTypes;
  }

  public long getQueryNode() {
    return queryNode;
  }

  /**
   * Return the set of RHS nodes to be filtered from the output
   */
  public LongSet getToBeFiltered() {
    return toBeFiltered;
  }

  /**
   * Return the social proof types requested by the clients
   */
  public byte[] getSocialProofTypes() {
    return socialProofTypes;
  }
}
