package com.twitter.graphjet.bipartite.api;

public interface TimestampEdgeIterator {
  /**
   * Returns the time at which current edge was created.
   * @return the time at which current edge was created.
   */
  long getCurrentEdgeEngagementTimeInMillis();
}
