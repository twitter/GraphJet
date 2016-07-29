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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

import com.google.common.base.Preconditions;

import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * This class provides a fixed-length long array in which long keys are double-hashed into an array,
 * with the array indices serving as the internal int mapping. All the operations in this class are
 * guaranteed to be atomic, are lock-safe on modern CPUs, and are thread-safe under the single
 * writer and multiple readers model.
 *
 * Memory usage: the memory used by this implementation is 8*n/load_factor bytes, where n is the
 * maximum number of keys that will be inserted.
 *
 * Performance: assuming a load_factor of no more than .75, each get/set operation should require an
 * average of two hashes and two array lookups, which are both extremely fast. Further, getKey is
 * one array lookup (no hashes). For the interested reader, there is a great analysis of
 * double-hashing in TAOCP Vol 3 pg 529, by Knuth, and in my testing performance here is almost
 * exactly as expected [1], but if there is a deviation, it can hopefully be fixed by having a
 * better hash function. Leaving that as a TODO for future generations...
 * [1] Expected # probes for unsuccessful search = 1 / (1 - lf) = 4, Expected # probes for
 * successful search = -1/lf * ln(1 - lf) = 1.8
 *
 * Notes on thread-safety: writing/reading longs is not guaranteed to be atomic under the JVM. The
 * primary (only?) way to read/write longs atomically is using the {@link sun.misc.Unsafe} package:
 * http://www.docjar.com/docs/api/gnu/classpath/Unsafe.html In fact, the AtomicLongArray (and other
 * classes in the java atomic package) use that internally as can be seen from the code:
 * http://fuseyism.com/classpath/doc/java/util/concurrent/atomic/AtomicLongArray-source.html
 * It is interesting to look at the implementations of the Unsafe package and in particular the
 * Unsafe_SetOrderedLong instructions therein:
 * http://hg.openjdk.java.net/jdk7/jdk7/hotspot/file/9b0ca45cd756/src/share/vm/prims/unsafe.cpp
 * Note that if the CPU supports the cx8 instruction set then there is no locking in writing longs.
 * A quick (unscientific) survey on our mesos cluster notes that
 * "cat /proc/cpuinfo | grep cx8 | wc -l" == "cat /proc/cpuinfo | grep cores | wc -l", which means
 * that the cx8 instruction set is supported on our hardware AFAICT.
 * Final thing to note is that AtomicLongArray also is conservative in terms of establishing memory
 * barriers: as a reader, you only cross the memory barrier if the location you are reading had
 * a volatile write. This is theoretically much better than cross the barrier on every write (no
 * matter what location), but no benchmark was run to determine the difference.
 */
public class ArrayBasedLongToInternalIntFixedLengthBiMap implements LongToInternalIntBiMap {
  private final AtomicLongArray array; // backing array that stores the map
  private final long defaultGetKeyReturnValue;
  private final int defaultGetReturnValue;
  private final int bitMask;
  private final int capacity;
  private final AtomicInteger size = new AtomicInteger(0);
  private final Counter numStoredKeysCounter;

  /**
   * Returns a new instance that allocates a large array internally. Note that the used memory
   * here will be 8*pow(2, ceil(lg(capacity / loadFactor))) bytes. In case capacity is flexible,
   * clients can tune capacity/loadFactor so that it is a power of 2.
   *
   * @param capacity                  is the maximum number of keys that can be inserted in this
   *                                  map. Inserting more than these number of keys will throw
   *                                  a RuntimeException.
   * @param loadFactor                is the load factor to set: always stay below 0.75, and if you
   *                                  are very performance sensitive, go to 0.5.
   * @param defaultGetReturnValue     is what a get returns to indicate empty.
   * @param defaultGetKeyReturnValue  is what a getKey returns to indicate empty. Please note that
   *                                  it is the client's responsibility to ensure that this value is
   *                                  disjoint from the keys in the map.
   */
  public ArrayBasedLongToInternalIntFixedLengthBiMap(
      int capacity,
      double loadFactor,
      int defaultGetReturnValue,
      long defaultGetKeyReturnValue,
      StatsReceiver statsReceiver) {
    int arraySize = (int) (capacity / loadFactor);
    // Keep the number of buckets to be power of 2 so that we can use bit-masks instead of mod
    // the min values here address some pathological cases
    int arrayLength = Math.max(Integer.highestOneBit(arraySize - 1) << 1, 16);
    this.capacity = (int) (arrayLength * loadFactor);
    // Note that 'arrayLength' is always a power of 2.
    long[] dataArray = new long[arrayLength];
    if (defaultGetKeyReturnValue != 0L) {
      Arrays.fill(dataArray, defaultGetKeyReturnValue);
    }
    this.array = new AtomicLongArray(dataArray);
    bitMask = dataArray.length - 1;
    Preconditions.checkArgument(defaultGetReturnValue < 0 || defaultGetReturnValue >= capacity,
        "defaultGetReturnValue must NOT be one of the indices that can be returned otherwise.");
    this.defaultGetReturnValue = defaultGetReturnValue;
    this.defaultGetKeyReturnValue = defaultGetKeyReturnValue;
    // initialize counters
    StatsReceiver scopedStatsReceiver = statsReceiver.scope(this.getClass().getSimpleName());
    numStoredKeysCounter = scopedStatsReceiver.counter("numStoredKeys");
  }

  // Thomas Wang's long hash function: https://gist.github.com/badboy/6267743
  // TODO: compare the performance of this bit-hack hash function with the fastutil implementation:
  // http://fastutil.di.unimi.it/docs/it/unimi/dsi/fastutil/HashCommon.html
  // Need to bump source to latest version for that too: https://github.com/vigna/fastutil
  private static long primaryHashFunction(long key) {
    long key2 = (~key) + (key << 21); // key = (key << 21) - key - 1;
    key2 = key2 ^ (key2 >>> 24);
    key2 = (key2 + (key2 << 3)) + (key2 << 8); // key * 265
    key2 = key2 ^ (key2 >>> 14);
    key2 = (key2 + (key2 << 2)) + (key2 << 4); // key * 21
    key2 = key2 ^ (key2 >>> 28);
    key2 = key2 + (key2 << 31);
    return key2;
  }

  // Thomas Wang's integer hash function: https://gist.github.com/badboy/6267743
  // Needs to be co-prime to primary hash value: the way we achieve this is to assume that table
  // size is a power of 2, say 2^m. Then we can return an odd number between 0 -> 2^(m-1), which is
  // always co-prime to the table size.
  private static int secondaryHashFunction(long key, int bitMask) {
    long key2 = (~key) + (key << 18);
    key2 = key2 ^ (key2 >>> 31);
    key2 = (key2 + (key2 << 2)) + (key2 << 4);
    key2 = key2 ^ (key2 >>> 11);
    key2 = key2 + (key2 << 6);
    key2 = key2 ^ (key2 >>> 22);
    return (((int) key2 & bitMask) >> 1) | 1;
  }

  // Returns defaultGetReturnValue if the key is not found, and bucket otherwise
  @Override
  public int get(long key) {
    long primaryHash = primaryHashFunction(key);
    int hashedValue = (int) (primaryHash & bitMask);
    long currentKey = array.get(hashedValue);
    if (currentKey == defaultGetReturnValue) {
      return defaultGetReturnValue;
    } else if (currentKey == key) {
      return hashedValue;
    }
    int secondaryHash = secondaryHashFunction(key, bitMask);
    do {
      hashedValue = (int) (((long) hashedValue + secondaryHash) & bitMask);
      currentKey = array.get(hashedValue);
    } while ((currentKey != defaultGetReturnValue) && (currentKey != key));
    if (currentKey == defaultGetReturnValue) {
      return defaultGetReturnValue;
    } else {
      return hashedValue;
    }
  }

  /**
   * Only a single writer can call this at one time. This method is NOT thread-safe!
   */
  @Override
  public int put(long key) {
    long primaryHashValue = primaryHashFunction(key);
    int bucket = (int) (primaryHashValue & bitMask);
    long currentKey = array.get(bucket);
    if (currentKey == defaultGetReturnValue) {
      if (isAtCapacity()) {
        throw new RuntimeException("Exceeded the maximum number of insertions");
      }
      // If the bucket is empty, set the value
      array.set(bucket, key);
      size.getAndIncrement();
      numStoredKeysCounter.incr();
      return bucket;
    } else if (currentKey == key) {
      // otherwise if the bucket already has the key, return
      return bucket;
    }
    // If the primary location has some other element, then open address using double hashing
    // Double hashed probing is given by: h1(key) + i * h2(key)
    int secondaryHashKey = secondaryHashFunction(key, bitMask);
    do {
      bucket = (int) (((long) bucket + secondaryHashKey) & bitMask);
      currentKey = array.get(bucket);
    } while ((currentKey != defaultGetReturnValue) && (currentKey != key));
    // At this point we either find an empty bucket or the bucket that currently contains the key
    if (currentKey == defaultGetReturnValue) {
      if (isAtCapacity()) {
        throw new RuntimeException("Exceeded the maximum number of insertions");
      }
      // If the bucket is empty, set the value
      array.set(bucket, key);
      size.getAndIncrement();
      numStoredKeysCounter.incr();
      return bucket;
    } else {
      // otherwise if the bucket already has the key, return
      return bucket;
    }
  }

  @Override
  public long getKey(int value) {
    if (value < 0 || value >= array.length()) {
      throw new IndexOutOfBoundsException("index " + value);
    }
    return array.get(value);
  }

  /**
   * Resets the internal state, but note that size does NOT change, and also counters are NOT reset!
   */
  @Override
  public void clear() {
    for (int i = 0; i < array.length(); i++) {
      array.set(i, defaultGetKeyReturnValue);
    }
    size.set(0);
  }

  protected boolean isAtCapacity() {
    return size.get() == capacity;
  }

  protected int getBackingArrayLength() {
    return array.length();
  }

  protected int getNumStoredKeys() {
    return size.get();
  }
}
