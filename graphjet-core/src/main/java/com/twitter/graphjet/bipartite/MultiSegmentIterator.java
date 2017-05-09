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
import com.twitter.graphjet.bipartite.segment.LeftIndexedBipartiteGraphSegment;

/**
 * This iterator process segments in the chronological direction, i.e. it accesses edges
 * from the oldest segment first. The ordering of edges within each segment is left up to the
 * segment, but usually that is chronological so this likely has the nice side-effect of providing
 * chronological ordering over all the edges.
 */
public abstract class MultiSegmentIterator<T extends LeftIndexedBipartiteGraphSegment>
    implements EdgeIterator {
  protected final LeftIndexedMultiSegmentBipartiteGraph<T> multiSegmentBipartiteGraph;
  protected final SegmentEdgeAccessor<T> segmentEdgeAccessor;
  protected MultiSegmentReaderAccessibleInfo<T> readerAccessibleInfo;
  protected T oldestSegment;
  protected int currentSegmentId;
  protected int liveSegmentId;
  protected int oldestSegmentId;
  protected EdgeIterator currentSegmentIterator;
  protected long node;

  /**
   * This constructor is for easy reuse in the random iterator derived from this one.
   *
   * @param multiSegmentBipartiteGraph  is the underlying {@link MultiSegmentBipartiteGraph}
   * @param segmentEdgeAccessor         abstracts the left/right access in a common interface
   */
  public MultiSegmentIterator(
      LeftIndexedMultiSegmentBipartiteGraph<T> multiSegmentBipartiteGraph,
      SegmentEdgeAccessor<T> segmentEdgeAccessor) {
    this.multiSegmentBipartiteGraph = multiSegmentBipartiteGraph;
    this.segmentEdgeAccessor = segmentEdgeAccessor;
    rebuildSegmentIterators();
  }

  /**
   * Even if the writer drops the segment at some point, we still have
   * the handle to the oldest segment and can continue to access it
   */
  private void rebuildSegmentIterators() {
    readerAccessibleInfo = multiSegmentBipartiteGraph.getReaderAccessibleInfo();
    segmentEdgeAccessor.setReaderAccessibleInfo(readerAccessibleInfo);
    oldestSegmentId = readerAccessibleInfo.oldestSegmentId;
    liveSegmentId = readerAccessibleInfo.liveSegmentId;
    oldestSegment = readerAccessibleInfo.segments.get(oldestSegmentId);
    segmentEdgeAccessor.rebuildIterators(oldestSegmentId, liveSegmentId);
  }

  protected void rebuildSegmentIteratorsForNode(long inputNode) {
    this.node = inputNode;
    MultiSegmentReaderAccessibleInfo<T> newReaderAccessibleInfo = multiSegmentBipartiteGraph.getReaderAccessibleInfo();
    // this is violated rarely: only when we drop or add segments
    if ((oldestSegmentId != newReaderAccessibleInfo.oldestSegmentId)
        || (liveSegmentId != newReaderAccessibleInfo.liveSegmentId)) {
      rebuildSegmentIterators();
    }
  }

  protected void initializeCurrentSegmentIterator() {
    currentSegmentIterator = segmentEdgeAccessor.getNodeEdges(currentSegmentId, node);
  }

  abstract boolean findNextSegmentForNode();

  public boolean hasNext() {
    return (currentSegmentIterator != null && currentSegmentIterator.hasNext()) || findNextSegmentForNode();
  }

  public Long next() {
    return nextLong();
  }

  public void remove() {
    currentSegmentIterator.remove();
  }

  // EdgeIterator interface
  public long nextLong() {
    return currentSegmentIterator.nextLong();
  }

  public byte currentEdgeType() {
    return currentSegmentIterator.currentEdgeType();
  }

  @Override
  public long currentMetadata() {
    return currentSegmentIterator.currentMetadata();
  }

  public int skip(int i) {
    return currentSegmentIterator.skip(i);
  }
}
