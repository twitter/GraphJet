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

import com.google.common.base.Preconditions;

import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * <p>This class implements a large array as an array of arrays that are sharded. There are two
 * benefits advantage of this implementation over a large one-dimensional array:</p>
 * 1) Since the array is allocated shard-by-shard, we expect better GC performance for large arrays
 * as each shard can be allocated/collected separately
 * 2) Accessing entries requires loading much less data in L1 cache since only the relevant "shards"
 * are accessed.
 * <p>There is a little bit of extra computational cost associated with sharding, namely that we need
 * to convert positions to shards and offsets. To make this cost negligible, we enforce that shard
 * lengths are powers of 2, which implies that conversions are bit operations. This does make the
 * array size a bit larger than desired, but the memory overhead is generally small.</p>
 *
 * <p>This class is thread-safe even though it does not do any locking: it achieves this by leveraging
 * the assumptions stated below and using a "memory barrier" between writes and reads to sync
 * updates.</p>
 *
 * <p>Here are the client assumptions needed to enable lock-free read/writes:</p>
 * 1. There is a SINGLE writer thread -- this is extremely important as we don't lock during writes.
 * 2. Readers are OK reading stale data, i.e. if even if a reader thread arrives after the writer
 * thread started doing a write, the update is NOT guaranteed to be available to it unless the
 * update has finished.
 *
 * <p>This class enables lock-free read/writes by guaranteeing the following:</p>
 * 1. The writes that are done are always "safe", i.e. in no time during the writing do they leave
 * things in a state such that a reader would either encounter an exception or do wrong
 * computation.
 * 2. After a write is done, it is explicitly "published" such that a reader that arrives after
 * the published write it would see updated data.
 *
 * <p>The way this works is as follows: suppose we have some linked objects X, Y and Z that need to be
 * maintained in a consistent state. First, our setup ensures that the reader is _only_ allowed to
 * access these in a linear manner as follows: read X -> read Y -> read Z. Then, we ensure that the
 * writer behavior is to write (safe, atomic) updates to each of these in the exact opposite order:
 * write Z --flush--&gt; write Y --flush--&gt; write X.</p>
 *
 * <p>Note that the flushing ensures that if a reader sees Y then it _must_ also see the updated Z,
 * and if sees X then it _must_ also see the updated Y and Z. Further, each update itself is safe.
 * For instance, a reader can safely access an updated Z even if X and Y are not updated since the
 * updated information will only be accessible through the updated X and Y (the converse though is
 * NOT true). Together, this ensures that the reader accesses to the objects are always consistent
 * with each other and safe to access by the reader.</p>
 */
public class ShardedBigIntArray implements BigIntArray {
  /**
   * <p>This class encapsulates ALL the state that will be accessed by a reader (refer to the X, Y, Z
   * comment above). The final members are used to guarantee visibility to other threads without
   * synchronization/using volatile.</p>
   *
   * <p>From 'Java Concurrency in practice' by Brian Goetz, p. 349:</p>
   *
   * <blockquote>"Initialization safety guarantees that for properly
   * constructed objects, all threads will see the correct values of
   * final fields that were set by the constructor, regardless of
   * how the object is published. Further, any variables that can be
   * reached through a final field of a properly constructed object
   * (such as the elements of a final array or the contents of a
   * HashMap referenced by a final field) are also guaranteed to be
   * visible to other threads."</blockquote>
   */
  public static final class ReaderAccessibleInfo {
    public final int[][] array;

    /**
     * A new instance is immediately visible to the readers due to publication safety.
     *
     * @param array contains all the array in the pool
     */
    public ReaderAccessibleInfo(int[][] array) {
      this.array = array;
    }
  }

  // Making the int array preferred size be 256KB ~ size of L2 cache
  public static final int PREFERRED_EDGES_PER_SHARD = 1 << 16;
  private static final double SHARD_GROWTH_FACTOR = 1.1;

  // This is is the only reader-accessible data
  protected ReaderAccessibleInfo readerAccessibleInfo;

  private final int nullEntry;
  private final int shardLength;
  private final int shardLengthNumBits;
  private final int offsetMask;
  private final Counter numArrayEntries;

  private int numShards;
  private int numAllocatedSlotsForEntries;
  private int numStoredEntries;

  /**
   * Reserves the needed memory for a {@link ShardedBigIntArray}, and initializes most of the
   * objects that would be needed for this graph. Note that actual memory would be allocated as
   * needed, and the amount of memory needed will increase if more nodes arrive than expected.
   *
   * @param numExpectedEntries  is the expected number of nodes that will be added into this pool.
   *                            The actual number of nodes can be larger and the pool will expand
   *                            itself to fit them till we hit the limit of max array size in Java.
   * @param minShardSize        is the minimum size of each shard. Internally, we round up to a
   *                            power of 2
   * @param nullEntry           is the default return value when a position has not been filled is
   *                            asked for
   * @param statsReceiver       is used to update storage stats
   */
  public ShardedBigIntArray(
      int numExpectedEntries,
      int minShardSize,
      int nullEntry,
      StatsReceiver statsReceiver) {
    int shardSize = Math.max(PREFERRED_EDGES_PER_SHARD, minShardSize);
    // We round shards up to be a power of two
    this.shardLengthNumBits =
        Math.max(Integer.numberOfTrailingZeros(Integer.highestOneBit(shardSize - 1) << 1), 4);
    this.shardLength = 1 << shardLengthNumBits;
    this.offsetMask = shardLength - 1;
    this.nullEntry = nullEntry;
    StatsReceiver scopedStatsReceiver = statsReceiver.scope("ShardedBigIntArray");
    this.numArrayEntries = scopedStatsReceiver.counter("numArrayEntries");
    numShards = Math.max(numExpectedEntries >> shardLengthNumBits, 1);
    Preconditions.checkArgument(numShards * shardLength < Integer.MAX_VALUE,
        "Exceeded the max storage capacity for ShardedBigIntArray");
    readerAccessibleInfo = new ReaderAccessibleInfo(new int[numShards][]);
    this.numAllocatedSlotsForEntries = shardLength;
  }

  /**
   * Synchronization comment: this method works fine without needing synchronization between the
   * writer and the readers due to the wrapping of the arrays in ReaderAccessibleInfo. See
   * the publication safety comment in those objects.
   */
  private void expandArray(int shardId) {
    int newNumShards = Math.max((int) Math.ceil(numShards * SHARD_GROWTH_FACTOR), shardId + 1);
    Preconditions.checkArgument(newNumShards * shardLength < Integer.MAX_VALUE,
        "Exceeded the max storage capacity for ShardedBigIntArray");
    int[][] newArray = new int[newNumShards][];
    // Important: the arraycopy assumes that the size of each shard remains same
    System.arraycopy(readerAccessibleInfo.array, 0, newArray, 0, readerAccessibleInfo.array.length);
    // This flushes all the reader-accessible data *together* to all threads: the readers are safe
    // as long as they reference the wrapper object since the data stays the same
    readerAccessibleInfo = new ReaderAccessibleInfo(newArray);
    numShards = newNumShards;
  }

  private void allocateMemoryForShard(int shardId) {
    int[] newShard = new int[shardLength];
    if (nullEntry != 0) {
      Arrays.fill(newShard, nullEntry);
    }
    readerAccessibleInfo.array[shardId] = newShard;
    numAllocatedSlotsForEntries += shardLength;
  }

  @Override
  public void addEntry(int entry, int position) {
    int shard = position >> shardLengthNumBits;
    int offset = position & offsetMask;
    // we may need more shards
    if (shard >= numShards) {
      expandArray(shard);
    }
    // the shard's memory may not have been allocated yet
    if (readerAccessibleInfo.array[shard] == null) {
      allocateMemoryForShard(shard);
    }
    readerAccessibleInfo.array[shard][offset] = entry; // int writes are atomic
    numStoredEntries++;
    numArrayEntries.incr();
  }

  @Override
  public void arrayCopy(int[] src, int srcPos, int desPos, int length, boolean updateStats) {
    int shard = desPos >> shardLengthNumBits;
    int offset = desPos & offsetMask;
    // we may need more shards
    if (shard >= numShards) {
      expandArray(shard);
    }

    // the shard's memory may not have been allocated yet
    if (readerAccessibleInfo.array[shard] == null) {
      allocateMemoryForShard(shard);
    }

    if (offset + length <= shardLength) {
      System.arraycopy(
        src,
        srcPos,
        readerAccessibleInfo.array[shard],
        offset,
        length
      );
    } else {
      System.arraycopy(
        src,
        srcPos,
        readerAccessibleInfo.array[shard],
        offset,
        shardLength - offset
      );

      int deltaLength = shardLength - offset;

      // if current shard does not have enough space to hold all elements in this batch, add them to
      // the next shard(s) recursively
      arrayCopy(
        src,
        srcPos + deltaLength,
        desPos + deltaLength,
        length - deltaLength,
        false /*updateStats*/
      );
    }

    if (updateStats) {
      numStoredEntries += length;
      numArrayEntries.incr(length);
    }
  }

  @Override
  public int getEntry(int position) {
    int shard = position >> shardLengthNumBits;
    int offset = position & offsetMask;
    // we may need more shards
    if ((shard >= numShards) || (readerAccessibleInfo.array[shard] == null)) {
      return nullEntry;
    }
    return readerAccessibleInfo.array[shard][offset];
  }

  @Override
  public int incrementEntry(int position, int delta) {
    int shard = position >> shardLengthNumBits;
    int offset = position & offsetMask;
    // we may need more shards
    if ((shard >= numShards) || (readerAccessibleInfo.array[shard] == null)) {
      return nullEntry;
    }

    readerAccessibleInfo.array[shard][offset] += delta;
    return readerAccessibleInfo.array[shard][offset];
  }

  public int[] getShard(int position) {
    int shard = position >> shardLengthNumBits;

    return readerAccessibleInfo.array[shard];
  }

  public int getShardOffset(int position) {
    return position & offsetMask;
  }

  @Override
  public double getFillPercentage() {
    return 100.0 * numStoredEntries / (double) numAllocatedSlotsForEntries;
  }

  @Override
  public void reset() {
    numStoredEntries = 0;
    for (int i = 0; i < numShards; i++) {
      if (readerAccessibleInfo.array[i] != null) {
        Arrays.fill(readerAccessibleInfo.array[i], nullEntry);
      }
    }
  }
}
