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

import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationType;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This class wraps a social proof recommendation result for one entity (right node).
 * The {@link SocialProofResponse} wraps a list of SocialProofResult objects.
 */
public class EntitySocialProofResult implements RecommendationInfo {

  private final int entity;
  private final Byte2ObjectMap<Long2ObjectMap<LongSet>> socialProof;
  private final double weight;
  private final RecommendationType recommendationType;

  public EntitySocialProofResult(
      Integer entity,
      Byte2ObjectMap<Long2ObjectMap<LongSet>> socialProof,
      double weight,
      RecommendationType recommendationType
  ) {
    this.entity = entity;
    this.socialProof = socialProof;
    this.weight = weight;
    this.recommendationType = recommendationType;
  }

  @Override
  public RecommendationType getRecommendationType() {
    return this.recommendationType;
  }

  @Override
  public double getWeight() {
    return this.weight;
  }

  public Byte2ObjectMap<Long2ObjectMap<LongSet>> getSocialProof() {
    return this.socialProof;
  }

  public int getEntity() {
    return this.entity;
  }

  /**
   * Calculate the total number of interactions for current entity (right node)
   * on given set of users (left nodes).
   *
   * @return the number of interactions.
   */
  public int getSocialProofSize() {
    int socialProofSize = 0;
    for (Long2ObjectMap<LongSet> tweetsMap: socialProof.values()) {
      for (LongSet connectingUsers: tweetsMap.values()) {
        socialProofSize += connectingUsers.size();
      }
    }
    return socialProofSize;
  }

}
