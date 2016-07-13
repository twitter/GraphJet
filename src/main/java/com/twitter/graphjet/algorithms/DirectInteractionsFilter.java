package com.twitter.graphjet.algorithms;

import com.twitter.graphjet.algorithms.filters.DirectInteractions;
import com.twitter.graphjet.bipartite.api.LeftIndexedBipartiteGraph;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * This filter removes the direct interactions of the query node based on the bipartite graph.
 */
public class DirectInteractionsFilter extends ResultFilter {
  private final DirectInteractions directInteractions;

  public DirectInteractionsFilter(
      LeftIndexedBipartiteGraph bipartiteGraph,
      StatsReceiver statsReceiver) {
    super(statsReceiver);
    this.directInteractions = new DirectInteractions(bipartiteGraph);
  }

  @Override
  public String getStatsScope() {
    return this.getClass().getSimpleName();
  }

  @Override
  public void resetFilter(RecommendationRequest request) {
    directInteractions.addDirectInteractions(request.getQueryNode());
  }

  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    return directInteractions.isDirectInteraction(resultNode);
  }
}
