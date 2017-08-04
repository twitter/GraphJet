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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeRandomIntIterator;
import com.twitter.graphjet.hashing.IntToIntPairArrayIndexBasedMap;
import com.twitter.graphjet.hashing.IntToIntPairHashMap;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * This edge pool stores edges compactly in a two-dimension array. The edge pool assumes that
 * the degree of each node is fixed when the edge pool is constructed, and it is able to allocate
 * the exact memory used in the two-dimension array. This edge pool does not handle synchronization
 * between writer and reader threads, and it accepts reader access only after it is completely
 * populated by a writer thread. It assumes that no new edges will be added to the pool after it
 * accepts reader access.
 */
public abstract class AbstractOptimizedEdgePool implements EdgePool {

  // This is is the only reader-accessible data
  protected EdgePoolReaderAccessibleInfo readerAccessibleInfo;

  protected IntToIntPairHashMap intToIntPairHashMap;

  protected int currentNumEdgesStored;
  protected int maxNumEdges;
  protected int maxDegree;
  protected int numOfNodes;

  protected static final Logger LOG = LoggerFactory.getLogger("graph");
  protected StatsReceiver scopedStatsReceiver;

  protected static final int[] POW_TABLE_30;
  static {
    POW_TABLE_30 = new int[30];
    POW_TABLE_30[0] = 0;
    for (int i = 1; i < 30; i++) {
      POW_TABLE_30[i] = (int) Math.pow(2.0, i) + POW_TABLE_30[i - 1];
    }
  }

  /**
   * AbstractOptimizedEdgePool
   *
   * @param nodeDegrees node degree map
   * @param maxNumEdges the max number of edges will be added in the pool
   * @param statsReceiver stats receiver
   */
  public AbstractOptimizedEdgePool(
    int[] nodeDegrees,
    int maxNumEdges,
    StatsReceiver statsReceiver
  ) {
    numOfNodes = nodeDegrees.length;
    currentNumEdgesStored = 0;
    scopedStatsReceiver = statsReceiver.scope(this.getClass().getSimpleName());

    this.maxNumEdges = maxNumEdges;

    intToIntPairHashMap = new IntToIntPairArrayIndexBasedMap(numOfNodes, -1, scopedStatsReceiver);

    int position = 0;
    maxDegree = 0;

    for (int i = 0; i < numOfNodes; i++) {
      int nodeDegree = nodeDegrees[i];
      if (nodeDegree == 0) {
        continue;
      }

      maxDegree = Math.max(maxDegree, nodeDegree);

      intToIntPairHashMap.put(i, position, nodeDegree);
      position += nodeDegree;
    }
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
    return readerAccessibleInfo.getEdges().getEntry(position + edgeNumber);
  }

  /**
   * Get the metadata of a specified edge for the node: note that it is the caller's responsibility
   * to check that the edge number is within the degree bounds.
   *
   * @param position is the position index for the node
   * @param edgeNumber is the required edge number
   * @return the requested edge metadata
   */
  protected abstract long getEdgeMetadata(int position, int edgeNumber);

  protected int getNodePosition(int node) {
    return readerAccessibleInfo.getNodeInfo().getFirstValue(node);
  }

  @Override
  public int getNodeDegree(int node) {
    return readerAccessibleInfo.getNodeInfo().getSecondValue(node);
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
      + "AbstractOptimizedEdgePool");
  }

  @Override
  public void addEdge(int nodeA, int nodeB, long metadata) {
    throw new UnsupportedOperationException("add a single edge one by one is not supported in "
      + "AbstractOptimizedEdgePool");
  }

  /**
   * Batch add edges in optimized segment.
   *
   * @param node the node id which the edges are associated to
   * @param pool the pool id which the edges are associated to
   * @param src  the source int edge array
   * @param metadata the source long edge metadata array
   * @param srcPos the starting position in the source array
   * @param length the number of edges to be copied
   */
  public abstract void addEdges(
    int node,
    int pool,
    int[] src,
    long[] metadata,
    int srcPos,
    int length
  );

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
