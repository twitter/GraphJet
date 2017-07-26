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

import com.twitter.graphjet.hashing.BigIntArray;
import com.twitter.graphjet.hashing.BigLongArray;
import com.twitter.graphjet.hashing.IntToIntPairHashMap;
import com.twitter.graphjet.hashing.ShardedBigIntArray;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 *
 * An {@link AbstractOptimizedEdgePool} which does not support edge metadata.
 *
 * Assuming n nodes and m edges, the amount of memory used by this pool is:
 * - 4*m bytes for edges (which is expected to dominate)
 * - O(4*3*n) bytes for nodes
 */
public class OptimizedEdgePool extends AbstractOptimizedEdgePool {

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
    super(nodeDegrees, maxNumEdges, statsReceiver);

    BigIntArray edges = new ShardedBigIntArray(maxNumEdges, maxDegree, 0, scopedStatsReceiver);

    readerAccessibleInfo = new ReaderAccessibleInfo(
      edges,
      intToIntPairHashMap
    );

    LOG.info(
      "OptimizedEdgePool: maxNumEdges " + maxNumEdges + " maxNumNodes " + numOfNodes
    );
  }

  @Override
  protected long getEdgeMetadata(int position, int edgeNumber) {
    return 0;
  }

  @Override
  public void addEdges(int node, int pool, int[] src, long[] metadata, int srcPos, int length) {
    int position = getNodePosition(node);

    readerAccessibleInfo.getEdges().arrayCopy(
      src,
      srcPos,
      position + POW_TABLE_30[pool],
      length,
      true /*updateStats*/
    );
  }
}
