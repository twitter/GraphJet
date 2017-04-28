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

import java.util.Arrays;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twitter.graphjet.algorithms.salsa.CommonInternalState;
import com.twitter.graphjet.algorithms.salsa.SalsaNodeVisitor;
import com.twitter.graphjet.algorithms.salsa.SalsaRequest;
import com.twitter.graphjet.algorithms.salsa.SalsaStats;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.api.LeftIndexedBipartiteGraph;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;

/**
 * This version of the internal state also stores a subgraph that can run a special SALSA version.
 */
public class SalsaSubgraphInternalState extends CommonInternalState<LeftIndexedBipartiteGraph> {
  private static final Logger LOG = LoggerFactory.getLogger("graph");

  private final LeftIndexedBipartiteGraph leftIndexedBipartiteGraph;
  private long[] subgraphLeftNodes;
  private int[] subgraphLeftNodeDegree;
  private final Long2DoubleMap subgraphRightNodeDegreeReciprocal;
  private long[] subgraphEdgesArray;
  private long[] subgraphEdgeMetadataArray;
  private byte[] subgraphEdgeTypesArray;
  private int numLeftNodesAdded;
  private int numEdgesAdded;

  /**
   * Get a new instance of a fresh internal state that can store a reusable subgraph.
   *
   * @param leftIndexedBipartiteGraph        is the underlying graph that SALSA runs on
   * @param salsaStats                       is the stats object to use
   * @param expectedNodesToHit               is the number of nodes the random walk is expected to
   *                                         hit
   * @param expectedNumLeftNodes             is the expected size of the seed set
   */
  public SalsaSubgraphInternalState(
      LeftIndexedBipartiteGraph leftIndexedBipartiteGraph,
      SalsaStats salsaStats,
      int expectedNodesToHit,
      int expectedNumLeftNodes) {
    super(salsaStats, expectedNodesToHit);
    this.leftIndexedBipartiteGraph = leftIndexedBipartiteGraph;
    this.subgraphLeftNodes = new long[expectedNumLeftNodes];
    this.subgraphLeftNodeDegree = new int[expectedNumLeftNodes];
    this.subgraphEdgesArray = new long[expectedNodesToHit];
    this.subgraphEdgeMetadataArray = new long[expectedNodesToHit];
    this.subgraphEdgeTypesArray = new byte[expectedNodesToHit];
    this.subgraphRightNodeDegreeReciprocal = new Long2DoubleOpenHashMap(expectedNodesToHit);
  }

  @Override
  public void resetWithRequest(SalsaRequest incomingSalsaRequest) {
    super.resetWithRequest(incomingSalsaRequest);
    numLeftNodesAdded = 0;
    numEdgesAdded = 0;
    subgraphRightNodeDegreeReciprocal.clear();
    int seedSetSize = incomingSalsaRequest.getLeftSeedNodesWithWeight().size() + 1;
    LOG.info("SALSA: resetting internal state");
    // Need to clear only if the array is NOT resized
    if (!resizeSubgraphEdgesArray(incomingSalsaRequest.getNumRandomWalks() + seedSetSize)) {
      Arrays.fill(subgraphEdgesArray, 0);
      Arrays.fill(subgraphEdgeMetadataArray, 0);
      Arrays.fill(subgraphEdgeTypesArray, (byte) 0);
    }
    LOG.info("SALSA: subgraph edges array size = " + subgraphEdgesArray.length);
    // Need to clear only if the array is NOT resized
    if (!resizeSubgraphLeftNodes(seedSetSize)) {
      Arrays.fill(subgraphLeftNodes, 0);
      Arrays.fill(subgraphLeftNodeDegree, 0);
    }
    LOG.info("SALSA: subgraph left nodes size = " + subgraphLeftNodes.length);
    LOG.info("SALSA: subgraph left node degree size = " + subgraphLeftNodeDegree.length);
  }

  // Returns true if it resized, false if not
  private boolean resizeSubgraphEdgesArray(int numRandomWalksToRun) {
    if (numRandomWalksToRun > subgraphEdgesArray.length) {
      subgraphEdgesArray = new long[numRandomWalksToRun];
      subgraphEdgeMetadataArray = new long[numRandomWalksToRun];
      subgraphEdgeTypesArray = new byte[numRandomWalksToRun];
      return true;
    }
    return false;
  }

  // Returns true if it resized, false if not
  private boolean resizeSubgraphLeftNodes(int numLeftSeedNodes) {
    if (numLeftSeedNodes > subgraphLeftNodes.length) {
      subgraphLeftNodes = new long[numLeftSeedNodes];
      subgraphLeftNodeDegree = new int[numLeftSeedNodes];
      return true;
    }
    return false;
  }

  /**
   * Constructs the SALSA subgraph and also traverses it once.
   *
   * @param nodeVisitor  contains the updating logic for the right hand side
   * @param random       is the random number generator to use
   */
  public void constructSubgraphAndTraverseOnce(
      SalsaNodeVisitor.NodeVisitor nodeVisitor,
      Random random) {
    for (Long2IntMap.Entry entry : currentLeftNodes.long2IntEntrySet()) {
      long leftNode = entry.getLongKey();
      int numWalks = entry.getIntValue();
      EdgeIterator sampledRightNodes =
          leftIndexedBipartiteGraph.getRandomLeftNodeEdges(leftNode, numWalks, random);
      int degree = 0;
      if (sampledRightNodes != null) {
        subgraphLeftNodes[numLeftNodesAdded] = leftNode;
        while (sampledRightNodes.hasNext()) {
          long rightNode = sampledRightNodes.nextLong();
          subgraphEdgesArray[numEdgesAdded] = rightNode;
          subgraphEdgeMetadataArray[numEdgesAdded] = sampledRightNodes.currentMetadata();
          subgraphEdgeTypesArray[numEdgesAdded] = sampledRightNodes.currentEdgeType();
          numEdgesAdded++;
          subgraphRightNodeDegreeReciprocal.put(
              rightNode, subgraphRightNodeDegreeReciprocal.get(rightNode) + 1);
          int numVisits = visitRightNode(
            nodeVisitor,
            leftNode,
            rightNode,
            sampledRightNodes.currentEdgeType(),
            sampledRightNodes.currentMetadata(),
            1.0
          );
          salsaStats.updateVisitStatsPerRightNode(numVisits);
          degree++;
        }
        subgraphLeftNodeDegree[numLeftNodesAdded++] = degree;
      }
    }
    // compute degree inverse
    for (long entry : subgraphRightNodeDegreeReciprocal.keySet()) {
      subgraphRightNodeDegreeReciprocal.put(
          entry, 1.0 / subgraphRightNodeDegreeReciprocal.get(entry));
    }
    salsaStats.addToNumRHSVisits(numEdgesAdded);
    LOG.info("SALSA subgraph iteration initialized"
            + " with numLeftNodes = "
            + currentLeftNodes.size()
            + " numEdgesAdded = "
            + numEdgesAdded
            + " numVisitedRightNodes = "
            + visitedRightNodes.size()
    );
  }

  /**
   * Traverses the contructed subgraph from left to right
   *
   * @param nodeVisitor  contains the updating logic for the right hand side
   */
  public void traverseSubgraphLeftToRight(SalsaNodeVisitor.NodeVisitor nodeVisitor) {
    int currentEdgeArrayIndex = 0;
    double weightResetToQueryNode = 0;
    for (int i = 0; i < numLeftNodesAdded; i++) {
      long leftNode = subgraphLeftNodes[i];
      int degree = subgraphLeftNodeDegree[i];
      double weightPerEdge = (double) currentLeftNodes.get(leftNode) / (double) degree;
      weightResetToQueryNode += currentLeftNodes.get(leftNode) * salsaRequest.getResetProbability();
      for (int j = 0; j < degree; j++) {
        long rightNode = subgraphEdgesArray[currentEdgeArrayIndex];
        long edgeMetadata = subgraphEdgeMetadataArray[currentEdgeArrayIndex];
        byte edgeType = subgraphEdgeTypesArray[currentEdgeArrayIndex];
        currentEdgeArrayIndex++;
        int numVisits = visitRightNode(
          nodeVisitor,
          leftNode,
          rightNode,
          edgeType,
          edgeMetadata,
          weightPerEdge
        );
        salsaStats.updateVisitStatsPerRightNode(numVisits);
      }
    }
    salsaStats.addToNumRHSVisits(currentEdgeArrayIndex);
    resetCurrentLeftNodes(salsaRequest.getQueryNode(), (int) weightResetToQueryNode);
  }

  /**
   * Traverses the constructed subgraph from right to left
   */
  public void traverseSubgraphRightToLeft() {
    int currentEdgeArrayIndex = 0;
    for (int i = 0; i < numLeftNodesAdded; i++) {
      long leftNode = subgraphLeftNodes[i];
      int degree = subgraphLeftNodeDegree[i];
      double leftNodeWeight = currentLeftNodes.get(leftNode);
      for (int j = 0; j < degree; j++) {
        long rightNode = subgraphEdgesArray[currentEdgeArrayIndex++];
        leftNodeWeight += visitedRightNodes.get(rightNode).getWeight()
            * subgraphRightNodeDegreeReciprocal.get(rightNode);
      }
      currentLeftNodes.put(leftNode, (int) Math.ceil(leftNodeWeight));
    }
    salsaStats.addToNumRHSVisits(currentEdgeArrayIndex);
  }

  @Override
  public LeftIndexedBipartiteGraph getBipartiteGraph() {
    return leftIndexedBipartiteGraph;
  }
}
