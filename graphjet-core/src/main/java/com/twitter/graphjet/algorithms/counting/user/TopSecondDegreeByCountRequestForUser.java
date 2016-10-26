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

package com.twitter.graphjet.algorithms.counting.user;

import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.ResultFilterChain;
import com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCountRequest;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.Map;

/**
 * Request data structure for calculating user recommendations.
 */
public class TopSecondDegreeByCountRequestForUser extends TopSecondDegreeByCountRequest {
  private final Map<Byte, Integer> minUserPerSocialProof;
  private final int maxNumResults;
  private final RecommendationType recommendationType = RecommendationType.USER;

  /**
   * @param queryNode                 is the query node for running TopSecondDegreeByCountForUser
   * @param leftSeedNodesWithWeight   is the set of seed nodes and their weights to use for calculation
   * @param toBeFiltered              is the list of users to be excluded from recommendations
   * @param maxNumResults             is the maximum number of recommendations returned in the response
   * @param maxSocialProofTypeSize    is the number of social proof types in the graph
   * @param minUserPerSocialProof     for each social proof, require a minimum number of users to be valid
   * @param socialProofTypes          is the list of valid social proofs, (i.e, Follow, Mention, Mediatag)
   * @param resultFilterChain         is the chain of filters to be applied
   */
  public TopSecondDegreeByCountRequestForUser(
    long queryNode,
    Long2DoubleMap leftSeedNodesWithWeight,
    LongSet toBeFiltered,
    int maxNumResults,
    int maxSocialProofTypeSize,
    Map<Byte, Integer> minUserPerSocialProof,
    byte[] socialProofTypes,
    ResultFilterChain resultFilterChain) {
    super(queryNode, leftSeedNodesWithWeight, toBeFiltered, maxSocialProofTypeSize, socialProofTypes, resultFilterChain);
    this.maxNumResults = maxNumResults;
    this.minUserPerSocialProof = minUserPerSocialProof;
  }

  public Map<Byte, Integer> getMinUserPerSocialProof() { return minUserPerSocialProof; }

  public int getMaxNumResults() { return maxNumResults; }

  public RecommendationType getRecommendationType() { return recommendationType;}
}
