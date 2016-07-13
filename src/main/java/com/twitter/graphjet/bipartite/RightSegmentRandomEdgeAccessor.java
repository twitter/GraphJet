package com.twitter.graphjet.bipartite;

import java.util.Random;

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeRandomIntIterator;
import com.twitter.graphjet.bipartite.segment.BipartiteGraphSegment;
import com.twitter.graphjet.bipartite.segment.ReusableInternalIdToLongIterator;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

class RightSegmentRandomEdgeAccessor extends SegmentEdgeRandomAccessor<BipartiteGraphSegment> {
  RightSegmentRandomEdgeAccessor(
      MultiSegmentReaderAccessibleInfo<BipartiteGraphSegment> readerAccessibleInfo,
      Int2ObjectMap<ReusableInternalIdToLongIterator> segmentInternalIdToLongIteratorMap,
      Int2ObjectMap<ReusableNodeRandomIntIterator> segmentNodeRandomIntIteratorMap) {
    super(readerAccessibleInfo,
          segmentInternalIdToLongIteratorMap,
          segmentNodeRandomIntIteratorMap);
  }

  @Override
  public EdgeIterator getRandomNodeEdges(
      int segmentId, long node, int numSamples, Random random) {
    return readerAccessibleInfo.segments.get(segmentId).getRandomRightNodeEdges(
        node,
        numSamples,
        random,
        segmentNodeRandomIntIteratorMap.get(segmentId),
        segmentInternalIdToLongIteratorMap.get(segmentId));
  }

  @Override
  public int getDegreeInSegment(long node, int segmentId) {
    return readerAccessibleInfo.segments.get(segmentId).getRightNodeDegree(node);
  }

  @Override
  public EdgeIterator getNodeEdges(int segmentId, long node) {
    return null;
  }

  @Override
  public void rebuildIterators(int oldestSegmentId, int liveSegmentId) {
    segmentInternalIdToLongIteratorMap.clear();
    segmentNodeRandomIntIteratorMap.clear();
    for (int i = oldestSegmentId; i <= liveSegmentId; i++) {
      BipartiteGraphSegment segment = readerAccessibleInfo.segments.get(i);
      segmentInternalIdToLongIteratorMap.put(
          i, segment.initializeRightInternalIdToLongIterator());
      segmentNodeRandomIntIteratorMap.put(i, segment.initializeRightNodeEdgesRandomIntIterator());
    }
  }
}
