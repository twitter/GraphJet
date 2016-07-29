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

import java.util.Random;

import javax.annotation.Nullable;

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeRandomIntIterator;

/**
 * This interface specifies all the read-only operations for a Bipartite graph segment that can
 * reuse iterators cleanly, i.e. without allocating and reallocating memory. It is meant to
 * supplement the high-level {@link com.twitter.graphjet.bipartite.api.BipartiteGraph} interface for
 * allowing memory reuse.
 */
public interface ReusableRightIndexedBipartiteGraphSegment {

  /**
   * This operation can be O(n) so it should be used sparingly, if at all. Note that it might be
   * faster if the list is populated lazily, but that is not guaranteed.
   *
   * @param rightNode                       is the right node whose edges are being fetched
   * @param rightNodeEdgeIterator           provides an iterator over the right node edges, but
   *                                        giving out only internal int ids for the edges
   * @param rightInternalIdToLongIterator   provides a mapping from an internal edge ids to their
   *                                        regular long ids
   * @return the list of left nodes this rightNode is pointing to, or null if the rightNode doesn't
   *         exist in the graph
   */
  @Nullable EdgeIterator getRightNodeEdges(
      long rightNode,
      ReusableNodeIntIterator rightNodeEdgeIterator,
      ReusableInternalIdToLongIterator rightInternalIdToLongIterator);

  /**
   * This operation is expected to be O(numSamples). Note that this is sampling with replacement.
   *
   * @param rightNode                       is the right node, numSamples of whose neighbors are
   *                                        chosen at random
   * @param numSamples                      is the number of samples to return
   * @param random                          is the random number generator to use for all the random
   *                                        choices to be made
   * @param rightNodeEdgeRandomIterator     provides a random iterator over the right node edges,
   *                                        but giving out only internal int ids for the edges
   * @param rightInternalIdToLongIterator   provides a mapping from an internal edge ids to their
   *                                        regular long ids
   * @return numSamples randomly chosen right neighbors of this rightNode, or null if the rightNode
   * doesn't exist in the graph. Note that the returned list may contain repetitions.
   */
  @Nullable EdgeIterator getRandomRightNodeEdges(
      long rightNode,
      int numSamples,
      Random random,
      ReusableNodeRandomIntIterator rightNodeEdgeRandomIterator,
      ReusableInternalIdToLongIterator rightInternalIdToLongIterator);
}
