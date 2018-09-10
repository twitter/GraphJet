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


package com.twitter.graphjet.algorithms.salsa;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twitter.graphjet.algorithms.ConnectingUsersWithMetadata;
import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.TweetIDMask;
import com.twitter.graphjet.algorithms.counting.tweet.TweetRecommendationInfo;
import com.twitter.graphjet.bipartite.api.LeftIndexedBipartiteGraph;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

import it.unimi.dsi.fastutil.longs.LongArrayList;

/**
 * This class selects the top recommendations from a SALSA run.
 */
public class SalsaSelectResults<T extends LeftIndexedBipartiteGraph> {
  private static final Logger LOG = LoggerFactory.getLogger("graph");
  private static final TweetIDMask TWEET_ID_MASK = new TweetIDMask();

  private final CommonInternalState<T> salsaInternalState;
  private final SalsaStats salsaStats;

  /**
   * Default constructor that requires a {@link CommonInternalState} populated with visited
   * information to extract recommendations.
   *
   * @param salsaInternalState  is the state populated with visited information
   */
  public SalsaSelectResults(CommonInternalState<T> salsaInternalState) {
    this.salsaInternalState = salsaInternalState;
    this.salsaStats = salsaInternalState.getSalsaStats();
  }

  /**
   * Picks the top-k visited nodes in SALSA.
   */
  public SalsaResponse pickTopNodes() {
    int maxNumResults = Math.min(salsaInternalState.getSalsaRequest().getMaxNumResults(),
      RecommendationRequest.MAX_RECOMMENDATION_RESULTS);
    PriorityQueue<NodeInfo> topResults = new PriorityQueue<NodeInfo>(maxNumResults);

    int numFilteredNodes = 0;
    for (NodeInfo nodeInfo : salsaInternalState.getVisitedRightNodes().values()) {
      if (salsaInternalState.getSalsaRequest().filterResult(
        nodeInfo.getNodeId(),
        nodeInfo.getSocialProofs())
      ) {
        numFilteredNodes++;
        continue;
      }
      nodeInfo.setWeight(
          nodeInfo.getWeight() / salsaInternalState.getSalsaStats().getNumRHSVisits());
      addResultToPriorityQueue(topResults, nodeInfo, maxNumResults);
    }

    List<RecommendationInfo> outputResults =
        Lists.newArrayListWithCapacity(topResults.size());

    byte[] validSocialProofs = salsaInternalState.getSalsaRequest().getSocialProofTypes();

    while (!topResults.isEmpty()) {
      NodeInfo nodeInfo = topResults.poll();
      outputResults.add(
        new TweetRecommendationInfo(
          TWEET_ID_MASK.restore(nodeInfo.getNodeId()),
          nodeInfo.getWeight(),
          pickTopSocialProofs(nodeInfo.getSocialProofs(), validSocialProofs)));
    }
    Collections.reverse(outputResults);

    salsaStats.setNumRightNodesFiltered(numFilteredNodes);
    salsaStats.setNumRightNodesReached(salsaInternalState.getVisitedRightNodes().size());

    LOG.info("SALSA: after running iterations for request_id = "
        + salsaInternalState.getSalsaRequest().getQueryNode()
        + ", we get numSeedNodes = "
        + salsaStats.getNumSeedNodes()
        + ", numDirectNeighbors = "
        + salsaStats.getNumDirectNeighbors()
        + ", numRHSVisits = "
        + salsaStats.getNumRHSVisits()
        + ", numRightNodesReached = "
        + salsaStats.getNumRightNodesReached()
        + ", numRightNodesFiltered = "
        + salsaStats.getNumRightNodesFiltered()
        + ", minVisitsPerRightNode = "
        + salsaStats.getMinVisitsPerRightNode()
        + ", maxVisitsPerRightNode = "
        + salsaStats.getMaxVisitsPerRightNode()
        + ", numOutputResults = "
        + outputResults.size()
    );

    return new SalsaResponse(outputResults, salsaStats);
  }

  /**
   * Pick the top social proofs for each RHS node
   */
  private Map<Byte, ConnectingUsersWithMetadata> pickTopSocialProofs(
    SmallArrayBasedLongToDoubleMap[] socialProofs,
    byte[] validSocialProofs
  ) {
    Map<Byte, ConnectingUsersWithMetadata> results = new HashMap<Byte, ConnectingUsersWithMetadata>();
    int length = validSocialProofs.length;

    for (int i = 0; i < length; i++) {
      SmallArrayBasedLongToDoubleMap socialProof = socialProofs[validSocialProofs[i]];
      if (socialProof != null) {
        if (socialProof.size() > 1) {
          socialProof.sort();
        }

        socialProof.trim(socialProof.size());
        results.put((byte) i, new ConnectingUsersWithMetadata(
          new LongArrayList(socialProof.keys()),
          new LongArrayList(socialProof.metadata())
        ));
      }
    }
    return results;
  }

  private void addResultToPriorityQueue(
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
}
