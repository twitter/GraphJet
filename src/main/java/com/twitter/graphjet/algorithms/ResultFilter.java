package com.twitter.graphjet.algorithms;

import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * Abstracts out the filtering contract for results.
 */
public abstract class ResultFilter {
  protected final Counter inputCounter;
  protected final Counter filteredCounter;

  public ResultFilter(StatsReceiver statsReceiver) {
    StatsReceiver scopedStatsReceiver = statsReceiver.scope(getStatsScope());
    this.inputCounter = scopedStatsReceiver.counter("input");
    this.filteredCounter = scopedStatsReceiver.counter("filtered");
  }

  /**
   * Resets the filter for the given request
   *
   * @param request is the incoming request
   */
  public abstract void resetFilter(RecommendationRequest request);

  /**
   * Provides an interface for clients to check whether a node should be filtered or not
   *
   * @param resultNode  is the result node to be checked
   * @param socialProofs is the socialProofs of different types associated with the node
   * @return true if the node should be discarded, and false if it should not be
   */
  public abstract boolean filterResult(
    long resultNode,
    SmallArrayBasedLongToDoubleMap[] socialProofs
  );

  /**
   * Provides methods for manipulating filtered stats;
   *
   * @return statsscope for stats tracking
   */
  public String getStatsScope() {
    return this.getClass().getSimpleName();
  }
}
