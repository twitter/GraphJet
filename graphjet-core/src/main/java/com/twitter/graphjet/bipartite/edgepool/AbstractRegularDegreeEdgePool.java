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
    this.scopedStatsReceiver = statsReceiver;
    this.numEdgesCounter = scopedStatsReceiver.counter("numEdges");
    this.numNodesCounter = scopedStatsReceiver.counter("numNodes");
    currentPositionOffset = 0;
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

  /**
   * Get the metadata of a specified edge for the node: note that it is the caller's responsibility
   * to check that the edge number is within the degree bounds.
   *
   * @param position    is the position index for the node
   * @param edgeNumber  is the required edge number
   * @return the requested edge metadata
   */
  abstract protected long getNumberedEdgeMetadata(int position, int edgeNumber);
  /*
  protected long getNumberedEdgeMetadata(int position, int edgeNumber) {
    return readerAccessibleInfo.getMetadata().getEntry(position + edgeNumber);
  }
  */

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
  public boolean isOptimized() {
    return false;
  }

  public int[] getShard(int node) {
    return ((ShardedBigIntArray) readerAccessibleInfo.getEdges()).
      getShard(readerAccessibleInfo.getNodeInfo().getFirstValue(node));
  }

  abstract public long[] getMetadataShard(int node);

  /*
  public long[] getMetadataShard(int node) {
    return ((ShardedBigLongArray) readerAccessibleInfo.getMetadata()).
      getShard(readerAccessibleInfo.getNodeInfo().getFirstValue(node));
  }
  */

  public int getShardOffset(int node) {
    return ((ShardedBigIntArray) readerAccessibleInfo.getEdges()).
      getShardOffset(readerAccessibleInfo.getNodeInfo().getFirstValue(node));
  }

  @Override
  public void removeEdge(int nodeA, int nodeB) {
    throw new UnsupportedOperationException("The remove operation is currently not supported");
  }

  @Override
  public double getFillPercentage() {
    return readerAccessibleInfo.getEdges().getFillPercentage();
  }

}
