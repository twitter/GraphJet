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
import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * Filters a right hand side node if it has less than a minimum number of interactions.
 */
public class MinNumInteractionsFilter extends ResultFilter {
  private final BipartiteGraph bipartiteGraph;
  private final int minNumInteractions;

  public MinNumInteractionsFilter(BipartiteGraph bipartiteGraph,
                                  int minNumInteractions,
                                  StatsReceiver statsReceiver) {
    super(statsReceiver);
    this.bipartiteGraph = bipartiteGraph;
    this.minNumInteractions = minNumInteractions;
  }

  @Override
  public String getStatsScope() {
    return this.getClass().getSimpleName();
  }

  @Override
  public void resetFilter(RecommendationRequest request) {
  }

  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    return bipartiteGraph.getRightNodeDegree(resultNode) < minNumInteractions;
  }
}
