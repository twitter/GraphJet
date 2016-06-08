package com.twitter.graphjet.algorithms.salsa.fullgraph;

import com.twitter.graphjet.algorithms.salsa.SingleSalsaIteration;
import com.twitter.graphjet.bipartite.api.EdgeIterator;

public class RightSalsaIteration extends SingleSalsaIteration {
  protected final SalsaInternalState salsaInternalState;

  public RightSalsaIteration(SalsaInternalState salsaInternalState) {
    this.salsaInternalState = salsaInternalState;
  }

  /**
   * Runs a single right-to-left SALSA iteration.
   */
  @Override
  public void runSingleIteration() {
    for (long rightNode : salsaInternalState.getCurrentRightNodes().keySet()) {
      int numWalks = salsaInternalState.getCurrentRightNodes().get(rightNode);
      EdgeIterator sampledLeftNodes = salsaInternalState.getBipartiteGraph()
          .getRandomRightNodeEdges(rightNode, numWalks, random);
      if (sampledLeftNodes != null) {
        while (sampledLeftNodes.hasNext()) {
          salsaInternalState.addNodeToCurrentLeftNodes(sampledLeftNodes.nextLong());
        }
      }
    }
    salsaInternalState.clearCurrentRightNodes();
  }
}
