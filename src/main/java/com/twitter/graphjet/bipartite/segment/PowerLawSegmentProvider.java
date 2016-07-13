package com.twitter.graphjet.bipartite.segment;

import com.twitter.graphjet.bipartite.api.EdgeTypeMask;
import com.twitter.graphjet.stats.StatsReceiver;

public class PowerLawSegmentProvider extends BipartiteGraphSegmentProvider<BipartiteGraphSegment> {
  private final int expectedNumLeftNodes;
  private final int expectedMaxLeftDegree;
  private final double leftPowerLawExponent;
  private final int expectedNumRightNodes;
  private final int expectedMaxRightDegree;
  private final double rightPowerLawExponent;

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
   *                                  {@link
   *                                    com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool}
   *                                 for details
   * @param edgeTypeMask             is the mask to encode edge type into integer node id
   * @param statsReceiver            tracks the internal stats
   */
  public PowerLawSegmentProvider(
      int expectedNumLeftNodes,
      int expectedMaxLeftDegree,
      double leftPowerLawExponent,
      int expectedNumRightNodes,
      int expectedMaxRightDegree,
      double rightPowerLawExponent,
      EdgeTypeMask edgeTypeMask,
      StatsReceiver statsReceiver) {
    super(edgeTypeMask, statsReceiver);
    this.expectedNumLeftNodes = expectedNumLeftNodes;
    this.expectedMaxLeftDegree = expectedMaxLeftDegree;
    this.leftPowerLawExponent = leftPowerLawExponent;
    this.expectedNumRightNodes = expectedNumRightNodes;
    this.expectedMaxRightDegree = expectedMaxRightDegree;
    this.rightPowerLawExponent = rightPowerLawExponent;

  }

  @Override
  public BipartiteGraphSegment generateNewSegment(int segmentId, int maxNumEdges) {
    return new PowerLawBipartiteGraphSegment(
        expectedNumLeftNodes,
        expectedMaxLeftDegree,
        leftPowerLawExponent,
        expectedNumRightNodes,
        expectedMaxRightDegree,
        rightPowerLawExponent,
        maxNumEdges,
        edgeTypeMask,
        statsReceiver.scope("segment_" + segmentId));
  }
}
