package com.twitter.graphjet.bipartite.api;

/**
 * This interface provides the engagement time of the edges in the graph,
 * i.e. when the actions represented by the edges took place
 */
public interface TimestampEdgeIterator {
  /**
   * Returns the time at which current edge was engaged with.
   * @return the time at which current edge was engaged with.
   */
  long getCurrentEdgeEngagementTimeInMillis();
}
