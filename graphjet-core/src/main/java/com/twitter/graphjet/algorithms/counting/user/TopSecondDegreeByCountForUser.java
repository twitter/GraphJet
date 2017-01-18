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

package com.twitter.graphjet.algorithms.counting.user;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCount;
import com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCountResponse;
import com.twitter.graphjet.bipartite.LeftIndexedPowerLawMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.api.TimestampEdgeIterator;
import com.twitter.graphjet.stats.StatsReceiver;

import java.util.List;

public class TopSecondDegreeByCountForUser extends
  TopSecondDegreeByCount<TopSecondDegreeByCountRequestForUser, TopSecondDegreeByCountResponse> {

  /**
   * Construct a TopSecondDegreeByCount algorithm runner for user related recommendations.
   * @param leftIndexedBipartiteGraph is the
   *                                  {@link LeftIndexedPowerLawMultiSegmentBipartiteGraph}
   *                                  to run TopSecondDegreeByCountForUser on
   * @param expectedNodesToHit        is an estimate of how many nodes can be hit in
   *                                  TopSecondDegreeByCountForUser. This is purely for allocating needed
   *                                  memory right up front to make requests fast.
   * @param statsReceiver             tracks the internal stats
   */
  public TopSecondDegreeByCountForUser(
    LeftIndexedPowerLawMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
    int expectedNodesToHit,
    StatsReceiver statsReceiver) {
    super(leftIndexedBipartiteGraph, expectedNodesToHit, statsReceiver);
  }

  /**
   * Return whether the edge is within the age limit which is specified in the request.
   * It is used to filter information of unwanted edges from being aggregated.
   * @param keepEdgeWithinTime      is the longest time to live for the edge
   * @param edgeIterator            is the iterator being used to iterate node's edges.
   *                                It carries information such as the engagement time of the current edge
   * @return true if this edge is within the max age limit, false otherwise.
   */
  private boolean isEdgeEngagementWithinAgeLimit(long keepEdgeWithinTime, EdgeIterator edgeIterator) {
    long edgeEngagementTime = ((TimestampEdgeIterator)edgeIterator).getCurrentEdgeEngagementTimeInMillis();
    return (edgeEngagementTime >= System.currentTimeMillis() - keepEdgeWithinTime);
  }

  /**
   * Only social proof types specified in the user request are counted
   * For example, a request's social proof types only contain "Follow", and a node has "Follow" and "Mention" edges.
   * Only the "Follow" edge will be counted, and the "Mention" edge is considered invalid
   * @param validEdgeTypes an array of valid types. In User recs there are very few possible types (less than 4),
   *                       so it is okay to iterate
   * @param edgeType       the edge type being validated
   */
  private boolean isEdgeTypeValid(byte[] validEdgeTypes, byte edgeType) {
    for (byte validType: validEdgeTypes) {
      if (edgeType == validType) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected boolean isEdgeUpdateValid(TopSecondDegreeByCountRequestForUser request, EdgeIterator edgeIterator) {
    // Do not update on expired edges or invalid edge types
    return (isEdgeTypeValid(request.getSocialProofTypes(), edgeIterator.currentEdgeType()) &&
      isEdgeEngagementWithinAgeLimit(request.getMaxEdgeEngagementAgeInMillis(), edgeIterator));
  }

  @Override
  protected void updateNodeInfo(
    long leftNode,
    long rightNode,
    byte edgeType,
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
    nodeInfo.addToSocialProof(leftNode, edgeType, weight);
  }

  @Override
  public TopSecondDegreeByCountResponse generateRecommendationFromNodeInfo(
    TopSecondDegreeByCountRequestForUser request) {
    List<RecommendationInfo> userRecommendations =
      TopSecondDegreeByCountUserRecsGenerator.generateUserRecs(
        request,
        super.nodeInfosAfterFiltering);

    LOG.info(getResultLogMessage(request)
      + ", numUserResults = " + userRecommendations.size()
      + ", totalResults = " + userRecommendations.size());
    return new TopSecondDegreeByCountResponse(userRecommendations, topSecondDegreeByCountStats);
  }
}
