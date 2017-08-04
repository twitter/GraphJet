/**
 * Copyright 2017 Twitter. All rights reserved.
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

import java.util.Random;

import com.google.common.base.Preconditions;

import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeRandomIntIterator;
import com.twitter.graphjet.hashing.IntToIntPairArrayIndexBasedMap;
import com.twitter.graphjet.hashing.ShardedBigIntArray;
import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.ints.IntIterator;


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
abstract public class AbstractRegularDegreeEdgePool implements EdgePool {

  // This is is the only reader-accessible data
  protected EdgePoolReaderAccessibleInfo readerAccessibleInfo;

  // Writes and subsequent reads across this will cross the memory barrier
  protected volatile int currentNumEdgesStored;

  protected final int maxDegree;

  protected int currentPositionOffset;
  protected int currentNumNodes = 0;
  protected int currentShardId = 0;

  protected StatsReceiver scopedStatsReceiver;
  protected final Counter numEdgesCounter;
  protected final Counter numNodesCounter;

  public AbstractRegularDegreeEdgePool(int expectedNumNodes, int maxDegree, StatsReceiver statsReceiver) {
    Preconditions.checkArgument(expectedNumNodes > 0, "Need to have at least one node!");
    Preconditions.checkArgument(maxDegree > 0, "Max degree must be non-zero!");
    this.maxDegree = maxDegree;
    this.scopedStatsReceiver = statsReceiver.scope(this.getClass().getSimpleName());
    this.numEdgesCounter = scopedStatsReceiver.counter("numEdges");
    this.numNodesCounter = scopedStatsReceiver.counter("numNodes");
    this.currentPositionOffset = 0;
  }

  // Read the volatile int, which forces a happens-before ordering on the read-write operations
  protected int crossMemoryBarrier() {
    return currentNumEdgesStored;
  }

  // degree is set to 0 initially
  private long addNodeInfo(int node) {
    long nodeInfo = ((long) currentPositionOffset) << 32; // degree is 0 to start
    readerAccessibleInfo.getNodeInfo().put(node, currentPositionOffset, 0);
    return nodeInfo;
  }

  // ALL readers who want to get the latest update should first go through this to cross the memory
  // barrier (and for optimizing look-ups into the hash table) and ONLY then access the edges
  protected long getNodeInfo(int node) {
    // Hopefully branch prediction should make the memory barrier check really cheap as it'll
    // always be false!
    if (crossMemoryBarrier() == 0) {
      return -1;
    }
    return readerAccessibleInfo.getNodeInfo().getBothValues(node);
  }

  public static int getNodePositionFromNodeInfo(long nodeInfo) {
    return IntToIntPairArrayIndexBasedMap.getFirstValueFromNodeInfo(nodeInfo);
  }

  public static int getNodeDegreeFromNodeInfo(long nodeInfo) {
    return IntToIntPairArrayIndexBasedMap.getSecondValueFromNodeInfo(nodeInfo);
  }

  /**
   * Get a specified edge for the node: note that it is the caller's responsibility to check that
   * the edge number is within the degree bounds.
   *
   * @param node         is the node whose edges are being requested
   * @param edgeNumber   is the required edge number
   * @return the requested edge
   */
  protected int getNodeEdge(int node, int edgeNumber) {
    long nodeInfo = getNodeInfo(node);
    if (edgeNumber > getNodeDegreeFromNodeInfo(nodeInfo)) {
      return -1;
    }
    return getNumberedEdge(getNodePositionFromNodeInfo(nodeInfo), edgeNumber);
  }

  /**
   * Get a specified edge metadata for the node: note that it is the caller's responsibility to
   * check that the edge number is within the degree bounds.
   *
   * @param node         is the node whose edges are being requested
   * @param edgeNumber   is the required edge number
   * @return the requested edge metadata
   */
  protected long getNodeEdgeMetadata(int node, int edgeNumber) {
    long nodeInfo = getNodeInfo(node);
    if (edgeNumber > getNodeDegreeFromNodeInfo(nodeInfo)) {
      return -1;
    }
    return getNumberedEdgeMetadata(getNodePositionFromNodeInfo(nodeInfo), edgeNumber);
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
    return readerAccessibleInfo.getEdges().getEntry(position + edgeNumber);
  }

  protected long addNewNode(int nodeA) {
    // This is an atomic entry, so it is safe for readers to access the node as long as they
    // account for the degree being 0
    long nodeInfo = addNodeInfo(nodeA);
    currentPositionOffset += maxDegree;
    currentNumNodes++;
    numNodesCounter.incr();
    return nodeInfo;
  }

  @Override
  public int getNodeDegree(int node) {
    long nodeInfo = getNodeInfo(node);
    if (nodeInfo == -1) {
      return 0;
    }
    return getNodeDegreeFromNodeInfo(getNodeInfo(node));
  }

  @Override
  public IntIterator getNodeEdges(int node) {
    return getNodeEdges(node, new RegularDegreeEdgeIterator(this));
  }

  /**
   * Reuses the given iterator to point to the current nodes edges.
   *
   * @param node                       is the node whose edges are being returned
   * @param regularDegreeEdgeIterator  is the iterator to reuse
   * @return the iterator itself, reset over the nodes edges
   */
  @Override
  public IntIterator getNodeEdges(int node, ReusableNodeIntIterator regularDegreeEdgeIterator) {
    return regularDegreeEdgeIterator.resetForNode(node);
  }

  @Override
  public IntIterator getRandomNodeEdges(int node, int numSamples, Random random) {
    return getRandomNodeEdges(node, numSamples, random, new RegularDegreeEdgeRandomIterator(this));
  }

  @Override
  public IntIterator getRandomNodeEdges(
    int node,
    int numSamples,
    Random random,
    ReusableNodeRandomIntIterator regularDegreeEdgeRandomIterator) {
    return regularDegreeEdgeRandomIterator.resetForNode(node, numSamples, random);
  }

  @Override
  public boolean isOptimized() {
    return false;
  }

  public int[] getShard(int node) {
    return ((ShardedBigIntArray) readerAccessibleInfo.getEdges()).
      getShard(readerAccessibleInfo.getNodeInfo().getFirstValue(node));
  }

  public int getShardOffset(int node) {
    return ((ShardedBigIntArray) readerAccessibleInfo.getEdges()).
      getShardOffset(readerAccessibleInfo.getNodeInfo().getFirstValue(node));
  }


  /**
   * Get the metadata of a specified edge for the node: note that it is the caller's responsibility
   * to check that the edge number is within the degree bounds.
   *
   * @param position    is the position index for the node
   * @param edgeNumber  is the required edge number
   * @return the requested edge metadata
   */
  abstract protected long getNumberedEdgeMetadata(int position, int edgeNumber);

  abstract public long[] getMetadataShard(int node);

  @Override
  public void removeEdge(int nodeA, int nodeB) {
    throw new UnsupportedOperationException("The remove operation is currently not supported");
  }

  @Override
  public double getFillPercentage() {
    return readerAccessibleInfo.getEdges().getFillPercentage();
  }

}
