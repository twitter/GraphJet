package com.twitter.graphjet.bipartite.edgepool;

import java.util.Random;

import com.google.common.base.Preconditions;

import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeRandomIntIterator;
import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.ints.IntIterator;

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
   * Reserves the needed memory for a {@link PowerLawDegreeEdgePool}, and initializes most of the
   * objects that would be needed for this graph. Note that memory would be allocated as needed,
   * and the amount of memory needed can change if the input parameters are violated.
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
    this.statsReceiver = statsReceiver.scope("PowerLawDegreeEdgePool");
    this.numEdgesCounter = this.statsReceiver.counter("numEdges");
    this.numNodesCounter = this.statsReceiver.counter("numNodes");
    this.numPoolsCounter = this.statsReceiver.counter("numPools");
    numPools = Math.max((int) Math.ceil(Math.log(expectedMaxDegree + 1) / Math.log(2.0) - 1.0), 1);
    numPoolsCounter.incr(numPools);
    /*
    RegularDegreeEdgePool[] edgePools = new RegularDegreeEdgePool[numPools];
    int[] poolDegrees = new int[numPools];
    readerAccessibleInfo =
      new ReaderAccessibleInfo(edgePools, poolDegrees, new int[expectedNumNodes]);
    for (int i = 0; i < numPools; i++) {
      initPool(i);
    }
    */
    currentNumEdgesStored = 0;
  }

  private void initPool(int poolNumber) {
    int expectedNumNodesInPool =
      (int) Math.ceil(expectedNumNodes / Math.pow(powerLawExponent, poolNumber));
    int maxDegreeInPool = (int) Math.pow(2, poolNumber + 1);
    readerAccessibleInfo.edgePools[poolNumber] = new RegularDegreeEdgePool(
      expectedNumNodesInPool, maxDegreeInPool, statsReceiver.scope("poolNumber_" + poolNumber));
    readerAccessibleInfo.poolDegrees[poolNumber] = maxDegreeInPool;
  }

  @Override
  public int getNodeDegree(int node) {
    if (node >= readerAccessibleInfo.nodeDegrees.length) {
      return 0;
    } else {
      return readerAccessibleInfo.nodeDegrees[node];
    }
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

  protected int getNextPoolForNode(int node) {
    return getPoolForEdgeNumber(getNodeDegree(node));
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

  protected int getDegreeForPool(int pool) {
    // Hopefully branch prediction should make this really cheap as it'll always be false!
    if (crossMemoryBarrier() == 0) {
      return -1;
    }
    return readerAccessibleInfo.poolDegrees[pool];
  }

  private int[] growArray(int[] array, int minIndex) {
    int arrayLength = array.length;
    int[] newArray =
      new int[Math.max((int) Math.ceil(arrayLength * ARRAY_GROWTH_FACTOR), minIndex + 1)];
    System.arraycopy(array, 0, newArray, 0, arrayLength);
    return newArray;
  }

  protected void expandArray(int nodeA) {
    readerAccessibleInfo = new ReaderAccessibleInfo(
      readerAccessibleInfo.edgePools,
      readerAccessibleInfo.poolDegrees,
      growArray(readerAccessibleInfo.nodeDegrees, nodeA)
    );
    numNodesCounter.incr();
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
}
