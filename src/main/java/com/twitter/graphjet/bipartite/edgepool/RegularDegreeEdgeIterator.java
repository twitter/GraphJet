package com.twitter.graphjet.bipartite.edgepool;

import com.twitter.graphjet.bipartite.api.ReadOnlyIntIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;

/**
 * Returns an iterator over the edges stored in a {@link RegularDegreeEdgePool}. The iterator is
 * meant to be reusable via the resetForIndex method.
 */
public class RegularDegreeEdgeIterator extends ReadOnlyIntIterator
                                       implements ReusableNodeIntIterator {
  protected final RegularDegreeEdgePool regularDegreeEdgePool;
  protected int position;
  protected int degree;
  protected int currentEdge;

  /**
   * Creates an iterator that can be reused. Note that the client needs to call the resetForNode
   * method before using the iterator.
   *
   * @param regularDegreeEdgePool  is the underlying {@link RegularDegreeEdgePool}
   */
  public RegularDegreeEdgeIterator(RegularDegreeEdgePool regularDegreeEdgePool) {
    this.regularDegreeEdgePool = regularDegreeEdgePool;
  }

  /**
   * Resets the iterator to return edges of a node with this information. Note that calling this
   * method resets the position of the iterator to the first edge of the node.
   *
   * @param node   is the node that this iterator resets to
   * @return the iterator itself for ease of use
   */
  @Override
  public RegularDegreeEdgeIterator resetForNode(int node) {
    long nodeInfo = regularDegreeEdgePool.getNodeInfo(node);
    if (nodeInfo == -1L) {
      this.degree = 0;
    } else {
      this.position = RegularDegreeEdgePool.getNodePositionFromNodeInfo(nodeInfo);
      this.degree = RegularDegreeEdgePool.getNodeDegreeFromNodeInfo(nodeInfo);
    }
    currentEdge = 0;
    return this;
  }

  @Override
  public int nextInt() {
    return regularDegreeEdgePool.getNumberedEdge(position, currentEdge++);
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
