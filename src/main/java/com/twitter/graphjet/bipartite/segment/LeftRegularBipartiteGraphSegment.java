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
import com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgeIterator;
import com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool;
import com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgeRandomIterator;
import com.twitter.graphjet.bipartite.edgepool.RegularDegreeEdgeIterator;
import com.twitter.graphjet.bipartite.edgepool.RegularDegreeEdgePool;
import com.twitter.graphjet.bipartite.edgepool.RegularDegreeEdgeRandomIterator;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * A graph segment is a bounded portion of the graph with a cap on the number of nodes and edges
 * one can store in it. This particular segment is meant to be left-regular with the "regular"
 * constraint really being a max constraint, i.e. each node on the left hand side has a maxDegree
 * that is not too large. The right hand side is assumed to have a power law degree distribution.
 *
 * This class is thread-safe as the underlying
 * {@link com.twitter.graphjet.bipartite.segment.BipartiteGraphSegment} is thread-safe and all this
 * class does is provide implementations of edge pools and iterators.
 */
public class LeftRegularBipartiteGraphSegment extends BipartiteGraphSegment {
  /**
   * The constructor tries to reserve most of the memory that is needed for the graph, although
   * as edges are added in, more memory will be allocated as needed.
   *
   * @param expectedNumLeftNodes     is the expected number of left nodes that would be inserted in
   *                                 the segment
   * @param leftDegree               is the maximum degree allowed for any left node
   * @param expectedNumRightNodes    is the expected number of right nodes that would be inserted in
   *                                 the segment
   * @param expectedMaxRightDegree   is the maximum degree allowed for any right node
   * @param rightPowerLawExponent    is the exponent of the RHS power-law graph. see
   *                                 {@link PowerLawDegreeEdgePool} for details
   * @param maxNumEdges              the max number of edges this segment is supposed to hold
   * @param statsReceiver            tracks the internal stats
   */
  public LeftRegularBipartiteGraphSegment(
      int expectedNumLeftNodes,
      int leftDegree,
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
            new RegularDegreeEdgePool(expectedNumLeftNodes, leftDegree, statsReceiver),
            new PowerLawDegreeEdgePool(
                expectedNumRightNodes,
                expectedMaxRightDegree,
                rightPowerLawExponent,
                statsReceiver),
            statsReceiver),
        edgeTypeMask,
        statsReceiver);
  }

  public ReusableNodeIntIterator initializeLeftNodeEdgesIntIterator() {
    return new RegularDegreeEdgeIterator((RegularDegreeEdgePool) getLeftNodeEdgePool());
  }

  public ReusableNodeRandomIntIterator initializeLeftNodeEdgesRandomIntIterator() {
    return new RegularDegreeEdgeRandomIterator((RegularDegreeEdgePool) getLeftNodeEdgePool());
  }

  public ReusableInternalIdToLongIterator initializeLeftInternalIdToLongIterator() {
    return new InternalIdToLongIterator(getRightNodesToIndexBiMap(), edgeTypeMask);
  }

  public ReusableNodeIntIterator initializeRightNodeEdgesIntIterator() {
    return new PowerLawDegreeEdgeIterator((PowerLawDegreeEdgePool) getRightNodeEdgePool());
  }

  public ReusableNodeRandomIntIterator initializeRightNodeEdgesRandomIntIterator() {
    return new PowerLawDegreeEdgeRandomIterator((PowerLawDegreeEdgePool) getRightNodeEdgePool());
  }

  public ReusableInternalIdToLongIterator initializeRightInternalIdToLongIterator() {
    return new InternalIdToLongIterator(getLeftNodesToIndexBiMap(), edgeTypeMask);
  }
}
