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


package com.twitter.graphjet.algorithms.counting.recommendationGenerator;

import java.util.*;

import com.google.common.collect.Lists;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.counting.recommendationInfo.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.TweetIDMask;
import com.twitter.graphjet.algorithms.counting.recommendationInfo.RecommendationInfoTweet;
import com.twitter.graphjet.algorithms.counting.request.TopSecondDegreeByCountRequestForTweet;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public final class TopSecondDegreeByCountTweetRecsGenerator implements GeneratorUtils {

  /**
   * Return tweet recommendations
   *
   * @param request       topSecondDegreeByCount request
   * @param nodeInfoList  a list of node info containing engagement social proof and weights
   * @return a list of tweet recommendations
   */
  public static List<RecommendationInfo> generateTweetRecs(
    TopSecondDegreeByCountRequestForTweet request,
    List<NodeInfo> nodeInfoList) {
    int maxNumResults = request.getMaxNumResultsByType().containsKey(RecommendationType.TWEET)
      ? Math.min(request.getMaxNumResultsByType().get(RecommendationType.TWEET),
                 RecommendationRequest.MAX_RECOMMENDATION_RESULTS)
      : RecommendationRequest.DEFAULT_RECOMMENDATION_RESULTS;

    PriorityQueue<NodeInfo> topResults = new PriorityQueue<NodeInfo>(maxNumResults);

    int minUserSocialProofSize =
      request.getMinUserSocialProofSizes().containsKey(RecommendationType.TWEET)
        ? request.getMinUserSocialProofSizes().get(RecommendationType.TWEET)
        : RecommendationRequest.DEFAULT_MIN_USER_SOCIAL_PROOF_SIZE;

    // handling specific rules of tweet recommendations
    for (NodeInfo nodeInfo : nodeInfoList) {
      // do not return tweet recommendations with only Tweet social proofs.
      if (isTweetSocialProofOnly(nodeInfo.getSocialProofs(), 4 /* tweet social proof type */)) {
        continue;
      }
      // do not return if size of each social proof is less than minUserSocialProofSize.
      if (isLessThanMinUserSocialProofSize(nodeInfo.getSocialProofs(), minUserSocialProofSize) &&
          // do not return if size of each social proof union is less than minUserSocialProofSize.
          isLessThanMinUserSocialProofSizeCombined(
              nodeInfo.getSocialProofs(), minUserSocialProofSize, request.getSocialProofTypeUnions())) {
        continue;
      }
      addResultToPriorityQueue(topResults, nodeInfo, maxNumResults);
    }

    byte[] validSocialProofs = request.getSocialProofTypes();
    int maxSocialProofSize = request.getMaxUserSocialProofSize();

    List<RecommendationInfo> outputResults =
      Lists.newArrayListWithCapacity(topResults.size());
    while (!topResults.isEmpty()) {
      NodeInfo nodeInfo = topResults.poll();
      outputResults.add(
        new RecommendationInfoTweet(
          TweetIDMask.restore(nodeInfo.getValue()),
          nodeInfo.getWeight(),
          pickTopSocialProofs(nodeInfo.getSocialProofs(), validSocialProofs, maxSocialProofSize)));
    }
    Collections.reverse(outputResults);

    return outputResults;
  }

  private static boolean isSocialProofUnionSizeLessThanMin(
      SmallArrayBasedLongToDoubleMap[] socialProofs,
      int minUserSocialProofSize,
      Set<byte[]> socialProofTypeUnions) {
    long socialProofSizeSum = 0;

    for (byte[] socialProofTypeUnion: socialProofTypeUnions) {
      socialProofSizeSum = 0;
      for (byte socialProofType: socialProofTypeUnion) {
        if (socialProofs[socialProofType] != null) {
          socialProofSizeSum += socialProofs[socialProofType].size();
          if (socialProofSizeSum >= minUserSocialProofSize) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private static boolean isLessThanMinUserSocialProofSizeCombined(
      SmallArrayBasedLongToDoubleMap[] socialProofs,
      int minUserSocialProofSize,
      Set<byte[]> socialProofTypeUnions) {
    if (socialProofTypeUnions.isEmpty() ||
        // check if the size of any social proof union is greater than minUserSocialProofSize before dedupping
        isSocialProofUnionSizeLessThanMin(socialProofs, minUserSocialProofSize, socialProofTypeUnions)) {
      return true;
    }

    LongSet uniqueNodes = new LongOpenHashSet(minUserSocialProofSize);

    for (byte[] socialProofTypeUnion: socialProofTypeUnions) {
      // Clear removes all elements, but does not change the size of the set.
      // Thus, we only use one LongOpenHashSet with at most a size of 2*minUserSocialProofSize
      uniqueNodes.clear();
      for (byte socialProofType: socialProofTypeUnion) {
        if (socialProofs[socialProofType] != null) {
          for (int i = 0; i < socialProofs[socialProofType].size(); i++) {
            uniqueNodes.add(socialProofs[socialProofType].keys()[i]);
            if (uniqueNodes.size() >= minUserSocialProofSize) {
              return false;
            }
          }
        }
      }
    }
    return true;
  }

  public static boolean isLessThanMinUserSocialProofSize(
      SmallArrayBasedLongToDoubleMap[] socialProofs,
      int minUserSocialProofSize) {
    for (int i = 0; i < socialProofs.length; i++) {
      if (socialProofs[i] != null && socialProofs[i].size() >= minUserSocialProofSize) {
        return false;
      }
    }
    return true;
  }

  private static boolean isTweetSocialProofOnly(
      SmallArrayBasedLongToDoubleMap[] socialProofs,
      int tweetSocialProofType) {
    for (int i = 0; i < socialProofs.length; i++) {
      if (i != tweetSocialProofType && socialProofs[i] != null) {
        return false;
      }
    }
    return true;
  }

}
