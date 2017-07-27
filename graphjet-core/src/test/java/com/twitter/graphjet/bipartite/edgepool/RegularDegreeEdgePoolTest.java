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


package com.twitter.graphjet.bipartite.edgepool;

import java.util.List;
import java.util.Random;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import static org.junit.Assert.*;

import com.twitter.graphjet.hashing.IntToIntPairArrayIndexBasedMap;
import com.twitter.graphjet.hashing.ShardedBigIntArray;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import static com.twitter.graphjet.bipartite.edgepool.EdgePoolConcurrentTestHelper.*;

public class RegularDegreeEdgePoolTest {
  private static final double EPSILON = 0.00001;

  /**
   * Test helper that adds edges for a specific graph to the given edge pool. Might be reusable
   * across {@link EdgePool}s
   *
   * @param edgePool  to add edges to
   */
  public static void addEdgesToPool(EdgePool edgePool) {
    edgePool.addEdge(1, 11);
    edgePool.addEdge(1, 12);
    edgePool.addEdge(4, 41);
    edgePool.addEdge(2, 21);
    edgePool.addEdge(4, 42);
    edgePool.addEdge(3, 31);
    edgePool.addEdge(2, 22);
    edgePool.addEdge(1, 13);
    edgePool.addEdge(4, 43);
    edgePool.addEdge(5, 51); // violates the max num nodes assumption
  }

  /**
   * Test helper that adds edges for a specific graph to the given edge pool. Might be reusable
   * across {@link EdgePool}s
   *
   * @param edgePool                to add edges to
   * @param checkFillPercentage     whether to check fill percentage
   * @param expectedFillPercentage  the expected fill percentage, if it is to be checked
   */
  public static void testAndResetPool(
      EdgePool edgePool,
      boolean checkFillPercentage,
      double expectedFillPercentage) {
    assertEquals(3, edgePool.getNodeDegree(1));
    assertEquals(2, edgePool.getNodeDegree(2));
    assertEquals(1, edgePool.getNodeDegree(3));
    assertEquals(3, edgePool.getNodeDegree(4));

    assertEquals(new IntArrayList(new int[]{11, 12, 13}),
        new IntArrayList(edgePool.getNodeEdges(1)));
    assertEquals(new IntArrayList(new int[]{21, 22}),
        new IntArrayList(edgePool.getNodeEdges(2)));
    assertEquals(new IntArrayList(new int[]{31}),
        new IntArrayList(edgePool.getNodeEdges(3)));
    assertEquals(new IntArrayList(new int[]{41, 42, 43}),
        new IntArrayList(edgePool.getNodeEdges(4)));
    assertEquals(new IntArrayList(new int[]{51}),
        new IntArrayList(edgePool.getNodeEdges(5)));

    Random random = new Random(90238490238409L);
    int numSamples = 5;

    assertEquals(new IntArrayList(new int[]{12, 11, 13, 11, 11}),
        new IntArrayList(edgePool.getRandomNodeEdges(1, numSamples, random)));
    assertEquals(new IntArrayList(new int[]{22, 22, 22, 21, 21}),
        new IntArrayList(edgePool.getRandomNodeEdges(2, numSamples, random)));
    assertEquals(new IntArrayList(new int[]{31, 31, 31, 31, 31}),
        new IntArrayList(edgePool.getRandomNodeEdges(3, numSamples, random)));
    assertEquals(new IntArrayList(new int[]{43, 41, 43, 41, 42}),
        new IntArrayList(edgePool.getRandomNodeEdges(4, numSamples, random)));
    assertEquals(new IntArrayList(new int[]{51, 51, 51, 51, 51}),
        new IntArrayList(edgePool.getRandomNodeEdges(5, numSamples, random)));

    if (checkFillPercentage) {
      assertEquals(expectedFillPercentage, edgePool.getFillPercentage(), EPSILON);
    }

    RecyclePoolMemory.recycleRegularDegreeEdgePool((RegularDegreeEdgePool) edgePool);
  }

  @Test
  public void testRegularDegreeEdgePool() throws Exception {
    int maxNumNodes = 4;
    int maxDegree = 3;
    RegularDegreeEdgePool regularDegreeEdgePool =
        new RegularDegreeEdgePool(maxNumNodes, maxDegree, new NullStatsReceiver());

    for (int i = 0; i < 3; i++) {
      addEdgesToPool(regularDegreeEdgePool);
      testAndResetPool(regularDegreeEdgePool, true, 0.00762939453125);
    }
  }

  @Test
  public void testRegularDegreeEdgePoolWithLargeDegree() throws Exception {
    // test with degree > 2M
    int maxNumNodes = 4;
    int maxDegree = 1 << 22;
    RegularDegreeEdgePool regularDegreeEdgePool =
        new RegularDegreeEdgePool(maxNumNodes, maxDegree, new NullStatsReceiver());

    for (int i = 0; i < 3; i++) {
      addEdgesToPool(regularDegreeEdgePool);
      testAndResetPool(regularDegreeEdgePool, false, 0.0);
    }
  }

  @Test
  public void testConcurrentReadWrites() {
    int maxNumNodes = 4;
    int maxDegree = 3;
    RegularDegreeEdgePool regularDegreeEdgePool =
        new RegularDegreeEdgePool(maxNumNodes, maxDegree, new NullStatsReceiver());

    @SuppressWarnings("unchecked")
    List<Pair<Integer, Integer>> edgesToAdd = Lists.newArrayList(
        Pair.of(1, 11),
        Pair.of(1, 12),
        Pair.of(4, 41),
        Pair.of(2, 21),
        Pair.of(4, 42),
        Pair.of(3, 31),
        Pair.of(2, 22),
        Pair.of(1, 13),
        Pair.of(4, 43),
        Pair.of(5, 51) // violates the max num nodes assumption
    );

    // Sets up a concurrent read-write situation with the given pool and edges
    List<EdgePoolReader> readers = runConcurrentReadWriteThreads(regularDegreeEdgePool, edgesToAdd);

    // First check that the graph populated correctly
    testAndResetPool(regularDegreeEdgePool, true, 0.00762939453125);

    // Now test all the readers
    assertEquals(1, readers.get(0).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{11}), readers.get(0).getQueryNodeEdges());
    assertEquals(2, readers.get(1).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{11, 12}), readers.get(1).getQueryNodeEdges());
    assertEquals(1, readers.get(2).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{41}), readers.get(2).getQueryNodeEdges());
    assertEquals(1, readers.get(3).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{21}), readers.get(3).getQueryNodeEdges());
    assertEquals(2, readers.get(4).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{41, 42}), readers.get(4).getQueryNodeEdges());
    assertEquals(1, readers.get(5).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{31}), readers.get(5).getQueryNodeEdges());
    assertEquals(2, readers.get(6).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{21, 22}), readers.get(6).getQueryNodeEdges());
    assertEquals(3, readers.get(7).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{11, 12, 13}), readers.get(7).getQueryNodeEdges());
    assertEquals(3, readers.get(8).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{41, 42, 43}), readers.get(8).getQueryNodeEdges());
    assertEquals(1, readers.get(9).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{51}), readers.get(9).getQueryNodeEdges());
  }

  @Test
  public void testRandomConcurrentReadWrites() {
    int maxNumNodes = 10;
    int maxDegree = 100;
    RegularDegreeEdgePool regularDegreeEdgePool =
            new RegularDegreeEdgePool(maxNumNodes, maxDegree, new NullStatsReceiver());

    // Sets up a concurrent read-write situation with the given pool and edges
    Random random = new Random(89234758923475L);
    testRandomConcurrentReadWriteThreads(
            regularDegreeEdgePool, 3, 10 * maxNumNodes, maxDegree, 0.1, random);
  }

  @Test
  public void testGetNodeEdgeMetadataReturningNegative() {
    NullStatsReceiver nullStatsReceiver = new NullStatsReceiver();
    RegularDegreeEdgePool regularDegreeEdgePool =
            new RegularDegreeEdgePool(4, 4, nullStatsReceiver);
    regularDegreeEdgePool.addEdge(2048, 587);

    assertEquals((-1L), regularDegreeEdgePool.getNodeEdgeMetadata(2048, 426));
  }

  @Test
  public void testAddEdgeTakingThreeArgumentsThrowsIllegalArgumentException() {
    NullStatsReceiver nullStatsReceiver = new NullStatsReceiver();
    RegularDegreeEdgePool regularDegreeEdgePool =
            new RegularDegreeEdgePool(508, 2, nullStatsReceiver);
    ShardedBigIntArray shardedBigIntArray =
            new ShardedBigIntArray(3157, 2, (-1035), nullStatsReceiver);
    IntToIntPairArrayIndexBasedMap intToIntPairArrayIndexBasedMap =
            new IntToIntPairArrayIndexBasedMap(3157, 16, nullStatsReceiver);
    RegularDegreeEdgePool.ReaderAccessibleInfo regularDegreeEdgePool_ReaderAccessibleInfo =
            new RegularDegreeEdgePool.ReaderAccessibleInfo(shardedBigIntArray,
                    intToIntPairArrayIndexBasedMap);
    regularDegreeEdgePool.readerAccessibleInfo = regularDegreeEdgePool_ReaderAccessibleInfo;

    try {
      regularDegreeEdgePool.addEdge(0, 0);
      fail("Expecting exception: IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals(Preconditions.class.getName(), e.getStackTrace()[0].getClassName());
    }
  }

  @Test
  public void testFailsTakingThreeArgumentsOne() {
    try {
      new RegularDegreeEdgePool(2, 0, new NullStatsReceiver());
      fail("Expecting exception: IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals(Preconditions.class.getName(), e.getStackTrace()[0].getClassName());
    }
  }

  @Test
  public void testFailsTakingThreeArgumentsThrowsIllegalArgumentExceptionTwo() {
    try {
      new RegularDegreeEdgePool((-1), (-1), new NullStatsReceiver());
      fail("Expecting exception: IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals(Preconditions.class.getName(), e.getStackTrace()[0].getClassName());
    }
  }

  @Test
  public void testAddEdgeTakingTwoArgumentsReturningMinsOne() {
    RegularDegreeEdgePool regularDegreeEdgePool =
            new RegularDegreeEdgePool(4355, 4355, new NullStatsReceiver());
    regularDegreeEdgePool.addEdge(4355, 4355);

    assertEquals((-1), regularDegreeEdgePool.getNodeEdge(4355, 4355));
  }

  @Test
  public void testIsOptimized() {
    RegularDegreeEdgePool regularDegreeEdgePool =
            new RegularDegreeEdgePool(569, 569, new NullStatsReceiver());

    assertFalse(regularDegreeEdgePool.isOptimized());
  }

}
