package com.twitter.graphjet.bipartite.api;

/**
 * The bit mask is used to encode edge types in the top bits of an integer.
 */
public interface EdgeTypeMask {
  /**
   * Encode the edge type into the top bits of the integer node id.
   *
   * @param node the original node id
   * @param edgeType edge type
   * @return the node id with bitmask
   */
  int encode(int node, byte edgeType);

  /**
   * Retrieve the edge type from the integer node id.
   *
   * @param node the node id with bitmask
   * @return edge type
   */
  byte edgeType(int node);

  /**
   * Restore the original node id by removing the meta data saved in top bits.
   *
   * @param node the node id with bitmask
   * @return node id without the bitmask
   */
  int restore(int node);
}
