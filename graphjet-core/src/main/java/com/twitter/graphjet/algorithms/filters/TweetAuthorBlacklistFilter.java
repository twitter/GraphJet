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

package com.twitter.graphjet.algorithms.filters;

import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.bipartite.LeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.StatsReceiver;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This removes any tweet that is authored by the given list of blacklist userIds
 * If the blacklist is left unspecified or empty, no tweet will be filtered.
 */
public class TweetAuthorBlacklistFilter extends TweetAuthorFilterBase {
  public TweetAuthorBlacklistFilter(
      LeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
      LongSet tweetAuthors,
      StatsReceiver statsReceiver) {
    super(leftIndexedBipartiteGraph, tweetAuthors, statsReceiver);
  }

  @Override
  public void resetFilter(RecommendationRequest request) {}

  /**
   * @return true if resultNode is not in the authoredByUsersNodes, which means that the
   * resultNode was not authored by the specified users.
   */
  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    if (isTweetAuthorsEmpty()) {
      return false;
    }
    return isTweetedByTweetAuthors(resultNode);
  }
}
