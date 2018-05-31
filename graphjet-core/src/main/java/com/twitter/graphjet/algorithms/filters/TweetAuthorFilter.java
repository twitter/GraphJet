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
import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This filter filters tweets by tweet authors, depending on whether the node author is
 * on the blacklist or the whitelist.
 *
 * - For whitelist authors, only tweets from the whitelisted authors will pass the filter.
 *   However, if the whitelist is empty, the filter will pass all tweets, and only filter from the blacklist
 * - For blacklist authors, tweets authored by blacklisted authors will be filtered.
 */
public class TweetAuthorFilter extends ResultFilter {

  private boolean isIgnoreWhitelist;
  private LongSet whitelistedTweets;
  private LongSet blacklistedTweets;

  private Counter blacklistFilterCount = scopedStatsReceiver.counter("blacklist_filtered");
  private Counter whitelistFilterCount = scopedStatsReceiver.counter("whitelist_filtered");

  public TweetAuthorFilter(
      LeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
      LongSet whitelistTweetAuthors,
      LongSet blacklistTweetAuthors,
      StatsReceiver statsReceiver) {
    super(statsReceiver);
    this.isIgnoreWhitelist = whitelistTweetAuthors.isEmpty();
    if (this.isIgnoreWhitelist) {
      this.whitelistedTweets = new LongOpenHashSet();
      this.blacklistedTweets = getTweetsByAuthors(leftIndexedBipartiteGraph, blacklistTweetAuthors);
    } else {
      // Performance hack. Remove blacklisted authors from the whitelist, and only check whitelist
      LongSet dedupedWhitelistAuthors = dedupWhitelistAuthors(whitelistTweetAuthors, blacklistTweetAuthors);
      this.whitelistedTweets = getTweetsByAuthors(leftIndexedBipartiteGraph, dedupedWhitelistAuthors);
      this.blacklistedTweets = new LongOpenHashSet();
    }
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
    if (this.isIgnoreWhitelist) {
      return false; // If the whitelist is empty, filter nothing
    }
    boolean isFiltered = !whitelistedTweets.contains(tweetId);
    if (isFiltered) {
      whitelistFilterCount.incr();
    }
    return isFiltered;
  }

  private boolean isFilteredByBlacklist(long tweetId) {
    boolean isFiltered = blacklistedTweets.contains(tweetId);
    if (isFiltered) {
      blacklistFilterCount.incr();
    }
    return isFiltered;
  }

  @Override
  public void resetFilter(RecommendationRequest request) {}

  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    return isFilteredByWhitelist(resultNode) || isFilteredByBlacklist(resultNode);
  }
}
