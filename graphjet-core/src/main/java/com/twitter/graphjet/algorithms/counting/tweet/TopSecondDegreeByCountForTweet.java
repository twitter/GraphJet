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

package com.twitter.graphjet.algorithms.counting.tweet;

import java.util.ArrayList;
import java.util.List;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.filters.RecentTweetFilter;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCount;
import com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCountResponse;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.NodeMetadataMultiSegmentIterator;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.hashing.IntArrayIterator;
import com.twitter.graphjet.stats.StatsReceiver;

public class TopSecondDegreeByCountForTweet extends
  TopSecondDegreeByCount<TopSecondDegreeByCountRequestForTweet, TopSecondDegreeByCountResponse> {
  // Max number of node metadata associated with each right node.
  private static final int MAX_NUM_METADATA = 200;

  /**
   * Initialize all the states needed to run TopSecondDegreeByCountForTweet. Note that the object can
   * be reused for answering many different queries on the same graph, which allows for
   * optimizations such as reusing internally allocated maps etc.
   *
   * @param leftIndexedBipartiteGraph is the
   *                                  {@link NodeMetadataLeftIndexedMultiSegmentBipartiteGraph}
   *                                  to run TopSecondDegreeByCountForTweet on
   * @param expectedNodesToHit        is an estimate of how many nodes can be hit in
   *                                  TopSecondDegreeByCountForTweet. This is purely for allocating needed
   *                                  memory right up front to make requests fast.
   * @param statsReceiver             tracks the internal stats
   */
  public TopSecondDegreeByCountForTweet(
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
    int expectedNodesToHit,
    StatsReceiver statsReceiver
  ) {
    super(leftIndexedBipartiteGraph, expectedNodesToHit, statsReceiver);
  }

  private long getEdgeTimeStampInMillis(
    long timeStampFromTweetId,
    byte edgeType,
    long edgeMetadata
  ) {
    switch (edgeType) {
      case 0:  // CLICK
        return edgeMetadata;
      case 1:  // FAVORITE
        return edgeMetadata;
      case 2:  // RETWEET
        return RecentTweetFilter.timeStampFromTweetId(edgeMetadata);
      case 3:  // REPLY
        return RecentTweetFilter.timeStampFromTweetId(edgeMetadata);
      case 4:  // TWEET
        return timeStampFromTweetId;
      case 5:  // IS_MENTIONED
        return timeStampFromTweetId;
      case 6:  // IS_MEDIATAGGED
        return timeStampFromTweetId;
      case 7:  // QUOTE
        return RecentTweetFilter.timeStampFromTweetId(edgeMetadata);
      case 8:  // UNFAVORITE
        return edgeMetadata;
      default:
        throw new IllegalStateException("Invalid EdgeType in getEdgeTimeStampInMillis: " + edgeType);
    }
  }

  @Override
  protected boolean isEdgeUpdateValid(
    TopSecondDegreeByCountRequestForTweet request,
    long rightNode,
    byte edgeType,
    long edgeMetadata
    ) {
    long timeStampFromTweetId = RecentTweetFilter.originalTimeStampFromTweetId(rightNode);
    // if the timestamp of the right node exceeds the right node age limit;
    if (timeStampFromTweetId < (System.currentTimeMillis() - request.getMaxRightNodeAgeInMillis())) {
      return false;
    }

    // if the timestamp of the edge exceeds the edge age limit;
    return isEdgeEngagementWithinAgeLimit(
      getEdgeTimeStampInMillis(timeStampFromTweetId, edgeType, edgeMetadata),
      request.getMaxEdgeAgeInMillis()
    );
  }

  private int[][] collectNodeMetadata(EdgeIterator edgeIterator) {
    int metadataSize = RecommendationType.METADATASIZE.getValue();
    int[][] nodeMetadata = new int[metadataSize][];
    for (int i = 0; i < metadataSize; i++) {
      IntArrayIterator metadataIterator =
          (IntArrayIterator) ((NodeMetadataMultiSegmentIterator)edgeIterator).getRightNodeMetadata((byte) i);
      int numOfMetadata = metadataIterator.size();
      if (numOfMetadata > 0 && numOfMetadata <= MAX_NUM_METADATA) {
        int[] metadata = new int[numOfMetadata];
        int j = 0;
        while (metadataIterator.hasNext()) {
          metadata[j++] = metadataIterator.nextInt();
        }
        nodeMetadata[i] = metadata;
      }
    }
    return nodeMetadata;
  }

  @Override
  protected void updateNodeInfo(
    long leftNode,
    long rightNode,
    byte edgeType,
    long edgeMetadata,
    double weight,
    EdgeIterator edgeIterator,
    int maxSocialProofTypeSize) {

    NodeInfo nodeInfo;
    if (!super.visitedRightNodes.containsKey(rightNode)) {
      int[][] nodeMetadata = collectNodeMetadata(edgeIterator);
      nodeInfo = new NodeInfo(rightNode, nodeMetadata, 0.0, maxSocialProofTypeSize);
      super.visitedRightNodes.put(rightNode, nodeInfo);
    } else {
      nodeInfo = super.visitedRightNodes.get(rightNode);
    }

    nodeInfo.addToWeight(weight);
    nodeInfo.addToSocialProof(leftNode, edgeType, edgeMetadata, weight);
  }

  @Override
  public TopSecondDegreeByCountResponse generateRecommendationFromNodeInfo(
    TopSecondDegreeByCountRequestForTweet request) {
    int numTweetResults = 0;
    int numHashtagResults = 0;
    int numUrlResults = 0;

    List<RecommendationInfo> recommendations = new ArrayList<>();

    if (request.getRecommendationTypes().contains(RecommendationType.TWEET)) {
      List<RecommendationInfo> tweetRecommendations =
        TopSecondDegreeByCountTweetRecsGenerator.generateTweetRecs(
          request,
          super.nodeInfosAfterFiltering);
      numTweetResults = tweetRecommendations.size();
      recommendations.addAll(tweetRecommendations);
    }

    if (request.getRecommendationTypes().contains(RecommendationType.HASHTAG)) {
      List<RecommendationInfo> hashtagRecommendations =
        TopSecondDegreeByCountTweetMetadataRecsGenerator.generateTweetMetadataRecs(
          request,
          super.nodeInfosAfterFiltering,
          RecommendationType.HASHTAG);
      numHashtagResults = hashtagRecommendations.size();
      recommendations.addAll(hashtagRecommendations);
    }

    if (request.getRecommendationTypes().contains(RecommendationType.URL)) {
      List<RecommendationInfo> urlRecommendations =
        TopSecondDegreeByCountTweetMetadataRecsGenerator.generateTweetMetadataRecs(
          request,
          super.nodeInfosAfterFiltering,
          RecommendationType.URL);
      numUrlResults = urlRecommendations.size();
      recommendations.addAll(urlRecommendations);
    }

    LOG.info(getResultLogMessage(request)
      + ", numTweetResults = " + numTweetResults
      + ", numHashtagResults = " + numHashtagResults
      + ", numUrlResults = " + numUrlResults
      + ", totalResults = " + (numTweetResults + numHashtagResults + numUrlResults)
    );

    return new TopSecondDegreeByCountResponse(recommendations, topSecondDegreeByCountStats);
  }
}
