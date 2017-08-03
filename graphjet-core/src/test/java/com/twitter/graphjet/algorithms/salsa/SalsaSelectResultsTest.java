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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.algorithms.BipartiteGraphTestHelper;
import com.twitter.graphjet.algorithms.ConnectingUsersWithMetadata;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.filters.RequestedSetFilter;
import com.twitter.graphjet.algorithms.filters.ResultFilter;
import com.twitter.graphjet.algorithms.filters.ResultFilterChain;
import com.twitter.graphjet.algorithms.counting.tweet.TweetRecommendationInfo;
import com.twitter.graphjet.algorithms.salsa.fullgraph.SalsaInternalState;
import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class SalsaSelectResultsTest {
  @Test
  public void testPickTopNodes() {
    long queryNode = 1;
    BipartiteGraph bipartiteGraph = BipartiteGraphTestHelper.buildSmallTestBipartiteGraph();
    LongSet toBeFiltered = new LongOpenHashSet(new long[]{8});
    int numIterations = 5;
    double resetProbability = 0.3;
    int numResults = 3;
    int numRandomWalks = 1000;
    int maxSocialProofSize = 2;
    int expectedNodesToHit = numRandomWalks * numIterations / 2;
    SalsaStats salsaStats = new SalsaStats(1, 4, 3, 6, 1, 3, 0);
    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.<ResultFilter>newArrayList(
        new RequestedSetFilter(new NullStatsReceiver())
    ));

    SalsaRequest salsaRequest =
        new SalsaRequestBuilder(queryNode)
            .withLeftSeedNodes(null)
            .withToBeFiltered(toBeFiltered)
            .withMaxNumResults(numResults)
            .withResetProbability(resetProbability)
            .withMaxRandomWalkLength(numIterations)
            .withNumRandomWalks(numRandomWalks)
            .withMaxSocialProofSize(maxSocialProofSize)
            .withResultFilterChain(resultFilterChain)
            .build();

    SalsaInternalState salsaInternalState =
        new SalsaInternalState(bipartiteGraph, salsaStats, expectedNodesToHit);
    salsaInternalState.resetWithRequest(salsaRequest);
    salsaInternalState.getSalsaStats().setNumSeedNodes(1);
    salsaInternalState.getSalsaStats().setNumDirectNeighbors(4);
    salsaInternalState.getSalsaStats().setNumRightNodesReached(3);
    salsaInternalState.getSalsaStats().setNumRHSVisits(6);
    salsaInternalState.getSalsaStats().setMinVisitsPerRightNode(1);
    salsaInternalState.getSalsaStats().setMaxVisitsPerRightNode(3);
    salsaInternalState.getSalsaStats().setNumRightNodesFiltered(0);

    SalsaSelectResults<BipartiteGraph> salsaSelectResults =
        new SalsaSelectResults<BipartiteGraph>(salsaInternalState);

    SalsaNodeVisitor.NodeVisitor nodeVisitor =
        new SalsaNodeVisitor.NodeVisitorWithSocialProof(
            salsaInternalState.getVisitedRightNodes());
    nodeVisitor.resetWithRequest(salsaRequest);

    salsaInternalState.visitRightNode(nodeVisitor, 1, 4, (byte) 0, 0L, 1);
    salsaInternalState.visitRightNode(nodeVisitor, 2, 4, (byte) 0, 0L, 2);
    salsaInternalState.visitRightNode(nodeVisitor, 3, 4, (byte) 0, 0L, 3);
    salsaInternalState.visitRightNode(nodeVisitor, 3, 8, (byte) 0, 0L, 9);
    salsaInternalState.visitRightNode(nodeVisitor, 1, 2, (byte) 0, 0L, 1);
    salsaInternalState.visitRightNode(nodeVisitor, 4, 2, (byte) 0, 0L, 1);

    LongList metadata1 = new LongArrayList(new long[]{0});
    LongList metadata2 = new LongArrayList(new long[]{0, 0});
    ArrayList<HashMap<Byte, ConnectingUsersWithMetadata>> socialProof = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      socialProof.add(new HashMap<>());
    }
    socialProof.get(0).put((byte) 0, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{3, 2}), metadata2));
    socialProof.get(1).put((byte) 0, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{4}), metadata1));

    final List<RecommendationInfo> expectedTopResults = new ArrayList<RecommendationInfo>();
    expectedTopResults.add(new TweetRecommendationInfo(4, 0.5, socialProof.get(0)));
    expectedTopResults.add(new TweetRecommendationInfo(2, 0.3333333333333333, socialProof.get(1)));
    SalsaStats expectedTopSalsaStats = new SalsaStats(1, 4, 3, 6, 1, 3, 1);

    List<RecommendationInfo> salsaResults =
        Lists.newArrayList(salsaSelectResults.pickTopNodes().getRankedRecommendations());

    assertEquals(expectedTopResults, salsaResults);
    assertEquals(expectedTopSalsaStats, salsaStats);
  }

}
