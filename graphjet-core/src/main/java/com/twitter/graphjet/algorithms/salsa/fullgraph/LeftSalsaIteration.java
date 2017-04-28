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

import java.util.Random;

import com.twitter.graphjet.algorithms.salsa.SalsaNodeVisitor;
import com.twitter.graphjet.algorithms.salsa.SalsaRequest;
import com.twitter.graphjet.algorithms.salsa.SingleSalsaIteration;
import com.twitter.graphjet.bipartite.api.EdgeIterator;

public class LeftSalsaIteration extends SingleSalsaIteration {
  protected final SalsaInternalState salsaInternalState;
  protected final SalsaNodeVisitor.NodeVisitor nodeVisitor;

  /**
   * Default constructor that should be used for a regular iteration.
   *
   * @param salsaInternalState  is the internal state to use
   */
  public LeftSalsaIteration(SalsaInternalState salsaInternalState) {
    this(
        salsaInternalState,
        new SalsaNodeVisitor.SimpleNodeVisitor(salsaInternalState.getVisitedRightNodes())
    );
  }

  /**
   * This constructor allows passing in a custom visitor that can be used for the final iteration,
   * for instance to incorporate logic such as storing social proof.
   *
   * @param salsaInternalState  is the internal state to use
   * @param nodeVisitor         is the
   * {@link com.twitter.graphjet.algorithms.salsa.SalsaNodeVisitor.NodeVisitor} to use
   */
  public LeftSalsaIteration(
      SalsaInternalState salsaInternalState,
      SalsaNodeVisitor.NodeVisitor nodeVisitor) {
    this.salsaInternalState = salsaInternalState;
    this.nodeVisitor = nodeVisitor;
  }

  /**
   * Runs a single left-to-right SALSA iteration. This direction resets some of the random walks
   * to start again from the queryNode.
   */
  @Override
  public void runSingleIteration() {
    int numEdgesTraversed = 0;
    int numWalksResetToQueryNode = 0;
    for (long leftNode : salsaInternalState.getCurrentLeftNodes().keySet()) {
      int numWalksToStart = salsaInternalState.getCurrentLeftNodes().get(leftNode);
      int numWalks = 0;
      for (int i = 0; i < numWalksToStart; i++) {
        if (random.nextDouble() >= salsaInternalState.getSalsaRequest().getResetProbability()) {
          numWalks++;
        }
      }
      numWalksResetToQueryNode += numWalksToStart - numWalks;
      if (numWalks > 0) {
        EdgeIterator sampledRightNodes = salsaInternalState.getBipartiteGraph()
          .getRandomLeftNodeEdges(leftNode, numWalks, random);
        int leftNodeDegree = salsaInternalState.getBipartiteGraph().getLeftNodeDegree(leftNode);
        if (sampledRightNodes != null) {
          while (sampledRightNodes.hasNext()) {
            long rightNode = sampledRightNodes.nextLong();
            salsaInternalState.addNodeToCurrentRightNodes(rightNode);
            int numVisits =
                salsaInternalState.visitRightNode(nodeVisitor, leftNode,
                  rightNode, sampledRightNodes.currentEdgeType(),
                  sampledRightNodes.currentMetadata(), leftNodeDegree);
            salsaInternalState.getSalsaStats().updateVisitStatsPerRightNode(numVisits);
            numEdgesTraversed++;
          }
        }
      }
    }
    salsaInternalState.resetCurrentLeftNodes(
        salsaInternalState.getSalsaRequest().getQueryNode(), numWalksResetToQueryNode);
    salsaInternalState.getSalsaStats().addToNumRHSVisits(numEdgesTraversed);
  }

  @Override
  public void resetWithRequest(SalsaRequest salsaRequest, Random newRandom) {
    super.resetWithRequest(salsaRequest, newRandom);
    nodeVisitor.resetWithRequest(salsaRequest);
  }
}
