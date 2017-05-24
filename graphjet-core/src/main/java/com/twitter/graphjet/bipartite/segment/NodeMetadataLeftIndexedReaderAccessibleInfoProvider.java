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
        expectedNumLeftNodes, LOAD_FACTOR, -1, -1, statsReceiver.scope("left")),
      new ArrayBasedLongToInternalIntBiMap(
        expectedNumRightNodes, LOAD_FACTOR, -1, -1, statsReceiver.scope("right")),
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
