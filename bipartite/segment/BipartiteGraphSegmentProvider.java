package com.twitter.graphjet.bipartite.segment;

import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.graphjet.bipartite.api.EdgeTypeMask;

/**
 * Abstracts the notion of generating left-indexed bipartite graph segments.
 */
public abstract class BipartiteGraphSegmentProvider<T extends LeftIndexedBipartiteGraphSegment> {
  protected final EdgeTypeMask edgeTypeMask;
  protected final StatsReceiver statsReceiver;

  /**
   * Stores the statsReceiver that future segments would use.
   *
   * @param statsReceiver  tracks the internal stats
   */
  public BipartiteGraphSegmentProvider(EdgeTypeMask edgeTypeMask, StatsReceiver statsReceiver) {
    this(edgeTypeMask, statsReceiver, "BipartiteGraphSegment");
  }

  /**
   * Stores the statsReceiver that future segments would use.
   *
   * @param statsReceiver  tracks the internal stats
   * @param scopeString    sets the scope of the statsReceiver
   */
  public BipartiteGraphSegmentProvider(
      EdgeTypeMask edgeTypeMask,
      StatsReceiver statsReceiver,
      String scopeString) {
    this.edgeTypeMask = edgeTypeMask;
    this.statsReceiver = statsReceiver.scope(scopeString);
  }

  /**
   * Generate a new {@link T}. This is guaranteed to be thread-safe.
   *
   * @return the new segment
   */
  public abstract T generateNewSegment(int segmentId, int maxNumEdges);
}
