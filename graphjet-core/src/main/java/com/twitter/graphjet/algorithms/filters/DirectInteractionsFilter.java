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
import com.twitter.graphjet.bipartite.api.LeftIndexedBipartiteGraph;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * This filter removes the direct interactions of the query node based on the bipartite graph.
 */
public class DirectInteractionsFilter extends ResultFilter {
  private final DirectInteractions directInteractions;

  public DirectInteractionsFilter(
      LeftIndexedBipartiteGraph bipartiteGraph,
      StatsReceiver statsReceiver) {
    super(statsReceiver);
    this.directInteractions = new DirectInteractions(bipartiteGraph);
  }

  @Override
  public String getStatsScope() {
    return this.getClass().getSimpleName();
  }

  @Override
  public void resetFilter(RecommendationRequest request) {
    directInteractions.addDirectInteractions(request.getQueryNode());
  }

  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    return directInteractions.isDirectInteraction(resultNode);
  }
}
