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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twitter.graphjet.algorithms.RecommendationAlgorithm;
import com.twitter.graphjet.algorithms.salsa.SalsaIterations;
import com.twitter.graphjet.algorithms.salsa.SalsaNodeVisitor;
import com.twitter.graphjet.algorithms.salsa.SalsaRequest;
import com.twitter.graphjet.algorithms.salsa.SalsaResponse;
import com.twitter.graphjet.algorithms.salsa.SalsaSelectResults;
import com.twitter.graphjet.algorithms.salsa.SalsaStats;
import com.twitter.graphjet.bipartite.api.LeftIndexedBipartiteGraph;
import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * This is the entry point to the Subgraph version of the SALSA algorithm.
 */
public class SubgraphSalsa implements RecommendationAlgorithm<SalsaRequest, SalsaResponse> {
  private static final Logger LOG = LoggerFactory.getLogger("graph");

  private final SalsaIterations<LeftIndexedBipartiteGraph> salsaIterationsSubgraph;
  private final SalsaSelectResults<LeftIndexedBipartiteGraph> salsaSelectResultsSubgraph;

  private final StatsReceiver statsReceiver;
  private final Counter numRequestsCounter;

  /**
   * This initializes all the state needed to run SALSA. Note that the object can be reused for
   * answering many different queries on the same graph, which allows for optimizations such as
   * reusing internally allocated maps etc.
   *
   * @param bipartiteGraph        is the {@link LeftIndexedBipartiteGraph} to run SALSA on
   * @param expectedNodesToHit    is an estimate of how many nodes can be hit in SALSA. This is
   *                              purely for allocating needed memory right up front to make requests
   *                              fast.
   * @param expectedNumLeftNodes  is the expected size of the seed set
   * @param statsReceiver         tracks the internal stats
   */
  public SubgraphSalsa(
      LeftIndexedBipartiteGraph bipartiteGraph,
      int expectedNodesToHit,
      int expectedNumLeftNodes,
      StatsReceiver statsReceiver) {
    SalsaSubgraphInternalState salsaSubgraphInternalState = new SalsaSubgraphInternalState(
        bipartiteGraph,
        new SalsaStats(),
        expectedNodesToHit,
        expectedNumLeftNodes);
    this.salsaIterationsSubgraph = new SalsaIterations<LeftIndexedBipartiteGraph>(
        salsaSubgraphInternalState,
        new LeftSubgraphSalsaIteration(
            salsaSubgraphInternalState,
            new SalsaNodeVisitor.WeightedNodeVisitor(
                salsaSubgraphInternalState.getVisitedRightNodes())),
        new RightSubgraphSalsaIteration(salsaSubgraphInternalState),
        new FinalSubgraphSalsaIteration(salsaSubgraphInternalState)
    );
    this.salsaSelectResultsSubgraph =
        new SalsaSelectResults<LeftIndexedBipartiteGraph>(salsaSubgraphInternalState);
    this.statsReceiver = statsReceiver.scope("SubgraphSALSA");
    this.numRequestsCounter = this.statsReceiver.counter("numRequests");
  }

  @Override
  public SalsaResponse computeRecommendations(
      SalsaRequest request, Random random) {
    // First, update some stats
    numRequestsCounter.incr();
    long queryNode = request.getQueryNode();
    LOG.info("SubgraphSALSA: Incoming request with request_id = "
            + queryNode
            + " with numRandomWalks = "
            + request.getNumRandomWalks()
            + " with seed set size = "
            + request.getLeftSeedNodesWithWeight().size()
    );

    salsaIterationsSubgraph.runSalsaIterations(request, random);
    return salsaSelectResultsSubgraph.pickTopNodes();
  }
}
