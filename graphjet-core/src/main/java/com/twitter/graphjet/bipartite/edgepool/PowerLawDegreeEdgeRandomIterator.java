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
 * {@link PowerLawDegreeEdgePool}. The iterator is meant to be reusable via the resetForNode method.
 *
 * The implementation is based on a two level sample as the edges are distributed across a number
 * of pools so we first pick a random pool and then pick a random edge from that pool. The current
 * implementation is O(1) as it computes a integer logarithm that is 8 CPU operations.
 */
public class PowerLawDegreeEdgeRandomIterator extends PowerLawDegreeEdgeIterator
                                              implements ReusableNodeRandomIntIterator {
  private int numSamples;
  private Random random;
  private int currentPoolNumber;
  private int currentPoolIndex;

  /**
   * Creates an iterator that can be reused. Note that the client needs to call the resetForNode
   * method before using the iterator.
   *
   * @param powerLawDegreeEdgePool  is the underlying {@link AbstractPowerLawDegreeEdgePool}
   */
  public PowerLawDegreeEdgeRandomIterator(AbstractPowerLawDegreeEdgePool powerLawDegreeEdgePool) {
    super(powerLawDegreeEdgePool);
  }

  /**
   * Resets the internal state of the iterator for the given node.
   *
   * @param inputNode             is the node whose edges are returned by the iterator
   * @param numSamplesToGet  is the number of samples to return
   * @param randomGen        is the random number generator to use for all random choices
   * @return the iterator itself for ease of use
   */
  @Override
  public PowerLawDegreeEdgeRandomIterator resetForNode(
      int inputNode, int numSamplesToGet, Random randomGen) {
    super.resetForNode(inputNode);
    this.numSamples = numSamplesToGet;
    this.random = randomGen;
    return this;
  }

  @Override
  public int nextInt() {
    int randomEdgeNumber = random.nextInt(nodeDegree);
    currentPoolNumber = PowerLawDegreeEdgePool.getPoolForEdgeNumber(randomEdgeNumber);
    currentPoolIndex = PowerLawDegreeEdgePool.getEdgeNumberInPool(
      currentPoolNumber, randomEdgeNumber
    );

    currentEdge++;
    return powerLawDegreeEdgePool.getNumberedEdgeInRegularPool(
      node, currentPoolNumber, currentPoolIndex
    );
  }

  @Override
  public long currentMetadata() {
    return powerLawDegreeEdgePool.getNumberedEdgeMetadataInRegularPool(
      node, currentPoolNumber, currentPoolIndex
    );
  }

  @Override
  public boolean hasNext() {
    return currentEdge < numSamples;
  }
}
