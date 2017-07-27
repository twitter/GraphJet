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

package com.twitter.graphjet.algorithms;

import com.twitter.graphjet.bipartite.segment.HigherBitsEdgeTypeMask;
import com.twitter.graphjet.bipartite.segment.IdentityEdgeTypeMask;
import com.twitter.graphjet.directed.OutIndexedPowerLawMultiSegmentDirectedGraph;
import com.twitter.graphjet.stats.NullStatsReceiver;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PageRankTest {
  // This is the coappearance network of characters in the novel Les Miserables.
  // D. E. Knuth, The Stanford GraphBase: A Platform for Combinatorial Computing, Addison-Wesley, Reading, MA (1993).
  // Downloaded from http://www-personal.umich.edu/~mejn/netdata/
  // Note that the graph is undirected, but here we're treating it (arbitrarily) as a direct graph.
  private static final long[][] LES_MIS_GRAPH = {
      {1, 0},
      {2, 0},
      {3, 0},
      {3, 2},
      {4, 0},
      {5, 0},
      {6, 0},
      {7, 0},
      {8, 0},
      {9, 0},
      {11, 10},
      {11, 3},
      {11, 2},
      {11, 0},
      {12, 11},
      {13, 11},
      {14, 11},
      {15, 11},
      {17, 16},
      {18, 16},
      {18, 17},
      {19, 16},
      {19, 17},
      {19, 18},
      {20, 16},
      {20, 17},
      {20, 18},
      {20, 19},
      {21, 16},
      {21, 17},
      {21, 18},
      {21, 19},
      {21, 20},
      {22, 16},
      {22, 17},
      {22, 18},
      {22, 19},
      {22, 20},
      {22, 21},
      {23, 16},
      {23, 17},
      {23, 18},
      {23, 19},
      {23, 20},
      {23, 21},
      {23, 22},
      {23, 12},
      {23, 11},
      {24, 23},
      {24, 11},
      {25, 24},
      {25, 23},
      {25, 11},
      {26, 24},
      {26, 11},
      {26, 16},
      {26, 25},
      {27, 11},
      {27, 23},
      {27, 25},
      {27, 24},
      {27, 26},
      {28, 11},
      {28, 27},
      {29, 23},
      {29, 27},
      {29, 11},
      {30, 23},
      {31, 30},
      {31, 11},
      {31, 23},
      {31, 27},
      {32, 11},
      {33, 11},
      {33, 27},
      {34, 11},
      {34, 29},
      {35, 11},
      {35, 34},
      {35, 29},
      {36, 34},
      {36, 35},
      {36, 11},
      {36, 29},
      {37, 34},
      {37, 35},
      {37, 36},
      {37, 11},
      {37, 29},
      {38, 34},
      {38, 35},
      {38, 36},
      {38, 37},
      {38, 11},
      {38, 29},
      {39, 25},
      {40, 25},
      {41, 24},
      {41, 25},
      {42, 41},
      {42, 25},
      {42, 24},
      {43, 11},
      {43, 26},
      {43, 27},
      {44, 28},
      {44, 11},
      {45, 28},
      {47, 46},
      {48, 47},
      {48, 25},
      {48, 27},
      {48, 11},
      {49, 26},
      {49, 11},
      {50, 49},
      {50, 24},
      {51, 49},
      {51, 26},
      {51, 11},
      {52, 51},
      {52, 39},
      {53, 51},
      {54, 51},
      {54, 49},
      {54, 26},
      {55, 51},
      {55, 49},
      {55, 39},
      {55, 54},
      {55, 26},
      {55, 11},
      {55, 16},
      {55, 25},
      {55, 41},
      {55, 48},
      {56, 49},
      {56, 55},
      {57, 55},
      {57, 41},
      {57, 48},
      {58, 55},
      {58, 48},
      {58, 27},
      {58, 57},
      {58, 11},
      {59, 58},
      {59, 55},
      {59, 48},
      {59, 57},
      {60, 48},
      {60, 58},
      {60, 59},
      {61, 48},
      {61, 58},
      {61, 60},
      {61, 59},
      {61, 57},
      {61, 55},
      {62, 55},
      {62, 58},
      {62, 59},
      {62, 48},
      {62, 57},
      {62, 41},
      {62, 61},
      {62, 60},
      {63, 59},
      {63, 48},
      {63, 62},
      {63, 57},
      {63, 58},
      {63, 61},
      {63, 60},
      {63, 55},
      {64, 55},
      {64, 62},
      {64, 48},
      {64, 63},
      {64, 58},
      {64, 61},
      {64, 60},
      {64, 59},
      {64, 57},
      {64, 11},
      {65, 63},
      {65, 64},
      {65, 48},
      {65, 62},
      {65, 58},
      {65, 61},
      {65, 60},
      {65, 59},
      {65, 57},
      {65, 55},
      {66, 64},
      {66, 58},
      {66, 59},
      {66, 62},
      {66, 65},
      {66, 48},
      {66, 63},
      {66, 61},
      {66, 60},
      {67, 57},
      {68, 25},
      {68, 11},
      {68, 24},
      {68, 27},
      {68, 48},
      {68, 41},
      {69, 25},
      {69, 68},
      {69, 11},
      {69, 24},
      {69, 27},
      {69, 48},
      {69, 41},
      {70, 25},
      {70, 69},
      {70, 68},
      {70, 11},
      {70, 24},
      {70, 27},
      {70, 41},
      {70, 58},
      {71, 27},
      {71, 69},
      {71, 68},
      {71, 70},
      {71, 11},
      {71, 48},
      {71, 41},
      {71, 25},
      {72, 26},
      {72, 27},
      {72, 11},
      {73, 48},
      {74, 48},
      {74, 73},
      {75, 69},
      {75, 68},
      {75, 25},
      {75, 48},
      {75, 41},
      {75, 70},
      {75, 71},
      {76, 64},
      {76, 65},
      {76, 66},
      {76, 63},
      {76, 62},
      {76, 48},
      {76, 58}
  };

  @Test
  public void testLesMisGraph() throws Exception {
    OutIndexedPowerLawMultiSegmentDirectedGraph graph =
        new OutIndexedPowerLawMultiSegmentDirectedGraph(1, 1000, 100, 10, 2,
            new IdentityEdgeTypeMask(), new NullStatsReceiver());

    for (int i=0; i<LES_MIS_GRAPH.length; i++) {
      graph.addEdge(LES_MIS_GRAPH[i][0], LES_MIS_GRAPH[i][1], (byte) 0);
    }

    // Spot check the graph to make sure it's been loaded correctly.
    assertEquals(7, graph.getOutDegree(76));
    assertEquals(new LongArrayList(new long[]{64, 65, 66, 63, 62, 48, 58}), new LongArrayList(graph.getOutEdges(76)));

    assertEquals(1, graph.getOutDegree(30));
    assertEquals(new LongArrayList(new long[]{23}), new LongArrayList(graph.getOutEdges(30)));

    assertEquals(4, graph.getOutDegree(11));
    assertEquals(new LongArrayList(new long[]{10, 3, 2, 0}), new LongArrayList(graph.getOutEdges(11)));

    LongOpenHashSet nodes = new LongOpenHashSet();
    long maxNodeId = 0;
    for (int i=0; i<LES_MIS_GRAPH.length; i++) {
      if ( !nodes.contains(LES_MIS_GRAPH[i][0])) nodes.add(LES_MIS_GRAPH[i][0]);
      if ( !nodes.contains(LES_MIS_GRAPH[i][1])) nodes.add(LES_MIS_GRAPH[i][1]);
      if ( LES_MIS_GRAPH[i][0] > maxNodeId ) maxNodeId = LES_MIS_GRAPH[i][0];
      if ( LES_MIS_GRAPH[i][1] > maxNodeId ) maxNodeId = LES_MIS_GRAPH[i][1];
    }

    assertEquals(76, maxNodeId);
    PageRank pr = new PageRank(graph, nodes, maxNodeId, 0.85, 10, 1e-15);
    int numIterations = pr.run();
    double normL1 = pr.getL1Norm();
    double[] pagerank = pr.getPageRankVector();
    assertEquals(10, numIterations);
    assertEquals(0.00108, normL1, 10e-4);

    List<Map.Entry<Long, Double>> scores = new ArrayList<>();
    for (int i=0; i<maxNodeId+1; i++) {
      scores.add(new AbstractMap.SimpleEntry<>((long) i, pagerank[i]));
    }

    // Sort by score.
    scores.sort((e1, e2) -> e2.getValue() > e1.getValue() ? 1 : e2.getKey().compareTo(e1.getKey()));

    // We're going to verify that the ranking and score are both correct. These rankings have been verified against an
    // external implementation (JUNG).
    assertEquals(11, (long) scores.get(0).getKey());
    assertEquals(0.1088995, scores.get(0).getValue(), 10e-4);
    assertEquals(0, (long) scores.get(1).getKey());
    assertEquals(0.09538347, scores.get(1).getValue(), 10e-4);
    assertEquals(16, (long) scores.get(2).getKey());
    assertEquals(0.05104386, scores.get(2).getValue(), 10e-4);
    assertEquals(23, (long) scores.get(3).getKey());
    assertEquals(0.04389916, scores.get(3).getValue(), 10e-4);
    assertEquals(25, (long) scores.get(4).getKey());
    assertEquals(0.04095956, scores.get(4).getValue(), 10e-4);
    assertEquals(2, (long) scores.get(5).getKey());
    assertEquals(0.03868165, scores.get(5).getValue(), 10e-4);
    assertEquals(24, (long) scores.get(6).getKey());
    assertEquals(0.03617344, scores.get(6).getValue(), 10e-4);
    assertEquals(48, (long) scores.get(7).getKey());
    assertEquals(0.0290502, scores.get(7).getValue(), 10e-4);
    assertEquals(10, (long) scores.get(8).getKey());
    assertEquals(0.02714507, scores.get(8).getValue(), 10e-4);
    assertEquals(3, (long) scores.get(9).getKey());
    assertEquals(0.02714507, scores.get(9).getValue(), 10e-4);

    double totalMass = 0.0;
    for (int i=0; i<maxNodeId+1; i++) {
      totalMass += scores.get(i).getValue();
    }
    // Total mass should still be 1.0.
    assertEquals(1.0, totalMass, 10e-10);
  }

  @Test
  public void testRunReturningPositive() {
    HigherBitsEdgeTypeMask higherBitsEdgeTypeMask = new HigherBitsEdgeTypeMask();
    OutIndexedPowerLawMultiSegmentDirectedGraph powerLawMultiSegmentDirectedGraph =
            new OutIndexedPowerLawMultiSegmentDirectedGraph(1249,
                    1249,
                    1249,
                    1249,
                    1249,
                    higherBitsEdgeTypeMask,
                    new NullStatsReceiver());
    LongOpenHashSet longOpenHashSet = new LongOpenHashSet();
    powerLawMultiSegmentDirectedGraph.addEdge(0L, 1170L, (byte) (-126));
    longOpenHashSet.add(0L);
    PageRank pageRank =
            new PageRank(powerLawMultiSegmentDirectedGraph, longOpenHashSet, 1249, 1249, 1249, 1249);
    int resultInt = pageRank.run();

    assertEquals(0.0, pageRank.getL1Norm(), 0.01);
    assertEquals(3, resultInt);
  }


  @Test
  public void testFailsToCreateThrowsUnsupportedOperationException() {
    HigherBitsEdgeTypeMask higherBitsEdgeTypeMask = new HigherBitsEdgeTypeMask();
    OutIndexedPowerLawMultiSegmentDirectedGraph outIndexedPowerLawMultiSegmentDirectedGraph =
            new OutIndexedPowerLawMultiSegmentDirectedGraph(1249,
                    1249,
                    1249,
                    1249,
                    1249,
                    higherBitsEdgeTypeMask,
                    new NullStatsReceiver());

    try {
      new PageRank(outIndexedPowerLawMultiSegmentDirectedGraph,
              null,
              2147483657L,
              2147483657L,
              2640,
              1345.6519203075502);
      fail("Expecting exception: UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      assertEquals(PageRank.class.getName(), e.getStackTrace()[0].getClassName());
    }
  }

}