package com.twitter.graphjet.bipartite.api;

public interface TimestampEdgeIterator {
  /**
   * Returns the time at which current segment was created.
   * @return the time at which current segment was created.
   */
  long getCurrentEdgeEngagementTime();
}
