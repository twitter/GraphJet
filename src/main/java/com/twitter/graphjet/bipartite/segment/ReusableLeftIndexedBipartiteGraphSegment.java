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
public interface ReusableLeftIndexedBipartiteGraphSegment {

  /**
   * This retrieves the list of edges for a left side node.
   *
   * @param leftNode                      is the left node whose edges are being fetched
   * @param leftNodeEdgeIterator          provides an iterator over the internal integer id's for
   *                                      left node edges
   * @param leftInternalIdToLongIterator  maps the left internal ids to longs that clients use
   * @return the list of right nodes this leftNode is pointing to, or null if the leftNode doesn't
   *         exist in the graph
   */
  @Nullable
  EdgeIterator getLeftNodeEdges(
      long leftNode,
      ReusableNodeIntIterator leftNodeEdgeIterator,
      ReusableInternalIdToLongIterator leftInternalIdToLongIterator);

  /**
   * This operation is expected to be O(numSamples). Note that this is sampling with replacement.
   *
   * @param leftNode                       is the left node, numSamples of whose neighbors are
   *                                       chosen at random
   * @param numSamples                     is the number of samples to return
   * @param random                         is the random number generator to use for all the random
   *                                       choices to be made
   * @param leftNodeEdgeRandomIterator     provides a random iterator over the right node edges,
   *                                       but giving out only internal int ids for the edges
   * @param leftInternalIdToLongIterator   provides a mapping from an internal edge ids to their
   *                                       regular long ids
   * @return numSamples randomly chosen right neighbors of this leftNode, or null if the leftNode
   * doesn't exist in the graph. Note that the returned list may contain repetitions.
   */
  @Nullable EdgeIterator getRandomLeftNodeEdges(
      long leftNode,
      int numSamples,
      Random random,
      ReusableNodeRandomIntIterator leftNodeEdgeRandomIterator,
      ReusableInternalIdToLongIterator leftInternalIdToLongIterator);
}
