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
import com.twitter.graphjet.algorithms.*;

public class TopSecondDegreeByCountUserRecsGenerator {

    /**
     * Generate a list of recommendations based on given list of candidate nodes and the original request
     * @param request       original request message, contains filtering criteria
     * @param nodeInfoList  list of candidate nodes
     * @return              list of {@link UserRecommendationInfo}
     */
    public static List<RecommendationInfo> generateUserRecs(
            TopSecondDegreeByCountRequest request,
            List<NodeInfo> nodeInfoList) {

        int maxNumResults = RecommendationRequest.DEFAULT_RECOMMENDATION_RESULTS;
        if (request.getMaxNumResultsByType().containsKey(RecommendationType.USER)) {
            maxNumResults = request.getMaxNumResultsByType().get(RecommendationType.USER);
        }
        PriorityQueue<NodeInfo> topResults = new PriorityQueue<>(maxNumResults);

        int minUserSocialProofSize = request.getMinUserSocialProofSizes().containsKey(RecommendationType.USER) ?
                request.getMinUserSocialProofSizes().get(RecommendationType.USER) :
                RecommendationRequest.DEFAULT_MIN_USER_SOCIAL_PROOF_SIZE;

        for (NodeInfo nodeInfo : nodeInfoList) {
            if (GeneratorUtils.isLessThantMinUserSocialProofSize(nodeInfo.getSocialProofs(), minUserSocialProofSize)) {
                continue;
            }
            GeneratorUtils.addResultToPriorityQueue(topResults, nodeInfo, maxNumResults);
        }

        byte[] validSocialProofs = request.getSocialProofTypes();
        int maxSocialProofSize = request.getMaxUserSocialProofSize();

        List<RecommendationInfo> outputResults = Lists.newArrayListWithCapacity(topResults.size());
        while (!topResults.isEmpty()) {
            NodeInfo nodeInfo = topResults.poll();
            UserRecommendationInfo userRecs = new UserRecommendationInfo(
                    TweetIDMask.restore(nodeInfo.getValue()),
                    nodeInfo.getWeight(),
                    GeneratorUtils.pickTopSocialProofs(nodeInfo.getSocialProofs(), validSocialProofs, maxSocialProofSize));
            outputResults.add(userRecs);
        }
        Collections.reverse(outputResults);

        return outputResults;
    }
}
