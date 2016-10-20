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

import java.util.*;

import com.google.common.collect.Lists;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.TweetIDMask;
import com.twitter.graphjet.algorithms.TweetRecommendationInfo;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public final class TopSecondDegreeByCountTweetRecsGenerator {
  private static final int MIN_USER_SOCIAL_PROOF_SIZE = 1;

  private TopSecondDegreeByCountTweetRecsGenerator() {
  }
  /**
   * Pick the top social proofs for each RHS node
   */
  private static Map<Byte, LongList> pickTopSocialProofs(
    SmallArrayBasedLongToDoubleMap[] socialProofs,
    byte[] validSocialProofs,
    int maxSocialProofSize
  ) {
    Map<Byte, LongList> results = new HashMap<Byte, LongList>();
    int length = validSocialProofs.length;

    for (int i = 0; i < length; i++) {
      SmallArrayBasedLongToDoubleMap socialProof = socialProofs[validSocialProofs[i]];
      if (socialProof != null) {
        if (socialProof.size() > 1) {
          socialProof.sort();
        }

        socialProof.trim(maxSocialProofSize);
        results.put(validSocialProofs[i], new LongArrayList(socialProof.keys()));
      }
    }
    return results;
  }

  private static void addResultToPriorityQueue(
    PriorityQueue<NodeInfo> topResults,
    NodeInfo nodeInfo,
    int maxNumResults
  ) {
    if (topResults.size() < maxNumResults) {
      topResults.add(nodeInfo);
    } else if (nodeInfo.getWeight() > topResults.peek().getWeight()) {
      topResults.poll();
      topResults.add(nodeInfo);
    }
  }

  private static boolean isTweetSocialProofOnly(
    SmallArrayBasedLongToDoubleMap[] socialProofs,
    int tweetSocialProofType
  ) {
    boolean lessThan = true;
    for (int i = 0; i < socialProofs.length; i++) {
      if (i != tweetSocialProofType && socialProofs[i] != null) {
        lessThan = false;
        break;
      }
    }

    return lessThan;
  }

  private static boolean isLessThanMinUserSocialProofSize(
    SmallArrayBasedLongToDoubleMap[] socialProofs,
    int minUserSocialProofSize
  ) {
    boolean lessThan = true;
    for (int i = 0; i < socialProofs.length; i++) {
      if (socialProofs[i] != null && socialProofs[i].size() >= minUserSocialProofSize) {
        lessThan = false;
        break;
      }
    }

    return lessThan;
  }

  private static boolean socialProofUnionSizeLessThanMin(
    SmallArrayBasedLongToDoubleMap[] socialProofs,
    int minUserSocialProofSize,
    Set<byte[]> socialProofTypeUnions
  ) {
    boolean lessThan = true;
    long socialProofSizeSum = 0;

    outerloop:
    for (byte[] socialProofTypeUnion: socialProofTypeUnions) {
      socialProofSizeSum = 0;
      for (byte socialProofType: socialProofTypeUnion) {
        if (socialProofs[socialProofType] != null) {
          socialProofSizeSum += socialProofs[socialProofType].size();
          if (socialProofSizeSum >= minUserSocialProofSize) {
            lessThan = false;
            break outerloop;
          }
        }
      }
    }

    return lessThan;
  }

  private static boolean isLessThanMinUserSocialProofSizeCombined(
    SmallArrayBasedLongToDoubleMap[] socialProofs,
    int minUserSocialProofSize,
    Set<byte[]> socialProofTypeUnions
  ) {
    boolean lessThan = true;
    if (socialProofTypeUnions.isEmpty() ||
        // check if the size of any social proof union is greater than minUserSocialProofSize before dedupping
        socialProofUnionSizeLessThanMin(socialProofs, minUserSocialProofSize, socialProofTypeUnions)
    ) {
      return lessThan;
    }

    LongSet uniqueNodes = new LongOpenHashSet(minUserSocialProofSize);

    outerloop:
    for (byte[] socialProofTypeUnion: socialProofTypeUnions) {
      // Clear removes all elements, but does not change the size of the set.
      // Thus, we only use one LongOpenHashSet with at most a size of 2*minUserSocialProofSize
      uniqueNodes.clear();
      for (byte socialProofType: socialProofTypeUnion) {
        if (socialProofs[socialProofType] != null) {
          for (int i = 0; i < socialProofs[socialProofType].size(); i++) {
            uniqueNodes.add(socialProofs[socialProofType].keys()[i]);
            if (uniqueNodes.size() >= minUserSocialProofSize) {
              lessThan = false;
              break outerloop;
            }
          }
        }
      }
    }

    return lessThan;
  }

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
        : MIN_USER_SOCIAL_PROOF_SIZE;

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
            nodeInfo.getSocialProofs(), minUserSocialProofSize, request.getSocialProofTypeUnions()
          )
      ) {
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
        new TweetRecommendationInfo(
          TweetIDMask.restore(nodeInfo.getValue()),
          nodeInfo.getWeight(),
          pickTopSocialProofs(nodeInfo.getSocialProofs(), validSocialProofs, maxSocialProofSize)));
    }
    Collections.reverse(outputResults);

    return outputResults;
  }
}
