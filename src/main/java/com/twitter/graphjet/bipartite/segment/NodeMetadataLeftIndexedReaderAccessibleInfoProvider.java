package com.twitter.graphjet.bipartite.segment;

import java.util.ArrayList;
import java.util.List;

import com.twitter.graphjet.bipartite.edgepool.EdgePool;
import com.twitter.graphjet.hashing.ArrayBasedIntToIntArrayMap;
import com.twitter.graphjet.hashing.ArrayBasedLongToInternalIntBiMap;
import com.twitter.graphjet.hashing.IntToIntArrayMap;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * This class provides a {@link NodeMetadataLeftIndexedReaderAccessibleInfo} object, abstracting
 * away the logic of building and updating such an object.
 */
public class NodeMetadataLeftIndexedReaderAccessibleInfoProvider
  extends LeftIndexedReaderAccessibleInfoProvider {
  private static final int EXPECTED_ARRAY_SIZE = 15;

  private NodeMetadataLeftIndexedReaderAccessibleInfo readerAccessibleInfo;

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
  public NodeMetadataLeftIndexedReaderAccessibleInfoProvider(
    int expectedNumLeftNodes,
    int expectedNumRightNodes,
    int numRightNodeMetadataTypes,
    EdgePool leftNodeEdgePool,
    StatsReceiver statsReceiver) {
    List<IntToIntArrayMap> rightNodesToMetadataMap =
      new ArrayList<IntToIntArrayMap>(numRightNodeMetadataTypes);

    for (int i = 0; i < numRightNodeMetadataTypes; i++) {
      rightNodesToMetadataMap.add(new ArrayBasedIntToIntArrayMap(
        expectedNumRightNodes,
        EXPECTED_ARRAY_SIZE,
        statsReceiver
      ));
    }

    readerAccessibleInfo = new NodeMetadataLeftIndexedReaderAccessibleInfo(
      new ArrayBasedLongToInternalIntBiMap(
        expectedNumLeftNodes, LOAD_FACTOR, -1, -1, statsReceiver),
      new ArrayBasedLongToInternalIntBiMap(
        expectedNumRightNodes, LOAD_FACTOR, -1, -1, statsReceiver),
      leftNodeEdgePool,
      rightNodesToMetadataMap
    );
  }

  @Override
  public LeftIndexedReaderAccessibleInfo getLeftIndexedReaderAccessibleInfo() {
    return readerAccessibleInfo;
  }

  public NodeMetadataLeftIndexedReaderAccessibleInfo getReaderAccessibleInfo() {
    return readerAccessibleInfo;
  }
}
