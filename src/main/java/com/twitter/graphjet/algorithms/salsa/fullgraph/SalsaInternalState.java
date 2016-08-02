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


package com.twitter.graphjet.algorithms.salsa.fullgraph;

import com.twitter.graphjet.algorithms.salsa.CommonInternalState;
import com.twitter.graphjet.algorithms.salsa.SalsaStats;
import com.twitter.graphjet.bipartite.api.BipartiteGraph;

/**
 * This class encapsulates the state needed to run SALSA iterations.
 */
public class SalsaInternalState extends CommonInternalState<BipartiteGraph> {
  protected final BipartiteGraph bipartiteGraph;

  /**
   * Get a new instance of a fresh internal state.
   *
   * @param bipartiteGraph      is the underlying graph that SALSA runs on
   * @param salsaStats          is the stats object to use
   * @param expectedNodesToHit  is the number of nodes the random walk is expected to hit
   */
  public SalsaInternalState(
      BipartiteGraph bipartiteGraph,
      SalsaStats salsaStats,
      int expectedNodesToHit) {
    super(salsaStats, expectedNodesToHit);
    this.bipartiteGraph = bipartiteGraph;
  }

  @Override
  public BipartiteGraph getBipartiteGraph() {
    return bipartiteGraph;
  }
}
