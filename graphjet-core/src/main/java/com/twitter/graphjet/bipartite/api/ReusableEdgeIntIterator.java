package com.twitter.graphjet.bipartite.api;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * Abstracts out the notion of a reusable {@link IntIterator} in the context of providing iterative
 * access over ints edge metadata of the input edge.
 */
public interface ReusableEdgeIntIterator {
  /**
   * Resets the iterator state for the given edge.
   *
   * @param edge  the edge that this iterator now points to
   * @return the iterator
   */
  IntIterator resetForEdge(int edge);
}
