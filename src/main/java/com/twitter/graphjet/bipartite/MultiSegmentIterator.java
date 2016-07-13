package com.twitter.graphjet.bipartite;

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.segment.LeftIndexedBipartiteGraphSegment;

/**
 * This iterator process segments in the chronological direction, i.e. it accesses edges
 * from the oldest segment first. The ordering of edges within each segment is left up to the
 * segment, but usually that is chronological so this likely has the nice side-effect of providing
 * chronological ordering over all the edges.
 */
public class MultiSegmentIterator<T extends LeftIndexedBipartiteGraphSegment>
    implements EdgeIterator, ReusableNodeLongIterator {
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

  @Override
  public EdgeIterator resetForNode(long inputNode) {
    this.node = inputNode;
    MultiSegmentReaderAccessibleInfo<T> newReaderAccessibleInfo =
        multiSegmentBipartiteGraph.getReaderAccessibleInfo();
    // this is violated rarely: only when we drop or add segments
    if ((oldestSegmentId != newReaderAccessibleInfo.oldestSegmentId)
        || (liveSegmentId != newReaderAccessibleInfo.liveSegmentId)) {
      rebuildSegmentIterators();
    }
    currentSegmentId = oldestSegmentId;
    initializeCurrentSegmentIterator();
    return this;
  }

  protected void initializeCurrentSegmentIterator() {
    currentSegmentIterator = segmentEdgeAccessor.getNodeEdges(currentSegmentId, node);
  }

  @Override
  public long nextLong() {
    return currentSegmentIterator.nextLong();
  }

  @Override
  public byte currentEdgeType() {
    return currentSegmentIterator.currentEdgeType();
  }

  @Override
  public int skip(int i) {
    return currentSegmentIterator.skip(i);
  }

  // This finds segments in chronological order: returns false if it cannot find a non-empty
  // next segment for the node
  private boolean findNextSegmentForNode() {
    while ((currentSegmentIterator == null || !currentSegmentIterator.hasNext())
        && (currentSegmentId < liveSegmentId)) {
      currentSegmentIterator = segmentEdgeAccessor.getNodeEdges(++currentSegmentId, node);
    }
    return currentSegmentIterator != null && currentSegmentIterator.hasNext();
  }

  @Override
  public boolean hasNext() {
    return (currentSegmentIterator != null && currentSegmentIterator.hasNext())
        || findNextSegmentForNode();
  }

  @Override
  public Long next() {
    return nextLong();
  }

  @Override
  public void remove() {
    currentSegmentIterator.remove();
  }
}
