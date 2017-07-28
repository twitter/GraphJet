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
import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * This edge pool is for the case where the nodes degrees approximately follow a power law with the
 * restriction that the node degrees are upper bounded by the power law, i.e. if the power law
 * assumes that there are x nodes with degree d, then the graph will have at most x nodes with
 * degree d.
 *
 * Conceptually, the implementation works by spreading the edges of nodes across a number of pools.
 * Each of these pool services nodes at a particular level in the power law distribution, i.e. pool
 * 0 contains all nodes, and pool i only has nodes who have degree greater than 2^{i + 1}. Note that
 * our assumption of the power law (as stated above) implies that we have an exact upper bound on
 * the number of nodes in pool i. Within each pool, we allocate the same fixed amount of space for
 * each node. Since each pool allocates a fixed number of edges per node, it is implemented via the
 * {@link RegularDegreeEdgePool}. Note that the overall setup ensures that all pools are the same
 * size, with the degree per node increasing as the pool number increases while the number of nodes
 * in the pool decreases.
 *
 * NOTE: The implementation here-in assumes that the int id's being inserted are "packed" nicely. In
 * particular, suppose there are n nodes to be inserted. Then the actual int id's for these n nodes
 * _must_ always be no larger than c*n for some constant c. The memory usage here is proportional to
 * c*n, so it is best to make c as small as possible.
 *
 * Assuming that there are n nodes, we can allocate memory in the pools via a power-law scheme.
 * Here is a sample calculation assuming an exponent of 2:
 * - There will be k pools, numbered 1 through k
 * - Max edges per node stored in pool i = 2^i
 * - Max number of nodes in pool i = n / 2^{i-1} (power-law assumption)
 * - Total number of edges stored, m = 2n + 2^2 * n/2 + 2^3 * n/2^2 + ... + 2^k * n / 2^(k-1) = 2kn
 * - Max degree of a node = 2^{k+1} - 1 = 2^{m/2n + 1} - 1 (example: max degree = 2047, if m = 20n)
 *
 * Assuming n nodes and maximum degree in the pool is d, the amount of memory used by this pool is:
 * - O(4*2*lg(d)*n) bytes for edges (which is expected to dominate)
 * - O(4*lg(d)*n) bytes for nodes
 * - Note that if d = O(n), which is expected for power law graphs, the amount of memory used here
 *   is O(n*lg(n)) as opposed to O(n^2) which is what would happen if we allocate a regular-degree
 *   pool instead.
 *
 * Note that the power-law assumption is NOT binding, i.e. if the degree distribution violates the
 * power law (or has a different exponent), the graph operations will still work with the same
 * complexity. The only harmful effect is that the amount of memory needed would be more than the
 * calculation states above.
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
public abstract class AbstractPowerLawDegreeEdgePool implements EdgePool {

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
    // Together, these are the pools that make a PowerLawEdgePool
    protected final AbstractRegularDegreeEdgePool[] edgePools;
    protected final int[] poolDegrees;
    // This is the first object that a reader sees, i.e. it gates access to the edges
    protected final int[] nodeDegrees;

    /**
     * A new instance is immediately visible to the readers due to publication safety.
     *
     * @param edgePools      is the array of
     *                       {@link com.twitter.graphjet.bipartite.edgepool.RegularDegreeEdgePool}s
     * @param poolDegrees    contains the maximum degree in pool i
     * @param nodeDegrees    contains the degree of each node
     */
    public ReaderAccessibleInfo(
      AbstractRegularDegreeEdgePool[] edgePools,
      int[] poolDegrees,
      int[] nodeDegrees) {
      this.edgePools = edgePools;
      this.poolDegrees = poolDegrees;
      this.nodeDegrees = nodeDegrees;
    }

    public AbstractRegularDegreeEdgePool[] getEdgePools() {
      return edgePools;
    }

    public int[] getPoolDegrees() {
      return poolDegrees;
    }

    public int[] getNodeDegrees() {
      return nodeDegrees;
    }
  }

  protected static final double POOL_GROWTH_FACTOR = 1.1;
  protected static final double ARRAY_GROWTH_FACTOR = 1.1;
  protected static final int[] LOG_TABLE_256;
  static {
    LOG_TABLE_256 = new int[256];
    LOG_TABLE_256[0] = LOG_TABLE_256[1] = 0;
    for (int i = 2; i < 256; i++) {
      LOG_TABLE_256[i] = 1 + LOG_TABLE_256[i / 2];
    }
  }

  protected final int expectedNumNodes;
  protected final double powerLawExponent;
  protected int numPools;

  // This object contains ALL the reader-accessible data
  protected ReaderAccessibleInfo readerAccessibleInfo;
  // Writes and subsequent reads across this will cross the memory barrier
  protected volatile int currentNumEdgesStored;

  protected final StatsReceiver statsReceiver;
  protected final Counter numEdgesCounter;
  protected final Counter numNodesCounter;
  protected final Counter numPoolsCounter;

  /**
   * Common variable initialization for an {@link AbstractPowerLawDegreeEdgePool}, the bulk of the
   * memory is allocated in the constructors of its child classes.
   *
   * @param expectedNumNodes    is the expected number of nodes that will be added into this pool
   * @param expectedMaxDegree   is the expected maximum degree for a node in the pool
   * @param powerLawExponent    is the expected exponent of the power-law graph, i.e.
   *                            (# nodes with degree greater than 2^i) <= (n / powerLawExponent^i)
   */
  public AbstractPowerLawDegreeEdgePool(
    int expectedNumNodes,
    int expectedMaxDegree,
    double powerLawExponent,
    StatsReceiver statsReceiver) {
    Preconditions.checkArgument(expectedNumNodes > 0, "Need to have at least one node!");
    Preconditions.checkArgument(expectedMaxDegree > 0, "Max degree must be non-zero!");
    Preconditions.checkArgument(powerLawExponent > 1.0,
      "The power-law exponent must be greater than 1.0!");
    this.expectedNumNodes = expectedNumNodes;
    this.powerLawExponent = powerLawExponent;
    this.statsReceiver = statsReceiver.scope(this.getClass().getSimpleName());
    this.numEdgesCounter = this.statsReceiver.counter("numEdges");
    this.numNodesCounter = this.statsReceiver.counter("numNodes");
    this.numPoolsCounter = this.statsReceiver.counter("numPools");
    numPools = Math.max((int) Math.ceil(Math.log(expectedMaxDegree + 1) / Math.log(2.0) - 1.0), 1);
    numPoolsCounter.incr(numPools);
    currentNumEdgesStored = 0;
  }

  // Assumes that the node is already in the array
  protected void incrementNodeDegree(int node) {
    readerAccessibleInfo.nodeDegrees[node]++;
  }

  /**
   * Since the pools have power of two degrees and edges for a node are assigned sequentially
   * through pools, we can recover the pool edge number i for a node as floor(lg(i + 2) - 1). This
   * function does that as a bit hack rather than taking logs which is both error prone (due to
   * precision) and much slower. Copied shamelessly from:
   * http://graphics.stanford.edu/~seander/bithacks.html#IntegerLog
   *
   * @param edge  is the edge number to find the pool for
   * @return the pool number that this edge should be interested in. Note that this indexes at 0,
   *         so edges # 0 and 1 go in pool 0, edges 2-5 go in pool 1 and so on.
   */
  public static int getPoolForEdgeNumber(int edge) {
    int v = edge + 2; // 32-bit word to find the log of
    int r;  // r will be lg(v)
    int tt; // temporary
    if ((tt = v >> 24) != 0) {
      r = 24 + LOG_TABLE_256[tt];
    } else if ((tt = v >> 16) != 0) {
      r = 16 + LOG_TABLE_256[tt];
    } else if ((tt = v >> 8) != 0) {
      r = 8 + LOG_TABLE_256[tt];
    } else {
      r = LOG_TABLE_256[v];
    }
    r--;
    return r;
  }

  public static int getEdgeNumberInPool(int poolNumber, int edgeNumber) {
    // For pool i, this is: \sum_i 2^i - 1  = 2^{i+1} - 2
    int numEdgesBeforeThisPool = (1 << (poolNumber + 1)) - 2;
    return edgeNumber - numEdgesBeforeThisPool;
  }

  // Read the volatile int, which forces a happens-before ordering on the read-write operations
  private int crossMemoryBarrier() {
    return currentNumEdgesStored;
  }

  private int[] growArray(int[] array, int minIndex) {
    int arrayLength = array.length;
    int[] newArray =
      new int[Math.max((int) Math.ceil(arrayLength * ARRAY_GROWTH_FACTOR), minIndex + 1)];
    System.arraycopy(array, 0, newArray, 0, arrayLength);
    return newArray;
  }

  protected int getNextPoolForNode(int node) {
    return getPoolForEdgeNumber(getNodeDegree(node));
  }

  protected int getDegreeForPool(int pool) {
    // Hopefully branch prediction should make this really cheap as it'll always be false!
    if (crossMemoryBarrier() == 0) {
      return -1;
    }
    return readerAccessibleInfo.poolDegrees[pool];
  }

  protected void expandArray(int nodeA) {
    readerAccessibleInfo = new ReaderAccessibleInfo(
      readerAccessibleInfo.edgePools,
      readerAccessibleInfo.poolDegrees,
      growArray(readerAccessibleInfo.nodeDegrees, nodeA)
    );
    numNodesCounter.incr();
  }

  protected AbstractRegularDegreeEdgePool getRegularDegreeEdgePool(int poolNumber) {
    return readerAccessibleInfo.edgePools[poolNumber];
  }

  protected int getNumPools() {
    // Hopefully branch prediction should make this really cheap as it'll always be false!
    if (crossMemoryBarrier() == 0) {
      return 0;
    }
    return numPools;
  }

  public abstract boolean hasEdgeMetadata();

  @Override
  public int getNodeDegree(int node) {
    if (node >= readerAccessibleInfo.nodeDegrees.length) {
      return 0;
    } else {
      return readerAccessibleInfo.nodeDegrees[node];
    }
  }

  @Override
  public IntIterator getNodeEdges(int node) {
    return getNodeEdges(node, new PowerLawDegreeEdgeIterator(this));
  }

  @Override
  public IntIterator getNodeEdges(int node, ReusableNodeIntIterator powerLawDegreeEdgeIterator) {
    return powerLawDegreeEdgeIterator.resetForNode(node);
  }

  @Override
  public IntIterator getRandomNodeEdges(int node, int numSamples, Random random) {
    return getRandomNodeEdges(node, numSamples, random, new PowerLawDegreeEdgeRandomIterator(this));
  }

  @Override
  public IntIterator getRandomNodeEdges(
    int node,
    int numSamples,
    Random random,
    ReusableNodeRandomIntIterator powerLawDegreeEdgeRandomIterator) {
    return powerLawDegreeEdgeRandomIterator.resetForNode(node, numSamples, random);
  }

  @Override
  public boolean isOptimized() {
    return false;
  }

  @Override
  public void removeEdge(int nodeA, int nodeB) {
    throw new UnsupportedOperationException("The remove operation is currently not supported");
  }

  @Override
  public double getFillPercentage() {
    // Hopefully branch prediction should make this really cheap as it'll always be false!
    if (crossMemoryBarrier() == 0) {
      return 0.0;
    }
    double fillPercentage = 0.0;
    for (AbstractRegularDegreeEdgePool edgePool : readerAccessibleInfo.edgePools) {
      fillPercentage += edgePool.getFillPercentage();
    }
    return fillPercentage / readerAccessibleInfo.edgePools.length;
  }

  /**
   * Allows looking up an edge number by index for a given node.
   *
   * @param node        is the node whose edge is being looked up
   * @param edgeNumber  is the global index number of the edge
   * @return the edge at the index, i.e. if edgeNumber is i then we return the edge that was
   *         the i-th inserted edge
   */
  public int getNumberedEdge(int node, int edgeNumber) {
    int poolNumber = getPoolForEdgeNumber(edgeNumber);
    int edgeNumberInPool = getEdgeNumberInPool(poolNumber, edgeNumber);
    return readerAccessibleInfo.edgePools[poolNumber].getNodeEdge(node, edgeNumberInPool);
  }

  public int getNumberedEdgeInRegularPool(int node, int poolNumber, int edgeNumberInPool) {
    return readerAccessibleInfo.edgePools[poolNumber].getNodeEdge(node, edgeNumberInPool);
  }

  public long getNumberedEdgeMetadataInRegularPool(int node, int poolNumber, int edgeNumberInPool) {
    return readerAccessibleInfo.edgePools[poolNumber].getNodeEdgeMetadata(node, edgeNumberInPool);
  }

  public int getCurrentNumEdgesStored() {
    return currentNumEdgesStored;
  }

  public StatsReceiver getStatsReceiver() {
    return statsReceiver;
  }

  public ReaderAccessibleInfo getReaderAccessibleInfo() {
    return readerAccessibleInfo;
  }
}
