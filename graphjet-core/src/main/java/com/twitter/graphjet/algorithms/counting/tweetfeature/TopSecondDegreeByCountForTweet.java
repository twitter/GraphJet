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

import java.util.ArrayList;
import java.util.List;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCount;
import com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCountResponse;
import com.twitter.graphjet.algorithms.counting.tweet.TopSecondDegreeByCountRequestForTweet;
import com.twitter.graphjet.algorithms.filters.RecentTweetFilter;
import com.twitter.graphjet.bipartite.NodeMetadataMultiSegmentIterator;
import com.twitter.graphjet.bipartite.RightNodeMetadataLeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.RightNodeMetadataMultiSegmentIterator;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.hashing.IntArrayIterator;
import com.twitter.graphjet.stats.StatsReceiver;

public class TopSecondDegreeByCountForTweet extends
  TopSecondDegreeByCount<TopSecondDegreeByCountRequestForTweet, TopSecondDegreeByCountResponse> {
  // Max number of node metadata associated with each right node.
  private static final int MAX_NUM_METADATA = 200;
  // Number of features stored in shorts instead of integers.
  private static final int NUM_FEATURE_IN_SHORT_FORMAT = 4;
  // Space ratio between integer and short.
  private static final int SPACE_RATIO_BETWEEN_INTEGER_AND_SHORT = 2;
  // Number of additional integers to unpack features stored in short i16
  private static final int NUM_ADDITIONAL_INTEGER_TO_UNPACK_SHORT =
    NUM_FEATURE_IN_SHORT_FORMAT / SPACE_RATIO_BETWEEN_INTEGER_AND_SHORT;

  /**
   * Initialize all the states needed to run TopSecondDegreeByCountForTweet. Note that the object can
   * be reused for answering many different queries on the same graph, which allows for
   * optimizations such as reusing internally allocated maps etc.
   *
   * @param leftIndexedBipartiteGraph is the
   *                                  {@link RightNodeMetadataLeftIndexedMultiSegmentBipartiteGraph}
   *                                  to run TopSecondDegreeByCountForTweet on
   * @param expectedNodesToHit        is an estimate of how many nodes can be hit in
   *                                  TopSecondDegreeByCountForTweet. This is purely for allocating needed
   *                                  memory right up front to make requests fast.
   * @param statsReceiver             tracks the internal stats
   */
  public TopSecondDegreeByCountForTweet(
    RightNodeMetadataLeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
    int expectedNodesToHit,
    StatsReceiver statsReceiver
  ) {
    super(leftIndexedBipartiteGraph, expectedNodesToHit, statsReceiver);
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
    } else {
      return true;
    }
  }

  private int[][] collectNodeMetadata(long rightNode, RightNodeMetadataMultiSegmentIterator edgeIterator) {
    int metadataSize = TweetFeature.TWEET_FEATURE_SIZE.getValue();
    int[][] nodeMetadata = new int[metadataSize][];
    for (int i = 0; i < metadataSize; i++) {
      IntArrayIterator metadataIterator =
          (IntArrayIterator)edgeIterator.getRightNodeMetadata((byte) i);
      int numOfMetadata = metadataIterator.size();
      if (numOfMetadata > 0 && numOfMetadata <= MAX_NUM_METADATA) {
        // allocate an extra NUM_ADDITIONAL_INTEGER_TO_UNPACK_SHORT integers in the array to hold
        // the integer value of features in short i16.
        int[] metadata = new int[numOfMetadata + NUM_ADDITIONAL_INTEGER_TO_UNPACK_SHORT];

        edgeIterator.fetchFeatureArrayForNode(
          rightNode, i, metadata, NUM_ADDITIONAL_INTEGER_TO_UNPACK_SHORT
        );
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
      RightNodeMetadataMultiSegmentIterator castedIterator = (RightNodeMetadataMultiSegmentIterator)edgeIterator;
      int[][] nodeMetadata = collectNodeMetadata(rightNode, castedIterator);
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

    List<RecommendationInfo> recommendations = new ArrayList<>();

    if (request.getRecommendationTypes().contains(RecommendationType.TWEET)) {
      List<RecommendationInfo> tweetRecommendations =
        TopSecondDegreeByCountTweetRecsGenerator.generateTweetRecs(
          request,
          super.nodeInfosAfterFiltering);
      numTweetResults = tweetRecommendations.size();
      recommendations.addAll(tweetRecommendations);
    }

    LOG.info(getResultLogMessage(request)
      + ", numTweetResults = " + numTweetResults
    );

    return new TopSecondDegreeByCountResponse(recommendations, topSecondDegreeByCountStats);
  }
}
