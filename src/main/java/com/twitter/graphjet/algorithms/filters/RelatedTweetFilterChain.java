package com.twitter.graphjet.algorithms.filters;

import java.util.List;

/**
 * Provides a simple chaining filter that takes in a list of filters, applies them one after
 * the other, and filters the result if any one filter asks to filter the result.
 */
public class RelatedTweetFilterChain {
  private final List<RelatedTweetFilter> filterSet;

  public RelatedTweetFilterChain(List<RelatedTweetFilter> filterSet) {
    this.filterSet = filterSet;
  }

  /**
   * Provides an OR of the underlying filters, returning true if any of the underlying filters would
   * return true.
   *
   * @param tweet is the node to check for filtering
   * @return true if the node should be discarded, false otherwise
   */
  public boolean filter(long tweet) {
    for (RelatedTweetFilter filter : filterSet) {
      filter.inputCounter.incr();
      if (filter.filter(tweet)) {
        filter.filteredCounter.incr();
        return true;
      }
    }
    return false;
  }

  public static final RelatedTweetFilterChain NOOPFILTERCHAIN = new RelatedTweetFilterChain(null) {
    @Override
    public boolean filter(long tweet) {
      return false;
    }
  };
}
