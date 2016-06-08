package com.twitter.graphjet.algorithms.salsa.fullgraph;

import java.util.Random;

import scala.collection.Seq;

import com.twitter.finagle.stats.Counter;
import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.graphjet.algorithms.RecommendationAlgorithm;
import com.twitter.graphjet.algorithms.salsa.SalsaIterations;
import com.twitter.graphjet.algorithms.salsa.SalsaRequest;
import com.twitter.graphjet.algorithms.salsa.SalsaResponse;
import com.twitter.graphjet.algorithms.salsa.SalsaSelectResults;
import com.twitter.graphjet.algorithms.salsa.SalsaStats;
import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.logging.Logger;

/**
 * This is the entry point to the SALSA algorithm.
 */
public class Salsa implements RecommendationAlgorithm<SalsaRequest, SalsaResponse> {
  private static final Logger LOG = Logger.get("graph");
  private static final Seq<Object> EMPTY_SEQ = scala.collection.immutable.List$.MODULE$.empty();

  private final SalsaIterations<BipartiteGraph> salsaIterations;
  private final SalsaSelectResults<BipartiteGraph> salsaSelectResults;

  private final StatsReceiver statsReceiver;
  private final Counter numRequestsCounter;

  /**
   * This initializes all the state needed to run SALSA. Note that the object can be reused for
   * answering many different queries on the same graph, which allows for optimizations such as
   * reusing internally allocated maps etc.
   *
   * @param bipartiteGraph        is the {@link BipartiteGraph} to run SALSA on
   * @param expectedNodesToHit    is an estimate of how many nodes can be hit in SALSA. This is
   *                              purely for allocating needed memory right up front to make requests
   *                              fast.
   * @param statsReceiver         tracks the internal stats
   */
  public Salsa(
      BipartiteGraph bipartiteGraph,
      int expectedNodesToHit,
      StatsReceiver statsReceiver) {
    SalsaInternalState salsaInternalState = new SalsaInternalState(
        bipartiteGraph, new SalsaStats(), expectedNodesToHit);
    this.salsaIterations = new SalsaIterations<BipartiteGraph>(
        salsaInternalState,
        new LeftSalsaIteration(salsaInternalState),
        new RightSalsaIteration(salsaInternalState),
        new FinalSalsaIteration(salsaInternalState)
    );
    this.salsaSelectResults = new SalsaSelectResults<BipartiteGraph>(salsaInternalState);
    this.statsReceiver = statsReceiver.scope("SALSA");
    this.numRequestsCounter = this.statsReceiver.counter0("numRequests");
  }

  @Override
  public SalsaResponse computeRecommendations(
      SalsaRequest request, Random random) {
    // First, update some stats
    numRequestsCounter.incr();
    long queryNode = request.getQueryNode();
    LOG.info("SALSA: Incoming request with request_id = "
        + queryNode
        + " with numRandomWalks = "
        + request.getNumRandomWalks()
        + " with seed set size = "
        + request.getLeftSeedNodesWithWeight().size(),
        EMPTY_SEQ
    );

    LOG.info("SALSA: running the full graph algo for query node " + queryNode, EMPTY_SEQ);
    salsaIterations.runSalsaIterations(request, random);
    return salsaSelectResults.pickTopNodes();
  }
}
