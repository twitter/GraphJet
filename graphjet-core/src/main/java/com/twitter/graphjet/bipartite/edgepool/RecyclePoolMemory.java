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


package com.twitter.graphjet.bipartite.edgepool;

/**
 * This utility class allows recycling already allocated memory for edge pools, but note that this
 * is done a manner that is NOT thread-safe.
 */
public final class RecyclePoolMemory {

  private RecyclePoolMemory() {
    // Utility class
  }

  /**
   * This function provides a way to recycle memory from a
   * {@link com.twitter.graphjet.bipartite.edgepool.AbstractRegularDegreeEdgePool} by resetting it's
   * internal state.
   *
   * NOTE: This method is NOT thread-safe!
   */
  public static void recycleRegularDegreeEdgePool(
    AbstractRegularDegreeEdgePool regularDegreeEdgePool
  ) {
    regularDegreeEdgePool.currentPositionOffset = 0;
    regularDegreeEdgePool.currentNumNodes = 0;
    regularDegreeEdgePool.currentShardId = 0;

    regularDegreeEdgePool.currentNumEdgesStored = 0;
    regularDegreeEdgePool.readerAccessibleInfo.getEdges().reset();
    regularDegreeEdgePool.readerAccessibleInfo.getNodeInfo().clear();
  }

  /**
   * This function provides a way to recycle memory from a
   * {@link com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool} by resetting it's internal
   * state.
   *
   * NOTE: This method is NOT thread-safe!
   */
  public static void recyclePowerLawDegreeEdgePool(PowerLawDegreeEdgePool powerLawDegreeEdgePool) {
    for (AbstractRegularDegreeEdgePool regularDegreeEdgePool
        : powerLawDegreeEdgePool.readerAccessibleInfo.edgePools) {
      recycleRegularDegreeEdgePool(regularDegreeEdgePool);
    }
    powerLawDegreeEdgePool.readerAccessibleInfo = new PowerLawDegreeEdgePool.ReaderAccessibleInfo(
        powerLawDegreeEdgePool.readerAccessibleInfo.edgePools,
        powerLawDegreeEdgePool.readerAccessibleInfo.poolDegrees,
        new int[powerLawDegreeEdgePool.readerAccessibleInfo.nodeDegrees.length]);
  }
}
