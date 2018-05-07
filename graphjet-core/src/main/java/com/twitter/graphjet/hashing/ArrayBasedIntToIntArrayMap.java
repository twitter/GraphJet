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

import com.google.common.base.Preconditions;

import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;
import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * This class provides a map from int to int[]. All the operations in this class are guaranteed to
 * be atomic, are lock-safe on modern CPUs, and are thread-safe under the single writer and multiple
 * readers model.
 *
 * Memory usage: let n be the number of nodes to be inserted, and m be the sum of the number of
 * entities in each node, the total memory consumption is O(3 * 4 * n + 4 * m) bytes.
 *
 * Performance: Each get involves one hash map lookup and one array lookup, and each put involves
 * two hash map lookups and one array lookup.
 */
public class ArrayBasedIntToIntArrayMap implements IntToIntArrayMap {
  /**
   * This class encapsulates ALL the state that will be accessed by a reader (refer to the X, Y, Z
   * comment above). The final members are used to guarantee visibility to other threads without
   * synchronization/using volatile.
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
    public final BigIntArray edges;
    // Each entry contains 2 ints for a node: position, degree
    protected final IntToIntPairHashMap nodeInfo;

    /**
     * A new instance is immediately visible to the readers due to publication safety.
     *
     * @param edges                  contains all the edges in the pool
     * @param nodeInfo               contains all the node information that is stored
     */
    public ReaderAccessibleInfo(
      BigIntArray edges,
      IntToIntPairHashMap nodeInfo) {
      this.edges = edges;
      this.nodeInfo = nodeInfo;
    }
  }

  private static final long DEFAULT_RETURN_VALUE = -1L;
  // TODO: Jerry: consolidate the edge types into one file.
  private static final int INTEGER_TOP_TWO_BYTE_SHIFT = 1 << 16;
  private static final int INTEGER_TOP_TWO_BYTE_OVERFLOW = 0x7fff;
  private static final int FAVORITE_ACTION = 1;
  private static final int RETWEET_ACTION = 2;
  private static final int REPLY_ACTION = 3;
  private static final int QUOTE_ACTION = 7;

  // This is is the only reader-accessible data
  protected ReaderAccessibleInfo readerAccessibleInfo;
  // Writes and subsequent reads across this will cross the memory barrier
  protected volatile int currentNumEdgesStored;
  private int currentPositionOffset;
  protected int currentNumNodes = 0;

  private final Counter numEdgesCounter;
  private final Counter numNodesCounter;

  /**
   * Returns a new instance that is backed by BigIntArray and IntToIntPairHashMap internally.
   *
   * @param expectedNumNodes  is the expected number of keys that will be inserted in this map.
   *                          Inserting more than these number of keys will be fine because the map
   *                          handles the resizing internally.
   * @param expectedArraySize is the expected int array size associated with each key.
   * @param statsReceiver     enables keeping stats for this class.
   */
  public ArrayBasedIntToIntArrayMap(
    int expectedNumNodes,
    int expectedArraySize,
    StatsReceiver statsReceiver
  ) {
    Preconditions.checkArgument(expectedNumNodes > 0, "Need to have at least one node!");
    StatsReceiver scopedStatsReceiver = statsReceiver.scope("ArrayBasedIntToIntArrayMap");

    this.numEdgesCounter = scopedStatsReceiver.counter("numEdges");
    this.numNodesCounter = scopedStatsReceiver.counter("numNodes");

    IntToIntPairHashMap intToIntPairHashMap =
      new IntToIntPairConcurrentHashMap(
        expectedNumNodes,
        0.5,
        (int) DEFAULT_RETURN_VALUE,
        scopedStatsReceiver
      );

    // This doesn't allocate memory for all the edges, which is done lazily
    readerAccessibleInfo = new ReaderAccessibleInfo(
      new ShardedBigIntArray(expectedNumNodes, expectedArraySize, 0, scopedStatsReceiver),
      intToIntPairHashMap
    );
    currentPositionOffset = 0;
    currentNumEdgesStored = 0;
  }

  // Read the volatile int, which forces a happens-before ordering on the read-write operations
  protected int crossMemoryBarrier() {
    return currentNumEdgesStored;
  }

  @Override
  public boolean put(int key, int[] value) {
    if (value == null || readerAccessibleInfo.nodeInfo.getBothValues(key) != DEFAULT_RETURN_VALUE) {
      return false;
    }

    int nodeDegree = value.length;
    long nodeInfo = addNewNode(key, nodeDegree);

    int nodePosition = IntToIntPairArrayIndexBasedMap.getFirstValueFromNodeInfo(nodeInfo);

    readerAccessibleInfo.edges.arrayCopy(value, 0, nodePosition, nodeDegree, true/* updateStats */);

    // This is to guarantee that if a reader sees the updated degree later, it can find the edges
    // in readerAccessibleInfo.edges
    currentNumEdgesStored += nodeDegree + 1;
    readerAccessibleInfo.nodeInfo.incrementSecondValue(key, nodeDegree);

    // This is to guarantee that readers see the updated degree immediately after it is updated
    currentNumEdgesStored -= 1;
    numEdgesCounter.incr(nodeDegree);

    return true;
  }

  protected long getNodeInfo(int key) {
    if (crossMemoryBarrier() == 0) {
      return -1L;
    }

    return readerAccessibleInfo.nodeInfo.getBothValues(key);
  }

  @Override
  public IntIterator get(int key) {
    return get(key, new IntArrayIterator(this));
  }

  @Override
  public IntIterator get(int key, ReusableNodeIntIterator regularDegreeEdgeIterator) {
    return regularDegreeEdgeIterator.resetForNode(key);
  }

  @Override
  public int getArrayLength(int key) {
    if (crossMemoryBarrier() == 0) {
      return 0;
    }

    return readerAccessibleInfo.nodeInfo.getSecondValue(key);
  }

  @Override
  /**
   *  The method does not call crossMemoryBarrier on its own to save cost, and the caller of it
   *  needs to make sure the method is either preceded or followed by another method which calls
   *  crossMemoryBarrier.
   *
   *  This method stores like, retweet, reply and quote counts into four i16s. However, since the
   *  value of this store is i32s, we need to compress two i16s into one i32. The schema is to pack
   *  Retweet and Favorite counts into the first integer, and Reply and Quote counts into the second
   *  integer. Retweet and Quote counts occupy the most significant 16 bits of the integers, and the
   *  other two counts occupy the lower 16 bits of the integers.
   */
  public boolean incrementFeatureValue(int key, byte edgeType) {
    if (edgeType == FAVORITE_ACTION || edgeType == RETWEET_ACTION || edgeType == REPLY_ACTION
      || edgeType == QUOTE_ACTION) {
      // Get the starting position of the key.
      int position = readerAccessibleInfo.nodeInfo.getFirstValue(key);
      int featurePosition = getFeaturePosition(position, edgeType);
      int incrementValue = getIncrementValue(edgeType);
      int currentEntry = readerAccessibleInfo.edges.getEntry(featurePosition);
      int currentFeatureValue = getFeatureValue(currentEntry, edgeType);

      // Prevent overflow. Skip counting when the feature value reaches overflow value.
      if (currentFeatureValue == INTEGER_TOP_TWO_BYTE_OVERFLOW) {
        return false;
      }

      readerAccessibleInfo.edges.incrementEntry(featurePosition, incrementValue);
      return true;
    }

    return false;
  }

  // This method can only be accessed through incrementFeatureValue method.
  private int getFeaturePosition(int position, byte edgeType) {
    if (edgeType == FAVORITE_ACTION || edgeType == RETWEET_ACTION) {  // FAVORITE OR RETWEET
      return position;
    } else { // REPLY OR QUOTE
      return position + 1;
    }
  }

  // This method can only be accessed through incrementFeatureValue method.
  private int getIncrementValue(byte edgeType) {
    if (edgeType == FAVORITE_ACTION || edgeType == REPLY_ACTION) { // FAVORITE OR REPLY
      return 1;
    } else { // RETWEET OR QUOTE
      return INTEGER_TOP_TWO_BYTE_SHIFT;
    }
  }

  // This method can only be accessed through incrementFeatureValue method.
  private int getFeatureValue(int currentEntry, byte edgeType) {
    if (edgeType == FAVORITE_ACTION || edgeType == REPLY_ACTION) { // FAVORITE OR REPLY
      return currentEntry & 0xffff;
    } else { // RETWEET OR QUOTE
      return (currentEntry >> 16) & 0xffff;
    }
  }

  /**
   * Get a specified edge for the node: note that it is the caller's responsibility to check that
   * the edge number is within the degree bounds.
   *
   * @param position     is the position of the node whose edges are being requested
   * @param edgeNumber   is the required edge number
   * @return the requested edge
   */
  protected int getNumberedEdge(int position, int edgeNumber) {
    return readerAccessibleInfo.edges.getEntry(position + edgeNumber);
  }

  private long addNodeInfo(int node) {
    long nodeInfo = ((long) currentPositionOffset) << 32; // degree is 0 to start
    readerAccessibleInfo.nodeInfo.put(node, currentPositionOffset, 0);
    return nodeInfo;
  }

  private long addNewNode(int node, int length) {
    long nodeInfo = addNodeInfo(node);
    currentPositionOffset += length;
    currentNumNodes++;
    numNodesCounter.incr();
    return nodeInfo;
  }
}
