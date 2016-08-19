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


package com.twitter.graphjet.algorithms.salsa.subgraph;

import java.util.Random;

import com.twitter.graphjet.algorithms.salsa.SalsaNodeVisitor;
import com.twitter.graphjet.algorithms.salsa.SalsaRequest;
import com.twitter.graphjet.algorithms.salsa.SingleSalsaIteration;

public class LeftSubgraphSalsaIteration extends SingleSalsaIteration {
  protected final SalsaSubgraphInternalState salsaSubgraphInternalState;
  protected final SalsaNodeVisitor.NodeVisitor nodeVisitor;
  protected boolean firstIteration = true;

  public LeftSubgraphSalsaIteration(
      SalsaSubgraphInternalState salsaSubgraphInternalState,
      SalsaNodeVisitor.NodeVisitor nodeVisitor) {
    this.salsaSubgraphInternalState = salsaSubgraphInternalState;
    this.nodeVisitor = nodeVisitor;
  }

  /**
   * Runs a single left-to-right SALSA iteration. This direction resets some of the random walks
   * to start again from the queryNode.
   */
  @Override
  public void runSingleIteration() {
    if (firstIteration) {
      LOG.info("SALSA: running first left subgraph iteration");
      salsaSubgraphInternalState.constructSubgraphAndTraverseOnce(nodeVisitor, random);
      firstIteration = false;
    } else {
      LOG.info("SALSA: running left subgraph iteration");
      salsaSubgraphInternalState.traverseSubgraphLeftToRight(nodeVisitor);
    }
  }

  @Override
  public void resetWithRequest(SalsaRequest salsaRequest, Random newRandom) {
    super.resetWithRequest(salsaRequest, newRandom);
    nodeVisitor.resetWithRequest(salsaRequest);
    firstIteration = true;
  }
}
