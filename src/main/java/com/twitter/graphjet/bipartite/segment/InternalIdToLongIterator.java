package com.twitter.graphjet.bipartite.segment;

import com.twitter.graphjet.bipartite.api.EdgeIterator;
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
  private IntIterator intIterator;
  private int currentNodeId;

  public InternalIdToLongIterator(LongToInternalIntBiMap nodesIndexMap, EdgeTypeMask edgeTypeMask) {
    this.nodesIndexMap = nodesIndexMap;
    this.edgeTypeMask = edgeTypeMask;
    this.currentNodeId = 0;
  }

  @Override
  public EdgeIterator resetWithIntIterator(IntIterator inputIntIterator) {
    this.intIterator = inputIntIterator;
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
