/**
 * Copyright 2017 Twitter. All rights reserved.
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

package com.twitter.graphjet.algorithms.counting.moment;

import java.util.List;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCount;
import com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCountResponse;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.stats.StatsReceiver;

public class TopSecondDegreeByCountForMoment extends
  TopSecondDegreeByCount<TopSecondDegreeByCountRequestForMoment, TopSecondDegreeByCountResponse> {

  /**
   * Construct a TopSecondDegreeByCount algorithm runner for moment recommendations.
   * @param leftIndexedBipartiteGraph is the
   *                                  {@link NodeMetadataLeftIndexedMultiSegmentBipartiteGraph}
   *                                  to run TopSecondDegreeByCountForMoment on
   * @param expectedNodesToHit        is an estimate of how many nodes can be hit in
   *                                  TopSecondDegreeByCountForMoment. This is purely for allocating needed
   *                                  memory right up front to make requests fast.
   * @param statsReceiver             tracks the internal stats
   */
  public TopSecondDegreeByCountForMoment(
      NodeMetadataLeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
    int expectedNodesToHit,
    StatsReceiver statsReceiver) {
    super(leftIndexedBipartiteGraph, expectedNodesToHit, statsReceiver);
  }

  @Override
  protected boolean isEdgeUpdateValid(
    TopSecondDegreeByCountRequestForMoment request,
    long rightNode,
    byte edgeType,
    long edgeMetadata
  ) {
    return isEdgeEngagementWithinAgeLimit(edgeMetadata, request.getMaxEdgeAgeInMillis());
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
      nodeInfo = new NodeInfo(rightNode, 0.0, maxSocialProofTypeSize);
      super.visitedRightNodes.put(rightNode, nodeInfo);
    } else {
      nodeInfo = super.visitedRightNodes.get(rightNode);
    }
    nodeInfo.addToWeight(weight);
    nodeInfo.addToSocialProof(leftNode, edgeType, edgeMetadata, weight);
  }

  @Override
  public TopSecondDegreeByCountResponse generateRecommendationFromNodeInfo(
      TopSecondDegreeByCountRequestForMoment request) {
    List<RecommendationInfo> momentRecommendations =
      TopSecondDegreeByCountMomentRecsGenerator.generateMomentRecs(
        request,
        super.nodeInfosAfterFiltering);

    LOG.info(getResultLogMessage(request)
      + ", numMomentResults = " + momentRecommendations.size()
      + ", totalResults = " + momentRecommendations.size());
    return new TopSecondDegreeByCountResponse(momentRecommendations, topSecondDegreeByCountStats);
  }
}
