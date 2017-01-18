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


package com.twitter.graphjet.algorithms;

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.LeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.StatsReceiver;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class TweetAuthorFilter extends ResultFilter {
  private LongSet authoredTweets = new LongOpenHashSet();

  public TweetAuthorFilter(
      LeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
      LongSet tweetAuthors,
      StatsReceiver statsReceiver) {
    super(statsReceiver);
    generateAuthoredByUsersNodes(leftIndexedBipartiteGraph, tweetAuthors);
  }

  private void generateAuthoredByUsersNodes(
      LeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
      LongSet tweetAuthors) {
    for (long leftNode: tweetAuthors) {
      EdgeIterator edgeIterator = leftIndexedBipartiteGraph.getLeftNodeEdges(leftNode);
      if (edgeIterator == null) {
        continue;
      }

      // Sequentially iterating through the latest MAX_EDGES_PER_NODE edges per node
      int numEdgesPerNode = 0;
      while (edgeIterator.hasNext() && numEdgesPerNode++ < RecommendationRequest.MAX_EDGES_PER_NODE) {
        long rightNode = edgeIterator.nextLong();
        byte edgeType = edgeIterator.currentEdgeType();
        if (edgeType == RecommendationRequest.AUTHOR_SOCIAL_PROOF_TYPE) {
          authoredTweets.add(rightNode);
        }
      }
    }
  }

  @Override
  public void resetFilter(RecommendationRequest request) {}

  /**
   * @return true if resultNode is not in the authoredByUsersNodes, which means that the
   * resultNode was not authored by the specified users.
   */
  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    return !authoredTweets.contains(resultNode);
  }
}
