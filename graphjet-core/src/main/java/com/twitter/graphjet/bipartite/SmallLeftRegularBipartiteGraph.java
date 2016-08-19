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

import com.twitter.graphjet.bipartite.segment.IdentityEdgeTypeMask;
import com.twitter.graphjet.bipartite.segment.LeftRegularBipartiteGraphSegment;
import com.twitter.graphjet.bipartite.segment.RecycleSegmentMemory;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * This creates a single segment bipartite graph (hence the small) where each node on the left hand
 * side is almost regular (hence the LeftRegular), i.e. each node has a maxDegree that is not too
 * large. The right hand side is assumed to have a power law degree distribution.
 *
 * NOTE: This class is NOT thread-safe as it attempts to minimize object creation and aims for
 *       reuse.
 */
public class SmallLeftRegularBipartiteGraph extends LeftRegularBipartiteGraphSegment {

  /**
   * The constructor allocates all the memory that is needed for the graph. The instance can be
   * reused via the reset method to minimize the impact of GC in the application.
   *
   * @param maxNumLeftNodes        is the maximum number of left nodes allowed in the segment
   * @param leftDegree             is the maximum degree allowed for any left node
   * @param maxNumRightNodes       is the maximum number of right nodes allowed in the segment
   * @param maxRightDegree         is the maximum degree allowed for any right node
   * @param rightPowerLawExponent  is the exponent of the RHS power-law graph. see
   *                               {@link
   *                               com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool}
   *                               for details
   * @param maxNumEdges            the max number of edges this segment is supposed to hold
   * @param statsReceiver          tracks the internal stats
   */
  public SmallLeftRegularBipartiteGraph(
      int maxNumLeftNodes,
      int leftDegree,
      int maxNumRightNodes,
      int maxRightDegree,
      double rightPowerLawExponent,
      int maxNumEdges,
      StatsReceiver statsReceiver) {
    super(
        maxNumLeftNodes,
        leftDegree,
        maxNumRightNodes,
        maxRightDegree,
        rightPowerLawExponent,
        maxNumEdges,
        new IdentityEdgeTypeMask(),
        statsReceiver);
  }

  /**
   * This method resets all the internal state to enable reusing the allocated memory.
   *
   * NOTE: this method is NOT thread-safe.
   */
  public void reset() {
    RecycleSegmentMemory.recycleLeftRegularBipartiteGraphSegment(this);
  }
}
