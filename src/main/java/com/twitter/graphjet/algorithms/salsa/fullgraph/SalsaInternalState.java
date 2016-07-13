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
