/**
 * Copyright 2017 Twitter. All rights reserved.
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


package com.twitter.graphjet.bipartite.edgepool;

import com.google.common.base.Preconditions;

import com.twitter.graphjet.hashing.BigIntArray;
import com.twitter.graphjet.hashing.BigLongArray;
import com.twitter.graphjet.hashing.IntToIntPairArrayIndexBasedMap;
import com.twitter.graphjet.hashing.IntToIntPairConcurrentHashMap;
import com.twitter.graphjet.hashing.IntToIntPairHashMap;
import com.twitter.graphjet.hashing.ShardedBigIntArray;
import com.twitter.graphjet.hashing.ShardedBigLongArray;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * An {@link AbstractRegularDegreeEdgePool} which supports edge metadata.
 */
public class WithEdgeMetadataRegularDegreeEdgePool extends AbstractRegularDegreeEdgePool {

  /**
   * This class encapsulates ALL the state that will be accessed by a reader (refer to the X, Y, Z
   * comment above). The final members are used to guarantee visibility to other threads without
   * synchronization/using volatile.
   *
   * From 'Java Concurrency in practice' by Brian Goetz, p. 349:
   *
   * "Initialization safety guarantees that for properly constructed objects, all
   *  threads will see the correct values of final fields that were set by the con-
   *  structor, regardless of how the object is published. Further, any variables
   *  that can be reached through a final field of a properly constructed object
   *  (such as the elements of a final array or the contents of a HashMap refer-
   *  enced by a final field) are also guaranteed to be visible to other threads."
   */
  public static final class WithEdgeMetadataReaderAccessibleInfo
    implements EdgePoolReaderAccessibleInfo {
      public final BigIntArray edges;
      public final BigLongArray metadata;
      // Each entry contains 2 ints for a node: position, degree
      protected final IntToIntPairHashMap nodeInfo;

      /**
       * A new instance is immediately visible to the readers due to publication safety.
       *
       * @param edges                  contains all the edges in the pool
       * @param nodeInfo               contains all the node information that is stored
       */
      public WithEdgeMetadataReaderAccessibleInfo(
        BigIntArray edges,
        BigLongArray metadata,
        IntToIntPairHashMap nodeInfo) {
        this.edges = edges;
        this.metadata = metadata;
        this.nodeInfo = nodeInfo;
      }

      public BigIntArray getEdges() {
        return edges;
      }

      public BigLongArray getMetadata() {
        return metadata;
      }

      public IntToIntPairHashMap getNodeInfo() {
        return nodeInfo;
      }
  }

  /**
   * Reserves the needed memory for a {@link WithEdgeMetadataRegularDegreeEdgePool}, and initializes
   * most of the objects that would be needed for this graph. Note that actual memory would be
   * allocated as needed, and the amount of memory needed will increase if more nodes arrive than
   * expected.
   *
   * @param expectedNumNodes  is the expected number of nodes that will be added into this pool.
   *                          The actual number of nodes can be larger and the pool will expand
   *                          itself to fit them till we hit the limit of max array size in Java.
   * @param maxDegree         is the maximum degree for a node in this pool. There will be an error
   *                          if this is violated.
   */
  public WithEdgeMetadataRegularDegreeEdgePool(
    int expectedNumNodes,
    int maxDegree,
    StatsReceiver statsReceiver
  ) {
    super(expectedNumNodes, maxDegree, statsReceiver);

    // We use a faster map in the base case
    IntToIntPairHashMap intToIntPairHashMap;
    if (maxDegree == 2) {
      intToIntPairHashMap =
        new IntToIntPairArrayIndexBasedMap(expectedNumNodes, -1, scopedStatsReceiver);
    } else {
      intToIntPairHashMap =
        new IntToIntPairConcurrentHashMap(expectedNumNodes, 0.5, -1, scopedStatsReceiver);
    }
    // This doesn't allocate memory for all the edges, which is done lazily
    readerAccessibleInfo = new WithEdgeMetadataReaderAccessibleInfo(
      // We force each node's edges to fit within a shard
      new ShardedBigIntArray(expectedNumNodes, maxDegree, 0, scopedStatsReceiver),
      // Similarly, we force each node's edge data to fit within a shard
      new ShardedBigLongArray(expectedNumNodes, maxDegree, 0, scopedStatsReceiver),
      intToIntPairHashMap
    );
  }

  @Override
  public void addEdge(int nodeA, int nodeB) {
    throw new UnsupportedOperationException("add a single edge without metadata is not supported "
      + "in WithEdgeMetadataRegularDegreeEdgePool");
  }

  @Override
  public void addEdge(int nodeA, int nodeB, long metadata) {
    long nodeAInfo;
    // Add the node if it doesn't exist
    if (readerAccessibleInfo.getNodeInfo().getBothValues(nodeA) == -1L) {
      // Note that the degree is set to 0 so this is safe to access after this point
      nodeAInfo = addNewNode(nodeA);
    } else {
      nodeAInfo = readerAccessibleInfo.getNodeInfo().getBothValues(nodeA);
    }
    int nodeADegree = getNodeDegreeFromNodeInfo(nodeAInfo);
    Preconditions.checkArgument(nodeADegree < maxDegree,
      "Exceeded the maximum degree (" + maxDegree + ") for node " + nodeA);
    int nodeAPosition = getNodePositionFromNodeInfo(nodeAInfo);
    int position = nodeAPosition + nodeADegree;
    readerAccessibleInfo.getEdges().addEntry(nodeB, position);
    readerAccessibleInfo.getMetadata().addEntry(metadata, position);
    // This is to guarantee that if a reader sees the updated degree later, they can find the edge
    currentNumEdgesStored++;
    // The order is important -- the updated degree is the ONLY way for a reader for going to the
    // new edge, so this needs to be the last update
    // since this is a volatile increment any reader will now see the updated degree
    readerAccessibleInfo.getNodeInfo().incrementSecondValue(nodeA, 1);

    numEdgesCounter.incr();
  }

  @Override
  protected long getNumberedEdgeMetadata(int position, int edgeNumber) {
    return readerAccessibleInfo.getMetadata().getEntry(position + edgeNumber);
  }

  @Override
  public long[] getMetadataShard(int node) {
    return ((ShardedBigLongArray) readerAccessibleInfo.getMetadata()).
      getShard(readerAccessibleInfo.getNodeInfo().getFirstValue(node));
  }
}
