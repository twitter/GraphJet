package com.twitter.graphjet.bipartite.edgepool;

import com.twitter.graphjet.hashing.BigIntArray;
import com.twitter.graphjet.hashing.BigLongArray;
import com.twitter.graphjet.hashing.IntToIntPairHashMap;
import com.twitter.graphjet.hashing.ShardedBigIntArray;
import com.twitter.graphjet.hashing.ShardedBigLongArray;
import com.twitter.graphjet.stats.StatsReceiver;

public class WithMetadataOptimizedEdgePool extends AbstractOptimizedEdgePool {
  public static final class WithMetadataReaderAccessibleInfo
    implements EdgePoolReaderAccessibleInfo {
    public final BigIntArray edges;
    public final BigLongArray metadata;
    // Each entry contains 2 ints for a node: position, degree
    protected final IntToIntPairHashMap nodeInfo;

    /**
     * A new instance is immediately visible to the readers due to publication safety.
     *
     * @param edges                  contains all the edges in the pool
     * @param metadata               contains all the edge metadata in the pool
     * @param nodeInfo               contains all the node information that is stored
     */
    public WithMetadataReaderAccessibleInfo(
      BigIntArray edges,
      BigLongArray metadata,
      IntToIntPairHashMap nodeInfo) {
      this.edges = edges;
      this.metadata = metadata;
      this.nodeInfo = nodeInfo;
    }

    public BigIntArray getEdges() {
      return edges;
    }

    public BigLongArray getMetadata() {
      return metadata;
    }

    public IntToIntPairHashMap getNodeInfo() {
      return nodeInfo;
    }
  }

  /**
   * WithMetadataOptimizedEdgePool
   *
   * @param nodeDegrees node degree map
   * @param maxNumEdges the max number of edges will be added in the pool
   * @param statsReceiver stats receiver
   */
  public WithMetadataOptimizedEdgePool(
    int[] nodeDegrees,
    int maxNumEdges,
    StatsReceiver statsReceiver
  ) {
    super(nodeDegrees, maxNumEdges, statsReceiver);

    BigIntArray edges = new ShardedBigIntArray(maxNumEdges, maxDegree, 0, scopedStatsReceiver);
    BigLongArray metadata = new ShardedBigLongArray(maxNumEdges, maxDegree, 0, scopedStatsReceiver);

    readerAccessibleInfo = new WithMetadataReaderAccessibleInfo(
      edges,
      metadata,
      intToIntPairHashMap
    );

    LOG.info(
      "WithMetadataOptimizedEdgePool: maxNumEdges " + maxNumEdges + " maxNumNodes " + numOfNodes
    );
  }

  public void addEdges(int node, int pool, int[] src, long[] metadata, int srcPos, int length) {
    int position = getNodePosition(node);

    readerAccessibleInfo.getEdges().arrayCopy(
      src,
      srcPos,
      position + POW_TABLE_30[pool],
      length,
      true /*updateStats*/
    );

    readerAccessibleInfo.getMetadata().arrayCopy(
      metadata,
      srcPos,
      position + POW_TABLE_30[pool],
      length,
      true /*updateStats*/
    );
  }
}
