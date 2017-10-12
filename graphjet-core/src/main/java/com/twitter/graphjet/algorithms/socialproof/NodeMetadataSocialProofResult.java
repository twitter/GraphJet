/**
 * Copyright 2017 Twitter. All rights reserved.
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
 * This class wraps a nodeMetadataId social proof recommendation result for one nodeMetadataId
 * within a right node.
 * The {@link SocialProofResponse} wraps a list of NodeMetadataSocialProofResult objects.
 */
public class NodeMetadataSocialProofResult implements RecommendationInfo {

  private final int nodeMetadataId;
  // Engagement (Byte) -> User (Long) -> Tweets (LongSet)
  private final Byte2ObjectMap<Long2ObjectMap<LongSet>> socialProof;
  private final double weight;
  private final RecommendationType recommendationType;

  public NodeMetadataSocialProofResult(
    int nodeMetadataId,
    Byte2ObjectMap<Long2ObjectMap<LongSet>> socialProof,
    double weight,
    RecommendationType recommendationType
  ) {
    this.nodeMetadataId = nodeMetadataId;
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

  public int getNodeMetadataId() {
    return this.nodeMetadataId;
  }

  /**
   * Calculate the total number of interactions for the current nodeMetadataId (right node's metadata)
   * given the set of users (left nodes).
   *
   * @return the number of unique edgeType/user/tweet interactions.
   * For example (0 (byte), 12 (long), 99 (long)) would be a single unique interaction.
   */
  public int getSocialProofSize() {
    int socialProofSize = 0;
    for (Long2ObjectMap<LongSet> userToTweetsMap: socialProof.values()) {
      for (LongSet connectingTweets: userToTweetsMap.values()) {
        socialProofSize += connectingTweets.size();
      }
    }
    return socialProofSize;
  }

}
