package com.twitter.graphjet.bipartite.api;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * Abstracts out the notion of a reusable {@link IntIterator} in the context of providing iterative
 * access over edge metadata of edges that are ints.
 */
public interface ReusableEdgeIntIterator {
  /**
   * Resets the iterator state for the given node.
   *
   * @param node  the node that this iterator now points to
   * @return the iterator
   */
  IntIterator resetForNode(int node);
}
