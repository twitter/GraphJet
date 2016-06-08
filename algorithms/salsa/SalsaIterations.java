package com.twitter.graphjet.algorithms.salsa;

import java.util.Random;

import scala.collection.Seq;

import com.google.common.annotations.VisibleForTesting;

import com.twitter.graphjet.bipartite.api.LeftIndexedBipartiteGraph;
import com.twitter.logging.Logger;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This class implements the logic of SALSA iterations.
 */
public class SalsaIterations<T extends LeftIndexedBipartiteGraph> {
  private static final Logger LOG = Logger.get("graph");
  private static final Seq<Object> EMPTY_SEQ = scala.collection.immutable.List$.MODULE$.empty();

  private final CommonInternalState<T> salsaInternalState;
  private final SalsaStats salsaStats;
  private final SingleSalsaIteration leftSalsaIteration;
  private final SingleSalsaIteration rightSalsaIteration;
  private final SingleSalsaIteration finalSalsaIteration;

  /**
   * Initialize state needed to run SALSA iterations by plugging in different kinds of iterations.
   *
   * @param salsaInternalState   is the input state for the iterations to run
   * @param leftSalsaIteration   contains the logic of running the left-to-right iteration
   * @param rightSalsaIteration  contains the logic of running the right-to-left iteration
   * @param finalSalsaIteration  contains the logic of running the final left-to-right iteration
   */
  public SalsaIterations(
      CommonInternalState<T> salsaInternalState,
      SingleSalsaIteration leftSalsaIteration,
      SingleSalsaIteration rightSalsaIteration,
      SingleSalsaIteration finalSalsaIteration) {
    this.salsaInternalState = salsaInternalState;
    this.salsaStats = salsaInternalState.getSalsaStats();
    this.leftSalsaIteration = leftSalsaIteration;
    this.rightSalsaIteration = rightSalsaIteration;
    this.finalSalsaIteration = finalSalsaIteration;
  }

  /**
   * Main entry point to run the SALSA iterations. We do a monte-carlo implementation of the SALSA
   * algorithm in that we run multiple independent random walks from the queryNode, which then
   * implies that the # visits to nodes can be used for weighting. The particular implementation
   * here actually progresses all of the random walks simultaneously one step at a time. Thus, we
   * start on the left, run one step of the random walk for all walks, then start on the right, run
   * one step of the random walk for all walks and so on. The algorithm maintains visit counters
   * for nodes on the right, which are later used for picking top nodes in
   * {@link SalsaSelectResults}.
   *
   * @param salsaRequest        is the new incoming salsa request
   * @param random              is used for making all the random choices in SALSA
   */
  public void runSalsaIterations(SalsaRequest salsaRequest, Random random) {
    LOG.info("SALSA: starting to reset internal state", EMPTY_SEQ);
    resetWithRequest(salsaRequest, random);
    LOG.info("SALSA: done resetting internal state", EMPTY_SEQ);

    seedLeftSideForFirstIteration();
    LOG.info("SALSA: done seeding", EMPTY_SEQ);
    boolean isForwardIteration = true;
    SingleSalsaIteration singleSalsaIteration = leftSalsaIteration;

    for (int i = 0; i < salsaInternalState.getSalsaRequest().getMaxRandomWalkLength(); i++) {
      if (isForwardIteration) {
        singleSalsaIteration.runSingleIteration();
        singleSalsaIteration = rightSalsaIteration;
      } else {
        if (i < salsaInternalState.getSalsaRequest().getMaxRandomWalkLength() - 2) {
          singleSalsaIteration.runSingleIteration();
          singleSalsaIteration = leftSalsaIteration;
        } else {
          singleSalsaIteration.runSingleIteration();
          singleSalsaIteration = finalSalsaIteration;
        }
      }
      isForwardIteration = !isForwardIteration;
    }
  }

  @VisibleForTesting
  protected void seedLeftSideForFirstIteration() {
    long queryNode = salsaInternalState.getSalsaRequest().getQueryNode();
    salsaStats.setNumDirectNeighbors(
        salsaInternalState.getBipartiteGraph().getLeftNodeDegree(queryNode));

    Long2DoubleMap seedNodesWithWeight =
        salsaInternalState.getSalsaRequest().getLeftSeedNodesWithWeight();
    LongSet nonZeroSeedSet = salsaInternalState.getNonZeroSeedSet();

    double totalWeight = 0.0;
    for (Long2DoubleMap.Entry entry : seedNodesWithWeight.long2DoubleEntrySet()) {
      if (salsaInternalState.getBipartiteGraph().getLeftNodeDegree(entry.getLongKey())
          > 0) {
        totalWeight += entry.getDoubleValue();
        nonZeroSeedSet.add(entry.getLongKey());
      }
    }

    // If there is a pre-specified weight, we let it take precedence, but if not, then we reset
    // weights in accordance with the fraction of weight requested for the query node.
    if (!seedNodesWithWeight.containsKey(queryNode)
        && salsaInternalState.getBipartiteGraph().getLeftNodeDegree(queryNode) > 0) {
      double queryNodeWeight = 1.0;
      if (totalWeight > 0.0) {
        queryNodeWeight =
            totalWeight * salsaInternalState.getSalsaRequest().getQueryNodeWeightFraction()
                / (1.0 - salsaInternalState.getSalsaRequest().getQueryNodeWeightFraction());
      }
      seedNodesWithWeight.put(queryNode, queryNodeWeight);
      totalWeight += queryNodeWeight;
      nonZeroSeedSet.add(queryNode);
    }

    for (long leftNode : nonZeroSeedSet) {
      int numWalksToStart = (int) Math.ceil(
          seedNodesWithWeight.get(leftNode) / totalWeight
              * salsaInternalState.getSalsaRequest().getNumRandomWalks());
        salsaInternalState.getCurrentLeftNodes().put(leftNode, numWalksToStart);
    }

    salsaStats.setNumSeedNodes(salsaInternalState.getCurrentLeftNodes().size());
  }

  /**
   * Resets all internal state to answer new incoming request.
   *
   * @param salsaRequest        is the new incoming salsa request
   * @param random              is used for making all the random choices in SALSA
   */
  @VisibleForTesting
  protected void resetWithRequest(SalsaRequest salsaRequest, Random random) {
    salsaInternalState.resetWithRequest(salsaRequest);
    leftSalsaIteration.resetWithRequest(salsaRequest, random);
    rightSalsaIteration.resetWithRequest(salsaRequest, random);
    finalSalsaIteration.resetWithRequest(salsaRequest, random);
  }
}
