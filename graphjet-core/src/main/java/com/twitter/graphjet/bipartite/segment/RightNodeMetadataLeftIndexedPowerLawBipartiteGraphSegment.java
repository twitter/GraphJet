/**
 * Copyright 2018 Twitter. All rights reserved.
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
import com.twitter.graphjet.bipartite.edgepool.AbstractPowerLawDegreeEdgePool;
import com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgeIterator;
import com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool;
import com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgeRandomIterator;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * A graph segment is a bounded portion of the graph with a cap on the number of nodes and edges
 * one can store in it.
 *
 * This particular segment has several properties.
 * 1. It stores node metadata along with edges.
 * 2. It stores only edges indexed by left nodes, not edges indexed by right nodes.
 * 3. Each node on the left hand side is assumed to have a power law degree distribution.
 *
 * This class is thread-safe as the underlying
 * {@link com.twitter.graphjet.bipartite.segment.LeftIndexedBipartiteGraphSegment} is thread-safe
 * and all this class does is provide implementations of edge pools and iterators.
 */
public class RightNodeMetadataLeftIndexedPowerLawBipartiteGraphSegment
  extends RightNodeMetadataLeftIndexedBipartiteGraphSegment {
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
   * @param numRightNodeMetadataTypes is the max number of node metadata types associated with the
   *                                  right nodes
   * @param statsReceiver            tracks the internal stats
   */
  public RightNodeMetadataLeftIndexedPowerLawBipartiteGraphSegment(
    int expectedNumLeftNodes,
    int expectedMaxLeftDegree,
    double leftPowerLawExponent,
    int expectedNumRightNodes,
    int maxNumEdges,
    int numRightNodeMetadataTypes,
    EdgeTypeMask edgeTypeMask,
    StatsReceiver statsReceiver) {
    super(
      expectedNumLeftNodes,
      expectedNumRightNodes,
      maxNumEdges,
      new NodeMetadataLeftIndexedReaderAccessibleInfoProvider(
        expectedNumLeftNodes,
        expectedNumRightNodes,
        numRightNodeMetadataTypes,
        new PowerLawDegreeEdgePool(
          expectedNumLeftNodes,
          expectedMaxLeftDegree,
          leftPowerLawExponent,
          statsReceiver.scope("leftNodeEdgePool")),
        statsReceiver),
      edgeTypeMask,
      statsReceiver.scope("NodeMetadataPowerLaw"));
  }

  public ReusableNodeIntIterator initializeLeftNodeEdgesIntIterator() {
    return new PowerLawDegreeEdgeIterator(
      (AbstractPowerLawDegreeEdgePool) getLeftNodeEdgePool()
    );
  }

  public ReusableNodeRandomIntIterator initializeLeftNodeEdgesRandomIntIterator() {
    return new PowerLawDegreeEdgeRandomIterator(
      (AbstractPowerLawDegreeEdgePool) getLeftNodeEdgePool()
    );
  }

  /**
   * Create a reusable internal id to long iterator.
   *
   * @return a reusable internal id to long iterator of left edge pool
   */
  public ReusableInternalIdToLongIterator initializeLeftInternalIdToLongIterator() {
    return new NodeMetadataInternalIdToLongIterator(
      getRightNodesToIndexBiMap(),
      getRightNodesToMetadataMap(),
      edgeTypeMask
    );
  }

  /**
   * Create a reusable internal id to long iterator.
   *
   * @return a reusable internal id to long iterator of right edge pool
   */
  public ReusableInternalIdToLongIterator initializeRightInternalIdToLongIterator() {
    return new NodeMetadataInternalIdToLongIterator(
      getLeftNodesToIndexBiMap(),
      getLeftNodesToMetadataMap(),
      edgeTypeMask
    );
  }
}

