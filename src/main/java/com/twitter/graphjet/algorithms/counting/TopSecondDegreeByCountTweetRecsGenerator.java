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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.google.common.collect.Lists;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.TweetIDMask;
import com.twitter.graphjet.algorithms.TweetRecommendationInfo;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

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
    boolean keep = false;
    for (int i = 0; i < socialProofs.length; i++) {
      if (i != tweetSocialProofType && socialProofs[i] != null) {
        keep = true;
        break;
      }
    }

    return !keep;
  }

  private static boolean isLessThantMinUserSocialProofSize(
    SmallArrayBasedLongToDoubleMap[] socialProofs,
    int minUserSocialProofSize
  ) {
    boolean keep = false;
    for (int i = 0; i < socialProofs.length; i++) {
      if (socialProofs[i] != null && socialProofs[i].size() >= minUserSocialProofSize) {
        keep = true;
        break;
      }
    }

    return !keep;
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
      ? request.getMaxNumResultsByType().get(RecommendationType.TWEET) : Integer.MAX_VALUE;

    PriorityQueue<NodeInfo> topResults = new PriorityQueue<NodeInfo>(maxNumResults);

    int minUserSocialProofSize =
      request.getMinUserSocialProofSizes().containsKey(RecommendationType.TWEET)
        ? request.getMinUserSocialProofSizes().get(RecommendationType.TWEET)
        : MIN_USER_SOCIAL_PROOF_SIZE;

    // handling two specific rules of tweet recommendations
    // 1. do not return tweet recommendations with only Tweet social proofs.
    // 2. do not return social proofs less than minUserSocialProofSizeForTweetRecs.
    for (NodeInfo nodeInfo : nodeInfoList) {
      if (isTweetSocialProofOnly(nodeInfo.getSocialProofs(), 4 /* tweet social proof type */)) {
        continue;
      }
      if (isLessThantMinUserSocialProofSize(
        nodeInfo.getSocialProofs(),
        minUserSocialProofSize)
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
