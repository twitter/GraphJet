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


package com.twitter.graphjet.bipartite;

import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.twitter.graphjet.bipartite.segment.IdentityEdgeTypeMask;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.longs.LongArrayList;

public class MultiSegmentLeftIndexedPowerLawBipartiteGraphTest {
  private void addEdges(
      LeftIndexedPowerLawMultiSegmentBipartiteGraph multiSegmentLeftIndexedPowerLawBipartiteGraph) {
    multiSegmentLeftIndexedPowerLawBipartiteGraph.addEdge(1, 11, (byte) 0);
    multiSegmentLeftIndexedPowerLawBipartiteGraph.addEdge(1, 12, (byte) 0);
    multiSegmentLeftIndexedPowerLawBipartiteGraph.addEdge(4, 41, (byte) 0);
    multiSegmentLeftIndexedPowerLawBipartiteGraph.addEdge(2, 21, (byte) 0);
    multiSegmentLeftIndexedPowerLawBipartiteGraph.addEdge(4, 42, (byte) 0);
    multiSegmentLeftIndexedPowerLawBipartiteGraph.addEdge(3, 31, (byte) 0);
    multiSegmentLeftIndexedPowerLawBipartiteGraph.addEdge(2, 22, (byte) 0);
    multiSegmentLeftIndexedPowerLawBipartiteGraph.addEdge(1, 13, (byte) 0);
    multiSegmentLeftIndexedPowerLawBipartiteGraph.addEdge(4, 43, (byte) 0);
    // violates the max num nodes assumption
    multiSegmentLeftIndexedPowerLawBipartiteGraph.addEdge(5, 11, (byte) 0);
  }

  private void testGraph(
      LeftIndexedPowerLawMultiSegmentBipartiteGraph multiSegmentLeftIndexedPowerLawBipartiteGraph) {
    assertEquals(3, multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeDegree(1));
    assertEquals(2, multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeDegree(2));
    assertEquals(1, multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeDegree(3));
    assertEquals(3, multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeDegree(4));

    assertEquals(new LongArrayList(new long[]{13, 11, 12}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeEdges(1)));
    assertEquals(new LongArrayList(new long[]{22, 21}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeEdges(2)));
    assertEquals(new LongArrayList(new long[]{31}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeEdges(3)));
    assertEquals(new LongArrayList(new long[]{43, 42, 41}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeEdges(4)));
    assertEquals(new LongArrayList(new long[]{11}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeEdges(5)));

    Random random = new Random(90238490238409L);
    int numSamples = 5;

    assertEquals(new LongArrayList(new long[]{13, 13, 11, 11, 12}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getRandomLeftNodeEdges(
            1, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{22, 22, 22, 21, 21}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getRandomLeftNodeEdges(
            2, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{31, 31, 31, 31, 31}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getRandomLeftNodeEdges(
            3, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{43, 43, 41, 42, 42}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getRandomLeftNodeEdges(
            4, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{11, 11, 11, 11, 11}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getRandomLeftNodeEdges(
            5, numSamples, random)));
  }

  private void testGraphAfterSegmentDrop(
      LeftIndexedPowerLawMultiSegmentBipartiteGraph multiSegmentLeftIndexedPowerLawBipartiteGraph) {
    assertEquals(3, multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeDegree(1));
    assertEquals(2, multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeDegree(2));
    assertEquals(1, multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeDegree(3));
    assertEquals(3, multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeDegree(4));

    assertEquals(new LongArrayList(new long[]{13, 11, 12}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeEdges(1)));
    assertEquals(new LongArrayList(new long[]{22, 21}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeEdges(2)));
    assertEquals(new LongArrayList(new long[]{31}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeEdges(3)));
    assertEquals(new LongArrayList(new long[]{43, 41, 42}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeEdges(4)));
    assertEquals(new LongArrayList(new long[]{11}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeEdges(5)));

    Random random = new Random(90238490238409L);
    int numSamples = 5;

    assertEquals(new LongArrayList(new long[]{13, 13, 11, 11, 12}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getRandomLeftNodeEdges(
            1, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{22, 22, 22, 21, 21}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getRandomLeftNodeEdges(
            2, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{31, 31, 31, 31, 31}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getRandomLeftNodeEdges(
            3, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{43, 43, 43, 43, 43}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getRandomLeftNodeEdges(
            4, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{11, 11, 11, 11, 11}),
        new LongArrayList(multiSegmentLeftIndexedPowerLawBipartiteGraph.getRandomLeftNodeEdges(
            5, numSamples, random)));
  }

  /**
   * Build a random left-regular bipartite graph of given left and right sizes.
   *
   * @param leftSize   is the left hand size of the bipartite graph
   * @param rightSize  is the right hand size of the bipartite graph
   * @param random     is the random number generator to use for constructing the graph
   * @return a random bipartite graph
   */
  public static LeftIndexedPowerLawMultiSegmentBipartiteGraph buildRandomMultiSegmentBipartiteGraph(
      int maxNumSegments,
      int maxNumEdgesPerSegment,
      int leftSize,
      int rightSize,
      double edgeProbability,
      Random random) {
    LeftIndexedPowerLawMultiSegmentBipartiteGraph multiSegmentLeftIndexedPowerLawBipartiteGraph =
        new LeftIndexedPowerLawMultiSegmentBipartiteGraph(
            maxNumSegments,
            maxNumEdgesPerSegment,
            leftSize / 2,
            (int) (rightSize * edgeProbability / 2),
            2.0,
            rightSize / 2,
            new IdentityEdgeTypeMask(),
            new NullStatsReceiver());
    for (int i = 0; i < leftSize; i++) {
      for (int j = 0; j < rightSize; j++) {
        if (random.nextDouble() < edgeProbability) {
          multiSegmentLeftIndexedPowerLawBipartiteGraph.addEdge(i, j, (byte) 0);
        }
      }
    }

    return multiSegmentLeftIndexedPowerLawBipartiteGraph;
  }

  @Test
  public void testMultiSegmentConstruction() throws Exception {
    LeftIndexedPowerLawMultiSegmentBipartiteGraph multiSegmentLeftIndexedPowerLawBipartiteGraph =
        new LeftIndexedPowerLawMultiSegmentBipartiteGraph(
            4, 3, 4, 1, 2.0, 3, new IdentityEdgeTypeMask(), new NullStatsReceiver());

    addEdges(multiSegmentLeftIndexedPowerLawBipartiteGraph);
    testGraph(multiSegmentLeftIndexedPowerLawBipartiteGraph);

    // also test continuously adding and dropping edges with a graph that holds exactly 10 edges
    LeftIndexedPowerLawMultiSegmentBipartiteGraph smallMultiSegmentPowerLawBipartiteGraph =
        new LeftIndexedPowerLawMultiSegmentBipartiteGraph(
            2, 5, 4, 1, 2.0, 3, new IdentityEdgeTypeMask(), new NullStatsReceiver());
    for (int i = 0; i < 10; i++) {
      addEdges(smallMultiSegmentPowerLawBipartiteGraph);
    }
    // we should come back to the original 10 edges (we could test this each time but the internal
    // hashmaps affect the random number generator so the effect is unpredictable each time)
    testGraphAfterSegmentDrop(smallMultiSegmentPowerLawBipartiteGraph);
  }

  @Test
  public void testRandomSegmentConstruction() throws Exception {
    int maxNumSegments = 10;
    int maxNumEdgesPerSegment = 1500;
    int leftSize = 100;
    int rightSize = 1000;
    double edgeProbability = 0.1; // this implies ~10K edges
    int numSamples = 10;

    Random random = new Random(8904572034987501L);
    LeftIndexedPowerLawMultiSegmentBipartiteGraph multiSegmentLeftIndexedPowerLawBipartiteGraph =
        buildRandomMultiSegmentBipartiteGraph(
            maxNumSegments,
            maxNumEdgesPerSegment,
            leftSize,
            rightSize,
            edgeProbability,
            random);

    // on average, degree is a 100
    assertEquals(99, multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeDegree(10));
    Set<Long> leftNodeEdgeSet =
        Sets.newHashSet(multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeEdges(10));
    // all edges are unique
    assertEquals(99, leftNodeEdgeSet.size());
    List<Long> leftNodeRandomEdgeSample = Lists.newArrayList(
        multiSegmentLeftIndexedPowerLawBipartiteGraph.getRandomLeftNodeEdges(
            10, numSamples, random));
    assertEquals(numSamples, leftNodeRandomEdgeSample.size());
    for (Long id : leftNodeRandomEdgeSample) {
      assertTrue(leftNodeEdgeSet.contains(id));
    }
  }

  /**
   * This test is here as an example of checking for a memory leak: the idea here is to start with
   * a limited heap so that the JVM is forced to reclaim memory, enabling us to check that memory
   * from old segments is reclaimed correctly. For 500 rounds, we do see at least 5 GC cycles, and
   * this test should fail if memory recycling doesn't work properly.
   */
  @Test
  public void testMemoryRecycling() throws Exception {
    int maxNumSegments = 10;
    int maxNumEdgesPerSegment = 1000;
    int leftSize = 100;
    int rightSize = 1000;
    double edgeProbability = 0.1; // this implies ~10K edges
    int numRounds = 500;

    LeftIndexedPowerLawMultiSegmentBipartiteGraph multiSegmentLeftIndexedPowerLawBipartiteGraph =
        new LeftIndexedPowerLawMultiSegmentBipartiteGraph(
            maxNumSegments,
            maxNumEdgesPerSegment,
            leftSize / 2,
            (int) (rightSize * edgeProbability / 2),
            2.0,
            rightSize / 2,
            new IdentityEdgeTypeMask(),
            new NullStatsReceiver());

    for (int round = 0; round < numRounds; round++) {
      Random random = new Random(8904572034987501L);
      int numSamples = 10;
      for (int i = 0; i < leftSize; i++) {
        for (int j = 0; j < rightSize; j++) {
          if (random.nextDouble() < edgeProbability) {
            multiSegmentLeftIndexedPowerLawBipartiteGraph.addEdge(i, j, (byte) 0);
          }
        }
      }
      // on average, degree is a 100
      assertEquals(99, multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeDegree(10));
      Set<Long> leftNodeEdgeSet =
          Sets.newHashSet(multiSegmentLeftIndexedPowerLawBipartiteGraph.getLeftNodeEdges(10));
      // all edges are unique
      assertEquals(99, leftNodeEdgeSet.size());
      List<Long> leftNodeRandomEdgeSample = Lists.newArrayList(
          multiSegmentLeftIndexedPowerLawBipartiteGraph.getRandomLeftNodeEdges(
              10, numSamples, random));
      assertEquals(numSamples, leftNodeRandomEdgeSample.size());
      for (Long id : leftNodeRandomEdgeSample) {
        assertTrue(leftNodeEdgeSet.contains(id));
      }
      // checking an arbitrary node
      System.out.println("=== Round " + round);
      System.out.println("Total memory available to JVM (bytes): "
          + Runtime.getRuntime().totalMemory());
      System.out.println("Free memory (bytes): " + Runtime.getRuntime().freeMemory());
    }
  }
}
