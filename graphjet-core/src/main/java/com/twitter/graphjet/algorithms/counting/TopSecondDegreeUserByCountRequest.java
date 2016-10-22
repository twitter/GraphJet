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

package com.twitter.graphjet.algorithms.counting;

import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.ResultFilterChain;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.Map;
import java.util.Set;

/**
 * TODO (gtang): Change description
 */
public class TopSecondDegreeUserByCountRequest extends TopSecondDegreeByCountRequest {
  private final Map<Byte, Integer> minUserPerSocialProof;
  private final int maxNumResults;
  public final RecommendationType recommendationType = RecommendationType.USER;

  /**
   * @param queryNode                 LHS user node we generate recommendations for
   * @param leftSeedNodesWithWeight   List of seed LFS nodes we use for RHS node weight calculation
   * @param toBeFiltered              List of users to be excluded from recommendations
   * @param maxNumResults             Maximum number of recommendations returned in the response
   * @param maxSocialProofSize        Maximum number of all social proofs, which helps restrict network bandwidth
   * @param minUserPerSocialProof     For each social proof, require a minimum number of users to be valid
   * @param socialProofTypes          List of valid social proofs, (i.e, Follow, Mention, Mediatag)
   * @param resultFilterChain         Chain of filters for results
   */
  public TopSecondDegreeUserByCountRequest(
      long queryNode,
      Long2DoubleMap leftSeedNodesWithWeight,
      LongSet toBeFiltered,
      int maxNumResults,
      int maxSocialProofSize,
      Map<Byte, Integer> minUserPerSocialProof,
      byte[] socialProofTypes,
      ResultFilterChain resultFilterChain) {
    super(queryNode, leftSeedNodesWithWeight, toBeFiltered, maxSocialProofSize, socialProofTypes,resultFilterChain);
    this.maxNumResults = maxNumResults;
    this.minUserPerSocialProof = minUserPerSocialProof;
  }

  public Map<Byte, Integer> getMinUserPerSocialProof() { return minUserPerSocialProof; }

  public int getMaxNumResults() { return maxNumResults; }
}
