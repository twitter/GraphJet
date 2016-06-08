package com.twitter.graphjet.bipartite;

import java.util.Random;

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeRandomIntIterator;
import com.twitter.graphjet.bipartite.segment.LeftIndexedBipartiteGraphSegment;
import com.twitter.graphjet.bipartite.segment.ReusableInternalIdToLongIterator;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 * This enables transparently using the same iterator for left or right node edges. Note that the
 * choice is sticky and is set on startup.
 */
abstract class SegmentEdgeRandomAccessor<T extends LeftIndexedBipartiteGraphSegment>
    extends SegmentEdgeAccessor<T> {
  protected final Int2ObjectMap<ReusableInternalIdToLongIterator>
      segmentInternalIdToLongIteratorMap;
  protected final Int2ObjectMap<ReusableNodeRandomIntIterator> segmentNodeRandomIntIteratorMap;

  SegmentEdgeRandomAccessor(
      MultiSegmentReaderAccessibleInfo<T> readerAccessibleInfo,
      Int2ObjectMap<ReusableInternalIdToLongIterator> segmentInternalIdToLongIteratorMap,
      Int2ObjectMap<ReusableNodeRandomIntIterator> segmentNodeRandomIntIteratorMap) {
    super(readerAccessibleInfo);
    this.segmentInternalIdToLongIteratorMap = segmentInternalIdToLongIteratorMap;
    this.segmentNodeRandomIntIteratorMap = segmentNodeRandomIntIteratorMap;
  }

  public abstract EdgeIterator getRandomNodeEdges(
      int segmentId, long node, int numSamples, Random random);

  public abstract int getDegreeInSegment(long node, int segmentId);
}
