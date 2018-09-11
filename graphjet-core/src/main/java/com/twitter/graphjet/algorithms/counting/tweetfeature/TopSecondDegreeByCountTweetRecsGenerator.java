/**
 * Copyright 2018 Twitter. All rights reserved.
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

package com.twitter.graphjet.algorithms.counting.tweetfeature;

import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import com.google.common.collect.Lists;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.TweetIDMask;
import com.twitter.graphjet.algorithms.counting.GeneratorHelper;
import com.twitter.graphjet.algorithms.counting.tweet.TopSecondDegreeByCountRequestForTweet;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public final class TopSecondDegreeByCountTweetRecsGenerator {
  private static final TweetIDMask TWEET_ID_MASK = new TweetIDMask();
  private static final byte FavoriteSocialProofType = 1;
  private static final byte UnfavoriteSocialProofType = 8;

  private TopSecondDegreeByCountTweetRecsGenerator() {
  }

  private static boolean isUnfavoriteTypeSupported(NodeInfo nodeInfo) {
    return UnfavoriteSocialProofType < nodeInfo.getSocialProofs().length;
  }

  /**
   * Given a nodeInfo containing the collection of all social proofs on a tweet, remove the
   * Favorite social proofs that also have Unfavorite counterparts, and deduct the weight of the
   * nodeInfo accordingly.
   * The Unfavorite social proofs will always be reset to null.
   */
  private static void removeUnfavoriteSocialProofs(NodeInfo nodeInfo) {
    SmallArrayBasedLongToDoubleMap[] socialProofs = nodeInfo.getSocialProofs();
    SmallArrayBasedLongToDoubleMap unfavSocialProofs = socialProofs[UnfavoriteSocialProofType];
    SmallArrayBasedLongToDoubleMap favSocialProofs = socialProofs[FavoriteSocialProofType];

    if (unfavSocialProofs == null || favSocialProofs == null) {
      return;
    }

    SmallArrayBasedLongToDoubleMap newFavSocialProofs = new SmallArrayBasedLongToDoubleMap();
    double weightToRemove = 0;

    for (int i = 0; i < favSocialProofs.size(); i++) {
      long favUser = favSocialProofs.keys()[i];
      double favWeight = favSocialProofs.values()[i];
      long favMetadata = favSocialProofs.metadata()[i];

      if (unfavSocialProofs.contains(favUser)) {
        weightToRemove += favWeight * 2; // *2 to take into account of both fav and unfav edge
        continue;
      }
      newFavSocialProofs.put(favUser, favWeight, favMetadata);
    }

    nodeInfo.setWeight(nodeInfo.getWeight() - weightToRemove);
    socialProofs[FavoriteSocialProofType] = (newFavSocialProofs.size() != 0) ? newFavSocialProofs : null;
    socialProofs[UnfavoriteSocialProofType] = null;
  }

  /**
   * Given a nodeInfo, check all social proofs stored and determine if it still has
   * valid, non-empty social proofs.
   */
  private static boolean nodeInfoHasValidSocialProofs(NodeInfo nodeInfo) {
    for ( SmallArrayBasedLongToDoubleMap socialProof: nodeInfo.getSocialProofs()) {
      if (socialProof != null && socialProof.size() != 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return tweet recommendations.
   *
   * @param request       topSecondDegreeByCount request
   * @param nodeInfoList  a list of node info containing engagement social proof and weights
   * @return a list of tweet recommendations
   */
  public static List<RecommendationInfo> generateTweetRecs(
    TopSecondDegreeByCountRequestForTweet request,
    List<NodeInfo> nodeInfoList) {
    int maxNumResults = GeneratorHelper.getMaxNumResults(request, RecommendationType.TWEET);
    int minUserSocialProofSize = GeneratorHelper.getMinUserSocialProofSize(request, RecommendationType.TWEET);
    byte[] validSocialProofs = request.getSocialProofTypes();

    PriorityQueue<NodeInfo> topResults = new PriorityQueue<NodeInfo>(maxNumResults);

    // handling specific rules of tweet recommendations
    for (NodeInfo nodeInfo : nodeInfoList) {
      if (isUnfavoriteTypeSupported(nodeInfo)) {
        removeUnfavoriteSocialProofs(nodeInfo);
        if (!nodeInfoHasValidSocialProofs(nodeInfo)) {
          continue;
        }
      }

      // do not return if size of each social proof is less than minUserSocialProofSize.
      if (isLessThanMinUserSocialProofSize(nodeInfo.getSocialProofs(), validSocialProofs, minUserSocialProofSize) &&
        // do not return if size of each social proof union is less than minUserSocialProofSize.
        isLessThanMinUserSocialProofSizeCombined(
          nodeInfo.getSocialProofs(), minUserSocialProofSize, request.getSocialProofTypeUnions())) {
        continue;
      }
      GeneratorHelper.addResultToPriorityQueue(topResults, nodeInfo, maxNumResults);
    }

    List<RecommendationInfo> outputResults = Lists.newArrayListWithCapacity(topResults.size());
    while (!topResults.isEmpty()) {
      NodeInfo nodeInfo = topResults.poll();
      outputResults.add(
        new TweetRecommendationInfo(
          TWEET_ID_MASK.restore(nodeInfo.getNodeId()),
          nodeInfo.getWeight(),
          GeneratorHelper.pickTopSocialProofs(nodeInfo.getSocialProofs()),
          nodeInfo.getNodeMetadata(TweetFeature.TWEET_FEATURE.getValue())));
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
          socialProofSizeSum += socialProofs[socialProofType].uniqueKeysSize();
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

  private static boolean isLessThanMinUserSocialProofSize(
    SmallArrayBasedLongToDoubleMap[] socialProofs,
    byte[] validSocialProofTypes,
    int minUserSocialProofSize) {

    long authorId = getAuthorId(socialProofs);

    for (byte validSocialProofType: validSocialProofTypes) {
      if (socialProofs[validSocialProofType] != null) {
        int minUserSocialProofThreshold = minUserSocialProofSize;
        if (authorId != -1 &&
          // Skip tweet author social proof because its size can be only one
            validSocialProofType != RecommendationRequest.AUTHOR_SOCIAL_PROOF_TYPE &&
          socialProofs[validSocialProofType].contains(authorId)) {
          minUserSocialProofThreshold += 1;
        }
        if (socialProofs[validSocialProofType].uniqueKeysSize() >= minUserSocialProofThreshold) {
          return false;
        }
      }
    }
    return true;
  }

  // Return the authorId of the Tweet, if the author is in the leftSeedNodesWithWeight; otherwise, return -1.
  private static long getAuthorId(SmallArrayBasedLongToDoubleMap[] socialProofs) {
    int socialProofTypeTweet = RecommendationRequest.AUTHOR_SOCIAL_PROOF_TYPE;
    long authorId = -1;
    if (socialProofs[socialProofTypeTweet] != null) {
      // There cannot be more than one key associated with the Tweet socialProofType
      authorId = socialProofs[socialProofTypeTweet].keys()[0];
    }
    return authorId;
  }
}
