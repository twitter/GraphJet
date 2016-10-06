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


package com.twitter.graphjet.algorithms.randommultigraphneighbors;

import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import com.google.common.collect.Lists;

import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.filters.RelatedTweetFilterChain;
import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.math.AliasTableUtil;
import com.twitter.graphjet.math.IntArrayAliasTable;
import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

/**
 * A simple random sampling algorithm to get neighbors based on weighted seeds.
 * If S is the seed set, and N(S) is the neighborhood of S, assign d(v) for v in 
 * N(S) to be the number of incoming edges for v that originate in S. Then, this 
 * algorithm samples v proportional to d(v).
 */
public class RandomMultiGraphNeighbors {
  private static final int MULTIPER_FOR_ALIASTABLE = 100;
  private final BipartiteGraph bipartiteGraph;

  private final Counter numOfNeighborsCounter;
  private final Counter numOfUniqueNeighborsCounter;

  /**
   * Initialize the simple random sampling algorithm.
   *
   * @param bipartiteGraph            the bipartite graph to query on
   * @param statsReceiver             the stats receiver for logging
   */
  public RandomMultiGraphNeighbors(BipartiteGraph bipartiteGraph, StatsReceiver statsReceiver) {
    this.bipartiteGraph = bipartiteGraph;
    StatsReceiver scopedStatsReceiver = statsReceiver.scope(this.getClass().getSimpleName());
    this.numOfNeighborsCounter = scopedStatsReceiver.counter("numOfNeighbors");
    this.numOfUniqueNeighborsCounter = scopedStatsReceiver.counter("numOfUniqueNeighbors");
  }

  /**
   * Sample random neighbors given a request and random seed.
   * @param request                   the random neighbor request
   * @param random                    the random seed
   * @return response object encapsulating results
   */
  public RandomMultiGraphNeighborsResponse getRandomMultiGraphNeighbors(
      RandomMultiGraphNeighborsRequest request,
      Random random) {
    return this.getRandomMultiGraphNeighbors(
        request, random, RelatedTweetFilterChain.NOOPFILTERCHAIN);
  }

  /**
   * Main entry of the simple random sampling algorithm.
   * Sample random neighbors given a request, a random seed and a filter chain.
   *
   * @param request                   the random neighbor request
   * @param random                    the random seed
   * @param filterChain               the filter chain
   * @return response object encapsulating results
   */
  public RandomMultiGraphNeighborsResponse getRandomMultiGraphNeighbors(
      RandomMultiGraphNeighborsRequest request,
      Random random,
      RelatedTweetFilterChain filterChain) {
    Long2DoubleMap leftSeedNodeWithWeights = request.getLeftSeedNodesWithWeight();
    int maxNumSamples = request.getMaxNumSamples();
    int maxNumResults = Math.min(
      request.getMaxNumResults(),
      RecommendationRequest.MAX_RECOMMENDATION_RESULTS
    );

    // construct a IndexArray and AliasTableArray to sample from LHS seed nodes
    long[] indexArray = new long[leftSeedNodeWithWeights.size()];
    int[] aliasTableArray = IntArrayAliasTable.generateAliasTableArray(
      leftSeedNodeWithWeights.size());
    constructAliasTableArray(leftSeedNodeWithWeights, indexArray, aliasTableArray);

    // first, get number of samples for each of the seed nodes
    Long2IntMap nodeToNumSamples = new Long2IntOpenHashMap(leftSeedNodeWithWeights.size());
    nodeToNumSamples.defaultReturnValue(0);
    for (int i = 0; i < maxNumSamples; i++) {
      int index = AliasTableUtil.getRandomSampleFromAliasTable(aliasTableArray, random);
      long sampledLHSNode = indexArray[index];
      nodeToNumSamples.put(sampledLHSNode, nodeToNumSamples.get(sampledLHSNode) + 1);
    }

    // now actually retrieve neighbors of sampled seed nodes
    Long2IntMap seedNodeRightNeighbors = new Long2IntOpenHashMap(1024);
    seedNodeRightNeighbors.defaultReturnValue(0);
    for (Long2IntMap.Entry entry : nodeToNumSamples.long2IntEntrySet()) {
      long node = entry.getLongKey();
      int numSamples = entry.getIntValue();
      EdgeIterator currentIterator = bipartiteGraph.getRandomLeftNodeEdges(
        node, numSamples, random);
      if (currentIterator != null) {
        while (currentIterator.hasNext()) {
          long neighbor = currentIterator.nextLong();
          seedNodeRightNeighbors.put(
              neighbor, seedNodeRightNeighbors.get(neighbor) + 1);
        }
      }
    }

    // normalize and select top neighbors
    PriorityQueue<NeighborInfo> topResults = new PriorityQueue<NeighborInfo>(maxNumResults);
    numOfUniqueNeighborsCounter.incr(seedNodeRightNeighbors.size());
    for (Long2IntMap.Entry entry : seedNodeRightNeighbors.long2IntEntrySet()) {
      long neighborNode = entry.getLongKey();
      int occurrence = entry.getIntValue();
      numOfNeighborsCounter.incr(occurrence);
      if (filterChain.filter(neighborNode)) {
        continue;
      }
      int neighborNodeDegree = bipartiteGraph.getRightNodeDegree(neighborNode);
      NeighborInfo neighborInfo = new NeighborInfo(
        neighborNode, (double) occurrence / (double) maxNumSamples, neighborNodeDegree);
      addResultToPriorityQueue(topResults, neighborInfo, maxNumResults);
    }
    List<NeighborInfo> outputResults = Lists.newArrayListWithCapacity(topResults.size());
    while (!topResults.isEmpty()) {
      outputResults.add(topResults.poll());
    }
    Collections.reverse(outputResults);

    return new RandomMultiGraphNeighborsResponse(outputResults);
  }

  /**
   * Construct a index array and an alias table given the LHS seed nodes with weights.
   * The probability for a LHS seed node u to be sampled should be proportional to
   * degree(u) * Weight(u)
   *
   * @param seedsWithWeights          the LHS seed nodes with weights
   * @param indexArray                the index array for the seeds
   * @param aliasTableArray           the alias table used for sampling from the seeds
   */
  private void constructAliasTableArray(
      Long2DoubleMap seedsWithWeights,
      long[] indexArray,
      int[] aliasTableArray) {
    int index = 0;
    int averageWeight = 0;
    for (Long2DoubleMap.Entry entry: seedsWithWeights.long2DoubleEntrySet()) {
      long seed = entry.getLongKey();
      double seedWeight = entry.getDoubleValue();
      indexArray[index] = seed;
      int finalWeight = (int) (Math.round(MULTIPER_FOR_ALIASTABLE * seedWeight
        * bipartiteGraph.getLeftNodeDegree(seed)));
      IntArrayAliasTable.setEntry(aliasTableArray, index, index);
      IntArrayAliasTable.setWeight(aliasTableArray, index, finalWeight);
      averageWeight += finalWeight;
      index++;
    }
    // finally set the size and weight
    IntArrayAliasTable.setAliasTableSize(aliasTableArray, index);
    IntArrayAliasTable.setAliasTableAverageWeight(aliasTableArray, averageWeight / index);
    // now we can construct the alias table
    AliasTableUtil.constructAliasTable(aliasTableArray);
  }

  /**
   * Add a neighborInfo to the priority queue when the queue is not full
   * or the score of this neighborInfo is larger then the smallest score in the queue.
   *
   * @param topResults                the priority queue
   * @param neighborInfo              the neighborInfo to be added
   * @param maxNumResults             the maximum capacity of the queue
   */
  private void addResultToPriorityQueue(
      PriorityQueue<NeighborInfo> topResults,
      NeighborInfo neighborInfo,
      int maxNumResults) {
    if (topResults.size() < maxNumResults) {
      topResults.add(neighborInfo);
    } else if (neighborInfo.getScore() > topResults.peek().getScore()) {
      topResults.poll();
      topResults.add(neighborInfo);
    }
  }
}
