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

import com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool;
import com.twitter.graphjet.bipartite.edgepool.RecyclePoolMemory;
import com.twitter.graphjet.bipartite.edgepool.RegularDegreeEdgePool;

/**
 * This utility class allows recycling already allocated memory for segments, but note that this
 * is done a manner that is NOT thread-safe.
 */
public final class RecycleSegmentMemory {

  private RecycleSegmentMemory() {
    // Utility class
  }

  /**
   * This function provides a way to recycle memory from a
   * {@link com.twitter.graphjet.bipartite.segment.BipartiteGraphSegment} by resetting it's
   * internal state.
   *
   * NOTE: This method is NOT thread-safe!
   */
  public static void recycleLeftRegularBipartiteGraphSegment(
      BipartiteGraphSegment bipartiteGraphSegment) {
    bipartiteGraphSegment.getLeftNodesToIndexMap().clear();
    bipartiteGraphSegment.getRightNodesToIndexMap().clear();

    RecyclePoolMemory.recycleRegularDegreeEdgePool(
        (RegularDegreeEdgePool) bipartiteGraphSegment.getLeftNodeEdgePool());
    RecyclePoolMemory.recyclePowerLawDegreeEdgePool(
        (PowerLawDegreeEdgePool) bipartiteGraphSegment.getRightNodeEdgePool());

    bipartiteGraphSegment.currentNumEdges = 0;
  }
}
