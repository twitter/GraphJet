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


package com.twitter.graphjet.algorithms.counting.tweet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.twitter.graphjet.algorithms.*;
import com.twitter.graphjet.algorithms.counting.GeneratorHelper;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongList;

public final class TopSecondDegreeByCountTweetMetadataRecsGenerator {

  private TopSecondDegreeByCountTweetMetadataRecsGenerator() {
  }

  private static void addToSocialProof(
    NodeInfo nodeInfo,
    TweetMetadataRecommendationInfo recommendationInfo,
    int maxUserSocialProofSize,
    int maxTweetSocialProofSize
  ) {
    SmallArrayBasedLongToDoubleMap[] socialProofsByType = nodeInfo.getSocialProofs();
    for (int k = 0; k < socialProofsByType.length; k++) {
      if (socialProofsByType[k] != null) {
        recommendationInfo.addToTweetSocialProofs(
          (byte) k,
          socialProofsByType[k],
          TweetIDMask.restore(nodeInfo.getValue()),
          maxUserSocialProofSize,
          maxTweetSocialProofSize
        );
      }
    }
  }

  private static boolean isLessThanMinUserSocialProofSize(
    Map<Byte, Map<Long, LongList>> socialProofs,
    int minUserSocialProofSize
  ) {
    // the sum of tweet users and retweet users needs to be greater than a threshold
    int tweetUsers = socialProofs.get((byte) RecommendationRequest.SOCIAL_PROOF_TYPE_TWEET) == null ? 0 :
      socialProofs.get((byte) RecommendationRequest.SOCIAL_PROOF_TYPE_TWEET).size();
    int retweetUsers = socialProofs.get((byte) 2) == null ? 0 : socialProofs.get((byte) 2).size();

    return tweetUsers + retweetUsers < minUserSocialProofSize;
  }

  /**
   * Return tweet metadata recommendations, like hashtags and urls.
   *
   * @param request            topSecondDegreeByCount request
   * @param nodeInfoList       a list of node info containing engagement social proof and weights
   * @param recommendationType the recommendation type to return, like hashtag and url
   * @return a list of recommendations of the recommendation type
   */
  public static List<RecommendationInfo> generateTweetMetadataRecs(
    TopSecondDegreeByCountRequestForTweet request,
    List<NodeInfo> nodeInfoList,
    RecommendationType recommendationType
  ) {
    Int2ObjectMap<TweetMetadataRecommendationInfo> visitedMetadata = null;
    List<RecommendationInfo> results = new ArrayList<RecommendationInfo>();

    for (NodeInfo nodeInfo : nodeInfoList) {
      int[] metadata = nodeInfo.getNodeMetadata(recommendationType.getValue());

      if (metadata != null) {
        if (visitedMetadata == null) {
          visitedMetadata = new Int2ObjectOpenHashMap<TweetMetadataRecommendationInfo>();
        }
        for (int j = 0; j < metadata.length; j++) {
          TweetMetadataRecommendationInfo recommendationInfo =
            visitedMetadata.get(metadata[j]);

          if (recommendationInfo == null) {
            recommendationInfo = new TweetMetadataRecommendationInfo(
              metadata[j],
              RecommendationType.at(recommendationType.getValue()),
              0,
              new HashMap<Byte, Map<Long, LongList>>()
            );
          }
          recommendationInfo.addToWeight(nodeInfo.getWeight());
          addToSocialProof(
            nodeInfo,
            recommendationInfo,
            request.getMaxUserSocialProofSize(),
            request.getMaxTweetSocialProofSize()
          );

          visitedMetadata.put(metadata[j], recommendationInfo);
        }
      }
    }

    if (visitedMetadata != null) {
      int maxNumResults = GeneratorHelper.getMaxNumResults(request, recommendationType);
      int minUserSocialProofSize = GeneratorHelper.getMinUserSocialProofSize(request, recommendationType);

      List<TweetMetadataRecommendationInfo> filtered = null;

      for (Int2ObjectMap.Entry<TweetMetadataRecommendationInfo> entry
        : visitedMetadata.int2ObjectEntrySet()) {
        // handling one specific rule related to metadata recommendations.
        if (isLessThanMinUserSocialProofSize(
          entry.getValue().getSocialProof(),
          minUserSocialProofSize)) {
          continue;
        }

        if (filtered == null) {
          filtered = new ArrayList<TweetMetadataRecommendationInfo>();
        }
        filtered.add(entry.getValue());
      }

      if (filtered != null) {
        // sort the list of TweetMetadataRecommendationInfo in ascending order
        // according to their weights
        Collections.sort(filtered);
        int toIndex = Math.min(maxNumResults, filtered.size());
        for (int j = 0; j < toIndex; j++) {
          results.add(filtered.get(j));
        }
      }
    }

    return results;
  }
}
