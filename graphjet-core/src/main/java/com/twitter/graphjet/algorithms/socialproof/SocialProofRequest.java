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
  private final LongSet rightNodeIds;

  /**
   * Create a social proof request.
   *
   * @param rightNodeIds        is the set of right nodes to query for social proof.
   * @param weightedSeedNodes   is the set of left nodes to be used as social proofs.
   * @param socialProofTypes    is the social proof types to return.
   */
  public SocialProofRequest(
    LongSet rightNodeIds,
    Long2DoubleMap weightedSeedNodes,
    byte[] socialProofTypes
  ) {
    super(0, EMPTY_SET, socialProofTypes);
    this.leftSeedNodesWithWeight = weightedSeedNodes;
    this.rightNodeIds = rightNodeIds;
  }

  public Long2DoubleMap getLeftSeedNodesWithWeight() {
    return leftSeedNodesWithWeight;
  }

  public LongSet getRightNodeIds() {
    return this.rightNodeIds;
  }

}
