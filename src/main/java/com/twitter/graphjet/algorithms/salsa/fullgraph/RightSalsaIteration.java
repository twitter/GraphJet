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
