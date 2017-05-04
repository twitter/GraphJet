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


package com.twitter.graphjet.bipartite.edgepool;

import java.util.Random;

import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeRandomIntIterator;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * An edge pool contains all the operations related to edges: it decides how to allocate memory for
 * the edges, how to assign edges for nodes (keeping track of node pointers), and provides retrieval
 * operations on edges. Note that this is only a single side of a bipartite graph, i.e. although
 * this pool stores all the edges, it only indexes one direction and hence can only provide
 * operations for that direction.
 */
public interface EdgePool {

  /**
   * Retrieving the node degree is expected to be an O(1) operation.
   *
   * @param node  is the node to lookup
   * @return the degree of the node
   */
  int getNodeDegree(int node);

  /**
   * The iterator is expected to be lazy, so each next() call should be O(1).
   *
   * @param node  is the node whose edges are being looked up
   * @return an iterator for the edges of this node
   */
  IntIterator getNodeEdges(int node);

  /**
   * The iterator is expected to be lazy, so the randomness should be initiated only as needed. And
   * each next() call should be O(1). Note that this is sampling WITH replacement. Thus, if the
   * client asks for more samples than the degree of the node then there are guaranteed to be
   * repeats.
   *
   * @param node        is the node whose edges are being sampled
   * @param numSamples  is the number of samples to be returned
   * @param random      is the random number generator to be used
   * @return a sample of the edges of this node
   */
  IntIterator getRandomNodeEdges(int node, int numSamples, Random random);

  /**
   * The iterator is expected to be lazy, so each next() call should be O(1).
   *
   * @param node                     is the node whose edges are being fetched
   * @param reusableNodeIntIterator  is the iterator to reset and reuse for returning edges
   * @return the reusableNodeIntIterator iterator that has been reset to point to the edges of this
   *         node
   */
  IntIterator getNodeEdges(int node, ReusableNodeIntIterator reusableNodeIntIterator);

  /**
   * The iterator is expected to be lazy, so the randomness should be initiated only as needed. And
   * each next() call should be O(1). Note that this is sampling WITH replacement. Thus, if the
   * client asks for more samples than the degree of the node then there are guaranteed to be
   * repeats.
   *
   * @param node                           is the node whose edges are being sampled
   * @param numSamples                     is the number of samples to be returned
   * @param random                         is the random number generator to be used
   * @param reusableNodeRandomIntIterator  is the iterator to reset and reuse for returning samples
   * @return the reusableNodeRandomIntIterator iterator that has been reset to point to the samples
   *         of edges of this node
   */
  IntIterator getRandomNodeEdges(
      int node,
      int numSamples,
      Random random,
      ReusableNodeRandomIntIterator reusableNodeRandomIntIterator);

  /**
   * Adding a single edge is expected to be O(1) and ideally it should entail minimal memory
   * allocation to make as little garbage as possible. If the client tries to add more edges than
   * there is memory for, or if a condition (such as max degree) is being violated, then this will
   * throw an exception.
   * TODO (aneesh): add an exception type for this.
   *
   * @param nodeA  is the node whose edges are indexed
   * @param nodeB  is the other side node
   * @param metadata is the edge metadata
   */
  void addEdge(int nodeA, int nodeB, long metadata);

  /**
   * Adding a single edge is expected to be O(1) and ideally it should entail minimal memory
   * allocation to make as little garbage as possible. If the client tries to add more edges than
   * there is memory for, or if a condition (such as max degree) is being violated, then this will
   * throw an exception.
   * TODO (aneesh): add an exception type for this.
   *
   * @param nodeA  is the node whose edges are indexed
   * @param nodeB  is the other side node
   */
  void addEdge(int nodeA, int nodeB);

  /**
   * Edge removal is also expected to be O(1). For now, we assume that this operation is
   * unsupported and hence the implementation can (and likely will) return an
   * {@link UnsupportedOperationException}.
   *
   * @param nodeA  is the node whose edges are indexed
   * @param nodeB  is the other side node
   */
  void removeEdge(int nodeA, int nodeB);

  /**
   * Optimized edge pool flag.
   *
   * @return whether the EdgePool is an {@link OptimizedEdgePool}
   */
  boolean isOptimized();

  /**
   * The fill percentage is the percentage of memory allocated that is being occupied. This should
   * be very cheap to get and will be exported as a stat counter.
   *
   * @return the fill percentage
   */
  double getFillPercentage();
}
