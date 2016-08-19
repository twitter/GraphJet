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

import java.util.Random;

import javax.annotation.Nullable;

/**
 * <p>Interface that specifies read operations for a left-indexed bipartite graph. In particular, any graph
 * manipulations or graph algorithms that solely access the left-hand side index of a bipartite graph should only need
 * this interface.</p>
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
public interface LeftIndexedBipartiteGraph {
  /**
   * Returns the degree of a left node. This operation is expected to take O(1).
   *
   * @param leftNode the left query node
   * @return the degree of the left query node
   */
  int getLeftNodeDegree(long leftNode);

  /**
   * Returns all edges incident on a left node. This operation can take O(n) so it should be used sparingly. Note that
   * it might be faster if the iterator is populated lazily, but that is not guaranteed.
   *
   * @param leftNode the left query node
   * @return iterator over the right nodes the left query node points to, or null if the left query node does not exist
   * in the graph
   */
  @Nullable
  EdgeIterator getLeftNodeEdges(long leftNode);

  /**
   * Returns a sample of edges incident on a left node. This operation is expected to take O(numSamples). Note that this
   * is sampling with replacement.
   *
   * @param leftNode the left query node
   * @param numSamples number of samples to return
   * @return iterator over randomly sampled right nodes that the left query node points to, or null if the left query
   * node does not exist in the graph. Note that the results may contain repetitions.
   */
  @Nullable
  EdgeIterator getRandomLeftNodeEdges(long leftNode, int numSamples, Random random);
}
