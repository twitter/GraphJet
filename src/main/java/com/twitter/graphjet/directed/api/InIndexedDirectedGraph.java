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

import com.twitter.graphjet.bipartite.api.EdgeIterator;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * <p>Interface that specifies all the read operations needed for a directed graph indexed by incoming edges. In
 * particular, any graph manipulations or graph algorithms that solely access incoming edges should only need to use
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
public interface InIndexedDirectedGraph {
  /**
   * Returns the incoming degree of a node. This operation is expected to take O(1).
   *
   * @param node the query node
   * @return the incoming degree of the query node
   */
  int getInDegree(long node);

  /**
   * Returns all incoming edges of a node. This operation can take O(n) so it should be used sparingly. Note that
   * it might be faster if the iterator is populated lazily, but that is not guaranteed.
   *
   * @param node the query node
   * @return iterator over the incoming edges of the query node, or null if the query node does not exist in the graph
   */
  @Nullable
  EdgeIterator getInEdges(long node);

  /**
   * Returns a sample of incoming edges of a node. This operation is expected to take O(numSamples). Note that this is
   * sampling with replacement.
   *
   * @param node       the query node
   * @param numSamples number of samples to return
   * @return iterator over randomly sampled incoming edges of the query node, or null if the query node does not exist
   * in the graph
   */
  @Nullable
  EdgeIterator getRandomInEdges(long node, int numSamples, Random random);
}
