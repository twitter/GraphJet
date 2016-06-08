package com.twitter.graphjet.algorithms.filters;

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.api.LeftIndexedBipartiteGraph;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This is a utility class holding direct interactions of a left node
 */
public class DirectInteractions {
  private final LeftIndexedBipartiteGraph bipartiteGraph;
  private final LongSet leftNodeEdgeSet;

  public DirectInteractions(LeftIndexedBipartiteGraph bipartiteGraph) {
    this.bipartiteGraph = bipartiteGraph;
    this.leftNodeEdgeSet = new LongOpenHashSet();
  }

  /**
   * populate direct interactions
   *
   * @param queryNode is the left query node
   */
  public void addDirectInteractions(long queryNode) {
    EdgeIterator iterator = bipartiteGraph.getLeftNodeEdges(queryNode);
    if (iterator == null) {
      return;
    }
    leftNodeEdgeSet.clear();
    while (iterator.hasNext()) {
      leftNodeEdgeSet.add(iterator.nextLong());
    }
  }

  /**
   * filter magic
   *
   * @param resultNode is the result node to be checked
   * @return true if the node should be filtered out, and false if it should not be
   */
  public boolean isDirectInteraction(long resultNode) {
    return !leftNodeEdgeSet.isEmpty() && leftNodeEdgeSet.contains(resultNode);
  }
}
