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


package com.twitter.graphjet.algorithms.intersection;

import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import com.google.common.collect.Lists;

import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.SimilarityAlgorithm;
import com.twitter.graphjet.algorithms.SimilarityInfo;
import com.twitter.graphjet.algorithms.SimilarityResponse;
import com.twitter.graphjet.algorithms.filters.RelatedTweetFilterChain;
import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Runs an intersection based similarity algo.
 */
public class IntersectionSimilarity implements
    SimilarityAlgorithm<IntersectionSimilarityRequest, SimilarityResponse> {
  private final BipartiteGraph bipartiteGraph;
  private final RelatedTweetUpdateNormalization relatedTweetUpdateNormalization;

  private final Counter queryNodeDegreeIsZeroCounter;
  private final Counter lessThanMaxNumNeighborsCounter;
  private final Counter moreThanMaxNumNeighborsCounter;
  private final Counter queryNodeNullIteratorCounter;

  /**
   * An intersection based similarity algo.
   */
  public IntersectionSimilarity(BipartiteGraph bipartiteGraph,
      RelatedTweetUpdateNormalization relatedTweetUpdateNormalization,
      StatsReceiver statsReceiver) {
    this.bipartiteGraph = bipartiteGraph;
    this.relatedTweetUpdateNormalization = relatedTweetUpdateNormalization;
    StatsReceiver scopedStatsReceiver = statsReceiver.scope(this.getClass().getSimpleName());
    this.queryNodeDegreeIsZeroCounter = scopedStatsReceiver.counter("lessThanMaxNumNeighbors");
    this.lessThanMaxNumNeighborsCounter = scopedStatsReceiver.counter("lessThanMaxNumNeighbors");
    this.moreThanMaxNumNeighborsCounter = scopedStatsReceiver.counter("moreThanMaxNumNeighbors");
    this.queryNodeNullIteratorCounter = scopedStatsReceiver.counter("queryNodeNullIterator");
  }

  @Override
  public SimilarityResponse getSimilarNodes(IntersectionSimilarityRequest request, Random random) {
    return this.getSimilarNodes(request, random, RelatedTweetFilterChain.NOOPFILTERCHAIN);
  }

  @Override
  public SimilarityResponse getSimilarNodes(IntersectionSimilarityRequest request, Random random,
                                            RelatedTweetFilterChain filterChain) {
    long queryNode = request.getQueryNode();
    LongSet seedSet = request.getSeedSet();
    int queryNodeDegree = bipartiteGraph.getRightNodeDegree(queryNode);
    if (queryNodeDegree == 0) {
      queryNodeDegreeIsZeroCounter.incr();
      return null;
    }
    // first, get all neighbors of the query node and the seedset -- we sample if there are too many
    seedSet.add(queryNode);
    Long2IntMap queryNodeRightNeighbors = new Long2IntOpenHashMap(1024);
    LongSet neighborSet = new LongOpenHashSet(1024);
    for (long node : seedSet) {
      EdgeIterator currentIterator;
      if (bipartiteGraph.getRightNodeDegree(node) <= request.getMaxNumNeighbors()) {
        lessThanMaxNumNeighborsCounter.incr();
        currentIterator = bipartiteGraph.getRightNodeEdges(node);
      } else {
        moreThanMaxNumNeighborsCounter.incr();
        currentIterator =
            bipartiteGraph.getRandomRightNodeEdges(node, request.getMaxNumNeighbors(), random);
      }
      neighborSet.clear();
      int repeatedLHSNeighbors = 0;
      if (currentIterator != null) {
        while (currentIterator.hasNext()) {
          long neighbor = currentIterator.nextLong();
          if (neighborSet.contains(neighbor)) {  // check a set for neighbors to avoid double counts
            repeatedLHSNeighbors++;
            continue;
          }
          queryNodeRightNeighbors.put(neighbor, queryNodeRightNeighbors.get(neighbor) + 1);
          neighborSet.add(neighbor);
        }
      } else {
        queryNodeNullIteratorCounter.incr();
      }
    }

    // now, for each neighbor, get their neighbors (from left side) and update a map of counts
    Long2DoubleMap nodeToWeightedCooccurenceCountsMap = new Long2DoubleOpenHashMap(1024);
    Long2IntMap nodeToCooccurrenceCountsMap = new Long2IntOpenHashMap(1024);
    int repeatedRHSNeighbors = 0;
    for (long leftNeighbor : queryNodeRightNeighbors.keySet()) {
      int neighborDegree = bipartiteGraph.getLeftNodeDegree(leftNeighbor);
      if (neighborDegree < request.getMinNeighborDegree()) {
        continue;
      }
      int neighborWeight = queryNodeRightNeighbors.get(leftNeighbor);
      EdgeIterator rightNeighborsForLeftNeighbor;
      if (neighborDegree < request.getMaxNumSamplesPerNeighbor()) {
        rightNeighborsForLeftNeighbor = bipartiteGraph.getLeftNodeEdges(leftNeighbor);
      } else {
        rightNeighborsForLeftNeighbor = bipartiteGraph.getRandomLeftNodeEdges(
            leftNeighbor, request.getMaxNumSamplesPerNeighbor(), random);
      }
      neighborSet.clear();
      if (rightNeighborsForLeftNeighbor != null) { // redundant check if degree > 0
        while (rightNeighborsForLeftNeighbor.hasNext()) {
          long similarNode = rightNeighborsForLeftNeighbor.nextLong();
          if (neighborSet.contains(similarNode)) { // check a neighbors set to avoid double counts
            repeatedRHSNeighbors++;
            continue;
          }
          double currentCooccurrenceCount = nodeToWeightedCooccurenceCountsMap.get(similarNode);
          int currentRawCooccurrenceCount = nodeToCooccurrenceCountsMap.get(similarNode);
          double leftContrib = relatedTweetUpdateNormalization.computeLeftNeighborContribution(
              neighborDegree);
          leftContrib = Double.isInfinite(leftContrib) ? 0 : leftContrib;
          nodeToWeightedCooccurenceCountsMap.put(similarNode, currentCooccurrenceCount
              + neighborWeight * leftContrib);
          nodeToCooccurrenceCountsMap.put(similarNode,
            currentRawCooccurrenceCount + neighborWeight);
          neighborSet.add(similarNode);
        }
      }
    }
    // finally, normalize and select top neighbors
    int maxNumResults = Math.min(
      request.getMaxNumResults(),
      RecommendationRequest.MAX_RECOMMENDATION_RESULTS
    );
    PriorityQueue<SimilarityInfo> topResults = new PriorityQueue<SimilarityInfo>(maxNumResults);
    for (long similarNode : nodeToWeightedCooccurenceCountsMap.keySet()) {
      double cooccurrence = nodeToWeightedCooccurenceCountsMap.get(similarNode);
      int rawCooccurrence = nodeToCooccurrenceCountsMap.get(similarNode);
      int similarNodeDegree = bipartiteGraph.getRightNodeDegree(similarNode);
      if (rawCooccurrence < request.getMinCooccurrence()
          || filterChain.filter(similarNode)) {
        continue;
      }
      double normWeight = relatedTweetUpdateNormalization.computeScoreNormalization(
          cooccurrence, similarNodeDegree, queryNodeDegree);
      normWeight = Double.isInfinite(normWeight) ? 0 : normWeight;
      double score = cooccurrence * normWeight;
      if (topResults.size() < maxNumResults) {
        topResults.add(new SimilarityInfo(similarNode, score, rawCooccurrence, similarNodeDegree));
      } else if (score > topResults.peek().getWeight()) {
        topResults.poll();
        topResults.add(new SimilarityInfo(similarNode, score, rawCooccurrence, similarNodeDegree));
      }
    }
    List<SimilarityInfo> outputResults = Lists.newArrayListWithCapacity(topResults.size());
    while (!topResults.isEmpty()) {
      outputResults.add(topResults.poll());
    }
    Collections.reverse(outputResults);

    return new SimilarityResponse(outputResults, queryNodeDegree);
  }
}
