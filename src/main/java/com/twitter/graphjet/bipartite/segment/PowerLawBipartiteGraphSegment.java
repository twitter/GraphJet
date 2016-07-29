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


package com.twitter.graphjet.bipartite.segment;

import com.twitter.graphjet.bipartite.api.EdgeTypeMask;
import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeRandomIntIterator;
import com.twitter.graphjet.bipartite.edgepool.EdgePool;
import com.twitter.graphjet.bipartite.edgepool.OptimizedEdgeIterator;
import com.twitter.graphjet.bipartite.edgepool.OptimizedEdgePool;
import com.twitter.graphjet.bipartite.edgepool.OptimizedEdgeRandomIterator;
import com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgeIterator;
import com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool;
import com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgeRandomIterator;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * A graph segment is a bounded portion of the graph with a cap on the number of nodes and edges
 * one can store in it. This particular segment is meant to be power-law on both the left and the
 * right sides, i.e. each node on the left/right hand side is assumed to have a power law degree
 * distribution.
 *
 * This class is thread-safe as the underlying
 * {@link com.twitter.graphjet.bipartite.segment.BipartiteGraphSegment} is thread-safe and all this
 * class does is provide implementations of edge pools and iterators.
 */
public class PowerLawBipartiteGraphSegment extends BipartiteGraphSegment {

  /**
   * The constructor tries to reserve most of the memory that is needed for the graph, although
   * as edges are added in, more memory will be allocated as needed.
   *
   * @param expectedNumLeftNodes     is the expected number of left nodes that would be inserted in
   *                                 the segment
   * @param expectedMaxLeftDegree    is the maximum degree expected for any left node
   * @param expectedNumRightNodes    is the expected number of right nodes that would be inserted in
   *                                 the segment
   * @param expectedMaxRightDegree   is the maximum degree expected for any right node
   * @param rightPowerLawExponent    is the exponent of the RHS power-law graph. see
   *                                 {@link PowerLawDegreeEdgePool} for details
   * @param maxNumEdges              the max number of edges this segment is supposed to hold
   * @param edgeTypeMask             is the mask to encode edge type into integer node id
   * @param statsReceiver            tracks the internal stats
   */
  public PowerLawBipartiteGraphSegment(
      int expectedNumLeftNodes,
      int expectedMaxLeftDegree,
      double leftPowerLawExponent,
      int expectedNumRightNodes,
      int expectedMaxRightDegree,
      double rightPowerLawExponent,
      int maxNumEdges,
      EdgeTypeMask edgeTypeMask,
      StatsReceiver statsReceiver) {
    super(
        expectedNumLeftNodes,
        expectedNumRightNodes,
        maxNumEdges,
        new ReaderAccessibleInfoProvider(
            expectedNumLeftNodes,
            expectedNumRightNodes,
            new PowerLawDegreeEdgePool(
                expectedNumLeftNodes,
                expectedMaxLeftDegree,
                leftPowerLawExponent,
                statsReceiver.scope("leftNodeEdgePool")),
            new PowerLawDegreeEdgePool(
                expectedNumRightNodes,
                expectedMaxRightDegree,
                rightPowerLawExponent,
                statsReceiver.scope("rightNodeEdgePool")),
            statsReceiver),
        edgeTypeMask,
        statsReceiver.scope("PowerLaw"));
  }

  public static final class EdgeIteratorFactory {
    protected static ReusableNodeIntIterator createEdgeIterator(EdgePool edgePool) {
      if (edgePool.isOptimized()) {
        return new OptimizedEdgeIterator((OptimizedEdgePool) edgePool);
      } else {
        return new PowerLawDegreeEdgeIterator((PowerLawDegreeEdgePool) edgePool);
      }
    }

    protected static ReusableNodeRandomIntIterator createRandomEdgeIterator(EdgePool edgePool) {
      if (edgePool.isOptimized()) {
        return new OptimizedEdgeRandomIterator((OptimizedEdgePool) edgePool);
      } else {
        return new PowerLawDegreeEdgeRandomIterator((PowerLawDegreeEdgePool) edgePool);
      }
    }
  }

  @Override
  public ReusableNodeIntIterator initializeLeftNodeEdgesIntIterator() {
    return EdgeIteratorFactory.createEdgeIterator(getLeftNodeEdgePool());
  }

  @Override
  public ReusableNodeRandomIntIterator initializeLeftNodeEdgesRandomIntIterator() {
    return EdgeIteratorFactory.createRandomEdgeIterator(getLeftNodeEdgePool());
  }

  @Override
  public ReusableInternalIdToLongIterator initializeLeftInternalIdToLongIterator() {
    return new InternalIdToLongIterator(getRightNodesToIndexBiMap(), edgeTypeMask);
  }

  @Override
  public ReusableNodeIntIterator initializeRightNodeEdgesIntIterator() {
    return EdgeIteratorFactory.createEdgeIterator(getRightNodeEdgePool());
  }

  @Override
  public ReusableNodeRandomIntIterator initializeRightNodeEdgesRandomIntIterator() {
    return EdgeIteratorFactory.createRandomEdgeIterator(getRightNodeEdgePool());
  }

  @Override
  public ReusableInternalIdToLongIterator initializeRightInternalIdToLongIterator() {
    return new InternalIdToLongIterator(getLeftNodesToIndexBiMap(), edgeTypeMask);
  }
}
