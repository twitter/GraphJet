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
 * This class provides a straight-up array index based implementation of a type-specific map from an
 * int key (that'll just index into the array) to a pair of int values. All the operations in this
 * class are guaranteed to be atomic, are lock-free, and are thread-safe under the single writer and
 * multiple readers model. Note that by default there are no memory barriers being established here
 * so readers may not see any writer updates at all unless the client establishes some memory
 * barriers. We leave that behavior up to clients for finer-grained control over frequency of
 * crossing barriers.
 * TODO for future is to expose read/publish memory barrier functions.
 *
 * Memory usage: the memory used by this implementation is ~8*max_key bytes, where max_key is the
 * maximum value of the keys that will be inserted.
 *
 * Performance: every operation here is an array lookup so this should be pretty damn fast.
 */
public class IntToIntPairArrayIndexBasedMap implements IntToIntPairHashMap {
  public final BigIntArray array; // backing array that stores the map
  private final int defaultReturnValue;
  private final Counter numStoredKeysCounter;

  /**
   * Returns a new instance that allocates a large array internally.
   *
   * @param expectedNumNodes          is the expected number of keys that can be inserted in this
   *                                  map. Inserting more than these number of keys will grow the
   *                                  memory.
   * @param defaultReturnValue        is what a get returns to indicate empty.
   */
  public IntToIntPairArrayIndexBasedMap(
      int expectedNumNodes,
      int defaultReturnValue,
      StatsReceiver statsReceiver) {
    StatsReceiver scopedStatsReceiver = statsReceiver.scope(this.getClass().getSimpleName());
    int arraySize = Math.max(expectedNumNodes << 1, 8);
    this.array = new ShardedBigIntArray(
        arraySize,
        ShardedBigIntArray.PREFERRED_EDGES_PER_SHARD,
        defaultReturnValue,
        scopedStatsReceiver);
    this.defaultReturnValue = defaultReturnValue;
    numStoredKeysCounter = scopedStatsReceiver.counter("numStoredKeys");
  }

  public static int getFirstValueFromNodeInfo(long nodeInfo) {
    return (int) (nodeInfo >> 32);
  }

  public static int getSecondValueFromNodeInfo(long nodeInfo) {
    return (int) (nodeInfo & Integer.MAX_VALUE);
  }

  /**
   * Only a single writer can call this at one time. This method is NOT thread-safe!
   */
  @Override
  public boolean put(int key, int firstValue, int secondValue) {
    int position = key << 1;
    array.addEntry(firstValue, position);
    array.addEntry(secondValue, position + 1);
    numStoredKeysCounter.incr();
    return false;
  }

  @Override
  public int getFirstValue(int key) {
    return array.getEntry(key << 1);
  }

  @Override
  public int getSecondValue(int key) {
    return array.getEntry((key << 1) + 1);
  }

  @Override
  public long getBothValues(int key) {
    int position = key << 1;
    long firstValue = (long) array.getEntry(position);
    if (firstValue == defaultReturnValue) {
      return defaultReturnValue;
    }
    return (firstValue << 32) + array.getEntry(position + 1);
  }

  /**
   * Not thread-safe for multiple writers!
   *
   * @param key    is the key whose values are being updated
   * @return the new value if they key exists and defaultReturnValue otherwise
   */
  @Override
  public int incrementFirstValue(int key) {
    return array.incrementEntry(key << 1, 1);
  }

  /**
   * Not thread-safe for multiple writers!
   *
   * @param key    is the key whose values are being updated
   * @param delta  is the change in the values associated with the key
   * @return the new value if they key exists and defaultReturnValue otherwise
   */
  @Override
  public int incrementSecondValue(int key, int delta) {
    return array.incrementEntry((key << 1) + 1, delta);
  }

  /**
   * Resets the internal state, but note that size does NOT change, and also counters are NOT reset!
   */
  @Override
  public void clear() {
    array.reset();
  }
}
