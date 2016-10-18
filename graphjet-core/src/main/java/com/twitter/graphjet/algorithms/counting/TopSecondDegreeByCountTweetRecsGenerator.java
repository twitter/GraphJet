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

import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

import com.google.common.collect.Lists;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.TweetIDMask;
import com.twitter.graphjet.algorithms.TweetRecommendationInfo;

public final class TopSecondDegreeByCountTweetRecsGenerator {
  /**
   * Return tweet recommendations
   *
   * @param request       topSecondDegreeByCount request
   * @param nodeInfoList  a list of node info containing engagement social proof and weights
   * @return a list of tweet recommendations
   */
  public static List<RecommendationInfo> generateTweetRecs(
    TopSecondDegreeByCountRequest request,
    List<NodeInfo> nodeInfoList
  ) {
    int maxNumResults = request.getMaxNumResultsByType().containsKey(RecommendationType.TWEET)
      ? Math.min(request.getMaxNumResultsByType().get(RecommendationType.TWEET),
                 RecommendationRequest.MAX_RECOMMENDATION_RESULTS)
      : RecommendationRequest.DEFAULT_RECOMMENDATION_RESULTS;

    PriorityQueue<NodeInfo> topResults = new PriorityQueue<NodeInfo>(maxNumResults);

    int minUserSocialProofSize =
      request.getMinUserSocialProofSizes().containsKey(RecommendationType.TWEET)
        ? request.getMinUserSocialProofSizes().get(RecommendationType.TWEET)
        : RecommendationRequest.DEFAULT_MIN_USER_SOCIAL_PROOF_SIZE;

    // handling two specific rules of tweet recommendations
    // 1. do not return tweet recommendations with only Tweet social proofs.
    // 2. do not return social proofs less than minUserSocialProofSizeForTweetRecs.
    for (NodeInfo nodeInfo : nodeInfoList) {
      if (GeneratorUtils.isTweetSocialProofOnly(nodeInfo.getSocialProofs(), 4 /* tweet social proof type */)) {
        continue;
      }
      if (GeneratorUtils.isLessThantMinUserSocialProofSize(
        nodeInfo.getSocialProofs(),
        minUserSocialProofSize)
      ) {
        continue;
      }
      GeneratorUtils.addResultToPriorityQueue(topResults, nodeInfo, maxNumResults);
    }

    byte[] validSocialProofs = request.getSocialProofTypes();
    int maxSocialProofSize = request.getMaxUserSocialProofSize();

    List<RecommendationInfo> outputResults =
      Lists.newArrayListWithCapacity(topResults.size());
    while (!topResults.isEmpty()) {
      NodeInfo nodeInfo = topResults.poll();
      outputResults.add(
        new TweetRecommendationInfo(
          TweetIDMask.restore(nodeInfo.getValue()),
          nodeInfo.getWeight(),
                GeneratorUtils.pickTopSocialProofs(nodeInfo.getSocialProofs(), validSocialProofs, maxSocialProofSize)));
    }
    Collections.reverse(outputResults);

    return outputResults;
  }
}
