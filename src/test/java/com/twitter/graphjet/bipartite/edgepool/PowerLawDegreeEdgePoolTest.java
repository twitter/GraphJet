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

import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import static com.twitter.graphjet.bipartite.edgepool.EdgePoolConcurrentTestHelper.runConcurrentReadWriteThreads;
import static com.twitter.graphjet.bipartite.edgepool.EdgePoolConcurrentTestHelper.testRandomConcurrentReadWriteThreads;

public class PowerLawDegreeEdgePoolTest {
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
    edgePool.addEdge(1, 14);
    edgePool.addEdge(4, 43);
    edgePool.addEdge(1, 15);
    edgePool.addEdge(1, 16);
    edgePool.addEdge(5, 51); // violates the maxNumNodes param
    edgePool.addEdge(2, 23); // violates the power law assumption
    edgePool.addEdge(1, 17); // violates the max degree param
  }

  /**
   * Test helper that adds edges for a specific graph to the given edge pool. Might be reusable
   * across {@link EdgePool}s
   *
   * @param edgePool  to add edges to
   */
  public static void testAndResetPool(EdgePool edgePool) {
    assertEquals(7, edgePool.getNodeDegree(1));
    assertEquals(3, edgePool.getNodeDegree(2));
    assertEquals(1, edgePool.getNodeDegree(3));
    assertEquals(3, edgePool.getNodeDegree(4));

    assertEquals(new IntArrayList(new int[]{11, 12, 13, 14, 15, 16, 17}),
        new IntArrayList(edgePool.getNodeEdges(1)));
    assertEquals(new IntArrayList(new int[]{21, 22, 23}),
        new IntArrayList(edgePool.getNodeEdges(2)));
    assertEquals(new IntArrayList(new int[]{31}),
        new IntArrayList(edgePool.getNodeEdges(3)));
    assertEquals(new IntArrayList(new int[]{41, 42, 43}),
        new IntArrayList(edgePool.getNodeEdges(4)));
    assertEquals(new IntArrayList(new int[]{51}),
        new IntArrayList(edgePool.getNodeEdges(5)));

    Random random = new Random(90238490238409L);
    int numSamples = 5;

    assertEquals(new IntArrayList(new int[]{13, 13, 11, 15, 14}),
        new IntArrayList(edgePool.getRandomNodeEdges(1, numSamples, random)));
    assertEquals(new IntArrayList(new int[]{22, 22, 21, 23, 22}),
        new IntArrayList(edgePool.getRandomNodeEdges(2, numSamples, random)));
    assertEquals(new IntArrayList(new int[]{31, 31, 31, 31, 31}),
        new IntArrayList(edgePool.getRandomNodeEdges(3, numSamples, random)));
    assertEquals(new IntArrayList(new int[]{43, 41, 43, 41, 42}),
        new IntArrayList(edgePool.getRandomNodeEdges(4, numSamples, random)));
    assertEquals(new IntArrayList(new int[]{51, 51, 51, 51, 51}),
        new IntArrayList(edgePool.getRandomNodeEdges(5, numSamples, random)));

    assertEquals(0.003814697265625, edgePool.getFillPercentage(), EPSILON);

    RecyclePoolMemory.recyclePowerLawDegreeEdgePool((PowerLawDegreeEdgePool) edgePool);
  }

  @Test
  public void testPowerLawDegreeEdgePool() throws Exception {
    int maxNumNodes = 4;
    int maxDegree = 6;
    PowerLawDegreeEdgePool powerLawDegreeEdgePool =
        new PowerLawDegreeEdgePool(maxNumNodes, maxDegree, 2.0, new NullStatsReceiver());

    for (int i = 0; i < 3; i++) {
      addEdgesToPool(powerLawDegreeEdgePool);
      testAndResetPool(powerLawDegreeEdgePool);
    }
  }

  @Test
  public void testGetPoolForEdgeNumber() throws Exception {
    assertEquals(0, PowerLawDegreeEdgePool.getPoolForEdgeNumber(0));
    assertEquals(0, PowerLawDegreeEdgePool.getPoolForEdgeNumber(1));
    assertEquals(1, PowerLawDegreeEdgePool.getPoolForEdgeNumber(2));
    for (int i = 0; i < 1000; i++) {
      assertEquals((int) Math.floor(Math.log(i + 2) / Math.log(2.0) - 1.0),
                   PowerLawDegreeEdgePool.getPoolForEdgeNumber(i));
    }
  }

  @Test
  public void testGetPoolNumberedEdge() throws Exception {
    int maxNumNodes = 4;
    int maxDegree = 6;
    PowerLawDegreeEdgePool powerLawDegreeEdgePool =
        new PowerLawDegreeEdgePool(maxNumNodes, maxDegree, 2.0, new NullStatsReceiver());
    addEdgesToPool(powerLawDegreeEdgePool);

    assertEquals(11, powerLawDegreeEdgePool.getNumberedEdge(1, 0));
    assertEquals(12, powerLawDegreeEdgePool.getNumberedEdge(1, 1));
    assertEquals(13, powerLawDegreeEdgePool.getNumberedEdge(1, 2));
    assertEquals(14, powerLawDegreeEdgePool.getNumberedEdge(1, 3));
    assertEquals(15, powerLawDegreeEdgePool.getNumberedEdge(1, 4));
    assertEquals(16, powerLawDegreeEdgePool.getNumberedEdge(1, 5));
    assertEquals(21, powerLawDegreeEdgePool.getNumberedEdge(2, 0));
    assertEquals(22, powerLawDegreeEdgePool.getNumberedEdge(2, 1));
    assertEquals(31, powerLawDegreeEdgePool.getNumberedEdge(3, 0));
    assertEquals(41, powerLawDegreeEdgePool.getNumberedEdge(4, 0));
    assertEquals(42, powerLawDegreeEdgePool.getNumberedEdge(4, 1));
    assertEquals(43, powerLawDegreeEdgePool.getNumberedEdge(4, 2));
  }

  @Test
  public void testExtremelySkewedGraph() {
    int maxNumNodes = 4;
    int maxDegree = 6;
    PowerLawDegreeEdgePool powerLawDegreeEdgePool =
        new PowerLawDegreeEdgePool(maxNumNodes, maxDegree, 2.0, new NullStatsReceiver());

    int numEdgesToAdd = 100000;
    // Add a normal node
    powerLawDegreeEdgePool.addEdge(0, 2);
    // Then add a very high degree node
    for (int i = 0; i < numEdgesToAdd; i++) {
      powerLawDegreeEdgePool.addEdge(1, i);
    }
    // Add another node like that
    for (int i = 0; i < numEdgesToAdd; i++) {
      powerLawDegreeEdgePool.addEdge(2, i);
    }

    assertEquals(1, powerLawDegreeEdgePool.getNodeDegree(0));
    assertEquals(numEdgesToAdd, powerLawDegreeEdgePool.getNodeDegree(1));
    assertEquals(numEdgesToAdd, powerLawDegreeEdgePool.getNodeDegree(2));
  }

  @Test
  public void testConcurrentReadWrites() {
    int maxNumNodes = 4;
    int maxDegree = 6;
    PowerLawDegreeEdgePool powerLawDegreeEdgePool =
        new PowerLawDegreeEdgePool(maxNumNodes, maxDegree, 2.0, new NullStatsReceiver());

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
        Pair.of(1, 14),
        Pair.of(4, 43),
        Pair.of(1, 15),
        Pair.of(1, 16),
        Pair.of(5, 51), // violates the maxNumNodes param
        Pair.of(2, 23), // violates the power law assumption
        Pair.of(1, 17)  // violates the max degree param
    );

    // Sets up a concurrent read-write situation with the given pool and edges
    List<EdgePoolConcurrentTestHelper.EdgePoolReader> readers =
        runConcurrentReadWriteThreads(powerLawDegreeEdgePool, edgesToAdd);

    // First check that the graph populated correctly
    testAndResetPool(powerLawDegreeEdgePool);

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
    assertEquals(4, readers.get(8).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{11, 12, 13, 14}), readers.get(8).getQueryNodeEdges());
    assertEquals(3, readers.get(9).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{41, 42, 43}), readers.get(9).getQueryNodeEdges());
    assertEquals(5, readers.get(10).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{11, 12, 13, 14, 15}),
                 readers.get(10).getQueryNodeEdges());
    assertEquals(6, readers.get(11).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{11, 12, 13, 14, 15, 16}),
                 readers.get(11).getQueryNodeEdges());
    assertEquals(1, readers.get(12).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{51}), readers.get(12).getQueryNodeEdges());
    assertEquals(3, readers.get(13).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{21, 22, 23}), readers.get(13).getQueryNodeEdges());
    assertEquals(7, readers.get(14).getQueryNodeDegree());
    assertEquals(new IntArrayList(new int[]{11, 12, 13, 14, 15, 16, 17}),
                 readers.get(14).getQueryNodeEdges());
  }


  @Test
  public void testRandomConcurrentReadWrites() {
    int maxNumNodes = 10;
    int maxDegree = 10;
    PowerLawDegreeEdgePool powerLawDegreeEdgePool =
        new PowerLawDegreeEdgePool(maxNumNodes, maxDegree, 2.0, new NullStatsReceiver());

    // Sets up a concurrent read-write situation with the given pool and edges
    Random random = new Random(89234758923475L);
    testRandomConcurrentReadWriteThreads(
        powerLawDegreeEdgePool, 5, 10 * maxNumNodes, maxDegree, 0.1, random);
  }
}
