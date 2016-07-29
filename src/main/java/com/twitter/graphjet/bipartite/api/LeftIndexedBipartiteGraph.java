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
 * This interface should specify all the read-only operations that are needed from a Left-indexed
 * Bipartite graph. In particular, any recommendation algorithms that solely access the left-hand
 * side index of a bipartite graph should only need to use this interface.
 * <p/>
 * NOTE: the graph is assumed to have nodes that are longs -- this is a very deliberate choice to
 * avoid the need for (un)boxing at any point.
 * <p/>
 * We also note the expected runtime cost for the operations here that the clients will assume,
 * and assume that implementations respect that.
 */
public interface LeftIndexedBipartiteGraph {

  /**
   * This operation is expected to be O(1).
   *
   * @param leftNode is the left node whose degree is being asked
   * @return the number of right nodes this leftNode is pointing to
   */
  int getLeftNodeDegree(long leftNode);

  /**
   * This operation can be O(n) so it should be used sparingly, if at all. Note that it might be
   * faster if the list is populated lazily, but that is not guaranteed.
   *
   * @param leftNode is the left node whose edges are being fetched
   * @return the list of right nodes this leftNode is pointing to, or null if the leftNode doesn't
   * exist in the graph
   */
  @Nullable
  EdgeIterator getLeftNodeEdges(long leftNode);

  /**
   * This operation is expected to be O(numSamples). Note that this is sampling with replacement.
   *
   * @param leftNode   is the left node, numSamples of whose neighbors are chosen at random
   * @param numSamples is the number of samples to return
   * @return numSamples     randomly chosen right neighbors of this leftNode, or null if the
   * leftNode doesn't exist in the graph. Note that the returned list may
   * contain repetitions.
   */
  @Nullable
  EdgeIterator getRandomLeftNodeEdges(long leftNode, int numSamples, Random random);
}
