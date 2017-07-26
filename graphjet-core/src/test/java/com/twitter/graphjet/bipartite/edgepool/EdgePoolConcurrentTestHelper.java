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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * This is a utility class that contains common paradigms for testing concurrent read-writes for
 * an {@link com.twitter.graphjet.bipartite.edgepool.EdgePool}.
 */
public final class EdgePoolConcurrentTestHelper {

  private EdgePoolConcurrentTestHelper() {
    // Utility class
  }

  /**
   * Helper class to allow reading from a
   * {@link com.twitter.graphjet.bipartite.edgepool.EdgePool} in a controlled manner.
   */
  public static class EdgePoolReader implements Runnable {
    private final EdgePool edgePool;
    private final CountDownLatch startSignal;
    private final CountDownLatch doneSignal;
    private final int queryNode;
    private final int sleepTimeInMilliseconds;
    private int queryNodeDegree;
    private IntArrayList queryNodeEdges;

    public EdgePoolReader(
        EdgePool edgePool,
        CountDownLatch startSignal,
        CountDownLatch doneSignal,
        int queryNode,
        int sleepTimeInMilliseconds) {
      this.edgePool = edgePool;
      this.startSignal = startSignal;
      this.doneSignal = doneSignal;
      this.queryNode = queryNode;
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
      queryNodeDegree = edgePool.getNodeDegree(queryNode);
      if (queryNodeDegree > 0) {
        queryNodeEdges = new IntArrayList(edgePool.getNodeEdges(queryNode));
      }
      doneSignal.countDown();
    }

    public int getQueryNodeDegree() {
      return queryNodeDegree;
    }

    public IntArrayList getQueryNodeEdges() {
      return queryNodeEdges;
    }
  }

  /**
   * Helper class to allow writing to a
   * {@link com.twitter.graphjet.bipartite.edgepool.EdgePool} in a controlled manner.
   */
  public static class EdgePoolWriter implements Runnable {
    private final EdgePool edgePool;
    private final List<WriterInfo> writerInfoList;

    public EdgePoolWriter(EdgePool edgePool, List<WriterInfo> writerInfoList) {
      this.edgePool = edgePool;
      this.writerInfoList = writerInfoList;
    }

    @Override
    public void run() {
      for (WriterInfo writerInfo : writerInfoList) {
        try {
          writerInfo.startSignal.await();
        } catch (InterruptedException e) {
          throw new RuntimeException("Interrupted while waiting: ", e);
        }
        edgePool.addEdge(writerInfo.leftNode, writerInfo.rightNode);
        writerInfo.doneSignal.countDown();
      }
    }
  }

  /**
   * This class encapsulates information needed by a writer to process a single edge.
   */
  public static class WriterInfo {
    private final int leftNode;
    private final int rightNode;
    private final CountDownLatch startSignal;
    private final CountDownLatch doneSignal;

    public WriterInfo(
        int leftNode, int rightNode, CountDownLatch startSignal, CountDownLatch doneSignal) {
      this.leftNode = leftNode;
      this.rightNode = rightNode;
      this.startSignal = startSignal;
      this.doneSignal = doneSignal;
    }
  }

  /**
   * This helper method sets up a concurrent read-write situation with a single writer and multiple
   * readers that access the same underlying edgePool, and tests for correct edge access after
   * every single edge write via latches. This helps test write flushing after every edge insertion.
   *
   * @param edgePool    is the underlying {@link com.twitter.graphjet.bipartite.edgepool.EdgePool}
   * @param edgesToAdd  is a list of edges to add in the graph
   * @return the readers that store the state that they saw so that the state can be tested. There
   *         is a reader for every edge insertion.
   */
  public static List<EdgePoolReader> runConcurrentReadWriteThreads(
      EdgePool edgePool, List<Pair<Integer, Integer>> edgesToAdd) {
    int numReaders = edgesToAdd.size(); // start reading after first edge is written
    ExecutorService executor = Executors.newFixedThreadPool(numReaders + 1); // single writer

    List<CountDownLatch> readerStartLatches = Lists.newArrayListWithCapacity(numReaders);
    List<CountDownLatch> readerDoneLatches = Lists.newArrayListWithCapacity(numReaders);
    List<EdgePoolReader> readers = Lists.newArrayListWithCapacity(numReaders);

    for (Pair<Integer, Integer> edge : edgesToAdd) {
      CountDownLatch startLatch = new CountDownLatch(1);
      CountDownLatch doneLatch = new CountDownLatch(1);
      // Each time, get edges for the node added in the previous step
      EdgePoolReader edgePoolReader =
          new EdgePoolReader(
              edgePool,
              startLatch,
              doneLatch,
              edge.getLeft(),
              0);
      readers.add(edgePoolReader);
      executor.submit(edgePoolReader);
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
     */
    List<WriterInfo> writerInfo = Lists.newArrayListWithCapacity(edgesToAdd.size());
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

    executor.submit(new EdgePoolWriter(edgePool, writerInfo));

    // Wait for all the processes to finish and then confirm that they did what they worked as
    // expected
    try {
      readerDoneLatches.get(numReaders - 1).await();
    } catch (InterruptedException e) {
      throw new RuntimeException("Execution for last reader was interrupted: ", e);
    }
    return readers;
  }

  /**
   * This helper method sets up a concurrent read-write situation with a single writer and multiple
   * readers that access the same underlying edgePool, and tests for correct edge access during
   * simultaneous edge writes. This helps test read consistency during arbitrary points of
   * inserting edges. Note that the exact read-write sequence here is non-deterministic and would
   * vary depending on the machine, but the hope is that given the large number of readers the reads
   * would be done at many different points of edge insertion. The test itself checks only for
   * partial correctness (it could have false positives) so this should only be used as a supplement
   * to other testing.
   *
   * @param edgePool           is the underlying
   *                           {@link com.twitter.graphjet.bipartite.edgepool.EdgePool}
   * @param numReadersPerNode  is the number of reader threads to use per node
   * @param leftSize           is the number of left nodes
   * @param rightSize          is the number of right nodes
   * @param edgeProbability    is the probability of an edge between a left-right node pair
   * @param random             is the random number generator to use for generating a random graph
   */
  public static void testRandomConcurrentReadWriteThreads(
      EdgePool edgePool,
      int numReadersPerNode,
      int leftSize,
      int rightSize,
      double edgeProbability,
      Random random) {
    int maxWaitingTimeForThreads = 20; // in milliseconds
    int numReaders = leftSize * numReadersPerNode;
    CountDownLatch readersDoneLatch = new CountDownLatch(numReaders);
    // First, construct a random set of edges to insert in the graph
    Set<Pair<Integer, Integer>> edges =
        Sets.newHashSetWithExpectedSize((int) (leftSize * rightSize * edgeProbability));
    List<EdgePoolReader> readers = Lists.newArrayListWithCapacity(numReaders);
    Int2ObjectMap<IntSet> leftSideGraph = new Int2ObjectOpenHashMap<IntSet>(leftSize);
    int averageLeftDegree = (int) (rightSize * edgeProbability);
    for (int i = 0; i < leftSize; i++) {
      IntSet nodeEdges = new IntOpenHashSet(averageLeftDegree);
      for (int j = 0; j < rightSize; j++) {
        if (random.nextDouble() < edgeProbability) {
          nodeEdges.add(j);
          edges.add(Pair.of(i, j));
        }
      }
      leftSideGraph.put(i, nodeEdges);
    }

    // Create a bunch of leftReaders per node that'll read from the graph at random
    for (int i = 0; i < leftSize; i++) {
      for (int j = 0; j < numReadersPerNode; j++) {
        readers.add(new EdgePoolReader(
            edgePool,
            new CountDownLatch(0),
            readersDoneLatch,
            i,
            random.nextInt(maxWaitingTimeForThreads)));
      }
    }

    // Create a single writer that will insert these edges in random order
    List<WriterInfo> writerInfo = Lists.newArrayListWithCapacity(edges.size());
    List<Pair<Integer, Integer>> edgesList = Lists.newArrayList(edges);
    Collections.shuffle(edgesList);
    CountDownLatch writerDoneLatch = new CountDownLatch(edgesList.size());
    for (Pair<Integer, Integer> edge : edgesList) {
      writerInfo.add(new WriterInfo(
          edge.getLeft(),
          edge.getRight(),
          new CountDownLatch(0),
          writerDoneLatch));
    }

    ExecutorService executor =
        Executors.newFixedThreadPool(numReaders + 1); // single writer
    List<Callable<Integer>> allThreads = Lists.newArrayListWithCapacity(numReaders + 1);
    // First, we add the writer
    allThreads.add(Executors.callable(new EdgePoolWriter(edgePool, writerInfo), 1));
    // then the readers
    for (int i = 0; i < numReaders; i++) {
      allThreads.add(Executors.callable(readers.get(i), 1));
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
      throw new RuntimeException("Execution for last reader was interrupted: ", e);
    }

    // Check that all readers' read info is consistent with the graph
    for (EdgePoolReader reader : readers) {
      IntSet expectedEdges = leftSideGraph.get(reader.queryNode);
      assertTrue(reader.getQueryNodeDegree() <= expectedEdges.size());
      if (reader.getQueryNodeDegree() == 0) {
        assertNull(reader.getQueryNodeEdges());
      } else {
        for (int edge : reader.getQueryNodeEdges()) {
          assertTrue(expectedEdges.contains(edge));
        }
      }
    }
  }
}
