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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.google.common.collect.Lists;

import com.twitter.graphjet.algorithms.ConnectingUsersWithMetadata;
import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.counting.GeneratorHelper;

public final class TopSecondDegreeByCountMomentRecsGenerator {

  private TopSecondDegreeByCountMomentRecsGenerator() {
  }

  /**
   * Generate a list of recommendations based on given list of candidate nodes and the original request.
   * @param request         original request message, contains filtering criteria
   * @param candidateNodes  list of candidate nodes
   * @return                list of {@link MomentRecommendationInfo}
   */
  public static List<RecommendationInfo> generateMomentRecs(
    TopSecondDegreeByCountRequestForMoment request,
    List<NodeInfo> candidateNodes) {

    int maxNumResults = Math.min(request.getMaxNumResults(), RecommendationRequest.MAX_RECOMMENDATION_RESULTS);

    PriorityQueue<NodeInfo> validNodes =
      GeneratorHelper.getValidNodes(candidateNodes, request.getMinUserPerSocialProof(), maxNumResults);

    return getRecommendationsFromNodes(request, validNodes);
  }

  private static List<RecommendationInfo> getRecommendationsFromNodes(
    TopSecondDegreeByCountRequestForMoment request,
    PriorityQueue<NodeInfo> topNodes) {
    List<RecommendationInfo> outputResults = Lists.newArrayListWithCapacity(topNodes.size());

    while (!topNodes.isEmpty()) {
      NodeInfo nodeInfo = topNodes.poll();

      Map<Byte, ConnectingUsersWithMetadata> topSocialProofs = GeneratorHelper.pickTopSocialProofs(
        nodeInfo.getSocialProofs()
      );

      MomentRecommendationInfo momentRecs = new MomentRecommendationInfo(
        nodeInfo.getNodeId(),
        nodeInfo.getWeight(),
        topSocialProofs);
      outputResults.add(momentRecs);
    }
    Collections.reverse(outputResults);
    return outputResults;
  }
}
