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

import com.twitter.graphjet.bipartite.edgepool.EdgePool;
import com.twitter.graphjet.hashing.LongToInternalIntBiMap;

/**
 * This class encapsulates ALL the state that will be accessed by a reader of a
 * {@link BipartiteGraphSegment} (refer to the X, Y, Z comment therein). The final
 * members in this class are used to guarantee visibility to other threads without
 * synchronization/using volatile.
 *
 * NOTE: The access pattern here is slightly more involved than the simplified statement in the
 * comment referred to above since we have separate indices for left AND right side. So even if we
 * maintain correct ordering _within_ each side, the two sides might still be out of sync.
 * For instance, suppose we added the edge to the left but a reader reads this before we insert the
 * edge on the right side. This necessitates index existence checks in the reader functions.
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
public class ReaderAccessibleInfo extends LeftIndexedReaderAccessibleInfo {
  private final EdgePool rightNodeEdgePool;

  /**
   * A new instance is immediately visible to the readers due to publication safety.
   *
   * @param leftNodesToIndexBiMap   contains the mapping from external left node ids to internal ids
   * @param rightNodesToIndexBiMap  contains the mapping from external right node ids to internal
   *                              ids
   * @param leftNodeEdgePool      contains edges for the left nodes
   * @param rightNodeEdgePool     contains edges for the right nodes
   */
  public ReaderAccessibleInfo(
      LongToInternalIntBiMap leftNodesToIndexBiMap,
      LongToInternalIntBiMap rightNodesToIndexBiMap,
      EdgePool leftNodeEdgePool,
      EdgePool rightNodeEdgePool) {
    super(leftNodesToIndexBiMap, rightNodesToIndexBiMap, leftNodeEdgePool);
    this.rightNodeEdgePool = rightNodeEdgePool;
  }

  public EdgePool getRightNodeEdgePool() {
    return rightNodeEdgePool;
  }
}
