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
 * Returns an iterator over the edges stored in a {@link PowerLawDegreeEdgePool}. The iterator is
 * meant to be reusable via the resetForNode method.
 */
public class PowerLawDegreeEdgeIterator extends ReadOnlyIntIterator
                                        implements WithEdgeMetadataIntIterator, ReusableNodeIntIterator {
  protected final AbstractPowerLawDegreeEdgePool powerLawDegreeEdgePool;
  protected RegularDegreeEdgeIterator[] regularDegreeEdgeIterators;
  protected int node;
  protected int nodeDegree;
  protected int currentEdge;
  protected int currentPool;
  protected int currentPoolSize;
  protected int edgeInCurrentPool;
  protected RegularDegreeEdgeIterator currentRegularDegreeEdgeIterator;

  /**
   * Creates an iterator that can be reused. Note that the client needs to call the resetForNode
   * method before using the iterator.
   *
   * @param powerLawDegreeEdgePool  is the underlying {@link AbstractPowerLawDegreeEdgePool}
   */
  public PowerLawDegreeEdgeIterator(AbstractPowerLawDegreeEdgePool powerLawDegreeEdgePool) {
    this.powerLawDegreeEdgePool = powerLawDegreeEdgePool;
    int numPools = powerLawDegreeEdgePool.getNumPools();
    this.regularDegreeEdgeIterators = new RegularDegreeEdgeIterator[numPools];
    for (int i = 0; i < numPools; i++) {
      regularDegreeEdgeIterators[i] =
          new RegularDegreeEdgeIterator(powerLawDegreeEdgePool.getRegularDegreeEdgePool(i));
    }
  }

  /**
   * Resets the internal state of the iterator for the given node.
   *
   * @param inputNode  is the node whose edges are returned by the iterator
   * @return the iterator itself for ease of use
   */
  @Override
  public PowerLawDegreeEdgeIterator resetForNode(int inputNode) {
    if (regularDegreeEdgeIterators.length < powerLawDegreeEdgePool.getNumPools()) {
      reinitializeRegularDegreeEdgeIterators(powerLawDegreeEdgePool.getNumPools());
    }
    node = inputNode;
    nodeDegree = powerLawDegreeEdgePool.getNodeDegree(inputNode);
    currentEdge = 0;
    currentPool = 0;
    currentRegularDegreeEdgeIterator = regularDegreeEdgeIterators[currentPool];
    currentRegularDegreeEdgeIterator.resetForNode(node);
    currentPoolSize = powerLawDegreeEdgePool.getDegreeForPool(currentPool);
    edgeInCurrentPool = 0;
    return this;
  }

  private void reinitializeRegularDegreeEdgeIterators(int newNumPools) {
    RegularDegreeEdgeIterator[] newRegularDegreeEdgeIterators =
        new RegularDegreeEdgeIterator[newNumPools];
    System.arraycopy(regularDegreeEdgeIterators, 0,
                     newRegularDegreeEdgeIterators, 0, regularDegreeEdgeIterators.length);
    int numPools = regularDegreeEdgeIterators.length;
    for (int i = numPools; i < newNumPools; i++) {
      newRegularDegreeEdgeIterators[i] =
          new RegularDegreeEdgeIterator(powerLawDegreeEdgePool.getRegularDegreeEdgePool(i));
    }
    regularDegreeEdgeIterators = newRegularDegreeEdgeIterators;
  }

  @Override
  public int nextInt() {
    currentEdge++;
    edgeInCurrentPool++;
    return currentRegularDegreeEdgeIterator.nextInt();
  }

  @Override
  public long currentMetadata() {
    return currentRegularDegreeEdgeIterator.currentMetadata();
  }

  @Override
  public int skip(int i) {
    return 0;
  }

  @Override
  public boolean hasNext() {
    if (currentEdge == nodeDegree) {
      return false;
    } else if (edgeInCurrentPool == currentPoolSize) {
      currentPool++;
      edgeInCurrentPool = 0;
      currentPoolSize = powerLawDegreeEdgePool.getDegreeForPool(currentPool);
      currentRegularDegreeEdgeIterator = regularDegreeEdgeIterators[currentPool];
      currentRegularDegreeEdgeIterator.resetForNode(node);
    }
    return true;
  }

  @Override
  public Integer next() {
    return nextInt();
  }
}
