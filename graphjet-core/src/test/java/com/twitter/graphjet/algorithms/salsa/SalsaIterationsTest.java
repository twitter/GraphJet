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

import java.util.Random;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.twitter.graphjet.algorithms.BipartiteGraphTestHelper;
import com.twitter.graphjet.algorithms.filters.DirectInteractionsFilter;
import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.filters.RequestedSetFilter;
import com.twitter.graphjet.algorithms.filters.ResultFilterChain;
import com.twitter.graphjet.algorithms.salsa.fullgraph.FinalSalsaIteration;
import com.twitter.graphjet.algorithms.salsa.fullgraph.LeftSalsaIteration;
import com.twitter.graphjet.algorithms.salsa.fullgraph.RightSalsaIteration;
import com.twitter.graphjet.algorithms.salsa.fullgraph.SalsaInternalState;
import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class SalsaIterationsTest {
  private SalsaRequest salsaRequest;
  private SalsaRequest salsaRequestSmallSeed;
  private SalsaInternalState salsaInternalState;
  private SalsaInternalState salsaInternalStateSmallSeed;
  private Random random;
  private byte[] socialProofTypes;

  @Before
  public void setUp() {
    long queryNode = 1;
    BipartiteGraph bipartiteGraph = BipartiteGraphTestHelper.buildSmallTestBipartiteGraph();
    Long2DoubleMap seedSetWeights = new Long2DoubleOpenHashMap(3);
    seedSetWeights.put(2, 10.0);
    seedSetWeights.put(3, 1.0);
    LongSet toBeFiltered = new LongOpenHashSet(new long[]{8});
    int numIterations = 5;
    double resetProbability = 0.3;
    int numResults = 3;
    int numRandomWalks = 1000;
    int maxSocialProofSize = 2;
    double queryNodeWeightFraction = 0.9;
    int expectedNodesToHit = numRandomWalks * numIterations / 2;
    random = new Random(541454153145614L);
    socialProofTypes = new byte[]{0, 1};
    SalsaStats salsaStats = new SalsaStats();
    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.newArrayList(
        new RequestedSetFilter(new NullStatsReceiver()),
        new DirectInteractionsFilter(bipartiteGraph, new NullStatsReceiver())
    ));

    salsaRequest =
        new SalsaRequestBuilder(queryNode)
            .withLeftSeedNodes(seedSetWeights)
            .withToBeFiltered(toBeFiltered)
            .withMaxNumResults(numResults)
            .withResetProbability(resetProbability)
            .withMaxRandomWalkLength(numIterations)
            .withNumRandomWalks(numRandomWalks)
            .withMaxSocialProofSize(maxSocialProofSize)
            .withValidSocialProofTypes(socialProofTypes)
            .withQueryNodeWeightFraction(queryNodeWeightFraction)
            .withResultFilterChain(resultFilterChain)
            .build();

    salsaInternalState = new SalsaInternalState(
        bipartiteGraph, salsaStats, expectedNodesToHit);
    salsaInternalState.resetWithRequest(salsaRequest);

    salsaRequestSmallSeed =
        new SalsaRequestBuilder(queryNode)
            // This seed set should be ignored
            .withLeftSeedNodes(new Long2DoubleOpenHashMap(new long[]{5}, new double[]{1.0}))
            .withToBeFiltered(toBeFiltered)
            .withMaxNumResults(numResults)
            .withResetProbability(resetProbability)
            .withMaxRandomWalkLength(numIterations)
            .withNumRandomWalks(numRandomWalks)
            .withMaxSocialProofSize(maxSocialProofSize)
            .withValidSocialProofTypes(new byte[]{0, 1})
            .withQueryNodeWeightFraction(queryNodeWeightFraction)
            .withResultFilterChain(resultFilterChain)
            .build();

    salsaInternalStateSmallSeed = new SalsaInternalState(
        bipartiteGraph, salsaStats, expectedNodesToHit);
    salsaInternalStateSmallSeed.resetWithRequest(salsaRequestSmallSeed);
  }

  @Test
  public void testSeedIteration() throws Exception {
    SalsaIterations<BipartiteGraph> salsaIterations = new SalsaIterations<BipartiteGraph>(
        salsaInternalState,
        new LeftSalsaIteration(salsaInternalState),
        new RightSalsaIteration(salsaInternalState),
        new FinalSalsaIteration(salsaInternalState)
    );
    salsaIterations.resetWithRequest(salsaRequest, random);

    salsaIterations.seedLeftSideForFirstIteration();

    Long2IntMap expectedCurrentLeftNodes = new Long2IntOpenHashMap(3);
    expectedCurrentLeftNodes.put(1, 900);
    expectedCurrentLeftNodes.put(2, 91);
    expectedCurrentLeftNodes.put(3, 10);

    assertEquals(expectedCurrentLeftNodes, salsaInternalState.getCurrentLeftNodes());
  }

  @Test
  public void testNonZeroDegreeSeedIteration() throws Exception {
    SalsaIterations<BipartiteGraph> salsaIterations = new SalsaIterations<BipartiteGraph>(
        salsaInternalStateSmallSeed,
        new LeftSalsaIteration(salsaInternalStateSmallSeed),
        new RightSalsaIteration(salsaInternalStateSmallSeed),
        new FinalSalsaIteration(salsaInternalStateSmallSeed)
    );
    salsaIterations.resetWithRequest(salsaRequestSmallSeed, random);

    salsaIterations.seedLeftSideForFirstIteration();

    Long2IntMap expectedCurrentLeftNodes = new Long2IntOpenHashMap(1);
    expectedCurrentLeftNodes.put(1, 1000);

    assertEquals(expectedCurrentLeftNodes, salsaInternalStateSmallSeed.getCurrentLeftNodes());
  }

  @Test
  public void testSingleLeftIteration() throws Exception {
    SalsaIterations<BipartiteGraph> salsaIterations = new SalsaIterations<BipartiteGraph>(
        salsaInternalState,
        new LeftSalsaIteration(salsaInternalState),
        new RightSalsaIteration(salsaInternalState),
        new FinalSalsaIteration(salsaInternalState)
    );
    salsaIterations.resetWithRequest(salsaRequest, random);
    SingleSalsaIteration leftSalsaIteration =
        new LeftSalsaIteration(salsaInternalState);
    leftSalsaIteration.resetWithRequest(salsaRequest, random);

    salsaIterations.seedLeftSideForFirstIteration();
    leftSalsaIteration.runSingleIteration();

    Long2ObjectMap<NodeInfo> expectedVisitedRightNodesMap =
        new Long2ObjectOpenHashMap<NodeInfo>(9);

    // highest weight
    expectedVisitedRightNodesMap.put(2, new NodeInfo(2, 167, 1));
    expectedVisitedRightNodesMap.put(3, new NodeInfo(3, 168, 1));
    expectedVisitedRightNodesMap.put(4, new NodeInfo(4, 167, 1));
    expectedVisitedRightNodesMap.put(5, new NodeInfo(5, 177, 1));
    // medium weight
    expectedVisitedRightNodesMap.put(6, new NodeInfo(6, 22, 1));
    expectedVisitedRightNodesMap.put(10, new NodeInfo(10, 25, 1));
    // small weight
    expectedVisitedRightNodesMap.put(7, new NodeInfo(7, 2, 1));
    expectedVisitedRightNodesMap.put(9, new NodeInfo(9, 1, 1));
    expectedVisitedRightNodesMap.put(11, new NodeInfo(11, 1, 1));

    assertEquals(1, salsaInternalState.getCurrentLeftNodes().size());
    assertEquals(expectedVisitedRightNodesMap, salsaInternalState.getVisitedRightNodes());
  }

  @Test
  public void testSingleRightIteration() throws Exception {
    SalsaIterations<BipartiteGraph> salsaIterations = new SalsaIterations<BipartiteGraph>(
        salsaInternalState,
        new LeftSalsaIteration(salsaInternalState),
        new RightSalsaIteration(salsaInternalState),
        new FinalSalsaIteration(salsaInternalState)
    );
    salsaIterations.resetWithRequest(salsaRequest, random);
    SingleSalsaIteration leftSalsaIteration =
        new LeftSalsaIteration(salsaInternalState);
    leftSalsaIteration.resetWithRequest(salsaRequest, random);
    SingleSalsaIteration rightSalsaIteration =
        new RightSalsaIteration(salsaInternalState);
    rightSalsaIteration.resetWithRequest(salsaRequest, random);

    salsaIterations.seedLeftSideForFirstIteration();
    leftSalsaIteration.runSingleIteration();
    rightSalsaIteration.runSingleIteration();

    Long2IntMap expectedCurrentLeftNodes = new Long2IntOpenHashMap(3);
    expectedCurrentLeftNodes.put(1, 728);
    expectedCurrentLeftNodes.put(2, 86);
    expectedCurrentLeftNodes.put(3, 187);

    assertTrue(salsaInternalState.getCurrentRightNodes().isEmpty());
    assertEquals(expectedCurrentLeftNodes, salsaInternalState.getCurrentLeftNodes());
  }

  @Test
  public void testFinalLeftIteration() throws Exception {
    SalsaIterations<BipartiteGraph> salsaIterations = new SalsaIterations<BipartiteGraph>(
        salsaInternalState,
        new LeftSalsaIteration(salsaInternalState),
        new RightSalsaIteration(salsaInternalState),
        new FinalSalsaIteration(salsaInternalState)
    );
    salsaIterations.resetWithRequest(salsaRequest, random);
    SingleSalsaIteration leftSalsaIteration =
        new FinalSalsaIteration(salsaInternalState);
    leftSalsaIteration.resetWithRequest(salsaRequest, random);

    salsaIterations.seedLeftSideForFirstIteration();
    leftSalsaIteration.runSingleIteration();

    Long2ObjectMap<NodeInfo> expectedVisitedRightNodesMap =
        new Long2ObjectOpenHashMap<NodeInfo>(9);
    NodeInfo node2 = new NodeInfo(2, 167, 1);
    node2.addToSocialProof(3, (byte) 0, 0L, 1.0);
    expectedVisitedRightNodesMap.put(2, node2);
    NodeInfo node3 = new NodeInfo(3, 168, 1);
    expectedVisitedRightNodesMap.put(3, node3);
    NodeInfo node4 = new NodeInfo(4, 167, 1);
    expectedVisitedRightNodesMap.put(4, node4);
    NodeInfo node5 = new NodeInfo(5, 177, 1);
    node5.addToSocialProof(3, (byte) 0, 0L, 1.0);
    node5.addToSocialProof(2, (byte) 0, 0L, 1.0);
    expectedVisitedRightNodesMap.put(5, node5);
    NodeInfo node6 = new NodeInfo(6, 22, 1);
    node6.addToSocialProof(2, (byte) 0, 0L, 1.0);
    expectedVisitedRightNodesMap.put(6, node6);
    NodeInfo node7 = new NodeInfo(7, 2, 1);
    node7.addToSocialProof(3, (byte) 0, 0L, 1.0);
    expectedVisitedRightNodesMap.put(7, node7);
    NodeInfo node9 = new NodeInfo(9, 1, 1);
    node9.addToSocialProof(3, (byte) 0, 0L, 1.0);
    expectedVisitedRightNodesMap.put(9, node9);
    NodeInfo node10 = new NodeInfo(10, 25, 1);
    node10.addToSocialProof(3, (byte) 0, 0L, 1.0);
    node10.addToSocialProof(2, (byte) 0, 0L, 1.0);
    expectedVisitedRightNodesMap.put(10, node10);
    NodeInfo node11 = new NodeInfo(11, 1, 1);
    node11.addToSocialProof(3, (byte) 0, 0L, 1.0);
    expectedVisitedRightNodesMap.put(11, node11);

    assertEquals(expectedVisitedRightNodesMap, salsaInternalState.getVisitedRightNodes());
  }
}
