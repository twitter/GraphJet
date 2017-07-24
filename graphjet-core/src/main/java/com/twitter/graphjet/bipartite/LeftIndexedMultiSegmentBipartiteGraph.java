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

import java.util.Random;

import com.twitter.graphjet.bipartite.api.DynamicBipartiteGraph;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.api.LeftIndexedBipartiteGraph;
import com.twitter.graphjet.bipartite.api.OptimizableBipartiteGraph;
import com.twitter.graphjet.bipartite.optimizer.Optimizer;
import com.twitter.graphjet.bipartite.segment.BipartiteGraphSegmentProvider;
import com.twitter.graphjet.bipartite.segment.LeftIndexedBipartiteGraphSegment;
import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 * This class implements a left-indexed only multi-segment {@link DynamicBipartiteGraph}
 * that can consume an (almost) infinite stream of incoming edges while consuming a maximum
 * amount of memory. This is achieved by storing the graph in "segments" where each segment contains
 * a limited amount of edges and the segments themselves are time-ordered. Thus, the graph maintains
 * the last k segments in memory and drops the oldest segment each time it needs to create a new
 * one.
 *
 * This class is thread-safe even though it does not do any locking: it achieves this by leveraging
 * the assumptions stated below and using a "memory barrier" between writes and reads to sync
 * updates.
 *
 * Here are the client assumptions needed to enable lock-free read/writes:
 * 1. There is a SINGLE writer thread -- this is extremely important as we don't lock during writes.
 * 2. Readers are OK reading stale data, i.e. if even if a reader thread arrives after the writer
 * thread started doing a write, the update is NOT guaranteed to be available to it.
 *
 * This class enables lock-free read/writes by guaranteeing the following:
 * 1. The writes that are done are always "safe", i.e. in no time during the writing do they leave
 *    things in a state such that a reader would either encounter an exception or do wrong
 *    computation.
 * 2. After a write is done, it is explicitly "published" such that a reader that arrives after
 *    the published write it would see updated data.
 *
 * The way this class works is that it swaps out old reader accessible data with new one with an
 * atomic reassignment. Publication safety of this reader accessible data along with immutability
 * ensures that readers both see the update right away, and can safely access the data.
 */
public abstract class
  LeftIndexedMultiSegmentBipartiteGraph<T extends LeftIndexedBipartiteGraphSegment> implements
  LeftIndexedBipartiteGraph,
  DynamicBipartiteGraph,
  ReusableLeftIndexedBipartiteGraph,
  OptimizableBipartiteGraph {

  protected final BipartiteGraphSegmentProvider<T> bipartiteGraphSegmentProvider;
  protected final Int2IntMap numEdgesInNonLiveSegmentsMap;

  // Reader accessible state is encapsulated here
  protected final MultiSegmentReaderAccessibleInfoProvider<T>
      multiSegmentReaderAccessibleInfoProvider;

  // Marked as volatile so that future stats can be published safely
  protected volatile int numEdgesInLiveSegment;

  protected final int maxNumSegments;
  protected final int maxNumEdgesPerSegment;

  private T liveSegment;

  protected final StatsReceiver statsReceiver;
  protected final Counter numEdgesSeenInAllHistoryCounter;

  /**
   * This starts the graph off with a single segment, and additional ones are allocated as needed.
   *
   * @param maxNumSegments                 is the maximum number of segments we'll add to the graph.
   *                                       At that point, the oldest segments will start getting
   *                                       dropped
   * @param maxNumEdgesPerSegment          determines when the implementation decides to fork off a
   *                                       new segment
   * @param bipartiteGraphSegmentProvider  is used to generate new segments that are added to the
   *                                       graph
   * @param statsReceiver                  tracks the internal stats
   */
  public LeftIndexedMultiSegmentBipartiteGraph(
      int maxNumSegments,
      int maxNumEdgesPerSegment,
      BipartiteGraphSegmentProvider<T> bipartiteGraphSegmentProvider,
      MultiSegmentReaderAccessibleInfoProvider<T> multiSegmentReaderAccessibleInfoProvider,
      StatsReceiver statsReceiver) {
    this.maxNumSegments = maxNumSegments;
    this.maxNumEdgesPerSegment = maxNumEdgesPerSegment;
    this.bipartiteGraphSegmentProvider = bipartiteGraphSegmentProvider;
    this.statsReceiver = statsReceiver.scope("LeftIndexedMultiSegmentBipartiteGraph");
    this.numEdgesSeenInAllHistoryCounter = this.statsReceiver.counter("numEdgesSeenInAllHistory");
    this.multiSegmentReaderAccessibleInfoProvider = multiSegmentReaderAccessibleInfoProvider;
    this.numEdgesInNonLiveSegmentsMap = new Int2IntOpenHashMap(maxNumSegments);
    addNewSegment();
  }

  abstract ReusableNodeLongIterator initializeLeftNodeEdgesLongIterator();

  abstract ReusableNodeRandomLongIterator initializeLeftNodeEdgesRandomLongIterator();

  protected int crossMemoryBarrier() {
    return multiSegmentReaderAccessibleInfoProvider.getLiveSegmentId();
  }

  protected void addNewSegment() {
    liveSegment = multiSegmentReaderAccessibleInfoProvider.addNewSegment(
        numEdgesInLiveSegment,
        numEdgesInNonLiveSegmentsMap,
        statsReceiver,
        bipartiteGraphSegmentProvider
    );
    numEdgesInLiveSegment = 0;
  }

  public T getLiveSegment() {
    return liveSegment;
  }

  public void rollForwardSegment() {
    addNewSegment();
  }

  @Override
  public void addEdge(long leftNode, long rightNode, byte edgeType) {
    // usually very cheap check is it's only false very rarely
    if (numEdgesInLiveSegment == maxNumEdgesPerSegment) {
      T oldLiveSegment = liveSegment;

      addNewSegment();

      Optimizer.submitGraphOptimizerJob(this, oldLiveSegment);
    }

    liveSegment.addEdge(leftNode, rightNode, edgeType);
    numEdgesInLiveSegment++;

    numEdgesSeenInAllHistoryCounter.incr();
  }

  @Override
  public void removeEdge(long leftNode, long rightNode) {
    throw new UnsupportedOperationException("Directly removing edges is NOT supported!");
  }

  @Override
  public int getLeftNodeDegree(long leftNode) {
    // Hopefully branch prediction should make this really cheap as it'll always be false!
    if (crossMemoryBarrier() == -1) {
      return 0;
    }
    int degree = 0;
    for (LeftIndexedBipartiteGraphSegment segment
        : multiSegmentReaderAccessibleInfoProvider
          .getMultiSegmentReaderAccessibleInfo().getSegments().values()) {
      degree += segment.getLeftNodeDegree(leftNode);
    }
    return degree;
  }

  @Override
  public EdgeIterator getLeftNodeEdges(long leftNode) {
    return getLeftNodeEdges(leftNode, initializeLeftNodeEdgesLongIterator());
  }

  @Override
  public EdgeIterator getLeftNodeEdges(
      long leftNode, ReusableNodeLongIterator reusableNodeLongIterator) {
    return reusableNodeLongIterator.resetForNode(leftNode);
  }

  @Override
  public EdgeIterator getRandomLeftNodeEdges(long leftNode, int numSamples, Random random) {
    return getRandomLeftNodeEdges(
        leftNode, numSamples, random, initializeLeftNodeEdgesRandomLongIterator());
  }

  @Override
  public EdgeIterator getRandomLeftNodeEdges(
      long leftNode,
      int numSamples,
      Random random,
      ReusableNodeRandomLongIterator reusableNodeRandomLongIterator) {
    return reusableNodeRandomLongIterator.resetForNode(leftNode, numSamples, random);
  }

  protected Int2ObjectMap<T> getSegments() {
    // Hopefully branch prediction should make this really cheap as it'll always be false!
    if (crossMemoryBarrier() == -1) {
      return null;
    }
    return multiSegmentReaderAccessibleInfoProvider
        .getMultiSegmentReaderAccessibleInfo().getSegments();
  }

  protected MultiSegmentReaderAccessibleInfo<T> getReaderAccessibleInfo() {
    // Hopefully branch prediction should make this really cheap as it'll always be false!
    if (crossMemoryBarrier() == -1) {
      return null;
    }
    return multiSegmentReaderAccessibleInfoProvider
        .getMultiSegmentReaderAccessibleInfo();
  }

  protected int getMaxNumSegments() {
    return maxNumSegments;
  }

  protected int getNumEdgesInNonLiveSegments() {
    return multiSegmentReaderAccessibleInfoProvider.getNumEdgesInNonLiveSegments();
  }
}
