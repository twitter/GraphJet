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
import com.twitter.graphjet.stats.StatsReceiver;

public class NodeMetadataLeftIndexedPowerLawSegmentProvider
  extends BipartiteGraphSegmentProvider<NodeMetadataLeftIndexedBipartiteGraphSegment> {
  private final int expectedNumLeftNodes;
  private final int expectedMaxLeftDegree;
  private final double leftPowerLawExponent;
  private final int expectedNumRightNodes;
  private final int numRightNodeMetadataTypes;

  /**
   * The constructor tries to reserve most of the memory that is needed for the graph, although
   * as edges are added in, more memory will be allocated as needed.
   *
   * @param expectedNumLeftNodes     is the expected number of left nodes that would be inserted in
   *                                 the segment
   * @param expectedMaxLeftDegree    is the maximum degree expected for any left node
   * @param leftPowerLawExponent     is the exponent of the RHS power-law graph. see
   *                                 {@link
   *                                    com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool}
   *                                 for details
   * @param expectedNumRightNodes    is the expected number of right nodes that would be inserted in
   *                                 the segment
   * @param numRightNodeMetadataTypes is the max number of node metadata types associated with the
   *                                  right nodes
   * @param edgeTypeMask             the bit mask used to encode edge types
   * @param statsReceiver            tracks the internal stats
   */
  public NodeMetadataLeftIndexedPowerLawSegmentProvider(
    int expectedNumLeftNodes,
    int expectedMaxLeftDegree,
    double leftPowerLawExponent,
    int expectedNumRightNodes,
    int numRightNodeMetadataTypes,
    EdgeTypeMask edgeTypeMask,
    StatsReceiver statsReceiver) {
    super(edgeTypeMask, statsReceiver);
    this.expectedNumLeftNodes = expectedNumLeftNodes;
    this.expectedMaxLeftDegree = expectedMaxLeftDegree;
    this.leftPowerLawExponent = leftPowerLawExponent;
    this.expectedNumRightNodes = expectedNumRightNodes;
    this.numRightNodeMetadataTypes = numRightNodeMetadataTypes;
  }

  @Override
  public NodeMetadataLeftIndexedBipartiteGraphSegment generateNewSegment(
    int segmentId,
    int maxNumEdges
  ) {
    return new NodeMetadataLeftIndexedPowerLawBipartiteGraphSegment(
      expectedNumLeftNodes,
      expectedMaxLeftDegree,
      leftPowerLawExponent,
      expectedNumRightNodes,
      maxNumEdges,
      numRightNodeMetadataTypes,
      edgeTypeMask,
      statsReceiver.scope("segment_" + segmentId));
  }
}

