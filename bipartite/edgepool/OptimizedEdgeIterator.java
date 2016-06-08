package com.twitter.graphjet.bipartite.edgepool;

import com.twitter.graphjet.bipartite.api.ReadOnlyIntIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;

/**
 * Returns an iterator over the edges stored in an {@link OptimizedEdgePool}. The iterator is
 * meant to be reusable via the resetForIndex method.
 */
public class OptimizedEdgeIterator extends ReadOnlyIntIterator
    implements ReusableNodeIntIterator {
  protected final OptimizedEdgePool optimizedDegreeEdgePool;
  protected int position;
  protected int degree;
  protected int currentEdge;

  /**
   * Creates an iterator that can be reused. Note that the client needs to call the resetForNode
   * method before using the iterator.
   *
   * @param optimizedDegreeEdgePool is the underlying {@link OptimizedEdgePool}
   */
  public OptimizedEdgeIterator(OptimizedEdgePool optimizedDegreeEdgePool) {
    this.optimizedDegreeEdgePool = optimizedDegreeEdgePool;
  }

  /**
   * Resets the iterator to return edges of a node with this information. Note that calling this
   * method resets the position of the iterator to the first edge of the node.
   *
   * @param node is the node that this iterator resets to
   * @return the iterator itself for ease of use
   */
  @Override
  public OptimizedEdgeIterator resetForNode(int node) {
    this.position = optimizedDegreeEdgePool.getNodePosition(node);
    this.degree = optimizedDegreeEdgePool.getNodeDegree(node);
    currentEdge = 0;
    return this;
  }

  @Override
  public int nextInt() {
    return optimizedDegreeEdgePool.getNodeEdge(position, currentEdge++);
  }

  @Override
  public int skip(int i) {
    return 0;
  }

  @Override
  public boolean hasNext() {
    return currentEdge < degree;
  }

  @Override
  public Integer next() {
    return nextInt();
  }
}
