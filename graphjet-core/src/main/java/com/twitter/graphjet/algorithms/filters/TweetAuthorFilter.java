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
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class TweetAuthorFilter extends ResultFilter {

  private boolean isWhitelistAllTweets;
  private LongSet whitelistedTweets;
  private LongSet blacklistedTweets;

  /**
   * This filter filters tweets by tweet authors, depending on whether the node author is
   * on the blacklist or the whitelist.
   *
   * - For whitelist authors, only tweets from the whitelisted authors will pass the filter.
   *   However, if the whitelist is empty, the filter will pass all tweets.
   * - For blacklist authors, tweets authored by blacklisted authors will be filtered.
   */
  public TweetAuthorFilter(
      LeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
      LongSet whitelistTweetAuthors,
      LongSet blacklistTweetAuthors,
      StatsReceiver statsReceiver) {
    super(statsReceiver);
    this.isWhitelistAllTweets = whitelistTweetAuthors.isEmpty();
    LongSet dedupedWhitelistAuthors = dedupWhitelistAuthors(whitelistTweetAuthors, blacklistTweetAuthors);
    this.whitelistedTweets = getTweetsByAuthors(leftIndexedBipartiteGraph, dedupedWhitelistAuthors);
    this.blacklistedTweets = getTweetsByAuthors(leftIndexedBipartiteGraph, blacklistTweetAuthors);
  }

  /**
   * Remove whitelist authors that exist in the blacklist to remove redundant graph traversal
   */
  private LongSet dedupWhitelistAuthors(
      LongSet whitelistTweetAuthors,
      LongSet blacklistTweetAuthors) {

    whitelistTweetAuthors.removeAll(blacklistTweetAuthors);
    return whitelistTweetAuthors;
  }

  /**
   * Return the list of tweets authored by the input list of users
   */
  private LongSet getTweetsByAuthors(
      LeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
      LongSet tweetAuthors) {
    LongSet authoredTweets = new LongOpenHashSet();
    for (long authorId: tweetAuthors) {
      EdgeIterator edgeIterator = leftIndexedBipartiteGraph.getLeftNodeEdges(authorId);
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

  private boolean isFilteredByWhitelist(long tweetId) {
    if (this.isWhitelistAllTweets) {
      return false; // If the whitelist is empty, filter nothing
    }
    return !whitelistedTweets.contains(tweetId);
  }

  private boolean isFilteredByBlacklist(long tweetId) {
    return blacklistedTweets.contains(tweetId);
  }

  @Override
  public void resetFilter(RecommendationRequest request) {
    this.isWhitelistAllTweets = false;
    this.whitelistedTweets = new LongOpenHashSet();
    this.blacklistedTweets = new LongOpenHashSet();
  }

  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    return isFilteredByWhitelist(resultNode) || isFilteredByBlacklist(resultNode);
  }
}
