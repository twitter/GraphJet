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
 * {@link LeftIndexedBipartiteGraphSegment} (refer to the X, Y, Z comment therein). The final
 * members in this class are used to guarantee visibility to other threads without
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
public class LeftIndexedReaderAccessibleInfo {
  // These are the first objects to be accessed by the reader and they gate access to the pools
  private final LongToInternalIntBiMap leftNodesToIndexBiMap;
  private final LongToInternalIntBiMap rightNodesToIndexBiMap;
  // The edge pool is accessed next
  private final EdgePool leftNodeEdgePool;

  /**
   * A new instance is immediately visible to the readers due to publication safety.
   *
   * @param leftNodesToIndexBiMap   maps external left node ids to internal ids and vice versa
   * @param rightNodesToIndexBiMap  maps external right node ids to internal ids and vice versa
   * @param leftNodeEdgePool        contains edges for the left nodes
   */
  public LeftIndexedReaderAccessibleInfo(
      LongToInternalIntBiMap leftNodesToIndexBiMap,
      LongToInternalIntBiMap rightNodesToIndexBiMap,
      EdgePool leftNodeEdgePool) {
    this.leftNodesToIndexBiMap = leftNodesToIndexBiMap;
    this.rightNodesToIndexBiMap = rightNodesToIndexBiMap;
    this.leftNodeEdgePool = leftNodeEdgePool;
  }

  public int getIndexForLeftNode(long node) {
    return leftNodesToIndexBiMap.get(node);
  }

  public int addLeftNode(long node) {
    return leftNodesToIndexBiMap.put(node);
  }

  public int getIndexForRightNode(long node) {
    return rightNodesToIndexBiMap.get(node);
  }

  public int addRightNode(long node) {
    return rightNodesToIndexBiMap.put(node);
  }

  public LongToInternalIntBiMap getLeftNodesToIndexBiMap() {
    return leftNodesToIndexBiMap;
  }

  public EdgePool getLeftNodeEdgePool() {
    return leftNodeEdgePool;
  }

  public LongToInternalIntBiMap getRightNodesToIndexBiMap() {
    return rightNodesToIndexBiMap;
  }
}
