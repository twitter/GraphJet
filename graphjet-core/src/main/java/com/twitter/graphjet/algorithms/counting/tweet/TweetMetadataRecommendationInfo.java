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


package com.twitter.graphjet.algorithms.counting.tweet;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Objects;

import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

/**
 * Hashtag and url recommendation based on user-tweet interactions,
 * such as tweet creation, retweet, reply, favorite and quote.
 */

public class TweetMetadataRecommendationInfo
  implements RecommendationInfo, Comparable<TweetMetadataRecommendationInfo> {
  private final int recommendation;
  private final RecommendationType recommendationType;
  private double weight;
  private final Map<Byte, Map<Long, LongList>> socialProof;
  private static final int INITIAL_TWEET_ARRAY_SIZE = 4;

  /**
   * Specify the metadata recommendation, such as hashtag and url.
   */
  public TweetMetadataRecommendationInfo(int recommendation, RecommendationType type, double weight,
                                         Map<Byte, Map<Long, LongList>> socialProof) {
    this.recommendation = recommendation;
    this.recommendationType = type;
    this.weight = weight;
    this.socialProof = socialProof;
  }

  public int getRecommendation() {
    return recommendation;
  }

  public RecommendationType getRecommendationType() {
    return recommendationType;
  }

  public double getWeight() {
    return weight;
  }

  public Map<Byte, Map<Long, LongList>> getSocialProof() {
    return socialProof;
  }

  /**
   * Update the user and tweet social proofs associated with each metadata
   * recommendation.
   *
   * @param socialProofType          the social proof type, such as like, retweet, reply
   * @param userSocialProofs         the list of user social proofs
   * @param rightNode                the tweet with the metadata
   * @param maxUserSocialProofSize   max user social proof size to collect
   * @param maxTweetSocialProofSize  max tweet social proof size to collect
   */
  public void addToTweetSocialProofs(
    byte socialProofType,
    SmallArrayBasedLongToDoubleMap userSocialProofs,
    long rightNode,
    int maxUserSocialProofSize,
    int maxTweetSocialProofSize
  ) {
    Map<Long, LongList> socialProofByType = socialProof.get(socialProofType);
    // userSocialProofs.keys might contain legitimate duplicates. for example, a user replies a
    // tweet multiple times.
    long[] leftNodes = userSocialProofs.keys();

    if (socialProofByType == null) {
      // if this is the first social proof of this type, for each user social proof, create an empty
      // tweetIds list, add tweet id in the list, and then add the list to socialProof along with
      // the user id.
      socialProofByType = new HashMap<Long, LongList>();
      for (int i = 0; i < userSocialProofs.size(); i++) {

        // update tweetIds if one of the following conditions is true.
        // 1. userSocialProofs does not contain duplicates.
        // 2. userSocialProofs has duplicates but this is the first time to add rightNode.
        if (userSocialProofs.size() == userSocialProofs.uniqueKeysSize()
          || !socialProofByType.containsKey(leftNodes[i])) {

          LongList tweetIds = new LongArrayList(INITIAL_TWEET_ARRAY_SIZE);
          tweetIds.add(rightNode);
          socialProofByType.put(leftNodes[i], tweetIds);
        }
      }

      socialProof.put(socialProofType, socialProofByType);
    } else {
      // if the social proof type is already in the map, for each user social proof, create or
      // update the corresponding tweet social proof.
      for (int i = 0; i < userSocialProofs.size(); i++) {
        LongList tweetIds = socialProofByType.get(leftNodes[i]);

        // update tweetIds if one of the following conditions is true.
        // 1. userSocialProofs does not contain duplicates.
        // 2. userSocialProofs has duplicates but this is the first time to add rightNode.
        if (userSocialProofs.size() == userSocialProofs.uniqueKeysSize()
          || (tweetIds == null || !tweetIds.contains(rightNode))) {

          if (socialProofByType.size() < maxUserSocialProofSize) {
            if (tweetIds == null) {
              tweetIds = new LongArrayList(INITIAL_TWEET_ARRAY_SIZE);
              socialProofByType.put(leftNodes[i], tweetIds);
            }
            if (tweetIds.size() < maxTweetSocialProofSize) {
              tweetIds.add(rightNode);
            }
          } else {
            if (tweetIds != null && tweetIds.size() < maxTweetSocialProofSize) {
              tweetIds.add(rightNode);
            }
          }
        }
      }
    }
  }

  public void addToWeight(double delta) {
    this.weight += delta;
  }

  //sort the list of TweetMetadataRecommendationInfo in ascending order according to their weights
  public int compareTo(TweetMetadataRecommendationInfo otherRecommendationInfo) {
    return Double.compare(otherRecommendationInfo.getWeight(), this.weight);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(recommendation, recommendationType, weight, socialProof);
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

    TweetMetadataRecommendationInfo other = (TweetMetadataRecommendationInfo) obj;

    return
      Objects.equal(getRecommendation(), other.getRecommendation())
        && Objects.equal(getRecommendationType(), other.getRecommendationType())
        && Objects.equal(getWeight(), other.getWeight())
        && Objects.equal(getSocialProof(), other.getSocialProof());
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("recommendation", recommendation)
      .add("recommendationType", recommendationType)
      .add("weight", weight)
      .add("socialProof", socialProof)
      .toString();
  }
}
