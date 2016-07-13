package com.twitter.graphjet.algorithms;

import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * Filters a right hand side node if it has less than a minimum number of interactions.
 */
public class MinNumInteractionsFilter extends ResultFilter {
  private final BipartiteGraph bipartiteGraph;
  private final int minNumInteractions;

  public MinNumInteractionsFilter(BipartiteGraph bipartiteGraph,
                                  int minNumInteractions,
                                  StatsReceiver statsReceiver) {
    super(statsReceiver);
    this.bipartiteGraph = bipartiteGraph;
    this.minNumInteractions = minNumInteractions;
  }

  @Override
  public String getStatsScope() {
    return this.getClass().getSimpleName();
  }

  @Override
  public void resetFilter(RecommendationRequest request) {
  }

  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    return bipartiteGraph.getRightNodeDegree(resultNode) < minNumInteractions;
  }
}
