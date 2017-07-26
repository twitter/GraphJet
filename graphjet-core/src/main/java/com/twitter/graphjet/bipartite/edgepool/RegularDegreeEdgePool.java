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

import com.google.common.base.Preconditions;

import com.twitter.graphjet.hashing.BigIntArray;
import com.twitter.graphjet.hashing.BigLongArray;
import com.twitter.graphjet.hashing.IntToIntPairArrayIndexBasedMap;
import com.twitter.graphjet.hashing.IntToIntPairConcurrentHashMap;
import com.twitter.graphjet.hashing.IntToIntPairHashMap;
import com.twitter.graphjet.hashing.ShardedBigIntArray;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * This edge pool is for the case where all the nodes have a bounded maximum degree, and most nodes
 * are expected to be near or at the maximum degree. For simplicity, we assume that the nodes have
 * a regular degree and allocate memory accordingly.
 *
 * Assuming n nodes and maximum degree d, the amount of memory used by this pool is:
 * - 4*d*n bytes for edges (which is expected to dominate)
 * - O(4*3*n) bytes for nodes
 *
 * Conceptually, the implementation works by allocating an array of size 4*d*n, and then fills it up
 * sequentially. Thus, when a node arrives that has not been seen yet, it is added to the current
 * position in this array and the position is moved forward by d (reserving space for d edges for
 * this node). And a map for nodes keeps track of their positions in this array. The concrete
 * implementation differs from the conceptual one only in that it spreads the big array across a
 * few small ones, called shards. The only additional change this requires is that nodes also need
 * to keep track of their shard id in addition to the offset. If more nodes arrive than we expect,
 * then additional shards are added. Note that the shard length itself is never re-sized, which
 * we can fix if needed.
 *
 * NOTE: The implementation here-in assumes that the int id's being inserted are "packed" nicely. In
 * particular, suppose there are n nodes to be inserted. Then the actual int id's for these n nodes
 * _must_ always be no larger than c*n for some constant c. The memory usage here is proportional to
 * c, so it is best to make it as small as possible.
 *
 * This class is thread-safe even though it does not do any locking: it achieves this by leveraging
 * the assumptions stated below and using a "memory barrier" between writes and reads to sync
 * updates.
 *
 * Here are the client assumptions needed to enable lock-free read/writes:
 * 1. There is a SINGLE writer thread -- this is extremely important as we don't lock during writes.
 * 2. Readers are OK reading stale data, i.e. if even if a reader thread arrives after the writer
 * thread started doing a write, the update is NOT guaranteed to be available to it.
 *
 * This class enables lock-free read/writes by guaranteeing the following:
 * 1. The writes that are done are always "safe", i.e. in no time during the writing do they leave
 *    things in a state such that a reader would either encounter an exception or do wrong
 *    computation.
 * 2. After a write is done, it is explicitly "published" such that a reader that arrives after
 *    the published write it would see updated data.
 *
 * The way this works is as follows: suppose we have some linked objects X, Y and Z that need to be
 * maintained in a consistent state. First, our setup ensures that the reader is _only_ allowed to
 * access these in a linear manner as follows: read X -> read Y -> read Z. Then, we ensure that the
 * writer behavior is to write (safe, atomic) updates to each of these in the exact opposite order:
 * write Z --flush--> write Y --flush--> write X.
 *
 * Note that the flushing ensures that if a reader sees Y then it _must_ also see the updated Z,
 * and if sees X then it _must_ also see the updated Y and Z. Further, each update itself is safe.
 * For instance, a reader can safely access an updated Z even if X and Y are not updated since the
 * updated information will only be accessible through the updated X and Y (the converse though is
 * NOT true). Together, this ensures that the reader accesses to the objects are always consistent
 * with each other and safe to access by the reader.
 */
public class RegularDegreeEdgePool extends AbstractRegularDegreeEdgePool {
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
  public static final class ReaderAccessibleInfo implements EdgePoolReaderAccessibleInfo {
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

    public BigIntArray getEdges() {
      return edges;
    }

    public BigLongArray getMetadata() {
      throw new UnsupportedOperationException("get metadata is not supported in "
        + "ReaderAccessibleInfo");
    }

    public IntToIntPairHashMap getNodeInfo() {
      return nodeInfo;
    }
  }

  /**
   * Reserves the needed memory for a {@link RegularDegreeEdgePool}, and initializes most of the
   * objects that would be needed for this graph. Note that actual memory would be allocated as
   * needed, and the amount of memory needed will increase if more nodes arrive than expected.
   *
   * @param expectedNumNodes  is the expected number of nodes that will be added into this pool.
   *                          The actual number of nodes can be larger and the pool will expand
   *                          itself to fit them till we hit the limit of max array size in Java.
   * @param maxDegree         is the maximum degree for a node in this pool. There will be an error
   *                          if this is violated.
   */
  public RegularDegreeEdgePool(int expectedNumNodes, int maxDegree, StatsReceiver statsReceiver) {
    super(expectedNumNodes, maxDegree, statsReceiver);

    // We use a faster map in the base case
    IntToIntPairHashMap intToIntPairHashMap;
    if (maxDegree == 2) {
      intToIntPairHashMap =
          new IntToIntPairArrayIndexBasedMap(expectedNumNodes, -1, scopedStatsReceiver);
    } else {
      intToIntPairHashMap =
          new IntToIntPairConcurrentHashMap(expectedNumNodes, 0.5, -1, scopedStatsReceiver);
    }
    // This doesn't allocate memory for all the edges, which is done lazily
    readerAccessibleInfo = new ReaderAccessibleInfo(
        // We force each node's edges to fit within a shard
        new ShardedBigIntArray(expectedNumNodes, maxDegree, 0, scopedStatsReceiver),
        intToIntPairHashMap
    );
  }


  @Override
  public void addEdge(int nodeA, int nodeB) {
    long nodeAInfo;
    // Add the node if it doesn't exist
    if (readerAccessibleInfo.getNodeInfo().getBothValues(nodeA) == -1L) {
      // Note that the degree is set to 0 so this is safe to access after this point
      nodeAInfo = addNewNode(nodeA);
    } else {
      nodeAInfo = readerAccessibleInfo.getNodeInfo().getBothValues(nodeA);
    }
    int nodeADegree = getNodeDegreeFromNodeInfo(nodeAInfo);
    Preconditions.checkArgument(nodeADegree < maxDegree,
      "Exceeded the maximum degree (" + maxDegree + ") for node " + nodeA);
    int nodeAPosition = getNodePositionFromNodeInfo(nodeAInfo);
    int position = nodeAPosition + nodeADegree;
    readerAccessibleInfo.getEdges().addEntry(nodeB, position);
    // This is to guarantee that if a reader sees the updated degree later, they can find the edge
    currentNumEdgesStored++;
    // The order is important -- the updated degree is the ONLY way for a reader for going to the
    // new edge, so this needs to be the last update
    // since this is a volatile increment any reader will now see the updated degree
    readerAccessibleInfo.getNodeInfo().incrementSecondValue(nodeA, 1);

    numEdgesCounter.incr();
  }

  @Override
  public void addEdge(int nodeA, int nodeB, long metadata) {
    throw new UnsupportedOperationException("add edge with metadata is not supported in "
      + "RegularDegreeEdgePool");
  }

  @Override
  protected long getNumberedEdgeMetadata(int position, int edgeNumber) {
    return 0;
  }

  @Override
  public long[] getMetadataShard(int node) {
    return null;
  }
}
