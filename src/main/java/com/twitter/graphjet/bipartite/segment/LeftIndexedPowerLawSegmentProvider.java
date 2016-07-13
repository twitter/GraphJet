package com.twitter.graphjet.bipartite.segment;

import com.twitter.graphjet.bipartite.api.EdgeTypeMask;
import com.twitter.graphjet.stats.StatsReceiver;

public class LeftIndexedPowerLawSegmentProvider
    extends BipartiteGraphSegmentProvider<LeftIndexedBipartiteGraphSegment> {
  private final int expectedNumLeftNodes;
  private final int expectedMaxLeftDegree;
  private final double leftPowerLawExponent;
  private final int expectedNumRightNodes;

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
   * @param statsReceiver            tracks the internal stats
   */
  public LeftIndexedPowerLawSegmentProvider(
      int expectedNumLeftNodes,
      int expectedMaxLeftDegree,
      double leftPowerLawExponent,
      int expectedNumRightNodes,
      EdgeTypeMask edgeTypeMask,
      StatsReceiver statsReceiver) {
    super(edgeTypeMask, statsReceiver);
    this.expectedNumLeftNodes = expectedNumLeftNodes;
    this.expectedMaxLeftDegree = expectedMaxLeftDegree;
    this.leftPowerLawExponent = leftPowerLawExponent;
    this.expectedNumRightNodes = expectedNumRightNodes;
  }

  @Override
  public LeftIndexedBipartiteGraphSegment generateNewSegment(int segmentId, int maxNumEdges) {
    return new LeftIndexedPowerLawBipartiteGraphSegment(
        expectedNumLeftNodes,
        expectedMaxLeftDegree,
        leftPowerLawExponent,
        expectedNumRightNodes,
        maxNumEdges,
        edgeTypeMask,
        statsReceiver.scope("segment_" + segmentId));
  }
}
