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

import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.bipartite.api.DynamicBipartiteGraph;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.api.OptimizableBipartiteGraphSegment;
import com.twitter.graphjet.bipartite.optimizer.Optimizer;
import com.twitter.graphjet.bipartite.segment.BipartiteGraphSegment;
import com.twitter.graphjet.bipartite.segment.BipartiteGraphSegmentProvider;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * This class implements a multi-segment {@link DynamicBipartiteGraph}
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
public abstract class MultiSegmentBipartiteGraph
    extends LeftIndexedMultiSegmentBipartiteGraph<BipartiteGraphSegment>
    implements BipartiteGraph, DynamicBipartiteGraph, ReusableBipartiteGraph {

  /**
   * This starts the graph off with a single segment, and additional ones are allocated as needed.
   *
   * @param maxNumSegments                            is the maximum number of segments we'll add to
   *                                                  the graph. At that point, the oldest segments
   *                                                  will start getting dropped
   * @param maxNumEdgesPerSegment                     determines when the implementation decides to
   *                                                  fork off a new segment
   * @param bipartiteGraphSegmentProvider             is used to generate new segments that are
   *                                                  added to the graph
   * @param multiSegmentReaderAccessibleInfoProvider  is use to generate the
   *                                                  {@link MultiSegmentReaderAccessibleInfo}
   *                                                  object which contains all the information
   *                                                  needed by the readers
   * @param statsReceiver                             tracks the internal stats
   */
  public MultiSegmentBipartiteGraph(
      int maxNumSegments,
      int maxNumEdgesPerSegment,
      BipartiteGraphSegmentProvider<BipartiteGraphSegment> bipartiteGraphSegmentProvider,
      MultiSegmentReaderAccessibleInfoProvider<BipartiteGraphSegment>
          multiSegmentReaderAccessibleInfoProvider,
      StatsReceiver statsReceiver) {
    super(
        maxNumSegments,
        maxNumEdgesPerSegment,
        bipartiteGraphSegmentProvider,
        multiSegmentReaderAccessibleInfoProvider,
        statsReceiver.scope("MultiSegmentBipartiteGraph"));
  }

  abstract ReusableNodeLongIterator initializeRightNodeEdgesLongIterator();

  abstract ReusableNodeRandomLongIterator initializeRightNodeEdgesRandomLongIterator();

  @Override
  public void optimize(OptimizableBipartiteGraphSegment segment) {
    Optimizer.optimizeBipartiteGraphSegment((BipartiteGraphSegment) segment);
  }

  @Override
  public int getRightNodeDegree(long rightNode) {
    // Hopefully branch prediction should make this really cheap as it'll always be false!
    if (crossMemoryBarrier() == -1) {
      return 0;
    }
    int degree = 0;
    for (BipartiteGraphSegment segment
        : multiSegmentReaderAccessibleInfoProvider
          .getMultiSegmentReaderAccessibleInfo().segments.values()) {
      degree += segment.getRightNodeDegree(rightNode);
    }
    return degree;
  }

  @Override
  public EdgeIterator getRightNodeEdges(long rightNode) {
    return getRightNodeEdges(rightNode, initializeRightNodeEdgesLongIterator());
  }

  @Override
  public EdgeIterator getRightNodeEdges(
      long rightNode, ReusableNodeLongIterator reusableNodeLongIterator) {
    return reusableNodeLongIterator.resetForNode(rightNode);
  }

  @Override
  public EdgeIterator getRandomRightNodeEdges(long rightNode, int numSamples, Random random) {
    return getRandomRightNodeEdges(
        rightNode, numSamples, random, initializeRightNodeEdgesRandomLongIterator());
  }

  @Override
  public EdgeIterator getRandomRightNodeEdges(
      long rightNode,
      int numSamples,
      Random random,
      ReusableNodeRandomLongIterator reusableNodeRandomLongIterator) {
    return reusableNodeRandomLongIterator.resetForNode(rightNode, numSamples, random);
  }
}
