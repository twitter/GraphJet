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

import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * This class provides a bi-indexed map backed by a long array in which long keys are double-hashed
 * into an array, with the array indices serving as the internal int mapping. All the operations in
 * this class are guaranteed to be atomic, are lock-safe on modern CPUs, and are thread-safe under
 * the single writer and multiple readers model. See detailed notes on thread-safety in
 * {@link ArrayBasedLongToInternalIntFixedLengthBiMap}.
 *
 * Memory usage: let n be the expected number of keys to be inserted. We assume that n/load_factor
 * is a power of two. If the actual number of keys that are inserted is < n, then the memory used by
 * this implementation is 8*n/load_factor bytes. If the number of keys to be inserted is < 1.5n,
 * then memory goes up by 4*n/load_factor bytes, and so on.
 *
 * Performance: assuming a load_factor of no more than .75 and the actual number of inserted keys to
 * be no more than 1.5*expected, each get/set operation should require an average of two hashes
 * and three array lookups, which are both extremely fast. Further, getKey is one array lookup
 * (no hashes). See {@link ArrayBasedLongToInternalIntFixedLengthBiMap} for pointers to hashing
 * analysis. The overhead in this class is going down multiple
 * ArrayBasedLongToInternalIntFixedLengthBiMap(s), and that cost is going to be quite low as long as
 * the number of misses is small.
 */
public class ArrayBasedLongToInternalIntBiMap implements LongToInternalIntBiMap {
  /**
   * This class encapsulates ALL the state that will be accessed by a reader. The final members are
   * used to guarantee visibility to other threads without synchronization/using volatile.
   *
   * From 'Java Concurrency in practice' by Brian Goetz, p. 349:
   *
   * "Initialization safety guarantees that for properly constructed objects, all
   *  threads will see the correct values of final fields that were set by the con-
   *  structor, regardless of how the object is published. Further, any variables
   *  that can be reached through a final field of a properly constructed object
   *  (such as the elements of a final array or the contents of a HashMap refer-
   *  enced by a final field) are also guaranteed to be visible to other threads."
   */
  public static final class ReaderAccessibleInfo {
    private final ArrayBasedLongToInternalIntFixedLengthBiMap[] maps;
    private final int[] mapIndexOffsets;
    private final int[] cumulativeMapLengths;

    /**
     * A new instance is immediately visible to the readers due to publication safety.
     *
     * @param maps                  is the array of fixed-length maps stored
     * @param mapIndexOffsets       is the offset that needs to be added to the indices in the map
     * @param cumulativeMapLengths  stores the sum(map.length) for each map for each access
     */
    public ReaderAccessibleInfo(
        ArrayBasedLongToInternalIntFixedLengthBiMap[] maps,
        int[] mapIndexOffsets,
        int[] cumulativeMapLengths) {
      this.maps = maps;
      this.mapIndexOffsets = mapIndexOffsets;
      this.cumulativeMapLengths = cumulativeMapLengths;
    }
  }

  private final int expectedNumKeys;
  private final double loadFactor;
  private final int defaultGetReturnValue;
  private final long defaultGetKeyReturnValue;

  // stats
  private final StatsReceiver scopedStatsReceiver;
  private final Counter numStoredKeysCounter;
  private final Counter numFixedLengthMapsCounter;
  private final Counter totalAllocatedArrayBytesCounter;

  // only changes by being replaced with a new instance
  private ReaderAccessibleInfo readerAccessibleInfo;

  private int currentActiveMapId;
  private ArrayBasedLongToInternalIntFixedLengthBiMap currentActiveMap;
  private int currentActiveMapIndexOffset;

  /**
   * Returns a new instance that allocates a large array internally.
   *
   * @param expectedNumKeys           is the expected number of keys that will be inserted in this
   *                                  map. Inserting more than these number of keys will degrade
   *                                  performance so this should be a good estimate (within say
   *                                  1.5x of the actual number).
   * @param loadFactor                is the load factor to set: stay below 0.75, and if you want
   *                                  very fast performance, at 0.5.
   * @param defaultGetReturnValue     is what a get returns to indicate empty.
   * @param defaultGetKeyReturnValue  is what a getKey returns to indicate empty. Please note that
   *                                  it is the client's responsibility to ensure that this value is
   *                                  disjoint from the keys in the map.
   * @param statsReceiver             enables keeping stats for this class
   */
  public ArrayBasedLongToInternalIntBiMap(
      int expectedNumKeys,
      double loadFactor,
      int defaultGetReturnValue,
      long defaultGetKeyReturnValue,
      StatsReceiver statsReceiver) {
    this.expectedNumKeys = expectedNumKeys;
    this.loadFactor = loadFactor;
    this.defaultGetReturnValue = defaultGetReturnValue;
    this.defaultGetKeyReturnValue = defaultGetKeyReturnValue;
    this.scopedStatsReceiver = statsReceiver.scope(this.getClass().getSimpleName());
    numStoredKeysCounter = scopedStatsReceiver.counter("numStoredKeys");
    numFixedLengthMapsCounter = scopedStatsReceiver.counter("numFixedLengthMaps");
    totalAllocatedArrayBytesCounter = scopedStatsReceiver.counter("allocatedArrayBytes");
    initialize();
  }

  private void initialize() {
    ArrayBasedLongToInternalIntFixedLengthBiMap[] maps =
        new ArrayBasedLongToInternalIntFixedLengthBiMap[1];
    maps[0] = new ArrayBasedLongToInternalIntFixedLengthBiMap(
        expectedNumKeys,
        loadFactor,
        defaultGetReturnValue,
        defaultGetKeyReturnValue,
        scopedStatsReceiver.scope("0"));
    int[] cumulativeMapLengths = new int[1];
    cumulativeMapLengths[0] = maps[0].getBackingArrayLength();
    int[] mapIndexOffsets = new int[1];
    mapIndexOffsets[0] = 0;
    currentActiveMap = maps[0];
    currentActiveMapId = 0;
    currentActiveMapIndexOffset = 0;
    this.readerAccessibleInfo =
        new ReaderAccessibleInfo(maps, mapIndexOffsets, cumulativeMapLengths);
    numFixedLengthMapsCounter.incr();
    totalAllocatedArrayBytesCounter.incr(8 * currentActiveMap.getBackingArrayLength());
  }

  @Override
  public int get(long key) {
    int value = defaultGetReturnValue;
    // look through the maps in order: a successful search terminates early whereas an unsuccessful
    // one has to go through all the maps
    for (int i = 0; i < readerAccessibleInfo.maps.length; i++) {
      ArrayBasedLongToInternalIntFixedLengthBiMap map = readerAccessibleInfo.maps[i];
      value = map.get(key);
      if (value != defaultGetReturnValue) {
        return value + readerAccessibleInfo.mapIndexOffsets[i];
      }
    }
    return value;
  }

  private void addNewMap() {
    int numMaps = readerAccessibleInfo.maps.length;
    // first the actual maps
    ArrayBasedLongToInternalIntFixedLengthBiMap[] newMaps =
        new ArrayBasedLongToInternalIntFixedLengthBiMap[numMaps + 1];
    System.arraycopy(readerAccessibleInfo.maps, 0, newMaps, 0, numMaps);
    newMaps[numMaps] = new ArrayBasedLongToInternalIntFixedLengthBiMap(
        expectedNumKeys / 4,
        loadFactor,
        defaultGetReturnValue,
        defaultGetKeyReturnValue,
        scopedStatsReceiver.scope(Integer.toString(numMaps)));
    // now the lengths
    int[] newCumulativeMapLengths = new int[numMaps + 1];
    System.arraycopy(
        readerAccessibleInfo.cumulativeMapLengths, 0, newCumulativeMapLengths, 0, numMaps);
    newCumulativeMapLengths[numMaps] =
        newMaps[numMaps].getBackingArrayLength() + newCumulativeMapLengths[numMaps - 1];
    // and the index offsets
    int[] newMapIndexOffsets = new int[numMaps + 1];
    System.arraycopy(readerAccessibleInfo.mapIndexOffsets, 0, newMapIndexOffsets, 0, numMaps);
    newMapIndexOffsets[numMaps] = newCumulativeMapLengths[numMaps - 1];
    // change the active maps, which is only seen by the writer
    currentActiveMapId++;
    currentActiveMap = newMaps[currentActiveMapId];
    currentActiveMapIndexOffset = newMapIndexOffsets[currentActiveMapId];
    // reset the object which should publish the update to all readers
    readerAccessibleInfo = new ReaderAccessibleInfo(
        newMaps,
        newMapIndexOffsets,
        newCumulativeMapLengths);
    numFixedLengthMapsCounter.incr();
    totalAllocatedArrayBytesCounter.incr(8 * currentActiveMap.getBackingArrayLength());
  }

  /**
   * Only a single writer can call this at one time. This method is NOT thread-safe!
   */
  @Override
  public int put(long key) {
    // first check if this key exists in previous maps
    ArrayBasedLongToInternalIntFixedLengthBiMap map;
    int bucket;
    // look through the non-live maps in order: a successful search terminates early whereas an
    // unsuccessful one has to go through all the maps
    for (int i = 0; i < readerAccessibleInfo.maps.length - 1; i++) {
      map = readerAccessibleInfo.maps[i];
      bucket = map.get(key);
      if (bucket != defaultGetReturnValue) {
        return bucket + readerAccessibleInfo.mapIndexOffsets[i];
      }
    }
    // if we didn't find it, insert it in the active map
    int numStoredKeysBefore = currentActiveMap.getNumStoredKeys();
    map = currentActiveMap;
    bucket = map.put(key) + currentActiveMapIndexOffset;
    // sometimes the key already exists so we don't increment this counter in that case
    numStoredKeysCounter.incr(currentActiveMap.getNumStoredKeys() - numStoredKeysBefore);
    // resize if needed
    if (map.isAtCapacity()) {
      addNewMap();
    }
    return bucket;
  }

  @Override
  public long getKey(int value) {
    // first locate the map: since this is very likely to be a small list, just search linearly
    int mapId = 0;
    while (value >= readerAccessibleInfo.cumulativeMapLengths[mapId]) {
      mapId++;
    }
    return readerAccessibleInfo.maps[mapId]
        .getKey(value - readerAccessibleInfo.mapIndexOffsets[mapId]);
  }

  /**
   * Resets the internal state, but note that size does NOT change, and also counters are NOT reset!
   */
  @Override
  public void clear() {
    initialize();
  }
}
