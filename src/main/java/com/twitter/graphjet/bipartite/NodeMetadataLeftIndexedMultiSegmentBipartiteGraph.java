package com.twitter.graphjet.bipartite;

import com.twitter.graphjet.bipartite.api.LeftIndexedBipartiteGraph;
import com.twitter.graphjet.bipartite.api.NodeMetadataDynamicBipartiteGraph;
import com.twitter.graphjet.bipartite.api.OptimizableBipartiteGraphSegment;
import com.twitter.graphjet.bipartite.optimizer.Optimizer;
import com.twitter.graphjet.bipartite.segment.BipartiteGraphSegmentProvider;
import com.twitter.graphjet.bipartite.segment.NodeMetadataLeftIndexedBipartiteGraphSegment;
import com.twitter.graphjet.stats.StatsReceiver;

public abstract class NodeMetadataLeftIndexedMultiSegmentBipartiteGraph
  extends LeftIndexedMultiSegmentBipartiteGraph<NodeMetadataLeftIndexedBipartiteGraphSegment>
  implements LeftIndexedBipartiteGraph,
  NodeMetadataDynamicBipartiteGraph,
  ReusableLeftIndexedBipartiteGraph {
  /**
   * This starts the graph off with a single segment, and additional ones are allocated as needed.
   *
   * @param maxNumSegments                            is the maximum number of segments we'll add to
   *                                                  the graph. At that point, the oldest segments
   *                                                  will start getting dropped
   * @param maxNumEdgesPerSegment                     determines when the implementation decides to
   *                                                  fork off a new segment
   * @param bipartiteGraphSegmentProvider             is used to generate new segments that are
   *                                                  added to the graph
   * @param multiSegmentReaderAccessibleInfoProvider  is use to generate the
   *                                                  {@link MultiSegmentReaderAccessibleInfo}
   *                                                  object which contains all the information
   *                                                  needed by the readers
   * @param statsReceiver                             tracks the internal stats
   */
  public NodeMetadataLeftIndexedMultiSegmentBipartiteGraph(
    int maxNumSegments,
    int maxNumEdgesPerSegment,
    BipartiteGraphSegmentProvider<NodeMetadataLeftIndexedBipartiteGraphSegment>
      bipartiteGraphSegmentProvider,
    MultiSegmentReaderAccessibleInfoProvider<NodeMetadataLeftIndexedBipartiteGraphSegment>
      multiSegmentReaderAccessibleInfoProvider,
    StatsReceiver statsReceiver) {
    super(
      maxNumSegments,
      maxNumEdgesPerSegment,
      bipartiteGraphSegmentProvider,
      multiSegmentReaderAccessibleInfoProvider,
      statsReceiver.scope("NodeMetadataLeftIndexedMultiSegmentBipartiteGraph"));
  }

  @Override
  public void addEdge(
    long leftNode,
    long rightNode,
    byte edgeType,
    int[][] leftNodeMetadata,
    int[][] rightNodeMetadata
  ) {
    // usually very cheap check is it's only false very rarely
    if (numEdgesInLiveSegment == maxNumEdgesPerSegment) {
      addNewSegment();
    }
    getLiveSegment().addEdge(leftNode, rightNode, edgeType, leftNodeMetadata, rightNodeMetadata);
    numEdgesInLiveSegment++;

    numEdgesSeenInAllHistoryCounter.incr();
  }

  @Override
  public void optimize(OptimizableBipartiteGraphSegment segment) {
    Optimizer.optimizeLeftIndexedBipartiteGraphSegment(
      (NodeMetadataLeftIndexedBipartiteGraphSegment) segment);
  }
}
