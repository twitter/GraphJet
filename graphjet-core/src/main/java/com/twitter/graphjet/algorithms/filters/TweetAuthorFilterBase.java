/**
 * Copyright 2018 Twitter. All rights reserved.
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

package com.twitter.graphjet.algorithms.filters;

import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.bipartite.LeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public abstract class TweetAuthorFilterBase extends ResultFilter {

  private LongSet authoredTweets;
  private boolean isTweetAuthorsEmpty = false;

  public TweetAuthorFilterBase(
      LeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
      LongSet tweetAuthors,
      StatsReceiver statsReceiver
  ) {
    super(statsReceiver);
    this.isTweetAuthorsEmpty = tweetAuthors.isEmpty();
    this.authoredTweets = generateAuthoredByUsersNodes(leftIndexedBipartiteGraph, tweetAuthors);
  }

  public boolean isTweetAuthorsEmpty() {
    return this.isTweetAuthorsEmpty;
  }

  public boolean isTweetedByTweetAuthors(long tweetId) {
    return authoredTweets.contains(tweetId);
  }

  /**
   * Return the list of tweets authored by the input list of users
   */
  private LongSet generateAuthoredByUsersNodes(
      LeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
      LongSet tweetAuthors) {
    LongSet authoredTweets = new LongOpenHashSet();
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
    return authoredTweets;
  }

}
