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

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.tuple.Pair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.bipartite.api.DynamicBipartiteGraph;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This is a utility class that contains common paradigms for testing concurrent read-writes for
 * a {@link BipartiteGraph}.
 */
public final class GraphConcurrentTestHelper {

  private GraphConcurrentTestHelper() {
    // Utility class
  }

  /**
   * Helper class to allow reading from a
   * {@link BipartiteGraph} in a controlled manner.
   */
  public static class BipartiteGraphReader implements Runnable {
    private final BipartiteGraph bipartiteGraph;
    private final CountDownLatch startSignal;
    private final CountDownLatch doneSignal;
    private final long queryNode;
    private final boolean leftOrRight;
    private final int sleepTimeInMilliseconds;
    private int queryNodeDegree;
    private LongArrayList queryNodeEdges;

    public BipartiteGraphReader(
        BipartiteGraph bipartiteGraph,
        CountDownLatch startSignal,
        CountDownLatch doneSignal,
        long queryNode,
        boolean leftOrRight,
        int sleepTimeInMilliseconds) {
      this.bipartiteGraph = bipartiteGraph;
      this.startSignal = startSignal;
      this.doneSignal = doneSignal;
      this.queryNode = queryNode;
      this.leftOrRight = leftOrRight;
      this.sleepTimeInMilliseconds = sleepTimeInMilliseconds;
    }

    @Override
    public void run() {
      try {
        startSignal.await();
        Thread.sleep(sleepTimeInMilliseconds);
      } catch (InterruptedException e) {
        throw new RuntimeException("Unable to start waiting: ", e);
      }
      if (leftOrRight) {
        queryNodeDegree = bipartiteGraph.getLeftNodeDegree(queryNode);
        if (queryNodeDegree > 0) {
          queryNodeEdges = new LongArrayList(bipartiteGraph.getLeftNodeEdges(queryNode));
        }
      } else {
        queryNodeDegree = bipartiteGraph.getRightNodeDegree(queryNode);
        if (queryNodeDegree > 0) {
          queryNodeEdges = new LongArrayList(bipartiteGraph.getRightNodeEdges(queryNode));
        }
      }
      doneSignal.countDown();
    }

    public int getQueryNodeDegree() {
      return queryNodeDegree;
    }

    public LongArrayList getQueryNodeEdges() {
      return queryNodeEdges;
    }
  }

  /**
   * Helper class to allow writing to a
   * {@link DynamicBipartiteGraph} in a controlled manner.
   */
  public static class BipartiteGraphWriter implements Runnable {
    private final DynamicBipartiteGraph bipartiteGraph;
    private final List<WriterInfo> writerInfoList;

    public BipartiteGraphWriter(
        DynamicBipartiteGraph bipartiteGraph, List<WriterInfo> writerInfoList) {
      this.bipartiteGraph = bipartiteGraph;
      this.writerInfoList = writerInfoList;
    }

    @Override
    public void run() {
      int i = 0;
      for (WriterInfo writerInfo : writerInfoList) {
        try {
          writerInfo.startSignal.await();
        } catch (InterruptedException e) {
          throw new RuntimeException("Interrupted while waiting: ", e);
        }
        bipartiteGraph.addEdge(writerInfo.leftNode, writerInfo.rightNode, (byte) 0);
        writerInfo.doneSignal.countDown();
      }
    }
  }

  /**
   * This class encapsulates information needed by a writer to process a single edge.
   */
  public static class WriterInfo {
    private final long leftNode;
    private final long rightNode;
    private final CountDownLatch startSignal;
    private final CountDownLatch doneSignal;

    public WriterInfo(
        long leftNode, long rightNode, CountDownLatch startSignal, CountDownLatch doneSignal) {
      this.leftNode = leftNode;
      this.rightNode = rightNode;
      this.startSignal = startSignal;
      this.doneSignal = doneSignal;
    }
  }

  /**
   * This helper method tests up a concurrent read-write situation with a single writer and multiple
   * readers that access the same underlying bipartiteGraph, and tests for correct edge access after
   * every single edge write via latches. This helps test write flushing after every edge insertion.
   *
   * @param graph           is the underlying
   *                        {@link BipartiteGraph}
   * @param edgesToAdd      is a list of edges to add in the graph
   */
  public static <T extends BipartiteGraph & DynamicBipartiteGraph>
  void testConcurrentReadWriteThreads(
      T graph, List<Pair<Long, Long>> edgesToAdd) {
    int numReaders = edgesToAdd.size(); // start reading after first edge is written
    ExecutorService executor = Executors.newFixedThreadPool(2 * (2 * numReaders) + 1);

    List<CountDownLatch> readerStartLatches = Lists.newArrayListWithCapacity(numReaders);
    List<CountDownLatch> readerDoneLatches = Lists.newArrayListWithCapacity(numReaders);
    List<BipartiteGraphReader> leftReaders = Lists.newArrayListWithCapacity(numReaders);
    List<BipartiteGraphReader> rightReaders = Lists.newArrayListWithCapacity(numReaders);

    for (Pair<Long, Long> edge : edgesToAdd) {
      CountDownLatch startLatch = new CountDownLatch(1);
      CountDownLatch doneLatch = new CountDownLatch(2);
      // Each time, get edges for the node added in the previous step
      BipartiteGraphReader leftReader =
          new BipartiteGraphReader(
              graph,
              startLatch,
              doneLatch,
              edge.getLeft(),
              true,
              0);
      BipartiteGraphReader rightReader =
          new BipartiteGraphReader(
              graph,
              startLatch,
              doneLatch,
              edge.getRight(),
              false,
              0);
      leftReaders.add(leftReader);
      executor.submit(leftReader);
      rightReaders.add(rightReader);
      executor.submit(rightReader);
      readerStartLatches.add(startLatch);
      readerDoneLatches.add(doneLatch);
    }

    /**
     * The start/done latches achieve the following execution order: writer, then reader 1, then
     * writer, then reader 2, and so on. As a concrete example, suppose we have two readers and a
     * writer, then the start/done latches are used as follows:
     * Initial latches state:
     * s1 = 1, d1 = 1
     * s2 = 1, d2 = 1
     * Execution steps:
     * - writer writes edge 1, sets s1 = 0 and waits on d1
     * - reader 1 reads since s1 == 0 and sets d1 = 0
     * - writer writes edge 2, sets s2 = 0 and waits on d2
     * - reader 2 reads since s2 == 0 and sets d2 = 0
     *
     * One detail to note is that here we have two readers (one for left, one for right) so the done
     * latches are initialized to value 2 so that both readers complete the read before moving on.
     */
    List<WriterInfo> writerInfo = Lists.newArrayListWithCapacity(numReaders);
    for (int i = 0; i < numReaders; i++) {
      // Start writing immediately at first, but then write an edge once the reader finishes reading
      // the previous edge
      CountDownLatch startLatch = (i > 0) ? readerDoneLatches.get(i - 1) : new CountDownLatch(0);
      // Release the next reader
      CountDownLatch doneLatch = readerStartLatches.get(i);
      writerInfo.add(new WriterInfo(
          edgesToAdd.get(i).getLeft(),
          edgesToAdd.get(i).getRight(),
          startLatch,
          doneLatch));
    }

    executor.submit(new BipartiteGraphWriter(graph, writerInfo));

    // Wait for all the processes to finish and then confirm that they did what they worked as
    // expected
    try {
      readerDoneLatches.get(numReaders - 1).await();
    } catch (InterruptedException e) {
      throw new RuntimeException("Execution for last reader was interrupted: ", e);
    }

    // Now we test the readers
    Long2ObjectMap<LongArrayList> leftSideGraph =
        new Long2ObjectOpenHashMap<LongArrayList>(numReaders);
    Long2ObjectMap<LongArrayList> rightSideGraph =
        new Long2ObjectOpenHashMap<LongArrayList>(numReaders);
    for (int i = 0; i < numReaders; i++) {
      long leftNode = edgesToAdd.get(i).getLeft();
      long rightNode = edgesToAdd.get(i).getRight();
      // Add edges to the graph
      if (!leftSideGraph.containsKey(leftNode)) {
        leftSideGraph.put(leftNode, new LongArrayList(new long[]{rightNode}));
      } else {
        leftSideGraph.get(leftNode).add(rightNode);
      }
      if (!rightSideGraph.containsKey(rightNode)) {
        rightSideGraph.put(rightNode, new LongArrayList(new long[]{leftNode}));
      } else {
        rightSideGraph.get(rightNode).add(leftNode);
      }
      // Check the read info
      assertEquals(leftSideGraph.get(leftNode).size(), leftReaders.get(i).getQueryNodeDegree());
      assertEquals(leftSideGraph.get(leftNode), leftReaders.get(i).getQueryNodeEdges());
      assertEquals(rightSideGraph.get(rightNode).size(), rightReaders.get(i).getQueryNodeDegree());
      assertEquals(rightSideGraph.get(rightNode), rightReaders.get(i).getQueryNodeEdges());
    }
  }

  /**
   * This helper method sets up a concurrent read-write situation with a single writer and multiple
   * readers that access the same underlying bipartiteGraph, and tests for correct edge access during
   * simultaneous edge writes. This helps test read consistency during arbitrary points of
   * inserting edges. Note that the exact read-write sequence here is non-deterministic and would
   * vary depending on the machine, but the hope is that given the large number of readers the reads
   * would be done at many different points of edge insertion. The test itself checks only for
   * partial correctness (it could have false positives) so this should only be used as a supplement
   * to other testing.
   *
   * @param graph              is the underlying
   *                           {@link BipartiteGraph}
   * @param numReadersPerNode  is the number of reader threads to use per node
   * @param leftSize           is the number of left nodes
   * @param rightSize          is the number of right nodes
   * @param edgeProbability    is the probability of an edge between a left-right node pair
   * @param random             is the random number generator to use for generating a random graph
   */
  public static <T extends BipartiteGraph & DynamicBipartiteGraph>
  void testRandomConcurrentReadWriteThreads(
      T graph,
      int numReadersPerNode,
      int leftSize,
      int rightSize,
      double edgeProbability,
      Random random) {
    int maxWaitingTimeForThreads = 20; // in milliseconds
    int numLeftReaders = leftSize * numReadersPerNode;
    int numRightReaders = rightSize * numReadersPerNode;
    int totalNumReaders = numLeftReaders + numRightReaders;
    CountDownLatch readersDoneLatch = new CountDownLatch(totalNumReaders);
    // First, construct a random set of edges to insert in the graph
    Set<Pair<Long, Long>> edges =
        Sets.newHashSetWithExpectedSize((int) (leftSize * rightSize * edgeProbability));
    List<BipartiteGraphReader> leftReaders = Lists.newArrayListWithCapacity(numLeftReaders);
    List<BipartiteGraphReader> rightReaders = Lists.newArrayListWithCapacity(numRightReaders);
    Long2ObjectMap<LongSet> leftSideGraph = new Long2ObjectOpenHashMap<LongSet>(leftSize);
    Long2ObjectMap<LongSet> rightSideGraph = new Long2ObjectOpenHashMap<LongSet>(leftSize);
    int averageLeftDegree = (int) (rightSize * edgeProbability);
    for (int i = 0; i < leftSize; i++) {
      LongSet nodeEdges = new LongOpenHashSet(averageLeftDegree);
      for (int j = 0; j < rightSize; j++) {
        if (random.nextDouble() < edgeProbability) {
          nodeEdges.add(j);
          if (!rightSideGraph.containsKey(j)) {
            rightSideGraph.put(j, new LongOpenHashSet(new long[]{i}));
          } else {
            rightSideGraph.get(j).add(i);
          }
          edges.add(Pair.of((long) i, (long) j));
        }
      }
      leftSideGraph.put(i, nodeEdges);
    }

    // Create a bunch of leftReaders per node that'll read from the graph at random
    for (int i = 0; i < leftSize; i++) {
      for (int j = 0; j < numReadersPerNode; j++) {
        leftReaders.add(new BipartiteGraphReader(
            graph,
            new CountDownLatch(0),
            readersDoneLatch,
            i,
            true,
            random.nextInt(maxWaitingTimeForThreads)));
      }
    }

    // Create a bunch of rightReaders per node that'll read from the graph at random
    for (int i = 0; i < rightSize; i++) {
      for (int j = 0; j < numReadersPerNode; j++) {
        rightReaders.add(new BipartiteGraphReader(
            graph,
            new CountDownLatch(0),
            readersDoneLatch,
            i,
            false,
            random.nextInt(maxWaitingTimeForThreads)));
      }
    }

    // Create a single writer that will insert these edges in random order
    List<WriterInfo> writerInfo = Lists.newArrayListWithCapacity(edges.size());
    List<Pair<Long, Long>> edgesList = Lists.newArrayList(edges);
    Collections.shuffle(edgesList);
    CountDownLatch writerDoneLatch = new CountDownLatch(edgesList.size());
    for (Pair<Long, Long> edge : edgesList) {
      writerInfo.add(new WriterInfo(
          edge.getLeft(),
          edge.getRight(),
          new CountDownLatch(0),
          writerDoneLatch));
    }

    ExecutorService executor =
        Executors.newFixedThreadPool(totalNumReaders + 1); // single writer
    List<Callable<Integer>> allThreads = Lists.newArrayListWithCapacity(totalNumReaders + 1);
    // First, we add the writer
    allThreads.add(Executors.callable(new BipartiteGraphWriter(graph, writerInfo), 1));
    // then the readers
    for (int i = 0; i < numLeftReaders; i++) {
      allThreads.add(Executors.callable(leftReaders.get(i), 1));
    }
    for (int i = 0; i < numRightReaders; i++) {
      allThreads.add(Executors.callable(rightReaders.get(i), 1));
    }
    // these will execute in some non-deterministic order
    Collections.shuffle(allThreads, random);

    // Wait for all the processes to finish
    try {
      List<Future<Integer>> results = executor.invokeAll(allThreads, 10, TimeUnit.SECONDS);
      for (Future<Integer> result : results) {
        assertTrue(result.isDone());
        assertEquals(1, result.get().intValue());
      }
    } catch (InterruptedException e) {
      throw new RuntimeException("Execution for a thread was interrupted: ", e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Execution issue in an executor thread: ", e);
    }

    // confirm that these worked as expected
    try {
      readersDoneLatch.await();
      writerDoneLatch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException("Execution for a latch was interrupted: ", e);
    }

    // Check that all readers' read info is consistent with the graph
    // first check the left side
    for (int i = 0; i < numLeftReaders; i++) {
      LongSet expectedLeftEdges = leftSideGraph.get(leftReaders.get(i).queryNode);
      assertTrue(leftReaders.get(i).getQueryNodeDegree() <= expectedLeftEdges.size());
      if (leftReaders.get(i).getQueryNodeDegree() == 0) {
        assertNull(leftReaders.get(i).getQueryNodeEdges());
      } else {
        for (long edge : leftReaders.get(i).getQueryNodeEdges()) {
          assertTrue(expectedLeftEdges.contains(edge));
        }
      }
    }

    // then the right side
    for (int i = 0; i < numRightReaders; i++) {
      LongSet expectedRightEdges = rightSideGraph.get(rightReaders.get(i).queryNode);
      assertTrue(rightReaders.get(i).getQueryNodeDegree() <= expectedRightEdges.size());
      if (rightReaders.get(i).getQueryNodeDegree() == 0) {
        assertNull(rightReaders.get(i).getQueryNodeEdges());
      } else {
        for (long edge : rightReaders.get(i).getQueryNodeEdges()) {
          assertTrue(expectedRightEdges.contains(edge));
        }
      }
    }
  }
}

