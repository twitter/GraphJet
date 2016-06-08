package com.twitter.graphjet.algorithms.salsa.subgraph;

import com.twitter.graphjet.algorithms.salsa.SingleSalsaIteration;

public class RightSubgraphSalsaIteration extends SingleSalsaIteration {
  private final SalsaSubgraphInternalState salsaSubgraphInternalState;

  public RightSubgraphSalsaIteration(SalsaSubgraphInternalState salsaSubgraphInternalState) {
    this.salsaSubgraphInternalState = salsaSubgraphInternalState;
  }

  /**
   * Runs a single right-to-left SALSA iteration. This direction resets some of the random walks
   * to start again from the queryNode.
   */
  @Override
  public void runSingleIteration() {
    LOG.info("SALSA: running right subgraph iteration", EMPTY_SEQ);
    salsaSubgraphInternalState.traverseSubgraphRightToLeft();
  }
}
