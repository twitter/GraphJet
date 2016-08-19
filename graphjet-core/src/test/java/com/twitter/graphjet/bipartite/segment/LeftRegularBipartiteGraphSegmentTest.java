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


package com.twitter.graphjet.bipartite.segment;

import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import static com.twitter.graphjet.bipartite.GraphConcurrentTestHelper.testConcurrentReadWriteThreads;
import static com.twitter.graphjet.bipartite.GraphConcurrentTestHelper.testRandomConcurrentReadWriteThreads;

public class LeftRegularBipartiteGraphSegmentTest {
  private static final double EPSILON = 0.00001;

  private void addEdges(LeftRegularBipartiteGraphSegment leftRegularBipartiteGraphSegment) {
    leftRegularBipartiteGraphSegment.addEdge(1, 11, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(1, 12, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(4, 41, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(2, 21, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(4, 42, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(3, 31, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(2, 22, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(1, 13, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(4, 43, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(5, 11, (byte) 0);
    // violates the max num nodes assumption
  }

  private void testAndResetGraph(
      LeftRegularBipartiteGraphSegment leftRegularBipartiteGraphSegment) {
    assertEquals(3, leftRegularBipartiteGraphSegment.getLeftNodeDegree(1));
    assertEquals(2, leftRegularBipartiteGraphSegment.getLeftNodeDegree(2));
    assertEquals(1, leftRegularBipartiteGraphSegment.getLeftNodeDegree(3));
    assertEquals(3, leftRegularBipartiteGraphSegment.getLeftNodeDegree(4));
    assertEquals(1, leftRegularBipartiteGraphSegment.getRightNodeDegree(13));
    assertEquals(2, leftRegularBipartiteGraphSegment.getRightNodeDegree(11));

    assertEquals(new LongArrayList(new long[]{11, 12, 13}),
        new LongArrayList(leftRegularBipartiteGraphSegment.getLeftNodeEdges(1)));
    assertEquals(new LongArrayList(new long[]{21, 22}),
        new LongArrayList(leftRegularBipartiteGraphSegment.getLeftNodeEdges(2)));
    assertEquals(new LongArrayList(new long[]{31}),
        new LongArrayList(leftRegularBipartiteGraphSegment.getLeftNodeEdges(3)));
    assertEquals(new LongArrayList(new long[]{41, 42, 43}),
        new LongArrayList(leftRegularBipartiteGraphSegment.getLeftNodeEdges(4)));
    assertEquals(new LongArrayList(new long[]{11}),
        new LongArrayList(leftRegularBipartiteGraphSegment.getLeftNodeEdges(5)));
    assertEquals(new LongArrayList(new long[]{1, 5}),
        new LongArrayList(leftRegularBipartiteGraphSegment.getRightNodeEdges(11)));
    assertEquals(new LongArrayList(new long[]{3}),
        new LongArrayList(leftRegularBipartiteGraphSegment.getRightNodeEdges(31)));

    Random random = new Random(90238490238409L);
    int numSamples = 5;

    assertEquals(new LongArrayList(new long[]{12, 11, 13, 11, 11}),
        new LongArrayList(
            leftRegularBipartiteGraphSegment.getRandomLeftNodeEdges(1, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{22, 22, 22, 21, 21}),
        new LongArrayList(
            leftRegularBipartiteGraphSegment.getRandomLeftNodeEdges(2, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{31, 31, 31, 31, 31}),
        new LongArrayList(
            leftRegularBipartiteGraphSegment.getRandomLeftNodeEdges(3, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{43, 41, 43, 41, 42}),
        new LongArrayList(
            leftRegularBipartiteGraphSegment.getRandomLeftNodeEdges(4, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{11, 11, 11, 11, 11}),
        new LongArrayList(
            leftRegularBipartiteGraphSegment.getRandomLeftNodeEdges(5, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{5, 5, 5, 1, 5}),
        new LongArrayList(
            leftRegularBipartiteGraphSegment.getRandomRightNodeEdges(11, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{2, 2, 2, 2, 2}),
        new LongArrayList(
            leftRegularBipartiteGraphSegment.getRandomRightNodeEdges(21, numSamples, random)));

    RecycleSegmentMemory.recycleLeftRegularBipartiteGraphSegment(leftRegularBipartiteGraphSegment);
  }

  /**
   * Build a random left-regular bipartite graph of given left and right sizes.
   *
   * @param leftSize   is the left hand size of the bipartite graph
   * @param rightSize  is the right hand size of the bipartite graph
   * @param leftDegree is the degree of the left hand side
   * @param random     is the random number generator to use for constructing the graph
   * @return a random bipartite graph
   */
  public static LeftRegularBipartiteGraphSegment buildRandomLeftRegularBipartiteGraph(
      int leftSize, int rightSize, int leftDegree, Random random) {
    LeftRegularBipartiteGraphSegment leftRegularBipartiteGraphSegment =
        new LeftRegularBipartiteGraphSegment(
            leftSize / 2,
            leftDegree,
            rightSize / 2,
            leftSize / 2,
            2.0,
            Integer.MAX_VALUE,
            new IdentityEdgeTypeMask(),
            new NullStatsReceiver());
    LongSet addedIds = new LongOpenHashSet(leftDegree);
    for (int i = 0; i < leftSize; i++) {
      addedIds.clear();
      for (int j = 0; j < leftDegree; j++) {
        long idToAdd;
        do {
          idToAdd = random.nextInt(rightSize);
        } while (addedIds.contains(idToAdd));
        addedIds.add(idToAdd);
        leftRegularBipartiteGraphSegment.addEdge(i, idToAdd, (byte) 0);
      }
    }

    return leftRegularBipartiteGraphSegment;
  }

  @Test
  public void testSegmentConstruction() throws Exception {
    LeftRegularBipartiteGraphSegment leftRegularBipartiteGraphSegment =
        new LeftRegularBipartiteGraphSegment(
            4, 3, 2, 1, 2.0, Integer.MAX_VALUE, new IdentityEdgeTypeMask(),
            new NullStatsReceiver());

    for (int i = 0; i < 3; i++) {
      addEdges(leftRegularBipartiteGraphSegment);
      testAndResetGraph(leftRegularBipartiteGraphSegment);
    }
  }

  @Test
  public void testRandomSegmentConstruction() throws Exception {
    int leftSize = 100;
    int rightSize = 10000;
    int leftDegree = 200;
    int numSamples = 10;

    for (int i = 0; i < 3; i++) {
      Random random = new Random(8904572034987501L);
      LeftRegularBipartiteGraphSegment leftRegularBipartiteGraphSegment =
          buildRandomLeftRegularBipartiteGraph(leftSize, rightSize, leftDegree, random);

      assertEquals(leftDegree, leftRegularBipartiteGraphSegment.getLeftNodeDegree(10));
      Set<Long> leftNodeEdgeSet =
          Sets.newHashSet(leftRegularBipartiteGraphSegment.getLeftNodeEdges(10));
      assertEquals(leftDegree, leftNodeEdgeSet.size());
      List<Long> leftNodeRandomEdgeSample = Lists.newArrayList(
          leftRegularBipartiteGraphSegment.getRandomLeftNodeEdges(10, numSamples, random));
      assertEquals(numSamples, leftNodeRandomEdgeSample.size());
      for (Long id : leftNodeRandomEdgeSample) {
        assertTrue(leftNodeEdgeSet.contains(id));
      }
      assertEquals(2, leftRegularBipartiteGraphSegment.getRightNodeDegree(395));
      Set<Long> rightNodeEdgeSet =
          Sets.newHashSet(leftRegularBipartiteGraphSegment.getRightNodeEdges(395));
      assertEquals(2, rightNodeEdgeSet.size());
      List<Long> rightNodeRandomEdgeSample = Lists.newArrayList(
          leftRegularBipartiteGraphSegment.getRandomRightNodeEdges(395, numSamples, random));
      assertEquals(numSamples, rightNodeRandomEdgeSample.size());
      for (Long id : rightNodeRandomEdgeSample) {
        assertTrue(rightNodeEdgeSet.contains(id));
      }

      // This is not a power-law on the right, so the RHS fill percentage should be pretty poor
      assertEquals(15.258789063,
          leftRegularBipartiteGraphSegment.getLeftNodeEdgePoolFillPercentage(), EPSILON);
      assertEquals(3.0517578125,
          leftRegularBipartiteGraphSegment.getRightNodeEdgePoolFillPercentage(), EPSILON);

      RecycleSegmentMemory
          .recycleLeftRegularBipartiteGraphSegment(leftRegularBipartiteGraphSegment);
    }
  }

  @Test
  public void testConcurrentReadWrites() throws Exception {
    LeftRegularBipartiteGraphSegment leftRegularBipartiteGraphSegment =
        new LeftRegularBipartiteGraphSegment(
            4, 3, 2, 1, 2.0, Integer.MAX_VALUE, new IdentityEdgeTypeMask(),
            new NullStatsReceiver());

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

    testConcurrentReadWriteThreads(leftRegularBipartiteGraphSegment, edgesToAdd);
  }

  @Test
  public void testRandomConcurrentReadWrites() throws Exception {
    int numLeftNodes = 10;
    int numRightNodes = 100;
    LeftRegularBipartiteGraphSegment leftRegularBipartiteGraphSegment =
        new LeftRegularBipartiteGraphSegment(
            numLeftNodes,
            numRightNodes,
            numRightNodes,
            numRightNodes,
            2.0,
            Integer.MAX_VALUE,
            new IdentityEdgeTypeMask(),
            new NullStatsReceiver());

    // Sets up a concurrent read-write situation with the given pool and edges
    Random random = new Random(89234758923475L);

    testRandomConcurrentReadWriteThreads(
        leftRegularBipartiteGraphSegment, 3, 10 * numLeftNodes, numRightNodes, 0.1, random);
  }
}
