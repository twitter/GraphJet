package com.twitter.graphjet.bipartite;

import java.util.Random;

import com.twitter.graphjet.bipartite.api.EdgeIterator;

public interface ReusableBipartiteGraph {
  /**
   * This operation can be O(n) so it should be used sparingly, if at all. Note that it might be
   * faster if the list is populated lazily, but that is not guaranteed.
   *
   * @param leftNode  is the left node whose edges are being fetched
   * @return the list of right nodes this leftNode is pointing to
   */
  EdgeIterator getLeftNodeEdges(long leftNode, ReusableNodeLongIterator reusableNodeLongIterator);

  /**
   * This operation can be O(n) so it should be used sparingly, if at all. Note that it might be
   * faster if the list is populated lazily, but that is not guaranteed.
   *
   * @param rightNode  is the right node whose edges are being fetched
   * @return the list of left nodes this rightNode is pointing to
   */
  EdgeIterator getRightNodeEdges(long rightNode, ReusableNodeLongIterator reusableNodeLongIterator);

  /**
   * This operation is expected to be O(numSamples). Note that this is sampling with replacement.
   *
   * @param leftNode        is the left node, numSamples of whose neighbors are chosen at random
   * @param numSamples      is the number of samples to return
   * @return numSamples     randomly chosen right neighbors of this leftNode. Note that this
   *                        returned list may contain repetitions.
   */
  EdgeIterator getRandomLeftNodeEdges(
      long leftNode,
      int numSamples,
      Random random,
      ReusableNodeRandomLongIterator reusableNodeRandomLongIterator);

  /**
   * This operation is expected to be O(numSamples). Note that this is sampling with replacement.
   *
   * @param rightNode          is the right node, numSamples of whose neighbors are chosen at random
   * @param numSamples         is the number of samples to return
   * @return numSamples randomly chosen left neighbors of this rightNode. Note that this returned
   *         list may contain repetitions.
   */
  EdgeIterator getRandomRightNodeEdges(
      long rightNode,
      int numSamples,
      Random random,
      ReusableNodeRandomLongIterator reusableNodeRandomLongIterator);
}
