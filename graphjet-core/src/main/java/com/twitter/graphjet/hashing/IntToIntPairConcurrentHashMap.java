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
 * This class provides a double-hashing implementation of a type-specific map from an int key to a
 * pair of int values. All the operations in this class are guaranteed to be atomic, are lock-free,
 * and are thread-safe under the single writer and multiple readers model. Note that by default
 * there are no memory barriers being established here so readers may not see any writer updates at
 * all unless the client establishers some memory barriers. We leave that behavior up to clients for
 * finer-grained control over frequency of crossing barriers.
 * TODO for future is to expose read/publish memory barrier functions.
 *
 * Memory usage: the memory used by this implementation is 12*n/load_factor bytes, where n is the
 * maximum number of keys that will be inserted.
 *
 * Performance: assuming a load_factor of no more than .75, each get/set operation should require an
 * average of two hashes and two array lookups, which are both extremely fast. For the interested
 * reader, there is a great analysis of double-hashing in TAOCP Vol 3 pg 529, by Knuth, and in my
 * testing, performance here is almost exactly as expected [1], but if there is a deviation, it can
 * hopefully be fixed by having a better hash function. Leaving that as a TODO for future
 * generations...
 * [1] Expected # probes for unsuccessful search = 1 / (1 - lf) = 4, Expected # probes for
 * successful search = -1/lf * ln(1 - lf) = 1.8
 */
public class IntToIntPairConcurrentHashMap implements IntToIntPairHashMap {
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
    public final BigIntArray array; // backing array that stores the map
    public final int maxNumKeysToStore;
    public final int bitMask;

    /**
     * A new instance is immediately visible to the readers due to publication safety.
     * @param array              is the backing array that stores keys and values
     * @param bitMask            is the bit mask used for hashing into the array (works only when
     *                           array length is a power of 2)
     * @param maxNumKeysToStore  is the maximum number of keys to store in the array
     */
    public ReaderAccessibleInfo(BigIntArray array, int bitMask, int maxNumKeysToStore) {
      this.array = array;
      this.bitMask = bitMask;
      this.maxNumKeysToStore = maxNumKeysToStore;
    }
  }

  // This is is the only reader-accessible data
  protected ReaderAccessibleInfo readerAccessibleInfo;

  private static final int NUM_INTS_PER_KEY = 3; // 1 key and 2 values

  private final int defaultReturnValue;
  private final double loadFactor;
  private final Counter numStoredKeysCounter;
  private final StatsReceiver scopedStatsReceiver;

  private int numKeySlotsAllocated;

  private int size = 0;

  /**
   * Returns a new instance that allocates a large array internally. Note that the used memory
   * here will be 12*pow(2, ceil(lg(expectedNumNodes / loadFactor))) bytes. In case expectedNumNodes
   * is flexible, clients can tune expectedNumNodes/loadFactor so that it is a power of 2.
   *
   * @param expectedNumNodes          is the expected number of keys that can be inserted in this
   *                                  map. Inserting more than these number of keys will grow the
   *                                  memory.
   * @param loadFactor                is the load factor to set: always stay below 0.75, and if you
   *                                  are very performance sensitive, go to 0.5.
   * @param defaultReturnValue        is what a get returns to indicate empty.
   */
  public IntToIntPairConcurrentHashMap(
      int expectedNumNodes,
      double loadFactor,
      int defaultReturnValue,
      StatsReceiver statsReceiver) {
    this.scopedStatsReceiver = statsReceiver.scope(this.getClass().getName());
    this.loadFactor = loadFactor;
    int arraySize = (int) (expectedNumNodes / loadFactor);
    // Keep the number of buckets to be power of 2 so that we can use bit-masks instead of mod
    // The minimum values here address some pathological cases
    this.numKeySlotsAllocated = Math.max(Integer.highestOneBit(arraySize - 1) << 1, 8);
    // Note that 'arrayLength' is always a power of 2.
    BigIntArray array = new ShardedBigIntArray(
        numKeySlotsAllocated * NUM_INTS_PER_KEY,
        ShardedBigIntArray.PREFERRED_EDGES_PER_SHARD,
        defaultReturnValue,
        scopedStatsReceiver);
    int bitMask = numKeySlotsAllocated - 1;
    int maxNumKeysToStore = (int) (loadFactor * numKeySlotsAllocated);
    this.readerAccessibleInfo = new ReaderAccessibleInfo(array, bitMask, maxNumKeysToStore);
    this.defaultReturnValue = defaultReturnValue;
    // initialize counters
    numStoredKeysCounter = scopedStatsReceiver.counter("numStoredKeys");
  }

  // Thomas Wang's long hash function: https://gist.github.com/badboy/6267743
  // TODO: compare the performance of this bit-hack hash function with the fastutil implementation:
  // http://fastutil.di.unimi.it/docs/it/unimi/dsi/fastutil/HashCommon.html
  // Need to bump source to latest version for that too: https://github.com/vigna/fastutil
  private static int primaryHashFunction(int key) {
    int key2 = ~key + (key << 15);
    key2 = key2 ^ (key2 >>> 12);
    key2 = key2 + (key2 << 2);
    key2 = key2 ^ (key2 >>> 4);
    key2 = key2 * 2057;
    key2 = key2 ^ (key2 >>> 16);
    return key2;
  }

  // Thomas Wang's integer hash function: https://gist.github.com/badboy/6267743
  // Needs to be co-prime to primary hash value: the way we achieve this is to assume that table
  // size is a power of 2, say 2^m. Then we can return an odd number between 0 -> 2^(m-1), which is
  // always co-prime to the table size.
  private static int secondaryHashFunction(int key, int bitMask) {
    int c2 = 0x27d4eb2d; // a prime or an odd constant
    int key2 = (key ^ 61) ^ (key >>> 16);
    key2 = key2 + (key2 << 3);
    key2 = key2 ^ (key2 >>> 4);
    key2 = key2 * c2;
    key2 = key2 ^ (key2 >>> 15);
    return ((key2 & bitMask) >> 1) | 1;
  }

  private boolean setEntries(
      int position,
      int key,
      int firstValue,
      int secondValue,
      BigIntArray array,
      boolean updateCounters) {
    // If the bucket is empty, set the value
    array.addEntry(key, position);
    array.addEntry(firstValue, position + 1);
    array.addEntry(secondValue, position + 2);
    if (updateCounters) {
      size++;
      numStoredKeysCounter.incr();
    }
    return false;
  }

  private boolean put(
      int key,
      int firstValue,
      int secondValue,
      ReaderAccessibleInfo currentReaderAccessibleInfo,
      boolean updateCounters) {
    int primaryHashValue = primaryHashFunction(key);
    int hashedValue = primaryHashValue & currentReaderAccessibleInfo.bitMask;
    int position = hashedValue * NUM_INTS_PER_KEY;
    long currentKey = currentReaderAccessibleInfo.array.getEntry(position);
    if (currentKey == defaultReturnValue) {
      return setEntries(
          position,
          key,
          firstValue,
          secondValue,
          currentReaderAccessibleInfo.array,
          updateCounters);

    } else if (currentKey == key) {
      // otherwise if the bucket already has the key, return
      return true;
    }
    // If the primary location has some other element, then open address using double hashing
    // Double hashed probing is given by: h1(key) + i * h2(key)
    int secondaryHashKey = secondaryHashFunction(key, currentReaderAccessibleInfo.bitMask);
    do {
      hashedValue =
          (int) (((long) hashedValue + secondaryHashKey) & currentReaderAccessibleInfo.bitMask);
      position = hashedValue * NUM_INTS_PER_KEY;
      currentKey = currentReaderAccessibleInfo.array.getEntry(position);
    } while ((currentKey != defaultReturnValue) && (currentKey != key));
    // At this point we either find an empty bucket or the bucket that currently contains the key
    return setEntries(
        position, key, firstValue, secondValue, currentReaderAccessibleInfo.array, updateCounters);
  }

  /**
   * Only a single writer can call this at one time. This method is NOT thread-safe!
   */
  @Override
  public boolean put(int key, int firstValue, int secondValue) {
    if (size == readerAccessibleInfo.maxNumKeysToStore) {
      resize();
    }
    return put(key, firstValue, secondValue, readerAccessibleInfo, true);
  }

  private int getKeyPosition(int key, ReaderAccessibleInfo currentReaderAccessibleInfo) {
    long primaryHash = primaryHashFunction(key);
    int hashedValue = (int) (primaryHash & currentReaderAccessibleInfo.bitMask);
    int position = hashedValue * NUM_INTS_PER_KEY;
    long currentKey = currentReaderAccessibleInfo.array.getEntry(position);
    if ((currentKey == defaultReturnValue) || (currentKey == key)) {
      return position;
    }
    int secondaryHash = secondaryHashFunction(key, currentReaderAccessibleInfo.bitMask);
    do {
      hashedValue =
          (int) (((long) hashedValue + secondaryHash) & currentReaderAccessibleInfo.bitMask);
      position = hashedValue * NUM_INTS_PER_KEY;
      currentKey = currentReaderAccessibleInfo.array.getEntry(position);
    } while ((currentKey != defaultReturnValue) && (currentKey != key));
    return position;
  }

  @Override
  public int getFirstValue(int key) {
    ReaderAccessibleInfo currentReaderAccessibleInfo = readerAccessibleInfo;
    return currentReaderAccessibleInfo.array.getEntry(
        getKeyPosition(key, currentReaderAccessibleInfo) + 1);
  }

  @Override
  public int getSecondValue(int key) {
    ReaderAccessibleInfo currentReaderAccessibleInfo = readerAccessibleInfo;
    return currentReaderAccessibleInfo.array.getEntry(
        getKeyPosition(key, currentReaderAccessibleInfo) + 2);
  }

  @Override
  public long getBothValues(int key) {
    ReaderAccessibleInfo currentReaderAccessibleInfo = readerAccessibleInfo;
    int position = getKeyPosition(key, currentReaderAccessibleInfo);
    long firstValue = (long) currentReaderAccessibleInfo.array.getEntry(position + 1);
    if (firstValue == defaultReturnValue) {
      return defaultReturnValue;
    }
    return (firstValue << 32) + currentReaderAccessibleInfo.array.getEntry(position + 2);
  }

  /**
   * Not thread-safe for multiple writers!
   *
   * @param key    is the key whose values are being updated
   * @return the new value if they key exists and defaultReturnValue otherwise
   */
  @Override
  public int incrementFirstValue(int key) {
    return readerAccessibleInfo.array.incrementEntry(
      getKeyPosition(key, readerAccessibleInfo) + 1,
      1
    );
  }

  /**
   * Not thread-safe for multiple writers!
   *
   * @param key    is the key whose values are being updated
   * @param delta  is the delta to be incremented to the value
   * @return the new value if they key exists and defaultReturnValue otherwise
   */
  @Override
  public int incrementSecondValue(int key, int delta) {
    return readerAccessibleInfo.array.incrementEntry(
      getKeyPosition(key, readerAccessibleInfo) + 2,
      delta
    );
  }

  // we are at max load factor, so time to resize
  private void resize() {
    // Keep the number of buckets to be power of 2 so that we can use bit-masks instead of mod
    // The downside of that decision is that this can only grow by doubling
    int newNumKeySlotsAllocated = numKeySlotsAllocated << 1;
    BigIntArray newArray = new ShardedBigIntArray(
        newNumKeySlotsAllocated * NUM_INTS_PER_KEY,
        ShardedBigIntArray.PREFERRED_EDGES_PER_SHARD,
        defaultReturnValue,
        scopedStatsReceiver);
    int bitMask = newNumKeySlotsAllocated - 1;
    int maxNumKeysToStore = (int) (loadFactor * newNumKeySlotsAllocated);
    ReaderAccessibleInfo newReaderAccessibleInfo =
        new ReaderAccessibleInfo(newArray, bitMask, maxNumKeysToStore);

    // now re-hash all the old entries in the new array
    for (int i = 0; i < numKeySlotsAllocated * NUM_INTS_PER_KEY; i += NUM_INTS_PER_KEY) {
      int key = readerAccessibleInfo.array.getEntry(i);
      if (key != defaultReturnValue) {
        put(key,
            readerAccessibleInfo.array.getEntry(i + 1),
            readerAccessibleInfo.array.getEntry(i + 2),
            newReaderAccessibleInfo,
            false);
      }
    }

    numKeySlotsAllocated = newNumKeySlotsAllocated;
    // creating a new object publishes it
    readerAccessibleInfo = new ReaderAccessibleInfo(newArray, bitMask, maxNumKeysToStore);
  }

  /**
   * Resets the internal state, but note that size does NOT change, and also counters are NOT reset!
   */
  @Override
  public void clear() {
    readerAccessibleInfo.array.reset();
    size = 0;
  }
}
