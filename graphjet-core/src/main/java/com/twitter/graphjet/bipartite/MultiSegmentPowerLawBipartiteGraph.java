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
import com.twitter.graphjet.bipartite.api.ReusableNodeRandomIntIterator;
import com.twitter.graphjet.bipartite.segment.BipartiteGraphSegment;
import com.twitter.graphjet.bipartite.segment.PowerLawSegmentProvider;
import com.twitter.graphjet.bipartite.segment.ReusableInternalIdToLongIterator;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * A multi-segment bipartite graph where each segment is a
 * {@link com.twitter.graphjet.bipartite.segment.PowerLawBipartiteGraphSegment}.
 *
 * This class is thread-safe as the underlying
 * {@link com.twitter.graphjet.bipartite.MultiSegmentBipartiteGraph} is thread-safe and all this
 * class does is provide implementations of segments and iterators.
 */
public class MultiSegmentPowerLawBipartiteGraph extends MultiSegmentBipartiteGraph {
  /**
   * Create a multi-segment bipartite graph where both the left and right degrees are characterized by power laws.
   *
   * @param maxNumSegments           the maximum number of segments in the graph, after which the oldest segment will
   *                                 be dropped
   * @param maxNumEdgesPerSegment    the maximum number of edges in each segment, after which a new segment will be
   *                                 created
   * @param expectedNumLeftNodes     the expected number of left nodes in each segment
   * @param expectedMaxLeftDegree    the expected maximum degree for a left node (soft upper bound)
   * @param leftPowerLawExponent     the exponent of the power law characterizing the left degree distribution, see
   *                                 {@link com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool}
   * @param expectedNumRightNodes    the expected number of right nodes in each segment
   * @param expectedMaxRightDegree   the expected maximum degree for a left node (soft upper bound)
   * @param rightPowerLawExponent    the exponent of the power law characterizing the left degree distribution, see
   *                                 {@link com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool}
   * @param edgeTypeMask             the mask to encode edge type into the integer node id
   * @param statsReceiver            object for tracking internal stats
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
    return new ChronologicalMultiSegmentIterator<BipartiteGraphSegment>(
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
    return new ChronologicalMultiSegmentIterator<BipartiteGraphSegment>(
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
