package com.twitter.graphjet.bipartite.segment;

import com.twitter.graphjet.bipartite.edgepool.EdgePool;
import com.twitter.graphjet.hashing.ArrayBasedLongToInternalIntBiMap;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * This class provides a {@link ReaderAccessibleInfo} object, abstracting away the logic of building
 * and updating such an object.
 */
public class ReaderAccessibleInfoProvider extends LeftIndexedReaderAccessibleInfoProvider {
  private ReaderAccessibleInfo readerAccessibleInfo;

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
  public ReaderAccessibleInfoProvider(
      int expectedNumLeftNodes,
      int expectedNumRightNodes,
      EdgePool leftNodeEdgePool,
      EdgePool rightNodeEdgePool,
      StatsReceiver statsReceiver) {
    readerAccessibleInfo = new ReaderAccessibleInfo(
        new ArrayBasedLongToInternalIntBiMap(
            expectedNumLeftNodes, LOAD_FACTOR, -1, -1, statsReceiver.scope("left")),
        new ArrayBasedLongToInternalIntBiMap(
            expectedNumRightNodes, LOAD_FACTOR, -1, -1, statsReceiver.scope("right")),
        leftNodeEdgePool,
        rightNodeEdgePool);
  }

  @Override
  public LeftIndexedReaderAccessibleInfo getLeftIndexedReaderAccessibleInfo() {
    return readerAccessibleInfo;
  }

  public ReaderAccessibleInfo getReaderAccessibleInfo() {
    return readerAccessibleInfo;
  }

  /**
   * Update reader accessible info with the new optimized read-only edge pools.
   *
   * @param newLeftNodeEdgePool the optimized read-only edge pool of LHS graph
   * @param newRightNodeEdgePool the optimized read-only edge pool of RHS graph
   */
  public void updateReaderAccessibleInfoEdgePool(
      EdgePool newLeftNodeEdgePool,
      EdgePool newRightNodeEdgePool
  ) {
    readerAccessibleInfo = new ReaderAccessibleInfo(
        readerAccessibleInfo.getLeftNodesToIndexBiMap(),
        readerAccessibleInfo.getRightNodesToIndexBiMap(),
        newLeftNodeEdgePool,
        newRightNodeEdgePool
    );
  }
}
