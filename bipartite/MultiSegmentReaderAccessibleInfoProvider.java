package com.twitter.graphjet.bipartite;

import scala.collection.Seq;

import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.graphjet.bipartite.segment.BipartiteGraphSegmentProvider;
import com.twitter.graphjet.bipartite.segment.LeftIndexedBipartiteGraphSegment;
import com.twitter.logging.Logger;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class MultiSegmentReaderAccessibleInfoProvider<T extends LeftIndexedBipartiteGraphSegment> {
  private static final Logger LOG = Logger.get("graph");
  private static final Seq<Object> EMPTY_SEQ = scala.collection.immutable.List$.MODULE$.empty();

  private MultiSegmentReaderAccessibleInfo<T> multiSegmentReaderAccessibleInfo;
  // Writes and subsequent reads across this will cross the memory barrier
  private volatile int liveSegmentId;
  private int numEdgesInNonLiveSegments;
  private final int maxNumSegments;
  private final int maxNumEdgesPerSegment;

  /**
   * The constructor tries to reserve most of the memory that is needed for the graph.
   *
   * @param maxNumSegments         is the maximum number of segments to store
   * @param maxNumEdgesPerSegment  is the maximum number of edges a segment will store
   */
  public MultiSegmentReaderAccessibleInfoProvider(int maxNumSegments, int maxNumEdgesPerSegment) {
    // this is going to swapped out right away in the addNewSegment call
    this.multiSegmentReaderAccessibleInfo = new MultiSegmentReaderAccessibleInfo<T>(
        new Int2ObjectOpenHashMap<T>(maxNumSegments),
        0,
        -1);
    this.maxNumSegments = maxNumSegments;
    this.maxNumEdgesPerSegment = maxNumEdgesPerSegment;
  }

  public MultiSegmentReaderAccessibleInfo<T> getMultiSegmentReaderAccessibleInfo() {
    return multiSegmentReaderAccessibleInfo;
  }

  /**
   * Note that some readers might still access the oldest segment for a while -- we do NOT interrupt
   * readers accessing the old segment. The expected behavior is that after a while though, none of
   * them will reference it anymore (once their computation finishes) and the JVM can then
   * garbage-collect it. This lagging reading behavior is what makes it hard to explicitly recycle
   * the already allocated memory so we need memory for k+1 segments (where k is the number of
   * segments we'll maintain).
   *
   * @param numEdgesInLiveSegment          is the number of edges in the current live segment
   * @param numEdgesInNonLiveSegmentsMap   contains a map from segment id to number of edges in it
   * @param statsReceiver                  is where the stats are updated
   * @param bipartiteGraphSegmentProvider  provides the new segment to be added
   * @return the live segment that was added
   */
  public T addNewSegment(
      int numEdgesInLiveSegment,
      Int2IntMap numEdgesInNonLiveSegmentsMap,
      StatsReceiver statsReceiver,
      BipartiteGraphSegmentProvider<T> bipartiteGraphSegmentProvider
  ) {
    final Int2ObjectMap<T> segments =
        new Int2ObjectOpenHashMap<T>(multiSegmentReaderAccessibleInfo.getSegments());
    numEdgesInNonLiveSegmentsMap.put(liveSegmentId, numEdgesInLiveSegment);
    int oldestSegmentId = multiSegmentReaderAccessibleInfo.oldestSegmentId;
    // remove a segment if we're at the limit
    if (multiSegmentReaderAccessibleInfo.getSegments().size() == maxNumSegments) {
      segments.remove(oldestSegmentId);
      numEdgesInNonLiveSegmentsMap.remove(oldestSegmentId);
      LOG.info("Removed segment " + oldestSegmentId, EMPTY_SEQ);
      oldestSegmentId++;
    } else {
      statsReceiver.counter0("numSegments").incr();
    }
    int newLiveSegmentId =  multiSegmentReaderAccessibleInfo.liveSegmentId + 1;
    // add a new segment
    T liveSegment =
        bipartiteGraphSegmentProvider.generateNewSegment(newLiveSegmentId, maxNumEdgesPerSegment);
    segments.put(newLiveSegmentId, liveSegment);
    // now make the switch for the readers -- this is immediately published and visible!
    multiSegmentReaderAccessibleInfo = new MultiSegmentReaderAccessibleInfo<T>(
            segments, oldestSegmentId, newLiveSegmentId);

    // flush the write
    liveSegmentId = newLiveSegmentId;

    numEdgesInNonLiveSegments = 0;
    for (int segmentEdgeCount : numEdgesInNonLiveSegmentsMap.values()) {
      numEdgesInNonLiveSegments += segmentEdgeCount;
    }
    LOG.info("Total number of edges in graph = " + numEdgesInNonLiveSegments, EMPTY_SEQ);
    LOG.info("Created a new segment: oldestSegmentId = " + oldestSegmentId
        + ", and liveSegmentId = " + liveSegmentId, EMPTY_SEQ);

    return liveSegment;
  }

  public int getLiveSegmentId() {
    return liveSegmentId;
  }

  public int getNumEdgesInNonLiveSegments() {
    return numEdgesInNonLiveSegments;
  }
}
