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


package com.twitter.graphjet.bipartite.segment;

import java.util.Random;

import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.api.EdgeTypeMask;
import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeRandomIntIterator;
import com.twitter.graphjet.bipartite.api.RightIndexedBipartiteGraph;
import com.twitter.graphjet.bipartite.edgepool.EdgePool;
import com.twitter.graphjet.hashing.LongToInternalIntBiMap;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * A graph segment is a bounded portion of the graph with a cap on the number of nodes and edges
 * one can store in it. This class abstracts away the logic of maintain common indexes for all
 * bipartite graph segments and in particular it is transparent to the kind of edge pools used
 * for the actual segment. A client can plug in the kind of pools it wants to use for the left
 * and right side.
 * <p/>
 * The way the graph segment here works is via having separate edge pools for left and right side
 * adjacency lists. These pools hold all the logic of addition/deletion/retrieval of edges with the
 * role of this class being to maintain a mapping from the incoming long ids for nodes to ints.
 * This allows the edge pools to deal only with ints, hence reducing the memory usage a lot since
 * edges take up most of the memory. The transformation being long to ints however does impose a
 * limit on how many id's we can store in a segment, and this is constraint mentioned above.
 * <p/>
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
 * The way this works is as follows: suppose we have some linked objects X, Y and Z that need to be
 * maintained in a consistent state. First, our setup ensures that the reader is _only_ allowed to
 * access these in a linear manner as follows: read X -> read Y -> read Z. Then, we ensure that the
 * writer behavior is to write (safe, atomic) updates to each of these in the exact opposite order:
 * write Z --flush--> write Y --flush--> write X.
 *
 * Note that the flushing ensures that if a reader sees Y then it _must_ also see the updated Z,
 * and if sees X then it _must_ also see the updated Y and Z. Further, each update itself is safe.
 * For instance, a reader can safely access an updated Z even if X and Y are not updated since the
 * updated information will only be accessible through the updated X and Y (the converse though is
 * NOT true). Together, this ensures that the reader accesses to the objects are always consistent
 * with each other and safe to access by the reader.
 */
public abstract class BipartiteGraphSegment extends LeftIndexedBipartiteGraphSegment implements
  RightIndexedBipartiteGraph,
  BipartiteGraph,
  ReusableRightIndexedBipartiteGraphSegment {
  // This object contains ALL the reader-accessible data
  private final ReaderAccessibleInfoProvider readerAccessibleInfoProvider;

  /**
   * The constructor tries to reserve most of the memory that is needed for the graph.
   *
   * @param expectedNumLeftNodes               is the expected number of left nodes that
   *                                           would be inserted in the segment
   * @param expectedNumRightNodes              is the expected number of right nodes that
   *                                           would be inserted in the segment
   * @param maxNumberOfEdges                   is the maximum number of edges to keep in
   *                                           the segment
   * @param readerAccessibleInfoProvider       provides the
   *                                           {@link ReaderAccessibleInfo}
   *                                           that encapsulates all the info that a
   *                                           reader of the segment would access
   * @param edgeTypeMask                       is the mask to encode edge type into integer node id
   * @param statsReceiver                      tracks the internal stats
   */
  public BipartiteGraphSegment(
      int expectedNumLeftNodes,
      int expectedNumRightNodes,
      int maxNumberOfEdges,
      ReaderAccessibleInfoProvider readerAccessibleInfoProvider,
      EdgeTypeMask edgeTypeMask,
      StatsReceiver statsReceiver) {
    super(
        expectedNumLeftNodes,
        expectedNumRightNodes,
        maxNumberOfEdges,
        readerAccessibleInfoProvider,
        edgeTypeMask,
        statsReceiver.scope("BipartiteGraphSegment")
    );
    this.readerAccessibleInfoProvider = readerAccessibleInfoProvider;
  }

  /**
   * Provide an iterator over right node edges for the right node pool. Note that the edges are
   * still internal id's as stored in the pool.
   * @return a new iterator
   */
  public abstract ReusableNodeIntIterator initializeRightNodeEdgesIntIterator();

  /**
   * Provide an iterator to randomly sample right node edges for the right node pool. Note that the
   * edges are still internal id's as stored in the pool.
   * @return a new iterator
   */
  public abstract ReusableNodeRandomIntIterator initializeRightNodeEdgesRandomIntIterator();

  protected EdgePool getRightNodeEdgePool() {
    return readerAccessibleInfoProvider.getReaderAccessibleInfo().getRightNodeEdgePool();
  }

  @Override
  protected void updateEdgePool(
    int leftNodeInternalId,
    int rightNodeInternalId,
    byte edgeType
  ) {
    // First we add the edges to the left pool so that it is ready to be accessed
    readerAccessibleInfoProvider.getReaderAccessibleInfo()
      .getLeftNodeEdgePool().addEdge(
        leftNodeInternalId,
        edgeTypeMask.encode(rightNodeInternalId, edgeType)
      );

    readerAccessibleInfoProvider.getReaderAccessibleInfo()
      .getRightNodeEdgePool().addEdge(
        rightNodeInternalId,
        edgeTypeMask.encode(leftNodeInternalId, edgeType)
      );
  }

  @Override
  public EdgeIterator getRightNodeEdges(long rightNode) {
    int dummy = crossMemoryBarrier();
    return getRightNodeEdges(rightNode,
                             initializeRightNodeEdgesIntIterator(),
                             initializeRightInternalIdToLongIterator());
  }

  @Override
  public EdgeIterator getRightNodeEdges(
      long rightNode,
      ReusableNodeIntIterator rightNodeEdgeIterator,
      ReusableInternalIdToLongIterator rightInternalIdToLongIterator) {
    int dummy = crossMemoryBarrier();
    int rightNodeIndex =
        readerAccessibleInfoProvider.getReaderAccessibleInfo().getIndexForRightNode(rightNode);
    if (rightNodeIndex == -1) {
      return null;
    }
    return rightInternalIdToLongIterator.resetWithIntIterator(
        readerAccessibleInfoProvider.getReaderAccessibleInfo().getRightNodeEdgePool().getNodeEdges(
            rightNodeIndex,
            rightNodeEdgeIterator));
  }

  @Override
  public int getRightNodeDegree(long rightNode) {
    int dummy = crossMemoryBarrier();
    int rightNodeIndex =
        readerAccessibleInfoProvider.getReaderAccessibleInfo().getIndexForRightNode(rightNode);
    if (rightNodeIndex == -1) {
      return 0;
    }
    return readerAccessibleInfoProvider
        .getReaderAccessibleInfo().getRightNodeEdgePool().getNodeDegree(rightNodeIndex);
  }

  @Override
  public EdgeIterator getRandomRightNodeEdges(long rightNode, int numSamples, Random random) {
    int dummy = crossMemoryBarrier();
    return getRandomRightNodeEdges(
        rightNode,
        numSamples,
        random,
        initializeRightNodeEdgesRandomIntIterator(),
        initializeRightInternalIdToLongIterator());
  }

  @Override
  public EdgeIterator getRandomRightNodeEdges(
      long rightNode,
      int numSamples,
      Random random,
      ReusableNodeRandomIntIterator rightNodeEdgeRandomIterator,
      ReusableInternalIdToLongIterator rightInternalIdToLongIterator) {
    int dummy = crossMemoryBarrier();
    int rightNodeIndex =
        readerAccessibleInfoProvider.getReaderAccessibleInfo().getIndexForRightNode(rightNode);
    if (rightNodeIndex == -1) {
      return null;
    }
    return rightInternalIdToLongIterator.resetWithIntIterator(
        readerAccessibleInfoProvider
            .getReaderAccessibleInfo().getRightNodeEdgePool().getRandomNodeEdges(
            rightNodeIndex,
            numSamples,
            random,
            rightNodeEdgeRandomIterator));
  }

  public ReaderAccessibleInfoProvider getReaderAccessibleInfoProvider() {
    return readerAccessibleInfoProvider;
  }

  public double getRightNodeEdgePoolFillPercentage() {
    return readerAccessibleInfoProvider
        .getReaderAccessibleInfo().getRightNodeEdgePool().getFillPercentage();
  }

  public LongToInternalIntBiMap getLeftNodesToIndexMap() {
    return readerAccessibleInfoProvider
        .getLeftIndexedReaderAccessibleInfo().getLeftNodesToIndexBiMap();
  }

  public LongToInternalIntBiMap getRightNodesToIndexMap() {
    return readerAccessibleInfoProvider
        .getLeftIndexedReaderAccessibleInfo().getRightNodesToIndexBiMap();
  }
}
