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

import java.util.List;

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.api.WithEdgeMetadataIntIterator;
import com.twitter.graphjet.bipartite.api.EdgeTypeMask;
import com.twitter.graphjet.bipartite.api.NodeMetadataEdgeIterator;
import com.twitter.graphjet.hashing.IntArrayIterator;
import com.twitter.graphjet.hashing.IntToIntArrayMap;
import com.twitter.graphjet.hashing.LongToInternalIntBiMap;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * This iterator maintains the mapping from internal ids to longs and recovers the external-facing
 * long id's in {@link NodeMetadataLeftIndexedBipartiteGraphSegment}, along with the long id, it
 * also fetches the current left node metadata and right node metadata.
 */
public class NodeMetadataInternalIdToLongIterator
  implements NodeMetadataEdgeIterator, ReusableInternalIdToLongIterator {
  private final LongToInternalIntBiMap nodesIndexMap;
  private final List<IntToIntArrayMap> metadataMap;
  private EdgeTypeMask edgeTypeMask;
  private WithEdgeMetadataIntIterator intIterator;
  private IntIterator[] metadataIterators;
  private int currentNodeId;

  /**
   * This constructor is to map internal integer ids back to longs, and it allows node metadata
   * access.
   *
   * @param nodesIndexMap is the map from integer ids to longs
   * @param metadataMap   is the map from node metadata types to node metadata
   * @param edgeTypeMask  is the edge type between LHS node and RHS node
   */
  public NodeMetadataInternalIdToLongIterator(
    LongToInternalIntBiMap nodesIndexMap,
    List<IntToIntArrayMap> metadataMap,
    EdgeTypeMask edgeTypeMask
  ) {
    this.nodesIndexMap = nodesIndexMap;
    this.metadataMap = metadataMap;
    this.edgeTypeMask = edgeTypeMask;
    this.currentNodeId = 0;
    this.metadataIterators = new IntIterator[metadataMap.size()];
  }

  @Override
  public EdgeIterator resetWithIntIterator(IntIterator inputIntIterator) {
    this.intIterator = (WithEdgeMetadataIntIterator) inputIntIterator;
    this.currentNodeId = 0;
    return this;
  }

  @Override
  public long nextLong() {
    currentNodeId = intIterator.nextInt();
    return nodesIndexMap.getKey(edgeTypeMask.restore(currentNodeId));
  }

  @Override
  public byte currentEdgeType() {
    return edgeTypeMask.edgeType(currentNodeId);
  }

  @Override
  public long currentMetadata() {
    return intIterator.currentMetadata();
  }

  @Override
  public IntIterator getLeftNodeMetadata(byte nodeMetadataType) {
    throw new UnsupportedOperationException(
      "The getLeftNodeMetadata operation is currently not supported"
    );
  }

  @Override
  public IntIterator getRightNodeMetadata(byte nodeMetadataType) {
    IntIterator metadataIterator = metadataIterators[nodeMetadataType];
    if (metadataIterator == null) {
      metadataIterator = metadataMap.get(nodeMetadataType).get(edgeTypeMask.restore(currentNodeId));
      metadataIterators[nodeMetadataType] = metadataIterator;
    } else {
      metadataIterator = metadataMap.get(nodeMetadataType).get(
        edgeTypeMask.restore(currentNodeId),
        (IntArrayIterator) metadataIterator
      );
    }
    return metadataIterator;
  }

  @Override
  public int skip(int i) {
    return intIterator.skip(i);
  }

  @Override
  public boolean hasNext() {
    return intIterator.hasNext();
  }

  @Override
  public Long next() {
    return nextLong();
  }

  @Override
  public void remove() {
    intIterator.remove();
  }
}
