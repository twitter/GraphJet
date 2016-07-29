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


package com.twitter.graphjet.bipartite;

import com.twitter.graphjet.bipartite.api.EdgeTypeMask;
import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;
import com.twitter.graphjet.bipartite.segment.NodeMetadataLeftIndexedBipartiteGraphSegment;
import com.twitter.graphjet.bipartite.segment.NodeMetadataLeftIndexedPowerLawSegmentProvider;
import com.twitter.graphjet.bipartite.segment.ReusableInternalIdToLongIterator;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;


/**
 * This creates a left-indexed only multi-segment bipartite graph where each segment is a
 * {@link com.twitter.graphjet.bipartite.segment.LeftIndexedPowerLawBipartiteGraphSegment}.
 *
 * This class is thread-safe as the underlying
 * {@link LeftIndexedMultiSegmentBipartiteGraph} is thread-safe and all this
 * class does is provide implementations of segments and iterators.
 */
public class NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph
  extends NodeMetadataLeftIndexedMultiSegmentBipartiteGraph {
  /**
   * Create a multi-segment bipartite graph with both the left and right sides being power-law.
   *
   * @param maxNumSegments           is the maximum number of segments we'll add to the graph.
   *                                 At that point, the oldest segments will start getting dropped
   * @param maxNumEdgesPerSegment    determines when the implementation decides to fork off a
   *                                 new segment
   * @param expectedNumLeftNodes     is the expected number of left nodes that would be inserted in
   *                                 the segment
   * @param expectedMaxLeftDegree    is the maximum degree expected for any left node
   * @param leftPowerLawExponent     is the exponent of the LHS power-law graph. see
   *                                  {@link
   *                                    com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool}
   *                                 for details
   * @param expectedNumRightNodes    is the expected number of right nodes that would be inserted in
   *                                 the segment
   * @param numRightNodeMetadataTypes is the max number of node metadata types associated with the
   *                                  right nodes
   * @param statsReceiver            tracks the internal stats
   */
  public NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph(
    int maxNumSegments,
    int maxNumEdgesPerSegment,
    int expectedNumLeftNodes,
    int expectedMaxLeftDegree,
    double leftPowerLawExponent,
    int expectedNumRightNodes,
    int numRightNodeMetadataTypes,
    EdgeTypeMask edgeTypeMask,
    StatsReceiver statsReceiver) {
    super(
      maxNumSegments,
      maxNumEdgesPerSegment,
      new NodeMetadataLeftIndexedPowerLawSegmentProvider(
        expectedNumLeftNodes,
        expectedMaxLeftDegree,
        leftPowerLawExponent,
        expectedNumRightNodes,
        numRightNodeMetadataTypes,
        edgeTypeMask,
        statsReceiver),
      new MultiSegmentReaderAccessibleInfoProvider<NodeMetadataLeftIndexedBipartiteGraphSegment>(
        maxNumSegments, maxNumEdgesPerSegment),
      statsReceiver);
  }

  @Override
  ReusableNodeLongIterator initializeLeftNodeEdgesLongIterator() {
    return new NodeMetadataMultiSegmentIterator(
      this,
      new LeftSegmentEdgeAccessor<NodeMetadataLeftIndexedBipartiteGraphSegment>(
        getReaderAccessibleInfo(),
        new Int2ObjectOpenHashMap<ReusableNodeIntIterator>(getMaxNumSegments()),
        new Int2ObjectOpenHashMap<ReusableInternalIdToLongIterator>(getMaxNumSegments())
      )
    );
  }

  @Override
  ReusableNodeRandomLongIterator initializeLeftNodeEdgesRandomLongIterator() {
    throw new UnsupportedOperationException(
      "The getLeftNodeEdgesRandomLongIterator operation is currently not supported in "
        + "NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph."
    );
  }
}
