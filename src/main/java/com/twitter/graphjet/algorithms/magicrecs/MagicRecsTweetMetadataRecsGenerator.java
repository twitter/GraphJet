package com.twitter.graphjet.algorithms.magicrecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.TweetIDMask;
import com.twitter.graphjet.algorithms.TweetMetadataRecommendationInfo;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongList;

public final class MagicRecsTweetMetadataRecsGenerator {
  private static final int MIN_USER_SOCIAL_PROOF_SIZE = 1;

  private MagicRecsTweetMetadataRecsGenerator() {
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

  private static boolean isLessThantMinUserSocialProofSize(
    Map<Byte, Map<Long, LongList>> socialProofs,
    int minUserSocialProofSize
  ) {
    // the sum of tweet users and retweet users needs to be greater than a threshold
    int tweetUsers = socialProofs.get((byte) 2) == null ? 0 : socialProofs.get((byte) 2).size();
    int retweetUsers = socialProofs.get((byte) 4) == null ? 0 : socialProofs.get((byte) 4).size();

    return tweetUsers + retweetUsers < minUserSocialProofSize;
  }

  /**
   * Return tweet metadata recommendations, like hashtags and urls
   *
   * @param request            magicRecs request
   * @param nodeInfoList       a list of node info containing engagement social proof and weights
   * @param recommendationType the recommendation type to return, like hashtag and url
   * @return a list of recommendations of the recommendation type
   */
  public static List<RecommendationInfo> generateTweetMetadataRecs(
    MagicRecsRequest request,
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
      List<TweetMetadataRecommendationInfo> filtered = null;

      int minUserSocialProofSize =
        request.getMinUserSocialProofSizes().containsKey(recommendationType)
          ? request.getMinUserSocialProofSizes().get(recommendationType)
          : MIN_USER_SOCIAL_PROOF_SIZE;

      int maxNumResults = request.getMaxNumResultsByType().containsKey(recommendationType)
        ? request.getMaxNumResultsByType().get(recommendationType) : Integer.MAX_VALUE;

      for (Int2ObjectMap.Entry<TweetMetadataRecommendationInfo> entry
        : visitedMetadata.int2ObjectEntrySet()) {
        // handling one specific rule related to metadata recommendations.
        if (isLessThantMinUserSocialProofSize(
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
