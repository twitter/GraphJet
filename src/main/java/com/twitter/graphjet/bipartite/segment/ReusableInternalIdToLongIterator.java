package com.twitter.graphjet.bipartite.segment;

import com.twitter.graphjet.bipartite.api.EdgeIterator;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * Abstracts out the contract for a reusable {@link EdgeIterator} in the context of internal
 * int mappings.
 */
public interface ReusableInternalIdToLongIterator {

  /**
   * Resets the iterator state for the given node.
   *
   * @param inputIntIterator  the input {@link IntIterator} to use
   * @return the iterator itself, which additionally maps the ints back to longs
   */
  EdgeIterator resetWithIntIterator(IntIterator inputIntIterator);
}
