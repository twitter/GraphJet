package com.twitter.graphjet.algorithms.salsa.subgraph;

import com.twitter.graphjet.algorithms.salsa.SalsaNodeVisitor;

public class FinalSubgraphSalsaIteration extends LeftSubgraphSalsaIteration {
  /**
   * This constructs a left-to-right iteration on the subgraph that will also construct the social
   * proof while running the iteration
   *
   * @param salsaSubgraphInternalState  contains all the internal state for the subgraph iteration
   */
  public FinalSubgraphSalsaIteration(SalsaSubgraphInternalState salsaSubgraphInternalState) {
    super(
        salsaSubgraphInternalState,
        new SalsaNodeVisitor.WeightedNodeVisitorWithSocialProof(
            salsaSubgraphInternalState.getVisitedRightNodes()));
  }

  @Override
  public void runSingleIteration() {
    // LOG.info("SALSA: running final subgraph iteration");
    salsaSubgraphInternalState.traverseSubgraphLeftToRight(nodeVisitor);
  }
}
