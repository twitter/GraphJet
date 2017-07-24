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

import java.util.Random;

import com.twitter.graphjet.bipartite.api.ReusableNodeRandomIntIterator;

/**
 * Returns an iterator that can generate of random sample of the edges stored in a
 * {@link RegularDegreeEdgePool}. The iterator is meant to be reusable via the resetForIndex method.
 *
 * The implementation is pretty simple as the edges are stored in a contiguous array so random
 * sampling is the same as picking a random index.
 */
public class RegularDegreeEdgeRandomIterator extends RegularDegreeEdgeIterator
                                             implements ReusableNodeRandomIntIterator {
  private int numSamples;
  private Random random;

  /**
   * Creates an iterator that can be reused. Note that the client needs to call the resetForNode
   * method before using the iterator.
   *
   * @param regularDegreeEdgePool  is the underlying {@link RegularDegreeEdgePool}
   */
  public RegularDegreeEdgeRandomIterator(AbstractRegularDegreeEdgePool regularDegreeEdgePool) {
    super(regularDegreeEdgePool);
  }

  /**
   * Resets the iterator to return edges of a node with this information. Note that calling this
   * method resets the position of the iterator to the first edge of the node.
   *
   * @param node              is the node that this iterator resets to
   * @param numSamplesToGet   is the number of samples to retrieve
   * @param randomGen         is the random number generator to be used
   * @return the iterator itself for ease of use
   */
  @Override
  public RegularDegreeEdgeRandomIterator resetForNode(
      int node, int numSamplesToGet, Random randomGen) {
    super.resetForNode(node);
    this.numSamples = numSamplesToGet;
    this.random = randomGen;
    return this;
  }

  @Override
  public int nextInt() {
    currentEdge++;
    return regularDegreeEdgePool.getNumberedEdge(position, random.nextInt(degree));
  }

  @Override
  public boolean hasNext() {
    return currentEdge < numSamples;
  }
}
