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

import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * Abstracts out the filtering contract for related tweets.
 */
public abstract class RelatedTweetFilter {
  protected final Counter inputCounter;
  protected final Counter filteredCounter;

  public RelatedTweetFilter(StatsReceiver statsReceiver) {
    StatsReceiver scopedStatsReceiver = statsReceiver.scope(getStatsScope());
    this.inputCounter = scopedStatsReceiver.counter("input");
    this.filteredCounter = scopedStatsReceiver.counter("filtered");
  }

  /**
   * Provides an interface for clients to check whether a tweet should be filtered or not
   *
   * @param tweet is the result node to be checked
   * @return true if the node should be discarded, and false if it should not be
   */
  public abstract boolean filter(long tweet);

  /**
   * Provides methods for manipulating filtered stats;
   *
   * @return statsscope for stats tracking
   */
  public String getStatsScope() {
    return this.getClass().getSimpleName();
  }
}
