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
   * {@link com.twitter.graphjet.bipartite.edgepool.RegularDegreeEdgePool} by resetting it's internal
   * state.
   *
   * NOTE: This method is NOT thread-safe!
   */
  public static void recycleRegularDegreeEdgePool(RegularDegreeEdgePool regularDegreeEdgePool) {
    regularDegreeEdgePool.currentPositionOffset = 0;
    regularDegreeEdgePool.currentNumNodes = 0;
    regularDegreeEdgePool.currentShardId = 0;

    regularDegreeEdgePool.currentNumEdgesStored = 0;
    regularDegreeEdgePool.readerAccessibleInfo.edges.reset();
    regularDegreeEdgePool.readerAccessibleInfo.nodeInfo.clear();
  }

  /**
   * This function provides a way to recycle memory from a
   * {@link com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool} by resetting it's internal
   * state.
   *
   * NOTE: This method is NOT thread-safe!
   */
  public static void recyclePowerLawDegreeEdgePool(PowerLawDegreeEdgePool powerLawDegreeEdgePool) {
    for (RegularDegreeEdgePool regularDegreeEdgePool
        : powerLawDegreeEdgePool.readerAccessibleInfo.edgePools) {
      recycleRegularDegreeEdgePool(regularDegreeEdgePool);
    }
    powerLawDegreeEdgePool.readerAccessibleInfo = new PowerLawDegreeEdgePool.ReaderAccessibleInfo(
        powerLawDegreeEdgePool.readerAccessibleInfo.edgePools,
        powerLawDegreeEdgePool.readerAccessibleInfo.poolDegrees,
        new int[powerLawDegreeEdgePool.readerAccessibleInfo.nodeDegrees.length]);
  }
}
