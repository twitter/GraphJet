package com.twitter.graphjet.bipartite.api;

import it.unimi.dsi.fastutil.ints.IntIterator;

/** A type-specific Iterator; provides an additional method to avoid (un)boxing, and
 * the possibility to skip elements.
 */
public interface NodeMetadataEdgeIterator extends EdgeIterator {
  /**
   * Returns the metadata of current left node.
   *
   * @return the metadata of current left node.
   */
  IntIterator getLeftNodeMetadata(byte nodeMetadataType);

  /**
   * Returns the metadata of current right node.
   *
   * @return the metadata of current right node.
   */
  IntIterator getRightNodeMetadata(byte nodeMetadataType);
}

