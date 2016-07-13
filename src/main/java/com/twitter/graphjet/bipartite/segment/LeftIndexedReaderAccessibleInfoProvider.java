package com.twitter.graphjet.bipartite.segment;

import com.twitter.graphjet.bipartite.edgepool.EdgePool;
import com.twitter.graphjet.hashing.ArrayBasedLongToInternalIntBiMap;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * This class provides a {@link LeftIndexedReaderAccessibleInfo} object, abstracting away the logic
 * of building and updating such an object.
 */
public class LeftIndexedReaderAccessibleInfoProvider {
  protected static final double LOAD_FACTOR = 0.6;

  private LeftIndexedReaderAccessibleInfo leftIndexedReaderAccessibleInfo;

  /**
   * The constructor tries to reserve most of the memory that is needed for the graph.
   *
   * @param expectedNumLeftNodes   is the expected number of left nodes that would be inserted in
   *                               the segment
   * @param expectedNumRightNodes  is the expected number of right nodes that would be inserted in
   *                               the segment
   * @param leftNodeEdgePool       is the pool containing all the left-indexed edges
   * @param statsReceiver          is passed downstream for updating stats
   */
  public LeftIndexedReaderAccessibleInfoProvider(
      int expectedNumLeftNodes,
      int expectedNumRightNodes,
      EdgePool leftNodeEdgePool,
      StatsReceiver statsReceiver) {
    this.leftIndexedReaderAccessibleInfo = new LeftIndexedReaderAccessibleInfo(
        new ArrayBasedLongToInternalIntBiMap(
            expectedNumLeftNodes, LOAD_FACTOR, -1, -1, statsReceiver.scope("left")),
        new ArrayBasedLongToInternalIntBiMap(
            expectedNumRightNodes, LOAD_FACTOR, -1, -1, statsReceiver.scope("right")),
        leftNodeEdgePool);
  }

  // No-op constructor that protects usage of the leftIndexedReaderAccessibleInfo field by
  // subclasses, enabling use of other reader info objects that extend
  // {@link LeftIndexedReaderAccessibleInfo}
  protected LeftIndexedReaderAccessibleInfoProvider() { }

  public LeftIndexedReaderAccessibleInfo getLeftIndexedReaderAccessibleInfo() {
    return leftIndexedReaderAccessibleInfo;
  }

  /**
   * Update reader accessible info with the new optimized read-only edge pool.
   *
   * @param newLeftNodeEdgePool the optimized read-only edge pool of LHS graph
   */
  public void updateReaderAccessibleInfoLeftNodeEdgePool(EdgePool newLeftNodeEdgePool) {
    leftIndexedReaderAccessibleInfo = new LeftIndexedReaderAccessibleInfo(
        leftIndexedReaderAccessibleInfo.getLeftNodesToIndexBiMap(),
        leftIndexedReaderAccessibleInfo.getRightNodesToIndexBiMap(),
        newLeftNodeEdgePool
    );
  }
}
