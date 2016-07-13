package com.twitter.graphjet.bipartite;

import com.twitter.graphjet.bipartite.api.EdgeTypeMask;
import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeRandomIntIterator;
import com.twitter.graphjet.bipartite.segment.BipartiteGraphSegment;
import com.twitter.graphjet.bipartite.segment.PowerLawSegmentProvider;
import com.twitter.graphjet.bipartite.segment.ReusableInternalIdToLongIterator;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * This creates a multi-segment bipartite graph where each segment is a
 * {@link com.twitter.graphjet.bipartite.segment.PowerLawBipartiteGraphSegment}.
 *
 * This class is thread-safe as the underlying
 * {@link com.twitter.graphjet.bipartite.MultiSegmentBipartiteGraph} is thread-safe and all this
 * class does is provide implementations of segments and iterators.
 */
public class MultiSegmentPowerLawBipartiteGraph extends MultiSegmentBipartiteGraph {
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
   * @param expectedMaxRightDegree   is the maximum degree expected for any right node
   * @param rightPowerLawExponent    is the exponent of the RHS power-law graph. see
   *                                  {@link
   *                                    com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool}
   *                                 for details
   * @param edgeTypeMask             is the mask to encode edge type into integer node id
   * @param statsReceiver            tracks the internal stats
   */
  public MultiSegmentPowerLawBipartiteGraph(
      int maxNumSegments,
      int maxNumEdgesPerSegment,
      int expectedNumLeftNodes,
      int expectedMaxLeftDegree,
      double leftPowerLawExponent,
      int expectedNumRightNodes,
      int expectedMaxRightDegree,
      double rightPowerLawExponent,
      EdgeTypeMask edgeTypeMask,
      StatsReceiver statsReceiver) {
    super(
        maxNumSegments,
        maxNumEdgesPerSegment,
        new PowerLawSegmentProvider(
            expectedNumLeftNodes,
            expectedMaxLeftDegree,
            leftPowerLawExponent,
            expectedNumRightNodes,
            expectedMaxRightDegree,
            rightPowerLawExponent,
            edgeTypeMask,
            statsReceiver),
        new MultiSegmentReaderAccessibleInfoProvider<BipartiteGraphSegment>(
            maxNumSegments, maxNumEdgesPerSegment),
        statsReceiver);
  }

  @Override
  ReusableNodeLongIterator initializeLeftNodeEdgesLongIterator() {
    return new MultiSegmentIterator<BipartiteGraphSegment>(
        this,
        new LeftSegmentEdgeAccessor<BipartiteGraphSegment>(
            getReaderAccessibleInfo(),
            new Int2ObjectOpenHashMap<ReusableNodeIntIterator>(
                getMaxNumSegments()),
            new Int2ObjectOpenHashMap<ReusableInternalIdToLongIterator>(
                getMaxNumSegments())
        )
    );
  }

  @Override
  ReusableNodeRandomLongIterator initializeLeftNodeEdgesRandomLongIterator() {
    return new MultiSegmentRandomIterator<BipartiteGraphSegment>(
        this,
        new LeftSegmentRandomEdgeAccessor<BipartiteGraphSegment>(
            getReaderAccessibleInfo(),
            new Int2ObjectOpenHashMap<ReusableInternalIdToLongIterator>(
                getMaxNumSegments()),
            new Int2ObjectOpenHashMap<ReusableNodeRandomIntIterator>(
                getMaxNumSegments())
        )
    );
  }

  @Override
  ReusableNodeLongIterator initializeRightNodeEdgesLongIterator() {
    return new MultiSegmentIterator<BipartiteGraphSegment>(
        this,
        new RightSegmentEdgeAccessor(
            getReaderAccessibleInfo(),
            new Int2ObjectOpenHashMap<ReusableNodeIntIterator>(
                getMaxNumSegments()),
            new Int2ObjectOpenHashMap<ReusableInternalIdToLongIterator>(
                getMaxNumSegments())
        )
    );
  }

  @Override
  ReusableNodeRandomLongIterator initializeRightNodeEdgesRandomLongIterator() {
    return new MultiSegmentRandomIterator<BipartiteGraphSegment>(
        this,
        new RightSegmentRandomEdgeAccessor(
            getReaderAccessibleInfo(),
            new Int2ObjectOpenHashMap<ReusableInternalIdToLongIterator>(
                getMaxNumSegments()),
            new Int2ObjectOpenHashMap<ReusableNodeRandomIntIterator>(
                getMaxNumSegments())
        )
    );
  }
}
