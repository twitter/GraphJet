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

package com.twitter.graphjet.directed.api;

/**
 * <p>Interface that specifies all the write operations needed for a dynamically updating directed graph.</p>
 *
 * <p>Notes:</p>
 *
 * <ul>
 *
 * <li>The graph is assumed to have nodes that are identified by (primitive) longs. This is a deliberate choice to avoid
 * the need for (un)boxing at any point in accessing the graph.</li>
 *
 * <li>The expected runtime cost for each operation is noted in the documentation. Clients can assume that
 * implementations respect these costs.</li>
 *
 * </ul>
 */
public interface DynamicDirectedGraph {
  /**
   * Adds an edge to this graph. This is assumed to be an O(1) operation.
   *
   * @param srcNode      the source node
   * @param destNode     the destination node
   * @param edgeType     the edge type
   */
  void addEdge(long srcNode, long destNode, byte edgeType);

  /**
   * Removes an edge from this graph. This is assumed to be an O(1) operation.
   *
   * @param srcNode   the source node
   * @param destNode  the destination node
   */
  void removeEdge(long srcNode, long destNode);
}
