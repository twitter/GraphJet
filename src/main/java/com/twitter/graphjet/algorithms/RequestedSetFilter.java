package com.twitter.graphjet.algorithms;

import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This filter applies a simple set-based filtering: given a set, filter the result if it's in the
 * set.
 */
public class RequestedSetFilter extends ResultFilter {
  private LongSet filterSet;

  public RequestedSetFilter(StatsReceiver statsReceiver) {
    super(statsReceiver);
  }

  @Override
  public String getStatsScope() {
    return this.getClass().getSimpleName();
  }

  @Override
  public void resetFilter(RecommendationRequest request) {
    filterSet = request.getToBeFiltered();
  }

  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    return filterSet != null && filterSet.contains(TweetIDMask.restore(resultNode));
  }
}
