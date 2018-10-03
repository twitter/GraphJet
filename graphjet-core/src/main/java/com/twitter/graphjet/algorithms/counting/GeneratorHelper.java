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

package com.twitter.graphjet.algorithms.counting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.twitter.graphjet.algorithms.ConnectingUsersWithMetadata;
import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.counting.tweet.TopSecondDegreeByCountRequestForTweet;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import static com.twitter.graphjet.algorithms.RecommendationRequest.FAVORITE_SOCIAL_PROOF_TYPE;
import static com.twitter.graphjet.algorithms.RecommendationRequest.UNFAVORITE_SOCIAL_PROOF_TYPE;

/**
 * Shared utility functions among RecsGenerators.
 */
public final class GeneratorHelper {

  private GeneratorHelper() {
  }

  /**
   * Pick the top social proofs for each RHS node
   */
  public static Map<Byte, ConnectingUsersWithMetadata> pickTopSocialProofs(
    SmallArrayBasedLongToDoubleMap[] socialProofs){

    Map<Byte, ConnectingUsersWithMetadata> results = new HashMap<>();

    for (int i = 0; i < socialProofs.length; i++) {
      SmallArrayBasedLongToDoubleMap socialProof = socialProofs[i];
      if (socialProof != null) {
        if (socialProof.size() > 1) {
          socialProof.sort();
        }

        socialProof.trim(socialProof.size());
        results.put((byte)i, new ConnectingUsersWithMetadata(
          new LongArrayList(socialProof.keys()),
          new LongArrayList(socialProof.metadata())
        ));
      }
    }

    return results;
  }

  public static int getMinUserSocialProofSize(
    TopSecondDegreeByCountRequestForTweet request,
    RecommendationType recommendationType
  ) {
    return request.getMinUserSocialProofSizes().containsKey(recommendationType)
      ? request.getMinUserSocialProofSizes().get(recommendationType)
      : RecommendationRequest.DEFAULT_MIN_USER_SOCIAL_PROOF_SIZE;
  }

  public static int getMaxNumResults(
    TopSecondDegreeByCountRequestForTweet request,
    RecommendationType recommendationType
  ) {
    return request.getMaxNumResultsByType().containsKey(recommendationType)
      ? Math.min(request.getMaxNumResultsByType().get(recommendationType),
        RecommendationRequest.MAX_RECOMMENDATION_RESULTS)
      : RecommendationRequest.DEFAULT_RECOMMENDATION_RESULTS;
  }

  public static void addResultToPriorityQueue(
    PriorityQueue<NodeInfo> topResults,
    NodeInfo nodeInfo,
    int maxNumResults) {
    if (topResults.size() < maxNumResults) {
      topResults.add(nodeInfo);
    } else if (nodeInfo.getWeight() > topResults.peek().getWeight()) {
      topResults.poll();
      topResults.add(nodeInfo);
    }
  }

  public static PriorityQueue<NodeInfo> getValidNodes(
      List<NodeInfo> nodeInfoList,
      Map<Byte, Integer> minSocialProofSizes,
      int maxNumResults) {
    PriorityQueue<NodeInfo> topResults = new PriorityQueue<>(maxNumResults);

    for (NodeInfo nodeInfo: nodeInfoList) {
      if (isValidSocialProof(minSocialProofSizes, nodeInfo.getSocialProofs())) {
        addResultToPriorityQueue(topResults, nodeInfo, maxNumResults);
      }
    }
    return topResults;
  }

  public static boolean isValidSocialProof(
      Map<Byte, Integer> minSocialProofSizes,
      SmallArrayBasedLongToDoubleMap[] socialProofs) {
    for (int i = 0; i < socialProofs.length; i++) {
      byte proofType = (byte)i;
      if (!minSocialProofSizes.containsKey(proofType)) {
        // valid, if there is no limit on the size of this social proof
        continue;
      }
      if (socialProofs[proofType] == null) {
        // not valid, if node does not have this type of social proof
        return false;
      }
      if (socialProofs[proofType].uniqueKeysSize() < minSocialProofSizes.get(proofType)) {
        // not valid, if number of social proofs below threshold
        return false;
      }
    }
    return true;
  }

  /**
   * Given a nodeInfo, check all social proofs stored and determine if it still has
   * valid, non-empty social proofs.
   */
  public static boolean nodeInfoHasValidSocialProofs(NodeInfo nodeInfo) {
    for (SmallArrayBasedLongToDoubleMap socialProof: nodeInfo.getSocialProofs()) {
      if (socialProof != null && socialProof.size() != 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Given a nodeInfo, check whether this nodeInfo supports unfavorite edges.
   */
  public static boolean isUnfavoriteTypeSupported(NodeInfo nodeInfo) {
    return UNFAVORITE_SOCIAL_PROOF_TYPE < nodeInfo.getSocialProofs().length;
  }

  /**
   * Given a nodeInfo containing the collection of all social proofs on a tweet, remove the
   * Favorite social proofs that also have Unfavorite counterparts, and deduct the weight of the
   * nodeInfo accordingly. The Unfavorite social proofs will always be reset to null.
   *
   * @return true if the nodInfo has been modified, i.e. have Unfavorited removed, false otherwise.
   */
  public static boolean removeUnfavoriteSocialProofs(NodeInfo nodeInfo) {
    if (!isUnfavoriteTypeSupported(nodeInfo)) {
      return false;
    }

    SmallArrayBasedLongToDoubleMap[] socialProofs = nodeInfo.getSocialProofs();
    SmallArrayBasedLongToDoubleMap unfavSocialProofs = socialProofs[UNFAVORITE_SOCIAL_PROOF_TYPE];
    SmallArrayBasedLongToDoubleMap favSocialProofs = socialProofs[FAVORITE_SOCIAL_PROOF_TYPE];

    if (unfavSocialProofs == null) {
      return false;
    }

    // Always remove unfavorite social proofs, as they are only meant for internal processing and
    // not to be returned to the caller.
    double unfavWeightToRemove = 0;
    for (int i = 0; i < unfavSocialProofs.size(); i++) {
      unfavWeightToRemove += unfavSocialProofs.values()[i];
    }
    nodeInfo.setWeight(nodeInfo.getWeight() - unfavWeightToRemove);
    socialProofs[UNFAVORITE_SOCIAL_PROOF_TYPE] = null;

    // Remove favorite social proofs that were unfavorited and the corresponding weights
    if (favSocialProofs != null) {
      int favWeightToRemove = 0;
      SmallArrayBasedLongToDoubleMap newFavSocialProofs = new SmallArrayBasedLongToDoubleMap();
      for (int i = 0; i < favSocialProofs.size(); i++) {
        long favUser = favSocialProofs.keys()[i];
        double favWeight = favSocialProofs.values()[i];

        if (unfavSocialProofs.contains(favUser)) {
          favWeightToRemove += favWeight;
        } else {
          newFavSocialProofs.put(favUser, favWeight, favSocialProofs.metadata()[i]);
        }
      }
      // Add the filtered Favorite social proofs
      nodeInfo.setWeight(nodeInfo.getWeight() - favWeightToRemove);
      socialProofs[FAVORITE_SOCIAL_PROOF_TYPE] = (newFavSocialProofs.size() != 0) ? newFavSocialProofs : null;
    }

    return true;
  }
}
