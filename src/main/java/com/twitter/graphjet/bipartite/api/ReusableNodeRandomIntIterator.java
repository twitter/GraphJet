package com.twitter.graphjet.bipartite.api;

import java.util.Random;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * Abstracts out the notion of a reusable {@link IntIterator} in the context of providing random
 * access for edges of nodes that are ints.
 */
public interface ReusableNodeRandomIntIterator {

  /**
   * Resets the iterator state for the given node.
   *
   * @param node              is the node that this iterator now points to
   * @param numSamplesToGet   is the number of samples to retrieve
   * @param randomGen         is the random number generator to be used
   * @return the iterator itself
   */
  IntIterator resetForNode(int node, int numSamplesToGet, Random randomGen);
}
