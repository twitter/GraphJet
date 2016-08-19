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

import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * This filter removes tweets that have too few engagements.
 */
public class MinEngagementFilter extends RelatedTweetFilter {

  private final int minEngagement;
  private final BipartiteGraph bipartiteGraph;


  /**
   * Constructs a MinEngagementFilter with the minimum engagement and bipartite graph
   *
   * @param minEngagement minimum engagement required in order to not be filtered out
   * @param bipartiteGraph bipartite graph
   * @param statsReceiver stats
   */
  public MinEngagementFilter(int minEngagement,
                             BipartiteGraph bipartiteGraph,
                             StatsReceiver statsReceiver) {
    super(statsReceiver);
    this.minEngagement = minEngagement;
    this.bipartiteGraph = bipartiteGraph;
  }

  /**
   * filter magic
   *
   * @param tweet is the result node to be checked
   * @return true if the node should be filtered out, and false if it should not be
   */
  @Override
  public boolean filter(long tweet) {
    return bipartiteGraph.getRightNodeDegree(tweet) < minEngagement;
  }
}
