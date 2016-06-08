package com.twitter.graphjet.algorithms.filters;

import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.graphjet.bipartite.api.LeftIndexedBipartiteGraph;

/**
 * This filter removes the direct interactions of the query user based on the bipartite graph.
 */
public class RelatedTweetDirectInteractionsFilter extends RelatedTweetFilter {
  private final DirectInteractions directInteractions;

  public RelatedTweetDirectInteractionsFilter(
      LeftIndexedBipartiteGraph bipartiteGraph,
      long queryUser,
      StatsReceiver statsReceiver) {
    super(statsReceiver);
    directInteractions = new DirectInteractions(bipartiteGraph);
    directInteractions.addDirectInteractions(queryUser);
  }

  @Override
  public boolean filter(long tweet) {
    return directInteractions.isDirectInteraction(tweet);
  }
}
