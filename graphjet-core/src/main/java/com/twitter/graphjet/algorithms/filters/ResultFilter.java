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
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * Abstracts out the filtering contract for results.
 */
public abstract class ResultFilter {
  protected final Counter inputCounter;
  protected final Counter filteredCounter;
  protected final StatsReceiver scopedStatsReceiver;

  public ResultFilter(StatsReceiver statsReceiver) {
    this.scopedStatsReceiver = statsReceiver.scope(getStatsScope());
    this.inputCounter = scopedStatsReceiver.counter("input");
    this.filteredCounter = scopedStatsReceiver.counter("filtered");
  }

  /**
   * Resets the filter for the given request
   *
   * @param request is the incoming request
   */
  public abstract void resetFilter(RecommendationRequest request);

  /**
   * Provides an interface for clients to check whether a node should be filtered or not
   *
   * @param resultNode  is the result node to be checked
   * @param socialProofs is the socialProofs of different types associated with the node
   * @return true if the node should be discarded, and false if it should not be
   */
  public abstract boolean filterResult(
    long resultNode,
    SmallArrayBasedLongToDoubleMap[] socialProofs
  );

  /**
   * Provides methods for manipulating filtered stats;
   *
   * @return statsscope for stats tracking
   */
  public String getStatsScope() {
    return this.getClass().getSimpleName();
  }
}
