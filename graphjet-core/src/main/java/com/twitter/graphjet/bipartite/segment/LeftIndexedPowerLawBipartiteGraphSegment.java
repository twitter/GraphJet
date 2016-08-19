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
import com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * A graph segment is a bounded portion of the graph with a cap on the number of nodes and edges
 * one can store in it.
 *
 * This particular segment has two properties.
 * 1. It stores only edges indexed by left nodes, not edges indexed by right nodes.
 * 2. Each node on the left hand side is assumed to have a power law degree distribution.
 *
 * This class is thread-safe as the underlying
 * {@link com.twitter.graphjet.bipartite.segment.LeftIndexedBipartiteGraphSegment} is thread-safe
 * and all this class does is provide implementations of edge pools and iterators.
 */
public class LeftIndexedPowerLawBipartiteGraphSegment extends LeftIndexedBipartiteGraphSegment {

  /**
   * The constructor tries to reserve most of the memory that is needed for the graph, although
   * as edges are added in, more memory will be allocated as needed.
   *
   * @param expectedNumLeftNodes     is the expected number of left nodes that would be inserted in
   *                                 the segment
   * @param expectedMaxLeftDegree    is the maximum degree expected for any left node
   * @param expectedNumRightNodes    is the expected number of right nodes that would be inserted in
   *                                 the segment
   * @param maxNumEdges              the max number of edges this segment is supposed to hold
   * @param statsReceiver            tracks the internal stats
   */
  public LeftIndexedPowerLawBipartiteGraphSegment(
      int expectedNumLeftNodes,
      int expectedMaxLeftDegree,
      double leftPowerLawExponent,
      int expectedNumRightNodes,
      int maxNumEdges,
      EdgeTypeMask edgeTypeMask,
      StatsReceiver statsReceiver) {
    super(
        expectedNumLeftNodes,
        expectedNumRightNodes,
        maxNumEdges,
        new LeftIndexedReaderAccessibleInfoProvider(
            expectedNumLeftNodes,
            expectedNumRightNodes,
            new PowerLawDegreeEdgePool(
                expectedNumLeftNodes,
                expectedMaxLeftDegree,
                leftPowerLawExponent,
                statsReceiver.scope("leftNodeEdgePool")),
            statsReceiver),
        edgeTypeMask,
        statsReceiver.scope("PowerLaw"));
  }

  public ReusableNodeIntIterator initializeLeftNodeEdgesIntIterator() {
    return PowerLawBipartiteGraphSegment.EdgeIteratorFactory.
      createEdgeIterator(getLeftNodeEdgePool());
  }

  public ReusableNodeRandomIntIterator initializeLeftNodeEdgesRandomIntIterator() {
    return PowerLawBipartiteGraphSegment.EdgeIteratorFactory
      .createRandomEdgeIterator(getLeftNodeEdgePool());
  }

  public ReusableInternalIdToLongIterator initializeLeftInternalIdToLongIterator() {
    return new InternalIdToLongIterator(getRightNodesToIndexBiMap(), edgeTypeMask);
  }

  public ReusableInternalIdToLongIterator initializeRightInternalIdToLongIterator() {
    return new InternalIdToLongIterator(getLeftNodesToIndexBiMap(), edgeTypeMask);
  }
}
