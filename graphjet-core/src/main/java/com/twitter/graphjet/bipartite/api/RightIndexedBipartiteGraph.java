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
 * <p>Interface that specifies read operations for a right-indexed bipartite graph. In particular, any graph
 * manipulations or graph algorithms that solely access the right-hand side index of a bipartite graph should only need
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
public interface RightIndexedBipartiteGraph {
  /**
   * Returns the degree of a right node. This operation is expected to take O(1).
   *
   * @param rightNode the right node being queried
   * @return the number of left nodes the right node points to
   */
  int getRightNodeDegree(long rightNode);

  /**
   * Returns all edges incident on a right node. This operation can take O(n) so it should be used sparingly. Note that
   * it might be faster if the iterator is populated lazily, but that is not guaranteed.
   *
   * @param rightNode the right node whose edges are being queried
   * @return iterator over the left nodes the right query node points to, or null if the right query node does not
   * exist in the graph
   */
  @Nullable
  EdgeIterator getRightNodeEdges(long rightNode);

  /**
   * Returns a sample of edges incident on a right node. This operation is expected to take O(numSamples). Note that
   * this is sampling with replacement.
   *
   * @param rightNode the right query node
   * @param numSamples number of samples to return
   * @return iterator over randomly sampled left nodes that the right query node points to, or null if the right
   * query node does not exist in the graph. Note that the results may contain repetitions.
   */
  @Nullable
  EdgeIterator getRandomRightNodeEdges(long rightNode, int numSamples, Random random);
}
