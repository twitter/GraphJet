package com.twitter.graphjet.bipartite;

import java.util.Random;

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.segment.LeftIndexedBipartiteGraphSegment;
import com.twitter.graphjet.math.AliasTableUtil;
import com.twitter.graphjet.math.IntArrayAliasTable;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/**
 * This iterator provides random access over edges where the edges can be spread across segments.
 * The random access is provided via first randomly picking a segment (weighted according to degree)
 * and then randomly picking an edge in the segment.
 */
public class MultiSegmentRandomIterator<T extends LeftIndexedBipartiteGraphSegment>
    extends MultiSegmentIterator<T> implements ReusableNodeRandomLongIterator {
  private final SegmentEdgeRandomAccessor<T> segmentEdgeRandomAccessor;
  private final int[] aliasTableArray;
  private final Int2IntMap numSamplesInSegment;
  private int numSamplesReturned;
  private int numSamplesNeeded;
  private Random random;
  private long[] samples;
  private byte[] sampleEdgeTypes;

  /**
   * This constructor mirror the one in it's super-class to reuse common code.
   *
   * @param multiSegmentBipartiteGraph  is the underlying {@link
   *                                    LeftIndexedMultiSegmentBipartiteGraph}
   * @param segmentEdgeRandomAccessor   is the accessor for the segments
   */
  public MultiSegmentRandomIterator(
      LeftIndexedMultiSegmentBipartiteGraph<T> multiSegmentBipartiteGraph,
      SegmentEdgeRandomAccessor<T> segmentEdgeRandomAccessor) {
    super(multiSegmentBipartiteGraph, segmentEdgeRandomAccessor);
    this.segmentEdgeRandomAccessor = segmentEdgeRandomAccessor;
    // Allocate maximum possible memory for alias table as this is going to be reused
    this.aliasTableArray = IntArrayAliasTable.generateAliasTableArray(
        multiSegmentBipartiteGraph.getMaxNumSegments());
    this.numSamplesInSegment =
        new Int2IntOpenHashMap(multiSegmentBipartiteGraph.getMaxNumSegments());
  }

  // return true if degree > 0, false otherwise
  private boolean fillAliasTableForNode(long node) {
    IntArrayAliasTable.clearAliasTableArray(aliasTableArray);
    // iterate through all the segments and get this node's degree in each one
    int index = 0;
    int averageDegree = 0;
    for (int i = oldestSegmentId; i <= liveSegmentId; i++) {
      int degree = segmentEdgeRandomAccessor.getDegreeInSegment(node, i);
      // for non-empty segments, add to alias table
      if (degree > 0) {
        IntArrayAliasTable.setEntry(aliasTableArray, index, i);
        IntArrayAliasTable.setWeight(aliasTableArray, index, degree);
        index++;
        averageDegree += degree;
      }
    }
    if (averageDegree == 0) {
      return false;
    }
    // finally set the size and weight
    IntArrayAliasTable.setAliasTableSize(aliasTableArray, index);
    IntArrayAliasTable.setAliasTableAverageWeight(aliasTableArray, averageDegree / index);
    // now we can construct the alias table
    AliasTableUtil.constructAliasTable(aliasTableArray);
    return true;
  }

  @Override
  protected void initializeCurrentSegmentIterator() {
    currentSegmentIterator = null; // this should be unused in this iterator
  }

  private void sampleFromAliasTable() {
    // rest the number of samples
    numSamplesInSegment.clear();
    // first, pick a random segment (weighted according to # edges for this node in the segment)
    for (int i = 0; i < numSamplesNeeded; i++) {
      int randomSegmentId = AliasTableUtil.getRandomSampleFromAliasTable(aliasTableArray, random);
      numSamplesInSegment.put(randomSegmentId, numSamplesInSegment.get(randomSegmentId) + 1);
    }
    // now, get the required number of random edges from each non-empty segment
    samples = new long[numSamplesNeeded];
    sampleEdgeTypes = new byte[numSamplesNeeded];
    int sampledCount = 0;
    for (int segmentId : numSamplesInSegment.keySet()) {
      // we get all the samples we need from this segment in one go
      EdgeIterator segmentRandomIterator = segmentEdgeRandomAccessor.getRandomNodeEdges(
          segmentId,
          node,
          numSamplesInSegment.get(segmentId),
          random);
      while (segmentRandomIterator.hasNext()) {
        samples[sampledCount] = segmentRandomIterator.nextLong();
        sampleEdgeTypes[sampledCount] = segmentRandomIterator.currentEdgeType();
        sampledCount++;
      }
    }
  }

  @Override
  public EdgeIterator resetForNode(long node, int numSamplesToGet, Random randomGen) {
    super.resetForNode(node); // rebuilds the segmentEdgeRandomAccessor if needed
    this.numSamplesNeeded = numSamplesToGet;
    this.numSamplesReturned = 0;
    this.random = randomGen;
    // this is the main computation-intensive operation, although it should be fast for most nodes
    // as they won't occur in too many segments
    if (!fillAliasTableForNode(node)) {
      return null;
    }
    // sample all the elements right up-front to preserve locality of access: note that this will be
    // needlessly expensive if most callers are going to get less samples than they asked for and
    // if that's the behavior we should sample lazily
    sampleFromAliasTable();
    return this;
  }

  @Override
  public long nextLong() {
    return samples[numSamplesReturned++];
  }

  @Override
  public byte currentEdgeType() {
    return sampleEdgeTypes[numSamplesReturned - 1];
  }

  @Override
  public boolean hasNext() {
    return numSamplesReturned < numSamplesNeeded;
  }
}
