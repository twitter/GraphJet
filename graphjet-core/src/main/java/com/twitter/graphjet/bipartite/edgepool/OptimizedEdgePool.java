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

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeRandomIntIterator;
import com.twitter.graphjet.hashing.BigIntArray;
import com.twitter.graphjet.hashing.IntToIntPairArrayIndexBasedMap;
import com.twitter.graphjet.hashing.IntToIntPairHashMap;
import com.twitter.graphjet.hashing.ShardedBigIntArray;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * This edge pool stores edges compactly in a two-dimension array. The edge pool assumes that
 * the degree of each node is fixed when the edge pool is constructed, and it is able to allocate
 * the exact memory used in the two-dimension array. This edge pool does not handle synchronization
 * between writer and reader threads, and it accepts reader access only after it is completely
 * populated by a writer thread. It assumes that no new edges will be added to the pool after it
 * accepts reader access.
 *
 * Assuming n nodes and m edges, the amount of memory used by this pool is:
 * - 4*m bytes for edges (which is expected to dominate)
 * - O(4*3*n) bytes for nodes
 */
public class OptimizedEdgePool implements EdgePool {
  public static final class ReaderAccessibleInfo {
    protected final BigIntArray edges;
    // Each entry contains 2 ints for a node: position, degree
    protected final IntToIntPairHashMap nodeInfo;

    /**
     * A new instance is immediately visible to the readers due to publication safety.
     *
     * @param edges contains all the edges in the pool
     * @param nodeInfo contains all the node information that is stored
     */
    public ReaderAccessibleInfo(
      BigIntArray edges,
      IntToIntPairHashMap nodeInfo) {
      this.edges = edges;
      this.nodeInfo = nodeInfo;
    }
  }
  // This is is the only reader-accessible data
  protected ReaderAccessibleInfo readerAccessibleInfo;

  private int currentNumEdgesStored;
  private int maxNumEdges;

  private static final Logger LOG = LoggerFactory.getLogger("graph");

  protected static final int[] POW_TABLE_30;
  static {
    POW_TABLE_30 = new int[30];
    POW_TABLE_30[0] = 0;
    for (int i = 1; i < 30; i++) {
      POW_TABLE_30[i] = (int) Math.pow(2.0, i) + POW_TABLE_30[i - 1];
    }
  }

  /**
   * OptimizedEdgePool
   *
   * @param nodeDegrees node degree map
   * @param maxNumEdges the max number of edges will be added in the pool
   * @param statsReceiver stats receiver
   */
  public OptimizedEdgePool(
    int[] nodeDegrees,
    int maxNumEdges,
    StatsReceiver statsReceiver
  ) {
    int numOfNodes = nodeDegrees.length;
    currentNumEdgesStored = 0;
    StatsReceiver scopedStatsReceiver = statsReceiver.scope(this.getClass().getSimpleName());

    this.maxNumEdges = maxNumEdges;

    IntToIntPairHashMap intToIntPairHashMap =
      new IntToIntPairArrayIndexBasedMap(numOfNodes, -1, scopedStatsReceiver);

    int position = 0;
    int maxDegree = 0;

    for (int i = 0; i < numOfNodes; i++) {
      int nodeDegree = nodeDegrees[i];
      if (nodeDegree == 0) {
        continue;
      }

      maxDegree = Math.max(maxDegree, nodeDegree);

      intToIntPairHashMap.put(i, position, nodeDegree);
      position += nodeDegree;
    }

    BigIntArray edges = new ShardedBigIntArray(
      maxNumEdges,
      maxDegree,
      2, /* metadataSize */
      0, /* nullEntry */
      scopedStatsReceiver
    );

    readerAccessibleInfo = new ReaderAccessibleInfo(
      edges,
      intToIntPairHashMap
    );

    LOG.info(
      "OptimizedEdgePool: maxNumEdges " + maxNumEdges + " maxNumNodes " + numOfNodes
    );
  }

  /**
   * Get a specified edge for the node: note that it is the caller's responsibility to check that
   * the edge number is within the degree bounds.
   *
   * @param position is the position index for the node
   * @param edgeNumber is the required edge number
   * @return the requested edge node number
   */
  protected int getNodeEdge(int position, int edgeNumber) {
    /*
    IntIterator intIterator = readerAccessibleInfo.edges.getMetadata(position + edgeNumber);

    System.out.println("node " + (position + edgeNumber)
      + " metadata 0 " + intIterator.nextInt()
      + " metadata 1 " + intIterator.nextInt()
    );

    for (int i = 0; i < ((ShardedBigIntArray)readerAccessibleInfo.edges).readerAccessibleInfo.array[0].length; i++) {
      System.out.print(((ShardedBigIntArray)readerAccessibleInfo.edges).readerAccessibleInfo.array[0][i] + " ");
    }
    System.out.println();
    */

    return readerAccessibleInfo.edges.getEntry(position + edgeNumber);
  }

  protected int getNodePosition(int node) {
    return readerAccessibleInfo.nodeInfo.getFirstValue(node);
  }

  @Override
  public int getNodeDegree(int node) {
    return readerAccessibleInfo.nodeInfo.getSecondValue(node);
  }

  @Override
  public IntIterator getNodeEdges(int node) {
    return getNodeEdges(node, new OptimizedEdgeIterator(this));
  }

  /**
   * Reuses the given iterator to point to the current nodes edges.
   *
   * @param node is the node whose edges are being returned
   * @param optimizedEdgeRandomIterator  is the iterator to reuse
   * @return the iterator itself, reset over the nodes edges
   */
  @Override
  public IntIterator getNodeEdges(int node, ReusableNodeIntIterator optimizedEdgeRandomIterator) {

    return optimizedEdgeRandomIterator.resetForNode(node);
  }

  @Override
  public IntIterator getRandomNodeEdges(int node, int numSamples, Random random) {
    return getRandomNodeEdges(node, numSamples, random, new OptimizedEdgeRandomIterator(this));
  }

  @Override
  public IntIterator getRandomNodeEdges(
    int node,
    int numSamples,
    Random random,
    ReusableNodeRandomIntIterator optimizedEdgeRandomIterator) {
    return optimizedEdgeRandomIterator.resetForNode(node, numSamples, random);
  }

  @Override
  public void addEdge(int nodeA, int nodeB) {
    throw new UnsupportedOperationException("add a single edge one by one is not supported in "
      + "OptimizedEdgePool");
  }

  @Override
  public void addEdge(int nodeA, int nodeB, int[] metadata) {
    throw new UnsupportedOperationException("add a single edge one by one is not supported in "
      + "OptimizedEdgePool");
  }

  /**
   * Batch add edges in optimized segment.
   *
   * @param node the node id which the edges are associated to
   * @param pool the pool id which the edges are associated to
   * @param src  the source array
   * @param srcPos the starting position in the source array
   * @param length the number of edges to be copied
   */
  public void addEdges(int node, int pool, int[] src, int srcPos, int length) {
    int position = getNodePosition(node);

    readerAccessibleInfo.edges.arrayCopy(
      src,
      srcPos,
      position + POW_TABLE_30[pool],
      length,
      true /*updateStats*/
    );
  }

  @Override
  public boolean isOptimized() {
    return true;
  }

  @Override
  public void removeEdge(int nodeA, int nodeB) {
    throw new UnsupportedOperationException("The remove operation is currently not supported");
  }

  @Override
  public double getFillPercentage() {
    return 100.0 * (double) currentNumEdgesStored / maxNumEdges;
  }
}
