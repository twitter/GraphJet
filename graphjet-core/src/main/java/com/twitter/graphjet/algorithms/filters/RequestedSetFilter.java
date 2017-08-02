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
import com.twitter.graphjet.algorithms.TweetIDMask;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This filter applies a simple set-based filtering: given a set, filter the result if it's in the
 * set.
 */
public class RequestedSetFilter extends ResultFilter {
  private LongSet filterSet;
  private static final TweetIDMask TWEET_ID_MASK = new TweetIDMask();

  public RequestedSetFilter(StatsReceiver statsReceiver) {
    super(statsReceiver);
  }

  @Override
  public String getStatsScope() {
    return this.getClass().getSimpleName();
  }

  @Override
  public void resetFilter(RecommendationRequest request) {
    filterSet = request.getToBeFiltered();
  }

  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    return filterSet != null && filterSet.contains(TWEET_ID_MASK.restore(resultNode));
  }
}
