package com.twitter.graphjet.bipartite.segment;

import com.twitter.graphjet.bipartite.api.EdgeTypeMask;

/**
 * This edge type mask returns the original node id, which means the edge in graph is untyped.
 */
public class IdentityEdgeTypeMask implements EdgeTypeMask {
  public IdentityEdgeTypeMask() {
  }

  @Override
  public int encode(int node, byte edgeType) {
    return node;
  }

  @Override
  public byte edgeType(int node) {
    return 0;
  }

  @Override
  public int restore(int node) {
    return node;
  }
}
