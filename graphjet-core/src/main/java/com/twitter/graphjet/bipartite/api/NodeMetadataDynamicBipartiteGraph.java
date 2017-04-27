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
 * <p>Interface that specifies write operations for a dynamically updating bipartite graph that also stores
 * node metadata.</p>
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
public interface NodeMetadataDynamicBipartiteGraph {

  /**
   * Adding an edge is assumed to be an O(1) operation.
   *
   * @param leftNode          is the left hand side node in the bipartite graph.
   * @param rightNode         is the right hand side node in the bipartite graph.
   * @param edgeType          is the edge type relationship between leftNode and rightNode.
   * @param leftNodeMetadata  is the metadata associated with the left hand side node. A two
   *                          dimensional array stores the metadata, and each entry in the first
   *                          dimension array represents a unique metadata type id, starting from 0,
   *                          and each entry in the second dimension array represents a unique
   *                          metadata id. The library assumes the metadata type ids are consecutive
   *                          and small, for example, less than 10. Though a large number of
   *                          metadata types does not affect correctness, the memory usage in the
   *                          library could be further optimized. If a node does not have a
   *                          particular metadata type, client needs to initialize the corresponding
   *                          second dimension array to null.
   * @param rightNodeMetadata is the metadata associated with the right hand side node. Refer to
   *                          leftNodeMetadata for the details about the structure and usage of
   *                          metadata.
   */
  void addEdge(
    long leftNode,
    long rightNode,
    byte edgeType,
    long edgeMetadata,
    int[][] leftNodeMetadata,
    int[][] rightNodeMetadata
  );
}
