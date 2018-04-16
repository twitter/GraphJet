/**
 * Copyright 2018 Twitter. All rights reserved.
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

import java.util.List;

import com.twitter.graphjet.bipartite.api.EdgeTypeMask;
import com.twitter.graphjet.bipartite.api.LeftIndexedBipartiteGraph;
import com.twitter.graphjet.bipartite.api.RightNodeMetadataDynamicBipartiteGraph;
import com.twitter.graphjet.hashing.IntToIntArrayMap;
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
public abstract class RightNodeMetadataLeftIndexedBipartiteGraphSegment
  extends LeftIndexedBipartiteGraphSegment
  implements LeftIndexedBipartiteGraph,
  RightNodeMetadataDynamicBipartiteGraph,
  ReusableLeftIndexedBipartiteGraphSegment {

  // This object contains ALL the reader-accessible data
  private final NodeMetadataLeftIndexedReaderAccessibleInfoProvider readerAccessibleInfoProvider;

  /**
   * The constructor tries to reserve most of the memory that is needed for the graph.
   *
   * @param expectedNumLeftNodes               is the expected number of left nodes that
   *                                           would be inserted in the segment
   * @param expectedNumRightNodes              is the expected number of right nodes that
   *                                           would be inserted in the segment
   * @param maxNumberOfEdges                   is the maximum number of edges to keep in
   *                                           the segment
   * @param readerAccessibleInfoProvider       provides the {@link
   *                                           NodeMetadataLeftIndexedReaderAccessibleInfoProvider}
   *                                           that encapsulates all the info that a
   *                                           reader of the segment would access
   * @param edgeTypeMask                       is the mask to encode edge type into integer node id
   * @param statsReceiver                      tracks the internal stats
   */
  public RightNodeMetadataLeftIndexedBipartiteGraphSegment(
    int expectedNumLeftNodes,
    int expectedNumRightNodes,
    int maxNumberOfEdges,
    NodeMetadataLeftIndexedReaderAccessibleInfoProvider readerAccessibleInfoProvider,
    EdgeTypeMask edgeTypeMask,
    StatsReceiver statsReceiver) {
    super(
      expectedNumLeftNodes,
      expectedNumRightNodes,
      maxNumberOfEdges,
      readerAccessibleInfoProvider,
      edgeTypeMask,
      statsReceiver.scope("RightNodeMetadataLeftIndexedBipartiteGraphSegment")
    );
    this.readerAccessibleInfoProvider = readerAccessibleInfoProvider;
  }

  @Override
  public void addEdge(
    long leftNode,
    long rightNode,
    byte edgeType,
    int[][] rightNodeMetadata
  ) {
    // We need to the nodes to the map at the very top since once we write an edge, we need to be
    // able to find it's corresponding external id. On the other hand, if a reader finds an id in
    // the map it still won't be able to get to the edges till the edge is written so this is safe.
    // The following gets the internal id's and add the nodes if they're not already in there.
    int leftNodeInternalId = readerAccessibleInfoProvider
      .getReaderAccessibleInfo().addLeftNode(leftNode);
    int rightNodeInternalId = readerAccessibleInfoProvider
      .getReaderAccessibleInfo().addRightNode(rightNode);

    // Update RightNodesToMetadataMap, which has its own memory barrier internally.
    if (rightNodeMetadata != null) {
      for (int i = 0; i < rightNodeMetadata.length; i++) {
        IntToIntArrayMap metadataMap = readerAccessibleInfoProvider.getReaderAccessibleInfo().
          getRightNodesToMetadataMap(i);
        metadataMap.put(rightNodeInternalId, rightNodeMetadata[i]);
        metadataMap.incrementFeatureValue(rightNodeInternalId, edgeType);
      }
    }

    // At this point there is an internal memory barrier deep inside LeftIndexedReaderAccessibleInfo
    // so that node mappings are visible if the edge is visible

    // Now we can add the edge

    // First we add the edges to the left pool so that it is ready to be accessed
    readerAccessibleInfoProvider
      .getLeftIndexedReaderAccessibleInfo().getLeftNodeEdgePool().addEdge(
      leftNodeInternalId, edgeTypeMask.encode(rightNodeInternalId, edgeType));

    // Finally, explicitly flush the edge write so that the edge is visible to the readers
    currentNumEdges++;

    numEdgesCounter.incr();
  }

  public List<IntToIntArrayMap> getLeftNodesToMetadataMap() {
    return null;
  }

  public List<IntToIntArrayMap> getRightNodesToMetadataMap() {
    return readerAccessibleInfoProvider
      .getReaderAccessibleInfo().getRightNodesToMetadataMap();
  }
}

