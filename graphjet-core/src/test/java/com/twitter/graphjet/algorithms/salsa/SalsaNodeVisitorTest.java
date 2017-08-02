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
import com.twitter.graphjet.algorithms.salsa.fullgraph.SalsaInternalState;
import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class SalsaNodeVisitorTest {
  private SalsaRequest salsaRequest;
  private SalsaInternalState salsaInternalState;
  private byte[] socialProofTypes;

  @Before
  public void setUp() {
    long queryNode = 1;
    BipartiteGraph bipartiteGraph = BipartiteGraphTestHelper.buildSmallTestBipartiteGraph();
    LongSet toBeFiltered = new LongOpenHashSet(8);
    int numIterations = 5;
    double resetProbability = 0.3;
    int numResults = 3;
    int numRandomWalks = 1000;
    int maxSocialProofSize = 2;
    int expectedNodesToHit = numRandomWalks * numIterations / 2;
    SalsaStats salsaStats = new SalsaStats();
    socialProofTypes = new byte[]{0, 1, 2, 3};
    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.newArrayList(
        new RequestedSetFilter(new NullStatsReceiver()),
        new DirectInteractionsFilter(bipartiteGraph, new NullStatsReceiver())
    ));

    salsaRequest = new SalsaRequestBuilder(queryNode)
        .withLeftSeedNodes(null)
        .withToBeFiltered(toBeFiltered)
        .withMaxNumResults(numResults)
        .withResetProbability(resetProbability)
        .withMaxRandomWalkLength(numIterations)
        .withNumRandomWalks(numRandomWalks)
        .withMaxSocialProofSize(maxSocialProofSize)
        .withValidSocialProofTypes(socialProofTypes)
        .withResultFilterChain(resultFilterChain)
        .build();

    salsaInternalState = new SalsaInternalState(
        bipartiteGraph, salsaStats, expectedNodesToHit);
    salsaInternalState.resetWithRequest(salsaRequest);
  }

  @Test
  public void testSimpleNodeVisitor() throws Exception {
    SalsaNodeVisitor.SimpleNodeVisitor simpleNodeVisitor =
        new SalsaNodeVisitor.SimpleNodeVisitor(
            salsaInternalState.getVisitedRightNodes());
    simpleNodeVisitor.resetWithRequest(salsaRequest);

    simpleNodeVisitor.visitRightNode(1, 2, (byte) 0, 0L, 1);
    simpleNodeVisitor.visitRightNode(2, 3, (byte) 0, 0L, 1);
    simpleNodeVisitor.visitRightNode(1, 3, (byte) 0, 0L, 1);

    Long2ObjectMap<NodeInfo> expectedVisitedRightNodesMap =
        new Long2ObjectOpenHashMap<NodeInfo>(2);
    expectedVisitedRightNodesMap.put(2, new NodeInfo(2, 1, 1));
    expectedVisitedRightNodesMap.put(3, new NodeInfo(3, 2, 1));

    assertEquals(expectedVisitedRightNodesMap, salsaInternalState.getVisitedRightNodes());
  }

  @Test
  public void testNodeVisitorWithSocialProof() throws Exception {
    SalsaNodeVisitor.NodeVisitorWithSocialProof nodeVisitorWithSocialProof =
        new SalsaNodeVisitor.NodeVisitorWithSocialProof(
            salsaInternalState.getVisitedRightNodes());
    nodeVisitorWithSocialProof.resetWithRequest(salsaRequest);

    nodeVisitorWithSocialProof.visitRightNode(1, 2, (byte) 0, 0L, 1);
    nodeVisitorWithSocialProof.visitRightNode(2, 3, (byte) 0, 0L, 1);
    nodeVisitorWithSocialProof.visitRightNode(1, 3, (byte) 0, 0L, 1);

    NodeInfo node2 = new NodeInfo(2, 1, 1);
    NodeInfo node3 = new NodeInfo(3, 2, 1);
    assertTrue(node3.addToSocialProof(2, (byte) 0, 0L, 1));

    Long2ObjectMap<NodeInfo> expectedVisitedRightNodesMap =
        new Long2ObjectOpenHashMap<NodeInfo>(2);
    expectedVisitedRightNodesMap.put(2, node2);
    expectedVisitedRightNodesMap.put(3, node3);

    assertEquals(expectedVisitedRightNodesMap, salsaInternalState.getVisitedRightNodes());
  }
}
