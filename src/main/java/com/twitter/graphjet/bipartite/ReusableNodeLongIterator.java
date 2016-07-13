package com.twitter.graphjet.bipartite;

import com.twitter.graphjet.bipartite.api.EdgeIterator;

/**
 * Abstracts out the notion of a reusable {@link EdgeIterator} in the context of providing iterative
 * access over edges of nodes that are longs.
 */
public interface ReusableNodeLongIterator {

  /**
   * Resets the iterator state for the given node.
   *
   * @param node  the node that this iterator now points to
   * @return the iterator
   */
  EdgeIterator resetForNode(long node);
}
