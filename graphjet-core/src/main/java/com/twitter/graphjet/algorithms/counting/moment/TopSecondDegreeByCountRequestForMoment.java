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

package com.twitter.graphjet.algorithms.counting.moment;

import java.util.Map;

import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.filters.ResultFilterChain;
import com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCountRequest;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Request data structure for calculating moment recommendations.
 */
public class TopSecondDegreeByCountRequestForMoment extends TopSecondDegreeByCountRequest {
  private final Map<Byte, Integer> minUserPerSocialProof;
  private final int maxNumResults;
  private final int maxNumSocialProofs;
  private final RecommendationType recommendationType = RecommendationType.MOMENT;

  /**
   * @param queryNode                 is the query node for running TopSecondDegreeByCountRequestForMoment
   * @param leftSeedNodesWithWeight   is the set of seed nodes and their weights to use for calculation
   * @param toBeFiltered              is the set of RHS nodes to be filtered from the output
   * @param maxNumResults             is the maximum number of recommendations returned in the response
   * @param maxNumSocialProofs        is the maximum number of social proofs per recommendation
   * @param maxSocialProofTypeSize    is the number of social proof types in the graph
   * @param minUserPerSocialProof     for each social proof, require a minimum number of users to be valid
   * @param socialProofTypes          is the list of valid social proofs, (i.e. Create, Like etc)
   * @param maxRightNodeAgeInMillis   Max right node age in millisecond, such as moment age
   * @param maxEdgeAgeInMillis        Max edge age in millisecond such as like edge age
   * @param resultFilterChain         is the chain of filters to be applied
   */
  public TopSecondDegreeByCountRequestForMoment(
    long queryNode,
    Long2DoubleMap leftSeedNodesWithWeight,
    LongSet toBeFiltered,
    int maxNumResults,
    int maxNumSocialProofs,
    int maxSocialProofTypeSize,
    Map<Byte, Integer> minUserPerSocialProof,
    byte[] socialProofTypes,
    long maxRightNodeAgeInMillis,
    long maxEdgeAgeInMillis,
    ResultFilterChain resultFilterChain) {
    super(queryNode, leftSeedNodesWithWeight, toBeFiltered, maxSocialProofTypeSize,
        socialProofTypes, maxRightNodeAgeInMillis, maxEdgeAgeInMillis, resultFilterChain);
    this.maxNumResults = maxNumResults;
    this.maxNumSocialProofs = maxNumSocialProofs;
    this.minUserPerSocialProof = minUserPerSocialProof;
  }

  public Map<Byte, Integer> getMinUserPerSocialProof() { return minUserPerSocialProof; }

  public int getMaxNumResults() { return maxNumResults; }

  public int getMaxNumSocialProofs() { return maxNumSocialProofs; }

  public RecommendationType getRecommendationType() { return recommendationType; }

}
