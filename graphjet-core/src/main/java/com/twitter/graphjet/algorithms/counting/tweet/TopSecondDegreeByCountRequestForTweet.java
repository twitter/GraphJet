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

import java.util.Map;
import java.util.Set;

import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.filters.ResultFilterChain;
import com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCountRequest;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongSet;

public class TopSecondDegreeByCountRequestForTweet extends TopSecondDegreeByCountRequest {
  private final Set<RecommendationType> recommendationTypes;
  private final Map<RecommendationType, Integer> maxNumResultsByType;
  private final int maxUserSocialProofSize;
  private final int maxTweetSocialProofSize;
  private final Map<RecommendationType, Integer> minUserSocialProofSizes;
  private final Set<byte[]> socialProofTypeUnions;

  /**
   * Construct a TopSecondDegreeByCount algorithm runner for tweet related recommendations.
   * @param queryNode                 is the query node for running TopSecondDegreeByCountForTweet
   * @param leftSeedNodesWithWeight   is the set of seed nodes and their weights to use for
   *                                  TopSecondDegreeByCountForTweet
   * @param toBeFiltered              is the set of RHS nodes to be filtered from the output
   * @param recommendationTypes       is the list of recommendation types requested by clients
   * @param maxNumResultsByRecType    is the maximum number of results requested by clients per recommendation type
   * @param maxSocialProofTypeSize    is the maximum size of social proof types in the graph
   * @param maxUserSocialProofSize    is the maximum size of user social proof per type to return
   *                                  Set this to 0 to return no social proof
   * @param maxTweetSocialProofSize   is the maximum size of tweet social proof per user to return
   * @param minUserSocialProofSizes   is the minimum size of user social proof per recommendation
   *                                  type to return
   * @param socialProofTypes          is the social proof types to return
   * @param resultFilterChain         is the chain of filters to be applied
   * @param socialProofTypeUnions     is the set of groups of social proofs to be combined
   */
  public TopSecondDegreeByCountRequestForTweet(
    long queryNode,
    Long2DoubleMap leftSeedNodesWithWeight,
    LongSet toBeFiltered,
    Set<RecommendationType> recommendationTypes,
    Map<RecommendationType, Integer> maxNumResultsByRecType,
    int maxSocialProofTypeSize,
    int maxUserSocialProofSize,
    int maxTweetSocialProofSize,
    Map<RecommendationType, Integer> minUserSocialProofSizes,
    byte[] socialProofTypes,
    long maxRightNodeAgeInMillis,
    long maxEdgeAgeInMillis,
    ResultFilterChain resultFilterChain,
    Set<byte[]> socialProofTypeUnions
  ) {
    super(
      queryNode,
      leftSeedNodesWithWeight,
      toBeFiltered,
      maxSocialProofTypeSize,
      socialProofTypes,
      maxRightNodeAgeInMillis,
      maxEdgeAgeInMillis,
      resultFilterChain);
    this.recommendationTypes = recommendationTypes;
    this.maxNumResultsByType = maxNumResultsByRecType;
    this.maxUserSocialProofSize = maxUserSocialProofSize;
    this.maxTweetSocialProofSize = maxTweetSocialProofSize;
    this.minUserSocialProofSizes = minUserSocialProofSizes;
    this.socialProofTypeUnions = socialProofTypeUnions;
  }

  public Set<RecommendationType> getRecommendationTypes() {
    return recommendationTypes;
  }

  public Map<RecommendationType, Integer> getMaxNumResultsByType() { return maxNumResultsByType; }

  public int getMaxUserSocialProofSize() { return maxUserSocialProofSize; }

  public int getMaxTweetSocialProofSize() { return maxTweetSocialProofSize; }

  public Map<RecommendationType, Integer> getMinUserSocialProofSizes() { return minUserSocialProofSizes; }

  public Set<byte[]> getSocialProofTypeUnions() { return socialProofTypeUnions; }
}
