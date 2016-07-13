package com.twitter.graphjet.algorithms.filters;

import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * Abstracts out the filtering contract for related tweets.
 */
public abstract class RelatedTweetFilter {
  protected final Counter inputCounter;
  protected final Counter filteredCounter;

  public RelatedTweetFilter(StatsReceiver statsReceiver) {
    StatsReceiver scopedStatsReceiver = statsReceiver.scope(getStatsScope());
    this.inputCounter = scopedStatsReceiver.counter("input");
    this.filteredCounter = scopedStatsReceiver.counter("filtered");
  }

  /**
   * Provides an interface for clients to check whether a tweet should be filtered or not
   *
   * @param tweet is the result node to be checked
   * @return true if the node should be discarded, and false if it should not be
   */
  public abstract boolean filter(long tweet);

  /**
   * Provides methods for manipulating filtered stats;
   *
   * @return statsscope for stats tracking
   */
  public String getStatsScope() {
    return this.getClass().getSimpleName();
  }
}
