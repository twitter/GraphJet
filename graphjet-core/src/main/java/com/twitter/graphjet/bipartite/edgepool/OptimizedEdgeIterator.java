/**
 * Copyright 2016 Twitter. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.twitter.graphjet.bipartite.edgepool;

import com.twitter.graphjet.bipartite.api.WithEdgeMetadataIntIterator;
import com.twitter.graphjet.bipartite.api.ReadOnlyIntIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;

/**
 * Returns an iterator over the edges stored in an {@link AbstractOptimizedEdgePool}. The iterator
 * is meant to be reusable via the resetForIndex method.
 */
public class OptimizedEdgeIterator extends ReadOnlyIntIterator
  implements WithEdgeMetadataIntIterator, ReusableNodeIntIterator {
  protected final AbstractOptimizedEdgePool optimizedDegreeEdgePool;
  protected int position;
  protected int degree;
  protected int currentEdge;

  /**
   * Creates an iterator that can be reused. Note that the client needs to call the resetForNode
   * method before using the iterator.
   *
   * @param optimizedDegreeEdgePool is the underlying {@link AbstractOptimizedEdgePool}
   */
  public OptimizedEdgeIterator(AbstractOptimizedEdgePool optimizedDegreeEdgePool) {
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
  public long currentMetadata() {
    return optimizedDegreeEdgePool.getEdgeMetadata(position, currentEdge - 1);
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
