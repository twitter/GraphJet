package com.twitter.graphjet.algorithms.filters;

import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * This filter removes tweets that have too few engagements.
 */
public class MinEngagementFilter extends RelatedTweetFilter {

  private final int minEngagement;
  private final BipartiteGraph bipartiteGraph;


  /**
   * Constructs a MinEngagementFilter with the minimum engagement and bipartite graph
   *
   * @param minEngagement minimum engagement required in order to not be filtered out
   * @param bipartiteGraph bipartite graph
   * @param statsReceiver stats
   */
  public MinEngagementFilter(int minEngagement,
                             BipartiteGraph bipartiteGraph,
                             StatsReceiver statsReceiver) {
    super(statsReceiver);
    this.minEngagement = minEngagement;
    this.bipartiteGraph = bipartiteGraph;
  }

  /**
   * filter magic
   *
   * @param tweet is the result node to be checked
   * @return true if the node should be filtered out, and false if it should not be
   */
  @Override
  public boolean filter(long tweet) {
    return bipartiteGraph.getRightNodeDegree(tweet) < minEngagement;
  }
}
