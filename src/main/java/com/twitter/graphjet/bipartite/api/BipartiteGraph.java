package com.twitter.graphjet.bipartite.api;

/**
 * This interface should specify all the read-only operations that are needed from a Bipartite
 * graph. In particular, any recommendation algorithms should only need to use this interface.
 *
 * NOTE: the graph is assumed to have nodes that are longs -- this is a very deliberate choice to
 * avoid the need for (un)boxing at any point.
 *
 * We also note the expected runtime cost for the operations here that the clients will assume,
 * and assume that implementations respect that.
 */
public interface BipartiteGraph extends LeftIndexedBipartiteGraph, RightIndexedBipartiteGraph {
  // all operations are defined in the left/right interfaces and this interface is just a
  // convenience wrapper that is used to refer to a bi-indexed graph.
}
