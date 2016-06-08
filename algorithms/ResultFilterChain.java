package com.twitter.graphjet.algorithms;

import java.util.List;

import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

/**
 * Provides a simple chaining filter that takes in a list of filters, applies them one after
 * the other, and filters the result if any one filter asks to filter the result.
 */
public class ResultFilterChain {
  private final List<ResultFilter> resultFilterSet;

  public ResultFilterChain(List<ResultFilter> resultFilterSet) {
    this.resultFilterSet = resultFilterSet;
  }

  /**
   * Resets all the underlying filters with the request.
   *
   * @param request  is the incoming request to be used for resetting
   */
  public void resetFilters(RecommendationRequest request) {
    for (ResultFilter resultFilter : resultFilterSet) {
      resultFilter.resetFilter(request);
    }
  }

  /**
   * Provides an OR of the underlying filters, returning true if any of the underlying filters would
   * return true.
   *
   * @param node  is the node to check for filtering
   * @param socialProofs is the socialProofs of different types associated with the node
   * @return true if the node should be discarded, false otherwise
   */
  public boolean filterResult(long node, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    for (ResultFilter resultFilter : resultFilterSet) {
      resultFilter.inputCounter.incr();
      if (resultFilter.filterResult(node, socialProofs)) {
        resultFilter.filteredCounter.incr();
        return true;
      }
    }
    return false;
  }
}
