package com.twitter.graphjet.bipartite;

import java.util.Random;

import com.twitter.graphjet.bipartite.api.EdgeIterator;

/**
 * Abstracts out the notion of a reusable {@link EdgeIterator} in the context of providing random
 * access for edges of nodes that are longs.
 */
public interface ReusableNodeRandomLongIterator {

  /**
   * Resets the iterator state for the given node.
   *
   * @param node              is the node that this iterator now points to
   * @param numSamplesToGet   is the number of samples to retrieve
   * @param randomGen         is the randomGen number generator to be used
   * @return the iterator
   */
  EdgeIterator resetForNode(long node, int numSamplesToGet, Random randomGen);
}
