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

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.twitter.graphjet.bipartite.segment.IdentityEdgeTypeMask;
import com.twitter.graphjet.bipartite.segment.LeftIndexedBipartiteGraphSegment;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import static com.twitter.graphjet.bipartite.GraphConcurrentTestHelper.testConcurrentReadWriteThreads;
import static com.twitter.graphjet.bipartite.GraphConcurrentTestHelper.testRandomConcurrentReadWriteThreads;


public class MultiSegmentPowerLawBipartiteGraphTest {
  private void addEdges(LeftIndexedMultiSegmentBipartiteGraph leftIndexedMultiSegmentBipartiteGraph) {
    leftIndexedMultiSegmentBipartiteGraph.addEdge(1, 11, (byte) 0);
    leftIndexedMultiSegmentBipartiteGraph.addEdge(1, 12, (byte) 0);
    leftIndexedMultiSegmentBipartiteGraph.addEdge(4, 41, (byte) 0);
    leftIndexedMultiSegmentBipartiteGraph.addEdge(2, 21, (byte) 0);
    leftIndexedMultiSegmentBipartiteGraph.addEdge(4, 42, (byte) 0);
    leftIndexedMultiSegmentBipartiteGraph.addEdge(3, 31, (byte) 0);
    leftIndexedMultiSegmentBipartiteGraph.addEdge(2, 22, (byte) 0);
    leftIndexedMultiSegmentBipartiteGraph.addEdge(1, 13, (byte) 0);
    leftIndexedMultiSegmentBipartiteGraph.addEdge(4, 43, (byte) 0);
    leftIndexedMultiSegmentBipartiteGraph.addEdge(5, 11, (byte) 0);
    // violates the max num nodes assumption
  }

  private void testGraph(MultiSegmentPowerLawBipartiteGraph multiSegmentPowerLawBipartiteGraph) {
    assertEquals(3, multiSegmentPowerLawBipartiteGraph.getLeftNodeDegree(1));
    assertEquals(2, multiSegmentPowerLawBipartiteGraph.getLeftNodeDegree(2));
    assertEquals(1, multiSegmentPowerLawBipartiteGraph.getLeftNodeDegree(3));
    assertEquals(3, multiSegmentPowerLawBipartiteGraph.getLeftNodeDegree(4));
    assertEquals(1, multiSegmentPowerLawBipartiteGraph.getRightNodeDegree(13));
    assertEquals(2, multiSegmentPowerLawBipartiteGraph.getRightNodeDegree(11));

    assertEquals(new LongArrayList(new long[]{11, 12, 13}),
        new LongArrayList(multiSegmentPowerLawBipartiteGraph.getLeftNodeEdges(1)));
    assertEquals(new LongArrayList(new long[]{21, 22}),
        new LongArrayList(multiSegmentPowerLawBipartiteGraph.getLeftNodeEdges(2)));
    assertEquals(new LongArrayList(new long[]{31}),
        new LongArrayList(multiSegmentPowerLawBipartiteGraph.getLeftNodeEdges(3)));
    assertEquals(new LongArrayList(new long[]{41, 42, 43}),
        new LongArrayList(multiSegmentPowerLawBipartiteGraph.getLeftNodeEdges(4)));
    assertEquals(new LongArrayList(new long[]{11}),
        new LongArrayList(multiSegmentPowerLawBipartiteGraph.getLeftNodeEdges(5)));
    assertEquals(new LongArrayList(new long[]{1, 5}),
        new LongArrayList(multiSegmentPowerLawBipartiteGraph.getRightNodeEdges(11)));
    assertEquals(new LongArrayList(new long[]{3}),
        new LongArrayList(multiSegmentPowerLawBipartiteGraph.getRightNodeEdges(31)));

    Random random = new Random(90238490238409L);
    int numSamples = 5;

    assertEquals(new LongArrayList(new long[]{13, 13, 11, 11, 12}),
        new LongArrayList(
            multiSegmentPowerLawBipartiteGraph.getRandomLeftNodeEdges(1, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{22, 22, 22, 21, 21}),
        new LongArrayList(
            multiSegmentPowerLawBipartiteGraph.getRandomLeftNodeEdges(2, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{31, 31, 31, 31, 31}),
        new LongArrayList(
            multiSegmentPowerLawBipartiteGraph.getRandomLeftNodeEdges(3, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{43, 43, 41, 42, 42}),
        new LongArrayList(
            multiSegmentPowerLawBipartiteGraph.getRandomLeftNodeEdges(4, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{11, 11, 11, 11, 11}),
        new LongArrayList(
            multiSegmentPowerLawBipartiteGraph.getRandomLeftNodeEdges(5, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{5, 5, 1, 1, 1}),
        new LongArrayList(
            multiSegmentPowerLawBipartiteGraph.getRandomRightNodeEdges(11, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{2, 2, 2, 2, 2}),
        new LongArrayList(
            multiSegmentPowerLawBipartiteGraph.getRandomRightNodeEdges(21, numSamples, random)));
  }

  private void testGraphAfterSegmentDrop(
      MultiSegmentPowerLawBipartiteGraph multiSegmentPowerLawBipartiteGraph) {
    assertEquals(3, multiSegmentPowerLawBipartiteGraph.getLeftNodeDegree(1));
    assertEquals(2, multiSegmentPowerLawBipartiteGraph.getLeftNodeDegree(2));
    assertEquals(1, multiSegmentPowerLawBipartiteGraph.getLeftNodeDegree(3));
    assertEquals(3, multiSegmentPowerLawBipartiteGraph.getLeftNodeDegree(4));
    assertEquals(1, multiSegmentPowerLawBipartiteGraph.getRightNodeDegree(13));
    assertEquals(2, multiSegmentPowerLawBipartiteGraph.getRightNodeDegree(11));

    assertEquals(new LongArrayList(new long[]{11, 12, 13}),
        new LongArrayList(multiSegmentPowerLawBipartiteGraph.getLeftNodeEdges(1)));
    assertEquals(new LongArrayList(new long[]{21, 22}),
        new LongArrayList(multiSegmentPowerLawBipartiteGraph.getLeftNodeEdges(2)));
    assertEquals(new LongArrayList(new long[]{31}),
        new LongArrayList(multiSegmentPowerLawBipartiteGraph.getLeftNodeEdges(3)));
    assertEquals(new LongArrayList(new long[]{41, 42, 43}),
        new LongArrayList(multiSegmentPowerLawBipartiteGraph.getLeftNodeEdges(4)));
    assertEquals(new LongArrayList(new long[]{11}),
        new LongArrayList(multiSegmentPowerLawBipartiteGraph.getLeftNodeEdges(5)));
    assertEquals(new LongArrayList(new long[]{1, 5}),
        new LongArrayList(multiSegmentPowerLawBipartiteGraph.getRightNodeEdges(11)));
    assertEquals(new LongArrayList(new long[]{3}),
        new LongArrayList(multiSegmentPowerLawBipartiteGraph.getRightNodeEdges(31)));

    Random random = new Random(90238490238409L);
    int numSamples = 5;

    assertEquals(new LongArrayList(new long[]{13, 13, 11, 11, 12}),
        new LongArrayList(
            multiSegmentPowerLawBipartiteGraph.getRandomLeftNodeEdges(1, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{22, 22, 22, 21, 21}),
        new LongArrayList(
            multiSegmentPowerLawBipartiteGraph.getRandomLeftNodeEdges(2, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{31, 31, 31, 31, 31}),
        new LongArrayList(
            multiSegmentPowerLawBipartiteGraph.getRandomLeftNodeEdges(3, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{43, 43, 43, 43, 43}),
        new LongArrayList(
            multiSegmentPowerLawBipartiteGraph.getRandomLeftNodeEdges(4, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{11, 11, 11, 11, 11}),
        new LongArrayList(
            multiSegmentPowerLawBipartiteGraph.getRandomLeftNodeEdges(5, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{5, 5, 1, 1, 1}),
        new LongArrayList(
            multiSegmentPowerLawBipartiteGraph.getRandomRightNodeEdges(11, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{2, 2, 2, 2, 2}),
        new LongArrayList(
            multiSegmentPowerLawBipartiteGraph.getRandomRightNodeEdges(21, numSamples, random)));
  }

  /**
   * Build a random left-regular bipartite graph of given left and right sizes.
   *
   * @param leftSize   is the left hand size of the bipartite graph
   * @param rightSize  is the right hand size of the bipartite graph
   * @param random     is the random number generator to use for constructing the graph
   * @return a random bipartite graph
   */
  public static MultiSegmentPowerLawBipartiteGraph buildRandomMultiSegmentBipartiteGraph(
      int maxNumSegments,
      int maxNumEdgesPerSegment,
      int leftSize,
      int rightSize,
      double edgeProbability,
      Random random) {
    MultiSegmentPowerLawBipartiteGraph multiSegmentPowerLawBipartiteGraph =
        new MultiSegmentPowerLawBipartiteGraph(
            maxNumSegments,
            maxNumEdgesPerSegment,
            leftSize / 2,
            (int) (rightSize * edgeProbability / 2),
            2.0,
            rightSize / 2,
            (int) (leftSize * edgeProbability / 2),
            2.0,
            new IdentityEdgeTypeMask(),
            new NullStatsReceiver());
    for (int i = 0; i < leftSize; i++) {
      for (int j = 0; j < rightSize; j++) {
        if (random.nextDouble() < edgeProbability) {
          multiSegmentPowerLawBipartiteGraph.addEdge(i, j, (byte) 0);
        }
      }
    }

    return multiSegmentPowerLawBipartiteGraph;
  }

  @Test
  public void testMultiSegmentConstruction() throws Exception {
    MultiSegmentPowerLawBipartiteGraph multiSegmentPowerLawBipartiteGraph =
        new MultiSegmentPowerLawBipartiteGraph(
            4, 3, 4, 1, 2.0, 3, 2, 2.0, new IdentityEdgeTypeMask(), new NullStatsReceiver());

    addEdges(multiSegmentPowerLawBipartiteGraph);
    testGraph(multiSegmentPowerLawBipartiteGraph);

    // also test continuously adding and dropping edges with a graph that holds exactly 10 edges
    MultiSegmentPowerLawBipartiteGraph smallMultiSegmentPowerLawBipartiteGraph =
        new MultiSegmentPowerLawBipartiteGraph(
            2, 5, 4, 1, 2.0, 3, 2, 2.0, new IdentityEdgeTypeMask(), new NullStatsReceiver());
    for (int i = 0; i < 10; i++) {
      addEdges(smallMultiSegmentPowerLawBipartiteGraph);
    }
    // we should come back to the original 10 edges (we could test this each time but the internal
    // hashmaps affect the random number generator so the effect is unpredictable each time)
    testGraphAfterSegmentDrop(smallMultiSegmentPowerLawBipartiteGraph);
  }

  @Test
  public void testMultiSegmentReverseIterationIncompleteLiveSegment() throws Exception {
    LeftIndexedPowerLawMultiSegmentBipartiteGraph leftIndexedPowerLawMultiSegmentBipartiteGraph =
      new LeftIndexedPowerLawMultiSegmentBipartiteGraph(
        3, 3, 5, 2, 2.0, 5, new IdentityEdgeTypeMask(), new NullStatsReceiver());

    addEdges(leftIndexedPowerLawMultiSegmentBipartiteGraph);

    /** One segment is dropped so the segments should have the following edges:
     * Segment 0: Dropped
     * Segment 1: (2, 21), (4, 42), (3, 31)
     * Segment 2: (2, 22), (1, 13), (4, 43)
     * Segment 3: (5, 11)
     */
    Int2ObjectMap<LeftIndexedBipartiteGraphSegment> segments =
      leftIndexedPowerLawMultiSegmentBipartiteGraph.getSegments();

    LeftIndexedBipartiteGraphSegment segment1 = segments.get(1);
    assertEquals(new LongArrayList(new long[]{21}),
      new LongArrayList(segment1.getLeftNodeEdges(2)));
    assertEquals(new LongArrayList(new long[]{31}),
      new LongArrayList(segment1.getLeftNodeEdges(3)));
    assertEquals(new LongArrayList(new long[]{42}),
      new LongArrayList(segment1.getLeftNodeEdges(4)));

    LeftIndexedBipartiteGraphSegment segment2 = segments.get(2);
    assertEquals(new LongArrayList(new long[]{13}),
      new LongArrayList(segment2.getLeftNodeEdges(1)));
    assertEquals(new LongArrayList(new long[]{22}),
      new LongArrayList(segment2.getLeftNodeEdges(2)));
    assertEquals(new LongArrayList(new long[]{43}),
      new LongArrayList(segment2.getLeftNodeEdges(4)));

    LeftIndexedBipartiteGraphSegment segment3 = segments.get(3);
    assertEquals(new LongArrayList(new long[]{11}),
      new LongArrayList(segment3.getLeftNodeEdges(5)));

    // Test that the iterator returns the correct edges after the first segment is dropped.
    assertEquals(new LongArrayList(new long[]{13}),
      new LongArrayList(leftIndexedPowerLawMultiSegmentBipartiteGraph.getLeftNodeEdges(1)));

    // Test that the iterator returns the correct edges and in reverse order when the edges are in different segments.
    assertEquals(new LongArrayList(new long[]{43, 42}),
      new LongArrayList(leftIndexedPowerLawMultiSegmentBipartiteGraph.getLeftNodeEdges(4)));
  }

  @Test
  public void testMultiSegmentReverseIterationCompleteLiveSegment() throws Exception {
    LeftIndexedPowerLawMultiSegmentBipartiteGraph leftIndexedPowerLawMultiSegmentBipartiteGraph =
      new LeftIndexedPowerLawMultiSegmentBipartiteGraph(
        4, 4, 2, 10, 2.0, 20, new IdentityEdgeTypeMask(), new NullStatsReceiver());

    for (long leftNode = 1; leftNode <= 2; leftNode++) {
      for (long i = 0; i < 10; i++) {
        leftIndexedPowerLawMultiSegmentBipartiteGraph.addEdge(leftNode, leftNode*10 + i, (byte) 0);
      }
    }

    /** One segment is dropped so the segments should have the following edges:
     * Segment 0: Dropped
     * Segment 1: (1, 14), (1, 15), (1, 16), (1, 17)
     * Segment 2: (1, 18), (1, 19), (2, 20), (2, 21)
     * Segment 3: (2, 22), (2, 23), (2, 24), (2, 25)
     * Segment 4: (2, 26), (2, 27), (2, 28), (2, 29)
     */
    Int2ObjectMap<LeftIndexedBipartiteGraphSegment> segments =
      leftIndexedPowerLawMultiSegmentBipartiteGraph.getSegments();

    LeftIndexedBipartiteGraphSegment segment1 = segments.get(1);
    assertEquals(new LongArrayList(new long[]{14, 15, 16, 17}),
      new LongArrayList(segment1.getLeftNodeEdges(1)));

    LeftIndexedBipartiteGraphSegment segment2 = segments.get(2);
    assertEquals(new LongArrayList(new long[]{18, 19}),
      new LongArrayList(segment2.getLeftNodeEdges(1)));
    assertEquals(new LongArrayList(new long[]{20, 21}),
      new LongArrayList(segment2.getLeftNodeEdges(2)));

    LeftIndexedBipartiteGraphSegment segment3 = segments.get(3);
    assertEquals(new LongArrayList(new long[]{22, 23, 24, 25}),
      new LongArrayList(segment3.getLeftNodeEdges(2)));

    LeftIndexedBipartiteGraphSegment segment4 = segments.get(4);
    assertEquals(new LongArrayList(new long[]{26, 27, 28, 29}),
      new LongArrayList(segment4.getLeftNodeEdges(2)));

    // Test that the iterator returns the correct edges and in reverse order when the edges are in different segments.
    assertEquals(new LongArrayList(new long[]{26, 27, 28, 29, 22, 23, 24, 25, 20, 21}),
      new LongArrayList(leftIndexedPowerLawMultiSegmentBipartiteGraph.getLeftNodeEdges(2)));
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
    MultiSegmentPowerLawBipartiteGraph multiSegmentPowerLawBipartiteGraph =
        buildRandomMultiSegmentBipartiteGraph(
            maxNumSegments,
            maxNumEdgesPerSegment,
            leftSize,
            rightSize,
            edgeProbability,
            random);

    // on average, degree is a 100
    assertEquals(99, multiSegmentPowerLawBipartiteGraph.getLeftNodeDegree(10));
    Set<Long> leftNodeEdgeSet =
        Sets.newHashSet(multiSegmentPowerLawBipartiteGraph.getLeftNodeEdges(10));
    // all edges are unique
    assertEquals(99, leftNodeEdgeSet.size());
    List<Long> leftNodeRandomEdgeSample = Lists.newArrayList(
        multiSegmentPowerLawBipartiteGraph.getRandomLeftNodeEdges(10, numSamples, random));
    assertEquals(numSamples, leftNodeRandomEdgeSample.size());
    for (Long id : leftNodeRandomEdgeSample) {
      assertTrue(leftNodeEdgeSet.contains(id));
    }
    // checking an arbitrary node
    assertEquals(13, multiSegmentPowerLawBipartiteGraph.getRightNodeDegree(395));
    Set<Long> rightNodeEdgeSet =
        Sets.newHashSet(multiSegmentPowerLawBipartiteGraph.getRightNodeEdges(395));
    assertEquals(13, rightNodeEdgeSet.size());
    List<Long> rightNodeRandomEdgeSample = Lists.newArrayList(
        multiSegmentPowerLawBipartiteGraph.getRandomRightNodeEdges(395, numSamples, random));
    assertEquals(numSamples, rightNodeRandomEdgeSample.size());
    for (Long id : rightNodeRandomEdgeSample) {
      assertTrue(rightNodeEdgeSet.contains(id));
    }
  }

  @Test
  public void testConcurrentReadWrites() throws Exception {
    MultiSegmentPowerLawBipartiteGraph multiSegmentPowerLawBipartiteGraph =
        new MultiSegmentPowerLawBipartiteGraph(
            4, 3, 4, 1, 2.0, 3, 2, 2.0, new IdentityEdgeTypeMask(), new NullStatsReceiver());

    @SuppressWarnings("unchecked")
    List<Pair<Long, Long>> edgesToAdd = Lists.newArrayList(
        Pair.of(1L, 11L),
        Pair.of(1L, 12L),
        Pair.of(4L, 41L),
        Pair.of(2L, 21L),
        Pair.of(4L, 42L),
        Pair.of(3L, 31L),
        Pair.of(2L, 22L),
        Pair.of(1L, 13L),
        Pair.of(4L, 43L),
        Pair.of(5L, 51L) // violates the max num nodes assumption
    );

    testConcurrentReadWriteThreads(multiSegmentPowerLawBipartiteGraph, edgesToAdd);
  }

  @Test
  public void testRandomConcurrentReadWrites() throws Exception {
    int maxNumSegments = 10;
    int maxNumEdgesPerSegment = 1500;
    int numLeftNodes = 10;
    int numRightNodes = 100;
    MultiSegmentPowerLawBipartiteGraph multiSegmentPowerLawBipartiteGraph =
        new MultiSegmentPowerLawBipartiteGraph(
            maxNumSegments,
            maxNumEdgesPerSegment,
            numLeftNodes,
            numRightNodes,
            8.0,
            numRightNodes,
            numLeftNodes / 2,
            2.0,
            new IdentityEdgeTypeMask(),
            new NullStatsReceiver());

    // Sets up a concurrent read-write situation with the given pool and edges
    Random random = new Random(89234758923475L);

    // total number of threads needed = 3 * 10 * numLeftNodes + 3 * numRightNodes = 600
    testRandomConcurrentReadWriteThreads(
        multiSegmentPowerLawBipartiteGraph, 10, 10 * numLeftNodes, numRightNodes, 0.1, random);
  }

  /**
   * This test is here as an example of checking for a memory leak: the idea here is to start with
   * a limited heap so that the JVM is forced to reclaim memory, enabling us to check that memory
   * from old segments is reclaimed correctly. For 18MB of heap given to the test and 1000 rounds,
   * we should see 10s of GC cycles, and the large number of cycles ensure that this test will fail
   * if there is a big memory leak. One way to check that this is working properly is to grep
   * the test output log like this:
   * grep "Free memory" .pants.d/test/junit/TEST-com.twitter.graphjet.bipartite.MultiSegmentPowerLawBipartiteGraphTest.xml | sed 's/.*: //'
   * and then plot the data to see a oscillatory pattern that indicates memory reclaiming working
   * as expected.
   */
  @Test
  public void testMemoryRecycling() throws Exception {
    int maxNumSegments = 10;
    int maxNumEdgesPerSegment = 8192;
    int leftSize = 100;
    int rightSize = 1000;
    double edgeProbability = 0.5; // this implies ~50K edges
    int numRounds = 1000;

    MultiSegmentPowerLawBipartiteGraph multiSegmentPowerLawBipartiteGraph =
        new MultiSegmentPowerLawBipartiteGraph(
            maxNumSegments,
            maxNumEdgesPerSegment,
            leftSize / 2,
            (int) (rightSize * edgeProbability / 2),
            2.0,
            rightSize / 2,
            (int) (leftSize * edgeProbability / 2),
            2.0,
            new IdentityEdgeTypeMask(),
            new NullStatsReceiver());

    for (int round = 0; round < numRounds; round++) {
      Random random = new Random(8904572034987501L);
      int numSamples = 10;
      for (int i = 0; i < leftSize; i++) {
        for (int j = 0; j < rightSize; j++) {
          if (random.nextDouble() < edgeProbability) {
            multiSegmentPowerLawBipartiteGraph.addEdge(i, j, (byte) 0);
          }
        }
      }
      // on average, degree is a 500
      assertEquals(511, multiSegmentPowerLawBipartiteGraph.getLeftNodeDegree(10));
      Set<Long> leftNodeEdgeSet =
          Sets.newHashSet(multiSegmentPowerLawBipartiteGraph.getLeftNodeEdges(10));
      // all edges are unique
      assertEquals(511, leftNodeEdgeSet.size());
      List<Long> leftNodeRandomEdgeSample = Lists.newArrayList(
          multiSegmentPowerLawBipartiteGraph.getRandomLeftNodeEdges(10, numSamples, random));
      assertEquals(numSamples, leftNodeRandomEdgeSample.size());
      for (Long id : leftNodeRandomEdgeSample) {
        assertTrue(leftNodeEdgeSet.contains(id));
      }
      // checking an arbitrary node
      System.out.println("=== Round " + round);
      assertTrue(multiSegmentPowerLawBipartiteGraph.getRightNodeDegree(395) > 10);
      Set<Long> rightNodeEdgeSet =
          Sets.newHashSet(multiSegmentPowerLawBipartiteGraph.getRightNodeEdges(395));
      List<Long> rightNodeRandomEdgeSample = Lists.newArrayList(
          multiSegmentPowerLawBipartiteGraph.getRandomRightNodeEdges(395, numSamples, random));
      assertEquals(numSamples, rightNodeRandomEdgeSample.size());
      for (Long id : rightNodeRandomEdgeSample) {
        assertTrue(rightNodeEdgeSet.contains(id));
      }
      System.out.println("Total memory available to JVM (bytes): "
          + Runtime.getRuntime().totalMemory());
      System.out.println("Free memory (bytes): " + Runtime.getRuntime().freeMemory());
    }
  }
}
