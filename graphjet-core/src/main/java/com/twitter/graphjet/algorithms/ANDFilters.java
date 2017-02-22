package com.twitter.graphjet.algorithms;

import java.util.List;

import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * Applies AND operations to all the filters in this class
 */
public class ANDFilters extends ResultFilter {
  private final List<ResultFilter> resultFilterSet;

  public ANDFilters(List<ResultFilter> resultFilterSet, StatsReceiver statsReceiver) {
    super(statsReceiver);
    this.resultFilterSet = resultFilterSet;
  }

  @Override
  public void resetFilter(RecommendationRequest request) {
    for (ResultFilter filter: resultFilterSet) {
      filter.resetFilter(request);
    }
  }

  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    if (resultFilterSet.size() == 0) {
      return false;
    }
    boolean shouldFilter = true;
    for (ResultFilter filter: resultFilterSet) {
      shouldFilter &= filter.filterResult(resultNode, socialProofs);
    }
    return shouldFilter;
  }
}
