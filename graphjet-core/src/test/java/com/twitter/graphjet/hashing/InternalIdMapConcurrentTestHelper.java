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


package com.twitter.graphjet.hashing;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;

public final class InternalIdMapConcurrentTestHelper {

  private InternalIdMapConcurrentTestHelper() {
    // Utility class
  }

  /**
   * Helper class to allow reading from a {@link LongToInternalIntBiMap} in a controlled manner.
   */
  public static class LongToInternalIntBiMapReader implements Runnable {
    private final LongToInternalIntBiMap longToInternalIntBiMap;
    private final CountDownLatch startSignal;
    private final CountDownLatch doneSignal;
    private final long key;
    private final int sleepTimeInMilliseconds;
    private int value;

    public LongToInternalIntBiMapReader(
        LongToInternalIntBiMap longToInternalIntBiMap,
        CountDownLatch startSignal,
        CountDownLatch doneSignal,
        long key,
        int sleepTimeInMilliseconds) {
      this.longToInternalIntBiMap = longToInternalIntBiMap;
      this.startSignal = startSignal;
      this.doneSignal = doneSignal;
      this.key = key;
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
      value = longToInternalIntBiMap.get(key);
      doneSignal.countDown();
    }

    public int getValue() {
      return value;
    }
  }

  /**
   * Helper class to allow writing to a {@link LongToInternalIntBiMap} in a controlled manner.
   */
  public static class LongToInternalIntBiMapWriter implements Runnable {
    private final LongToInternalIntBiMap longToInternalIntBiMap;
    private final MapWriterInfo mapWriterInfo;

    public LongToInternalIntBiMapWriter(
        LongToInternalIntBiMap longToInternalIntBiMap, MapWriterInfo mapWriterInfo) {
      this.longToInternalIntBiMap = longToInternalIntBiMap;
      this.mapWriterInfo = mapWriterInfo;
    }

    @Override
    public void run() {
      for (long key : mapWriterInfo.keys) {
        try {
          mapWriterInfo.startSignal.await();
        } catch (InterruptedException e) {
          throw new RuntimeException("Interrupted while waiting: ", e);
        }
        longToInternalIntBiMap.put(key);
        mapWriterInfo.doneSignal.countDown();
      }
    }
  }

  /**
   * This class encapsulates information needed by a writer to add to a map.
   */
  public static class MapWriterInfo {
    private final List<Long> keys;
    private final CountDownLatch startSignal;
    private final CountDownLatch doneSignal;

    public MapWriterInfo(List<Long> keys, CountDownLatch startSignal, CountDownLatch doneSignal) {
      this.keys = keys;
      this.startSignal = startSignal;
      this.doneSignal = doneSignal;
    }
  }

  /**
   * This helper method sets up a concurrent read-write situation with a single writer and multiple
   * readers that access the same underlying map, and tests for correct recovery of entries after
   * every single entry insertion, via the use of latches. This helps test write flushing after
   * every entry insertion.
   *
   * @param map             is the underlying {@link LongToInternalIntBiMap}
   * @param keyTestInfo     contains all the keysAndValues to add to the map
   */
  public static void testConcurrentReadWrites(
      LongToInternalIntBiMap map, InternalIdMapTestHelper.KeyTestInfo keyTestInfo) {
    int numReaders = keyTestInfo.keys.length; // start reading after first edge is written
    ExecutorService executor = Executors.newFixedThreadPool(numReaders + 1); // single writer

    List<CountDownLatch> readerStartLatches = Lists.newArrayListWithCapacity(numReaders);
    List<CountDownLatch> readerDoneLatches = Lists.newArrayListWithCapacity(numReaders);
    List<LongToInternalIntBiMapReader> readers = Lists.newArrayListWithCapacity(numReaders);

    for (long key : keyTestInfo.keys) {
      CountDownLatch startLatch = new CountDownLatch(1);
      CountDownLatch doneLatch = new CountDownLatch(1);
      // Each time, get edges for the node added in the previous step
      LongToInternalIntBiMapReader mapReader =
          new LongToInternalIntBiMapReader(
              map,
              startLatch,
              doneLatch,
              key,
              0);
      readers.add(mapReader);
      executor.submit(mapReader);
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
    for (int i = 0; i < numReaders; i++) {
      // Start writing immediately at first, but then write an edge once the reader finishes reading
      // the previous edge
      CountDownLatch startLatch = (i > 0) ? readerDoneLatches.get(i - 1) : new CountDownLatch(0);
      // Release the next reader
      CountDownLatch doneLatch = readerStartLatches.get(i);
      List<Long> keyList = Lists.newArrayList();
      keyList.add(keyTestInfo.keys[i]);
      executor.submit(
          new LongToInternalIntBiMapWriter(map, new MapWriterInfo(keyList, startLatch, doneLatch)));
    }

    // Wait for all the processes to finish and then confirm that they did what they worked as
    // expected
    try {
      readerDoneLatches.get(numReaders - 1).await();
    } catch (InterruptedException e) {
      throw new RuntimeException("Execution for last reader was interrupted: ", e);
    }

    // Check that all readers' read info is consistent with the map
    for (LongToInternalIntBiMapReader reader : readers) {
      int expectedValue = map.get(reader.key);
      // either the entry was not written at the time it was read or we get the right value
      assertTrue(reader.getValue() == expectedValue);
    }
  }

  /**
   * This helper method sets up a concurrent read-write situation with a single writer and multiple
   * readers that access the same underlying LongToInternalIntBiMap, and tests for correct entry
   * access during simultaneous entry reads. This helps test read consistency during arbitrary
   * points of inserting entries. Note that the exact read-write sequence here is non-deterministic
   * and would vary depending on the machine, but the hope is that given the large number of readers
   * the reads would be done at many different points of edge insertion. The test itself checks only
   * for partial correctness (it could have false positives) so this should only be used as a
   * supplement to other testing.
   *
   * @param longToInternalIntBiMap  is the underlying {@link LongToInternalIntBiMap}
   * @param defaultValue            is the default value returned by the map for a non-entry
   * @param numReaders              is the number of reader threads to use
   * @param numKeysToInsert         is the number of keysAndValues to insert in the map
   * @param random                  is the random number generator to use for generating the keysAndValues
   */
  public static void testRandomConcurrentReadWriteThreads(
      LongToInternalIntBiMap longToInternalIntBiMap,
      int defaultValue,
      int numReaders,
      int numKeysToInsert,
      Random random) {
    int maxWaitingTimeForThreads = 20; // in milliseconds
    CountDownLatch readersDoneLatch = new CountDownLatch(numReaders);
    // First, construct a random set of edges to insert in the graph
    Long2IntMap keysToValueMap = new Long2IntOpenHashMap(numKeysToInsert);
    List<LongToInternalIntBiMapReader> readers = Lists.newArrayListWithCapacity(numReaders);
    for (int i = 0; i < numKeysToInsert; i++) {
      keysToValueMap.put(random.nextLong(), -1);
    }

    // Create a bunch of readers that'll read from the map at random
    LongIterator longIterator = keysToValueMap.keySet().iterator();
    for (int i = 0; i < numReaders; i++) {
      readers.add(new LongToInternalIntBiMapReader(
          longToInternalIntBiMap,
          new CountDownLatch(0),
          readersDoneLatch,
          longIterator.nextLong(),
          random.nextInt(maxWaitingTimeForThreads)));
    }

    // Create a single writer that will insert these edges in random order
    List<Long> keyList = Lists.newArrayList(keysToValueMap.keySet());
    Collections.shuffle(keyList, random);
    CountDownLatch writerDoneLatch = new CountDownLatch(keyList.size());
    MapWriterInfo mapWriterInfo =
        new MapWriterInfo(keyList, new CountDownLatch(0), writerDoneLatch);

    ExecutorService executor =
        Executors.newFixedThreadPool(numReaders + 1); // single writer
    List<Callable<Integer>> allThreads = Lists.newArrayListWithCapacity(numReaders + 1);
    // First, we add the writer
    allThreads.add(Executors.callable(
        new LongToInternalIntBiMapWriter(longToInternalIntBiMap, mapWriterInfo), 1));
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

    // Check that all readers' read info is consistent with the map
    for (LongToInternalIntBiMapReader reader : readers) {
      int expectedValue = longToInternalIntBiMap.get(reader.key);
      // either the entry was not written at the time it was read or we get the right value
      assertTrue((reader.getValue() == defaultValue) || (reader.getValue() == expectedValue));
    }
  }
}
