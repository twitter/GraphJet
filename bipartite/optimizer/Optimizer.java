package com.twitter.graphjet.bipartite.optimizer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import scala.collection.Seq;

import com.twitter.graphjet.bipartite.api.OptimizableBipartiteGraph;
import com.twitter.graphjet.bipartite.api.OptimizableBipartiteGraphSegment;
import com.twitter.graphjet.bipartite.edgepool.EdgePool;
import com.twitter.graphjet.bipartite.edgepool.OptimizedEdgePool;
import com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool;
import com.twitter.graphjet.bipartite.edgepool.RegularDegreeEdgePool;
import com.twitter.graphjet.bipartite.segment.BipartiteGraphSegment;
import com.twitter.graphjet.bipartite.segment.LeftIndexedBipartiteGraphSegment;
import com.twitter.logging.Logger;

/**
 * Converting an active index edge pool into an optimized read-only index edge pool. Index
 * optimization occurs in the background. A new copy of the index is created without touching the
 * original version. Upon completion, the original index will be dropped and replaced with the
 * optimized version.
 */
public final class Optimizer {
  private static final Logger LOG = Logger.get("graph");
  private static final Seq<Object> EMPTY_SEQ = scala.collection.immutable.List$.MODULE$.empty();
  private static final ExecutorService OPTIMIZER_SERVICE = Executors.newCachedThreadPool();

  private static final class GraphOptimizerJob implements Runnable {
    private OptimizableBipartiteGraph graph;
    private OptimizableBipartiteGraphSegment segment;

    public GraphOptimizerJob(
      OptimizableBipartiteGraph graph,
      OptimizableBipartiteGraphSegment segment
    ) {
      this.graph = graph;
      this.segment = segment;
    }

    @Override
    public void run() {
      graph.optimize(segment);
    }
  }

  /**
   * Private constructor.
   */
  private Optimizer() { }

  /**
   * Submit a runnable job to a thread pool which converts an active index edge pool into an
   * optimized read-only index edge pool.
   *
   * @param graph is the graph which starts the optimization
   * @param segment is the segment to be optimized
   */
  public static void submitGraphOptimizerJob(
    OptimizableBipartiteGraph graph,
    OptimizableBipartiteGraphSegment segment
  ) {
    OPTIMIZER_SERVICE.submit(new GraphOptimizerJob(graph, segment));
  }

  /**
   * Converting active index edge pool into an optimized read-only index edge pool.
   *
   * @param edgePool is an active index edge pool.
   * @return an optimized read-only index edge pool.
   */
  public static EdgePool optimizePowerLawDegreeEdgePool(PowerLawDegreeEdgePool edgePool) {
    long start = System.currentTimeMillis();
    LOG.info("PowerLawDegreeEdgePool optimization starts.", EMPTY_SEQ);

    PowerLawDegreeEdgePool.ReaderAccessibleInfo readerAccessibleInfo =
      edgePool.getReaderAccessibleInfo();

    OptimizedEdgePool optimizedEdgePool = new OptimizedEdgePool(
      readerAccessibleInfo.getNodeDegrees(),
      edgePool.getCurrentNumEdgesStored(),
      edgePool.getStatsReceiver()
    );

    int[] nodeDegrees = readerAccessibleInfo.getNodeDegrees();
    RegularDegreeEdgePool[] regularDegreeEdgePools = readerAccessibleInfo.getEdgePools();

    int nodeDegreeMapSize = nodeDegrees.length;

    for (int i = 0; i < nodeDegreeMapSize; i++) {
      // i is node id
      int nodeDegree = nodeDegrees[i];
      if (nodeDegree == 0) {
        continue;
      }
      int edgePoolNumber = PowerLawDegreeEdgePool.getPoolForEdgeNumber(nodeDegree - 1);

      for (int j = 0; j <= edgePoolNumber; j++) {
        int[] shard = regularDegreeEdgePools[j].getShard(i);
        int shardOffset = regularDegreeEdgePools[j].getShardOffset(i);
        int nodeDegreeInPool = regularDegreeEdgePools[j].getNodeDegree(i);

        optimizedEdgePool.addEdges(
          i, j, shard, shardOffset, nodeDegreeInPool
        );
      }
    }

    long end = System.currentTimeMillis();

    LOG.info("PowerLawDegreeEdgePool optimization finishes in "
      + (double) (end - start) / 1000.0 + " seconds.", EMPTY_SEQ);
    return optimizedEdgePool;
  }

  /**
   * Converting active index edge pool into an optimized read-only index edge pool and updating
   * {@link LeftIndexedBipartiteGraphSegment}.
   *
   * @param leftIndexedBipartiteGraphSegment is the left indexed bipartite graph segment.
   */
  public static void optimizeLeftIndexedBipartiteGraphSegment(
    LeftIndexedBipartiteGraphSegment leftIndexedBipartiteGraphSegment
  ) {
    long start = System.currentTimeMillis();

    LOG.info("LeftIndexedBipartiteGraphSegment optimization starts. ", EMPTY_SEQ);

    EdgePool optimizedEdgePool = optimizePowerLawDegreeEdgePool(
      (PowerLawDegreeEdgePool) leftIndexedBipartiteGraphSegment
        .getLeftIndexedReaderAccessibleInfoProvider()
        .getLeftIndexedReaderAccessibleInfo()
        .getLeftNodeEdgePool()
    );

    LOG.info("LeftIndexedBipartiteGraphSegment optimization finishes ", EMPTY_SEQ);

    // Safe publication ensures that readers who reference the object will see the new edge pool
    leftIndexedBipartiteGraphSegment.getLeftIndexedReaderAccessibleInfoProvider()
      .updateReaderAccessibleInfoLeftNodeEdgePool(optimizedEdgePool);

    long end = System.currentTimeMillis();

    LOG.info("LeftIndexedBipartiteGraphSegment optimization takes "
        + (double) (end - start) / 1000.0 + " seconds.",
      EMPTY_SEQ
    );
  }

  /**
   * Converting active index edge pool into an optimized read-only index edge pool and updating
   * {@link BipartiteGraphSegment}.
   *
   * @param bipartiteGraphSegment is the bipartite graph segment indexed both ways.
   */
  public static void optimizeBipartiteGraphSegment(
    BipartiteGraphSegment bipartiteGraphSegment
  ) {
    long start = System.currentTimeMillis();

    LOG.info("BipartiteGraphSegment optimization starts.", EMPTY_SEQ);

    PowerLawDegreeEdgePool leftNodeEdgePool =
      (PowerLawDegreeEdgePool) bipartiteGraphSegment
        .getLeftIndexedReaderAccessibleInfoProvider()
        .getLeftIndexedReaderAccessibleInfo()
        .getLeftNodeEdgePool();

    EdgePool leftOptimizedEdgePool = optimizePowerLawDegreeEdgePool(leftNodeEdgePool);

    LOG.info("BipartiteGraphSegment left edge pool optimization finishes.", EMPTY_SEQ);

    PowerLawDegreeEdgePool rightNodeEdgePool =
      (PowerLawDegreeEdgePool) bipartiteGraphSegment
        .getReaderAccessibleInfoProvider()
        .getReaderAccessibleInfo()
        .getRightNodeEdgePool();

    EdgePool rightOptimizedEdgePool = optimizePowerLawDegreeEdgePool(rightNodeEdgePool);

    LOG.info("BipartiteGraphSegment right edge pool optimization finishes.", EMPTY_SEQ);

    // Safe publication ensures that readers who reference the object will see the new edge pool
    bipartiteGraphSegment.getReaderAccessibleInfoProvider()
      .updateReaderAccessibleInfoEdgePool(leftOptimizedEdgePool, rightOptimizedEdgePool);

    long end = System.currentTimeMillis();

    LOG.info("BipartiteGraphSegment left + right edge pool optimization takes "
        + (double) (end - start) / 1000.0 + " seconds.",
      EMPTY_SEQ
    );
  }

}
