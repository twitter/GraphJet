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

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.api.WithEdgeMetadataIntIterator;
import com.twitter.graphjet.bipartite.api.EdgeTypeMask;
import com.twitter.graphjet.hashing.LongToInternalIntBiMap;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * This iterator maintains the mapping from internal ids to longs and recovers the external-facing
 * long id's in {@link LeftRegularBipartiteGraphSegment}.
 */
public class InternalIdToLongIterator implements EdgeIterator, ReusableInternalIdToLongIterator {
  private final LongToInternalIntBiMap nodesIndexMap;
  private EdgeTypeMask edgeTypeMask;
  private WithEdgeMetadataIntIterator intIterator;
  private int currentNodeId;

  public InternalIdToLongIterator(LongToInternalIntBiMap nodesIndexMap, EdgeTypeMask edgeTypeMask) {
    this.nodesIndexMap = nodesIndexMap;
    this.edgeTypeMask = edgeTypeMask;
    this.currentNodeId = 0;
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
