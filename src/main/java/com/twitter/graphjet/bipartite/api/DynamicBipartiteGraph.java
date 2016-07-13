package com.twitter.graphjet.bipartite.api;

/**
 * This interface should specify all the write operations that are needed from a dynamically
 * updating Bipartite graph. In particular, this interface is all that's needed from an
 * implementation.
 *
 * NOTE: the graph is assumed to have nodes that are longs -- this is a very deliberate choice to
 * avoid the need for (un)boxing at any point.
 *
 * We also note the expected runtime cost for the operations here that the clients will assume,
 * and assume that implementations respect that.
 */
public interface DynamicBipartiteGraph {

  /**
   * Adding an edge is assumed to be an O(1) operation.
   *
   * @param leftNode   is the left hand side node in the bipartite graph
   * @param rightNode  is the right hand side node in the bipartite graph
   * @param edgeType   is the edge type relationship between leftNode and rightNode
   */
  void addEdge(long leftNode, long rightNode, byte edgeType);

  /**
   * Removing an edge is assumed to be an O(1) operation.
   *
   * @param leftNode   is the left hand side node in the bipartite graph
   * @param rightNode  is the right hand side node in the bipartite graph
   */
  void removeEdge(long leftNode, long rightNode);
}
