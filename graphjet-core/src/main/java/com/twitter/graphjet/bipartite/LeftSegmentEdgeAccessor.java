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


package com.twitter.graphjet.bipartite;

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;
import com.twitter.graphjet.bipartite.segment.LeftIndexedBipartiteGraphSegment;
import com.twitter.graphjet.bipartite.segment.ReusableInternalIdToLongIterator;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 * This wraps access to edges in a {@link LeftIndexedBipartiteGraphSegment} using a common interface
 * in the {@link SegmentEdgeAccessor}
 */
public class LeftSegmentEdgeAccessor<T extends LeftIndexedBipartiteGraphSegment>
    extends SegmentEdgeAccessor<T> {
  private final Int2ObjectMap<ReusableInternalIdToLongIterator>
      segmentInternalIdToLongIteratorMap;
  private final Int2ObjectMap<ReusableNodeIntIterator> segmentNodeIntIteratorMap;

  /**
   * The class only requires access to the reader information
   *
   * @param readerAccessibleInfo                encapsulates all the information accessed by a
   *                                            reader
   * @param segmentNodeIntIteratorMap           provides an iterator over the node edges, but giving
   *                                            out only internal int ids for the edges
   * @param segmentInternalIdToLongIteratorMap  provides a mapping from an internal edge id to the
   *                                            regular long id
   */
  public LeftSegmentEdgeAccessor(
      MultiSegmentReaderAccessibleInfo<T> readerAccessibleInfo,
      Int2ObjectMap<ReusableNodeIntIterator> segmentNodeIntIteratorMap,
      Int2ObjectMap<ReusableInternalIdToLongIterator> segmentInternalIdToLongIteratorMap) {
    super(readerAccessibleInfo);
    this.segmentInternalIdToLongIteratorMap = segmentInternalIdToLongIteratorMap;
    this.segmentNodeIntIteratorMap = segmentNodeIntIteratorMap;
  }

  @Override
  public EdgeIterator getNodeEdges(int segmentId, long node) {
    return readerAccessibleInfo.segments.get(segmentId).getLeftNodeEdges(
        node,
        segmentNodeIntIteratorMap.get(segmentId),
        segmentInternalIdToLongIteratorMap.get(segmentId)
        );
  }

  @Override
  public void rebuildIterators(int oldestSegmentId, int liveSegmentId) {
    segmentInternalIdToLongIteratorMap.clear();
    segmentNodeIntIteratorMap.clear();
    for (int i = oldestSegmentId; i <= liveSegmentId; i++) {
      T segment = readerAccessibleInfo.segments.get(i);
      segmentInternalIdToLongIteratorMap.put(i, segment.initializeLeftInternalIdToLongIterator());
      segmentNodeIntIteratorMap.put(i, segment.initializeLeftNodeEdgesIntIterator());
    }
  }
}
