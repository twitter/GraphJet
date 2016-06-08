package com.twitter.graphjet.bipartite;

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.segment.LeftIndexedBipartiteGraphSegment;

/**
 * This enables transparently using the same iterator for left or right node edges. Note that the
 * choice is sticky and is set on startup.
 */
public abstract class SegmentEdgeAccessor<T extends LeftIndexedBipartiteGraphSegment> {
  protected MultiSegmentReaderAccessibleInfo<T> readerAccessibleInfo;

  /**
   * The class only requires access to the reader information
   *
   * @param readerAccessibleInfo  encapsulates all the information accessed by a reader
   */
  public SegmentEdgeAccessor(MultiSegmentReaderAccessibleInfo<T> readerAccessibleInfo) {
    this.readerAccessibleInfo = readerAccessibleInfo;
  }

  /**
   * Wraps the left/right access to edges in a common interface
   *
   * @param segmentId  is the id of the segment being accessed
   * @param node       is the node whose edges are being fetched
   * @return an iterator over the edges of the node
   */
  public abstract EdgeIterator getNodeEdges(int segmentId, long node);

  /**
   * Rebuilds the iterators from scratch as the internal state of the graph may have changed
   * completely.
   *
   * @param oldestSegmentId       is the id of the oldest segment in the graph
   * @param liveSegmentId         is the live segment's id
   */
  public abstract void rebuildIterators(int oldestSegmentId, int liveSegmentId);

  /**
   * Allows resetting the accessor to point to point to a new object.
   *
   * @param readerAccessibleInfo  encapsulates all the information accessed by a reader
   */
  public void setReaderAccessibleInfo(MultiSegmentReaderAccessibleInfo<T> readerAccessibleInfo) {
    this.readerAccessibleInfo = readerAccessibleInfo;
  }
}
