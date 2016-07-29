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


package com.twitter.graphjet.algorithms.socialproof;

import com.twitter.graphjet.algorithms.RecommendationRequest;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class SocialProofRequest extends RecommendationRequest {
  private static final LongSet EMPTY_SET = new LongArraySet();

  private final Long2DoubleMap leftSeedNodesWithWeight;
  private final LongSet inputTweets;

  /**
   * Create a social proof request.
   *
   * @param tweets              is the set of input tweets to query social proof.
   * @param weightedSeedNodes   is the set of seed users.
   * @param socialProofTypes    is the social proof types to return.
   */
  public SocialProofRequest(
    LongSet tweets,
    Long2DoubleMap weightedSeedNodes,
    byte[] socialProofTypes
  ) {
    super(0, EMPTY_SET, socialProofTypes);
    this.leftSeedNodesWithWeight = weightedSeedNodes;
    this.inputTweets = tweets;
  }

  public Long2DoubleMap getLeftSeedNodesWithWeight() {
    return leftSeedNodesWithWeight;
  }

  public LongSet getInputTweets() {
    return this.inputTweets;
  }

}
