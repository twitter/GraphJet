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


package com.twitter.graphjet.bipartite.api;

/**
 * This interface should specify all the write operations that are needed from a dynamically
 * updating Bipartite graph. In particular, this interface is all that's needed from an
 * implementation.
 *
 * NOTE: the graph is assumed to have nodes that are longs -- this is a very deliberate choice to
 * avoid the need for (un)boxing at any point.
 *
 * We also note the expected runtime cost for the operations here that the clients will assume,
 * and assume that implementations respect that.
 */
public interface DynamicBipartiteGraph {

  /**
   * Adding an edge is assumed to be an O(1) operation.
   *
   * @param leftNode   is the left hand side node in the bipartite graph
   * @param rightNode  is the right hand side node in the bipartite graph
   * @param edgeType   is the edge type relationship between leftNode and rightNode
   */
  void addEdge(long leftNode, long rightNode, byte edgeType);

  /**
   * Removing an edge is assumed to be an O(1) operation.
   *
   * @param leftNode   is the left hand side node in the bipartite graph
   * @param rightNode  is the right hand side node in the bipartite graph
   */
  void removeEdge(long leftNode, long rightNode);
}
