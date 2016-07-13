package com.twitter.graphjet.bipartite.api;

/**
 * This interface should specify all the optimize operations that are needed from a bipartite
 * graph. The function takes an {@link OptimizableBipartiteGraphSegment}, creates a compacted
 * version of its edge indexes, and replaces its old edge indexes with the new compacted version.
 */
public interface OptimizableBipartiteGraph {
  /**
   * Replace the edge indexes of the input segment to a new compacted version.
   *
   * @param segment the segment to be optimized
   */
  void optimize(OptimizableBipartiteGraphSegment segment);
}
