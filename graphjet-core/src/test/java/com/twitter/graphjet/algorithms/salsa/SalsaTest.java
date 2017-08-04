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
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.algorithms.BipartiteGraphTestHelper;
import com.twitter.graphjet.algorithms.ConnectingUsersWithMetadata;
import com.twitter.graphjet.algorithms.filters.DirectInteractionsFilter;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.filters.RequestedSetFilter;
import com.twitter.graphjet.algorithms.filters.ResultFilter;
import com.twitter.graphjet.algorithms.filters.ResultFilterChain;
import com.twitter.graphjet.algorithms.counting.tweet.TweetRecommendationInfo;
import com.twitter.graphjet.algorithms.salsa.fullgraph.Salsa;
import com.twitter.graphjet.algorithms.salsa.subgraph.SubgraphSalsa;
import com.twitter.graphjet.bipartite.SmallLeftRegularBipartiteGraph;
import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class SalsaTest {
  @Test
  public void testSalsa() throws Exception {
    testSalsaWithGraph(BipartiteGraphTestHelper.buildSmallTestBipartiteGraphSegment());
    testSalsaWithGraph(BipartiteGraphTestHelper.buildSmallTestBipartiteGraph());
  }

  private void testSalsaWithGraph(BipartiteGraph bipartiteGraph) {
    long queryNode = 1;
    LongSet toBeFiltered = new LongOpenHashSet(new long[]{3});
    int numIterations = 5;
    double resetProbability = 0.3;
    int numResults = 3;
    int numRandomWalks = 1000;
    int maxSocialProofSize = 2;
    int maxSocialProofTypeSize = 4;
    long randomSeed = 918324701982347L;
    int expectedNodesToHit = numRandomWalks * numIterations * 10;
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
            .withMaxSocialProofTypeSize(maxSocialProofTypeSize)
            .withResultFilterChain(resultFilterChain)
            .build();

    LongList metadata1 = new LongArrayList(new long[]{0});
    LongList metadata2 = new LongArrayList(new long[]{0, 0});
    ArrayList<HashMap<Byte, ConnectingUsersWithMetadata>> socialProof = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      socialProof.add(new HashMap<>());
    }
    socialProof.get(0).put((byte) 0, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{3, 2}), metadata2));
    socialProof.get(1).put((byte) 0, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{3}), metadata1));

    final List<RecommendationInfo> expectedTopResults = new ArrayList<RecommendationInfo>();
    expectedTopResults.add(new TweetRecommendationInfo(5, 0.2346316283435007, socialProof.get(0)));
    expectedTopResults.add(new TweetRecommendationInfo(2, 0.22430783669638668, socialProof.get(1)));
    expectedTopResults.add(new TweetRecommendationInfo(4, 0.20835288596902862, socialProof.get(2)));
    final SalsaStats expectedSalsaStats = new SalsaStats(1, 4, 11, 2131, 1, 500, 1);

    // Should be in sorted order of weight
    Salsa salsa = new Salsa(
        bipartiteGraph,
        expectedNodesToHit,
        new NullStatsReceiver());
    Random random = new Random(randomSeed);
    SalsaResponse salsaResponse = salsa.computeRecommendations(salsaRequest, random);
    List<RecommendationInfo> salsaResults =
        Lists.newArrayList(salsaResponse.getRankedRecommendations());

    assertEquals(expectedSalsaStats, salsaResponse.getSalsaStats());
    assertEquals(expectedTopResults, salsaResults);

    // Things should reset properly on a rerun
    random.setSeed(randomSeed);
    salsaResponse = salsa.computeRecommendations(salsaRequest, random);
    salsaResults = Lists.newArrayList(salsaResponse.getRankedRecommendations());

    assertEquals(expectedSalsaStats, salsaResponse.getSalsaStats());
    assertEquals(expectedTopResults, salsaResults);

    // try running the subgraph version
    SubgraphSalsa subgraphSalsa = new SubgraphSalsa(
        bipartiteGraph,
        expectedNodesToHit,
        1,
        new NullStatsReceiver());

    salsaRequest =
        new SalsaRequestBuilder(queryNode)
            .withLeftSeedNodes(new Long2DoubleOpenHashMap(new long[]{2}, new double[]{1.0}))
            .withToBeFiltered(toBeFiltered)
            .withMaxNumResults(numResults)
            .withResetProbability(resetProbability)
            .withMaxRandomWalkLength(numIterations)
            .withNumRandomWalks(numRandomWalks)
            .withMaxSocialProofSize(maxSocialProofSize)
            .withResultFilterChain(resultFilterChain)
            .build();

    ArrayList<HashMap<Byte, ConnectingUsersWithMetadata>> subSocialProof = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      subSocialProof.add(new HashMap<>());
    }
    subSocialProof.get(0).put((byte) 0, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{2}), metadata1));

    final List<RecommendationInfo> expectedTopResultsSubgraph = new ArrayList<RecommendationInfo>();
    expectedTopResultsSubgraph.add(
      new TweetRecommendationInfo(5, 0.3561333333333357, subSocialProof.get(0)));
    expectedTopResultsSubgraph.add(
      new TweetRecommendationInfo(2, 0.29866666666666786, subSocialProof.get(1)));
    expectedTopResultsSubgraph.add(
      new TweetRecommendationInfo(4, 0.2906666666666677, subSocialProof.get(2)));

    final SalsaStats expectedSalsaStatsSubgraph = new SalsaStats(2, 4, 6, 5000, 1, 810, 1);

    random.setSeed(randomSeed);
    salsaResponse = subgraphSalsa.computeRecommendations(salsaRequest, random);
    salsaResults =
        Lists.newArrayList(salsaResponse.getRankedRecommendations());

    assertEquals(expectedSalsaStatsSubgraph, salsaResponse.getSalsaStats());
    assertEquals(expectedTopResultsSubgraph, salsaResults);

    SubgraphSalsa subgraphSalsaLeftIndexedGraph = new SubgraphSalsa(
        BipartiteGraphTestHelper.buildSmallTestLeftIndexedBipartiteGraph(),
        expectedNodesToHit,
        1,
        new NullStatsReceiver());

    random.setSeed(randomSeed);
    salsaResponse = subgraphSalsaLeftIndexedGraph.computeRecommendations(salsaRequest, random);
    salsaResults =
        Lists.newArrayList(salsaResponse.getRankedRecommendations());

    assertEquals(expectedSalsaStatsSubgraph, salsaResponse.getSalsaStats());
    assertEquals(expectedTopResultsSubgraph, salsaResults);
  }

  @Test
  public void testSalsaWithRandomGraph() throws Exception {
    Random random = new Random(12653467345382L);

    int maxNumLeftNodes = 256;
    int leftDegree = 64;
    int maxNumRightNodes = maxNumLeftNodes * leftDegree;
    double rightPowerLawExponent = 2.0;

    SmallLeftRegularBipartiteGraph smallLeftRegularBipartiteGraph =
        new SmallLeftRegularBipartiteGraph(
            maxNumLeftNodes,
            leftDegree,
            maxNumRightNodes,
            maxNumLeftNodes,
            rightPowerLawExponent,
            Integer.MAX_VALUE,
            new NullStatsReceiver());

    double queryNodeWeightFraction = 0.9;
    int maxNumResults = 3;
    double resetProbability = 0.3;
    int maxRandomWalkLength = 5;
    int numRandomWalks = 10000;
    int maxSocialProofSize = 2;
    int maxSocialProofTypeSize = 4;
    int expectedNodesToHit = numRandomWalks * maxRandomWalkLength * 10;
    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.newArrayList(
        new RequestedSetFilter(new NullStatsReceiver()),
        new DirectInteractionsFilter(smallLeftRegularBipartiteGraph, new NullStatsReceiver())
    ));

    int maxUserId = 1000;

    final SalsaStats expectedSalsaStats = new SalsaStats(1, 64, 998, 21050, 1, 227, 64);

    LongList metadata7 = new LongArrayList(new long[]{0, 0, 0, 0, 0, 0, 0});
    LongList metadata10 = new LongArrayList(new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
    ArrayList<HashMap<Byte, ConnectingUsersWithMetadata>> socialProof = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      socialProof.add(new HashMap<>());
    }
    socialProof.get(0).put(
      (byte) 0, new ConnectingUsersWithMetadata(
        new LongArrayList(new long[]{718, 889, 109, 164, 207, 767, 302, 888, 453, 738}),
        metadata10
      )
    );
    socialProof.get(1).put(
      (byte) 0, new ConnectingUsersWithMetadata(
        new LongArrayList(new long[]{47, 96, 499, 306, 396, 805, 351, 875, 308, 186}),
        metadata10
      )
    );
    socialProof.get(2).put(
      (byte) 0, new ConnectingUsersWithMetadata(
        new LongArrayList(new long[]{623, 880, 550, 363, 886, 156, 130}),
        metadata7
      )
    );

    final List<RecommendationInfo> expectedTopResults = new ArrayList<RecommendationInfo>();
    expectedTopResults.add(new TweetRecommendationInfo(735, 0.0010926365795724466,
      socialProof.get(0)));
    expectedTopResults.add(new TweetRecommendationInfo(119, 0.0010451306413301663,
      socialProof.get(1)));
    expectedTopResults.add(new TweetRecommendationInfo(70, 0.0010451306413301663,
      socialProof.get(2)));

    Set<Long> sourceIdList = Sets.newHashSetWithExpectedSize(maxNumLeftNodes);
    Set<Long> destinationIds = Sets.newHashSetWithExpectedSize(leftDegree);

    smallLeftRegularBipartiteGraph.reset();
    long userId = random.nextInt(maxUserId);

    SalsaRequest salsaRequest =
        new SalsaRequestBuilder(userId)
            .withQueryNodeWeightFraction(queryNodeWeightFraction)
            .withMaxNumResults(maxNumResults)
            .withResetProbability(resetProbability)
            .withMaxRandomWalkLength(maxRandomWalkLength)
            .withNumRandomWalks(numRandomWalks)
            .withMaxSocialProofSize(maxSocialProofSize)
            .withMaxSocialProofTypeSize(maxSocialProofTypeSize)
            .withResultFilterChain(resultFilterChain)
            .build();

    sourceIdList.clear();
    for (int i = 1; i <= maxNumLeftNodes; i++) {
      sourceIdList.add((long) random.nextInt(maxUserId));
    }
    sourceIdList.add(userId);

    for (long sourceId : sourceIdList) {
      destinationIds.clear();
      for (int i = 1; i <= leftDegree; i++) {
        destinationIds.add((long) random.nextInt(maxUserId));
      }
      for (long destinationId : destinationIds) {
        smallLeftRegularBipartiteGraph.addEdge(sourceId, destinationId, (byte) 0);
      }
    }

    Salsa salsa = new Salsa(
        smallLeftRegularBipartiteGraph,
        expectedNodesToHit,
        new NullStatsReceiver());
    SalsaResponse salsaResponse = salsa.computeRecommendations(salsaRequest, random);
    List<RecommendationInfo> salsaResults =
        Lists.newArrayList(salsaResponse.getRankedRecommendations());

    assertEquals(expectedSalsaStats, salsaResponse.getSalsaStats());
    assertEquals(expectedTopResults, salsaResults);
  }


  @Test
  public void testSalsaWithLargeGraph() throws Exception {
    long queryNode = 9;
    int leftSize = 100;
    int rightSize = 1000;
    double edgeProbability = 0.3;
    Random random = new Random(918324701982347L);
    BipartiteGraph bipartiteGraph = BipartiteGraphTestHelper.buildRandomBipartiteGraph(
            leftSize, rightSize, edgeProbability, random);
    LongSet toBeFiltered = new LongOpenHashSet(new long[]{881});
    int numIterations = 5;
    double resetProbability = 0.3;
    int numResults = 3;
    int numRandomWalks = 10000;
    int maxSocialProofSize = 3;
    int expectedNodesToHit = numRandomWalks * numIterations / 2;
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

    LongList metadata7 = new LongArrayList(new long[]{0, 0, 0, 0, 0, 0, 0});
    LongList metadata8 = new LongArrayList(new long[]{0, 0, 0, 0, 0, 0, 0, 0});
    LongList metadata9 = new LongArrayList(new long[]{0, 0, 0, 0, 0, 0, 0, 0, 0});
    ArrayList<HashMap<Byte, ConnectingUsersWithMetadata>> socialProof = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      socialProof.add(new HashMap<>());
    }
    socialProof.get(0).put(
      (byte) 0, new ConnectingUsersWithMetadata(
        new LongArrayList(new long[]{79, 51, 19, 13, 97, 56, 36, 2, 22}),
        metadata9
      )
    );
    socialProof.get(1).put(
      (byte) 0, new ConnectingUsersWithMetadata(
        new LongArrayList(new long[]{44, 77, 13, 63, 16, 5, 43, 90}),
        metadata8
      )
    );
    socialProof.get(2).put(
      (byte) 0, new ConnectingUsersWithMetadata(
        new LongArrayList(new long[]{21, 65, 3, 8, 12, 38, 40}),
        metadata7
      )
    );

    final List<RecommendationInfo> expectedTopResults = new ArrayList<RecommendationInfo>();
    expectedTopResults.add(
      new TweetRecommendationInfo(704, 0.0037072243346007606, socialProof.get(0)));
    expectedTopResults.add(
      new TweetRecommendationInfo(509, 0.003326996197718631, socialProof.get(1)));
    expectedTopResults.add(
      new TweetRecommendationInfo(190, 0.003279467680608365, socialProof.get(2)));

    final SalsaStats expectedSalsaStats = new SalsaStats(1, 266, 999, 21040, 1, 78, 1);

    SalsaResponse salsaResponse = new Salsa(
        bipartiteGraph,
        expectedNodesToHit,
        new NullStatsReceiver())
        .computeRecommendations(salsaRequest, random);
    // Should be in sorted order of weight
    List<RecommendationInfo> salsaResults =
        Lists.newArrayList(salsaResponse.getRankedRecommendations());

    assertEquals(expectedTopResults, salsaResults);
    assertEquals(expectedSalsaStats, salsaResponse.getSalsaStats());
  }
}
