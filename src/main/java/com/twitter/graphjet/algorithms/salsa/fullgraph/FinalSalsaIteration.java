package com.twitter.graphjet.algorithms.salsa.fullgraph;

import com.twitter.graphjet.algorithms.salsa.SalsaNodeVisitor;

public class FinalSalsaIteration extends LeftSalsaIteration {
  /**
   * This constructs a left iteration that will also construct social proof for the
   * recommendations.
   *
   * @param salsaInternalState  is the internal state to use
   */
  public FinalSalsaIteration(SalsaInternalState salsaInternalState) {
    super(
        salsaInternalState,
        new SalsaNodeVisitor.NodeVisitorWithSocialProof(
            salsaInternalState.getVisitedRightNodes())
        );
  }
}
