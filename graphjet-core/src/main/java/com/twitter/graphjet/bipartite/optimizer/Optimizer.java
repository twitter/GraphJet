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


package com.twitter.graphjet.bipartite.optimizer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twitter.graphjet.bipartite.api.OptimizableBipartiteGraph;
import com.twitter.graphjet.bipartite.api.OptimizableBipartiteGraphSegment;
import com.twitter.graphjet.bipartite.edgepool.AbstractOptimizedEdgePool;
import com.twitter.graphjet.bipartite.edgepool.AbstractPowerLawDegreeEdgePool;
import com.twitter.graphjet.bipartite.edgepool.AbstractRegularDegreeEdgePool;
import com.twitter.graphjet.bipartite.edgepool.EdgePool;
import com.twitter.graphjet.bipartite.edgepool.OptimizedEdgePool;
import com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool;
import com.twitter.graphjet.bipartite.edgepool.WithEdgeMetadataOptimizedEdgePool;
import com.twitter.graphjet.bipartite.segment.BipartiteGraphSegment;
import com.twitter.graphjet.bipartite.segment.LeftIndexedBipartiteGraphSegment;

/**
 * Converting an active index edge pool into an optimized read-only index edge pool. Index
 * optimization occurs in the background. A new copy of the index is created without touching the
 * original version. Upon completion, the original index will be dropped and replaced with the
 * optimized version.
 */
public final class Optimizer {
  private static final Logger LOG = LoggerFactory.getLogger("graph");
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
  public static EdgePool optimizePowerLawDegreeEdgePool(AbstractPowerLawDegreeEdgePool edgePool) {
    long start = System.currentTimeMillis();
    LOG.info("PowerLawDegreeEdgePool optimization starts.");

    PowerLawDegreeEdgePool.ReaderAccessibleInfo readerAccessibleInfo =
      edgePool.getReaderAccessibleInfo();

    AbstractOptimizedEdgePool optimizedEdgePool;

    if (!edgePool.hasEdgeMetadata()) {
      optimizedEdgePool = new OptimizedEdgePool(
        readerAccessibleInfo.getNodeDegrees(),
        edgePool.getCurrentNumEdgesStored(),
        edgePool.getStatsReceiver()
      );
    } else {
      optimizedEdgePool = new WithEdgeMetadataOptimizedEdgePool(
        readerAccessibleInfo.getNodeDegrees(),
        edgePool.getCurrentNumEdgesStored(),
        edgePool.getStatsReceiver()
      );
    }

    int[] nodeDegrees = readerAccessibleInfo.getNodeDegrees();
    AbstractRegularDegreeEdgePool[] regularDegreeEdgePools = readerAccessibleInfo.getEdgePools();

    int nodeDegreeMapSize = nodeDegrees.length;

    for (int i = 0; i < nodeDegreeMapSize; i++) {
      // i is node id
      int nodeDegree = nodeDegrees[i];
      if (nodeDegree == 0) {
        continue;
      }
      int edgePoolNumber = AbstractPowerLawDegreeEdgePool.getPoolForEdgeNumber(nodeDegree - 1);

      for (int j = 0; j <= edgePoolNumber; j++) {
        int[] shard = regularDegreeEdgePools[j].getShard(i);
        long[] metadataShard = regularDegreeEdgePools[j].getMetadataShard(i);
        int shardOffset = regularDegreeEdgePools[j].getShardOffset(i);
        int nodeDegreeInPool = regularDegreeEdgePools[j].getNodeDegree(i);

        optimizedEdgePool.addEdges(
          i, j, shard, metadataShard, shardOffset, nodeDegreeInPool
        );
      }
    }

    long end = System.currentTimeMillis();

    LOG.info("PowerLawDegreeEdgePool optimization finishes in "
      + (double) (end - start) / 1000.0 + " seconds.");
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

    LOG.info("LeftIndexedBipartiteGraphSegment optimization starts. ");

    EdgePool optimizedEdgePool = optimizePowerLawDegreeEdgePool(
      (AbstractPowerLawDegreeEdgePool) leftIndexedBipartiteGraphSegment
        .getLeftIndexedReaderAccessibleInfoProvider()
        .getLeftIndexedReaderAccessibleInfo()
        .getLeftNodeEdgePool()
    );

    LOG.info("LeftIndexedBipartiteGraphSegment optimization finishes ");

    // Safe publication ensures that readers who reference the object will see the new edge pool
    leftIndexedBipartiteGraphSegment.getLeftIndexedReaderAccessibleInfoProvider()
      .updateReaderAccessibleInfoLeftNodeEdgePool(optimizedEdgePool);

    long end = System.currentTimeMillis();

    LOG.info("LeftIndexedBipartiteGraphSegment optimization takes "
        + (double) (end - start) / 1000.0 + " seconds."
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

    LOG.info("BipartiteGraphSegment optimization starts.");

    AbstractPowerLawDegreeEdgePool leftNodeEdgePool =
      (AbstractPowerLawDegreeEdgePool) bipartiteGraphSegment
        .getLeftIndexedReaderAccessibleInfoProvider()
        .getLeftIndexedReaderAccessibleInfo()
        .getLeftNodeEdgePool();

    EdgePool leftOptimizedEdgePool = optimizePowerLawDegreeEdgePool(leftNodeEdgePool);

    LOG.info("BipartiteGraphSegment left edge pool optimization finishes.");

    AbstractPowerLawDegreeEdgePool rightNodeEdgePool =
      (AbstractPowerLawDegreeEdgePool) bipartiteGraphSegment
        .getReaderAccessibleInfoProvider()
        .getReaderAccessibleInfo()
        .getRightNodeEdgePool();

    EdgePool rightOptimizedEdgePool = optimizePowerLawDegreeEdgePool(rightNodeEdgePool);

    LOG.info("BipartiteGraphSegment right edge pool optimization finishes.");

    // Safe publication ensures that readers who reference the object will see the new edge pool
    bipartiteGraphSegment.getReaderAccessibleInfoProvider()
      .updateReaderAccessibleInfoEdgePool(leftOptimizedEdgePool, rightOptimizedEdgePool);

    long end = System.currentTimeMillis();

    LOG.info("BipartiteGraphSegment left + right edge pool optimization takes "
        + (double) (end - start) / 1000.0 + " seconds."
    );
  }

}
