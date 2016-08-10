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


package com.twitter.graphjet.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.algorithms.filters.MinEngagementFilter;
import com.twitter.graphjet.algorithms.filters.RelatedTweetFilter;
import com.twitter.graphjet.algorithms.filters.RelatedTweetFilterChain;
import com.twitter.graphjet.algorithms.randommultigraphneighbors.NeighborInfo;
import com.twitter.graphjet.algorithms.randommultigraphneighbors.RandomMultiGraphNeighbors;
import com.twitter.graphjet.algorithms.randommultigraphneighbors.RandomMultiGraphNeighborsRequest;
import com.twitter.graphjet.algorithms.randommultigraphneighbors.RandomMultiGraphNeighborsResponse;
import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;

public class RandomMultiGraphNeighborsTest {
  @Test
  public void testRandomMultiGraphNeighborsWithGraph() throws Exception {
    BipartiteGraph bipartiteGraph = BipartiteGraphTestHelper.buildSmallTestBipartiteGraphSegment();
    int maxNumNeighbors = 1000;
    int maxNumResults = 3;
    long randomSeed = 5298057403198457L;
    Long2DoubleMap leftSeedNodesWithWeight = new Long2DoubleOpenHashMap(3);
    leftSeedNodesWithWeight.put(1, 0.4);
    leftSeedNodesWithWeight.put(2, 0.3);
    leftSeedNodesWithWeight.put(3, 0.3);

    RandomMultiGraphNeighborsRequest neighborRequest = new RandomMultiGraphNeighborsRequest(
        leftSeedNodesWithWeight,
        maxNumNeighbors,
        maxNumResults);

    final List<NeighborInfo> expectedResults =
        Lists.newArrayList(
            new NeighborInfo(5, 0.206, 3),
            new NeighborInfo(2, 0.155, 2),
            new NeighborInfo(10, 0.117, 2)
        );

    // Should be in sorted order of weight
    RandomMultiGraphNeighbors randomMultiGraphNeighbors = new RandomMultiGraphNeighbors(
        bipartiteGraph,
        new NullStatsReceiver());
    Random random = new Random(randomSeed);
    RandomMultiGraphNeighborsResponse neighborResponse =
        randomMultiGraphNeighbors.getRandomMultiGraphNeighbors(neighborRequest, random);
    List<NeighborInfo> results =
        Lists.newArrayList(neighborResponse.getNeighborNodes());

    assertEquals(expectedResults, results);

    final List<NeighborInfo> expectedFilteredResults =
        Lists.newArrayList(
            new NeighborInfo(5, 0.189, 3)
        );

    // also check whether minimum result degree filter is enforced
    MinEngagementFilter minEngagementFilter = new MinEngagementFilter(3, bipartiteGraph,
        new NullStatsReceiver());
    ArrayList<RelatedTweetFilter> filters = new ArrayList<RelatedTweetFilter>(1);
    filters.add(minEngagementFilter);
    RelatedTweetFilterChain filterChain = new RelatedTweetFilterChain(filters);

    RandomMultiGraphNeighborsResponse filteredResponse =
        randomMultiGraphNeighbors.getRandomMultiGraphNeighbors(
          neighborRequest, random, filterChain);
    List<NeighborInfo> filteredResults =
        Lists.newArrayList(filteredResponse.getNeighborNodes());

    assertEquals(expectedFilteredResults, filteredResults);
  }

  @Test
  public void testRandomMultiGraphNeighborsWithLargeGraph() throws Exception {
    int maxNumNeighbors = 100000;
    int maxNumResults = 3;
    int leftSize = 100;
    int rightSize = 1000;
    double edgeProbability = 0.3;
    long randomSeed = 5298057403198457L;
    Random random = new Random(randomSeed);

    BipartiteGraph bipartiteGraph = BipartiteGraphTestHelper.buildRandomBipartiteGraph(
        leftSize, rightSize, edgeProbability, random);
    Long2DoubleMap leftSeedNodesWithWeight = new Long2DoubleOpenHashMap(3);
    leftSeedNodesWithWeight.put(1, 0.4);
    leftSeedNodesWithWeight.put(2, 0.3);
    leftSeedNodesWithWeight.put(3, 0.2);
    leftSeedNodesWithWeight.put(4, 0.1);

    RandomMultiGraphNeighborsRequest neighborRequest = new RandomMultiGraphNeighborsRequest(
        leftSeedNodesWithWeight,
        maxNumNeighbors,
        maxNumResults);

    final List<NeighborInfo> expectedResults =
        Lists.newArrayList(
            new NeighborInfo(87, 0.00351, 32),
            new NeighborInfo(157, 0.0033, 26),
            new NeighborInfo(620, 0.00328, 30)
        );

    // Should be in sorted order of weight
    RandomMultiGraphNeighbors randomMultiGraphNeighbors = new RandomMultiGraphNeighbors(
        bipartiteGraph,
        new NullStatsReceiver());

    RandomMultiGraphNeighborsResponse neighborResponse =
        randomMultiGraphNeighbors.getRandomMultiGraphNeighbors(neighborRequest, random);
    List<NeighborInfo> results =
        Lists.newArrayList(neighborResponse.getNeighborNodes());

    assertEquals(expectedResults, results);

    final List<NeighborInfo> expectedFilteredResults =
        Lists.newArrayList(
            new NeighborInfo(927, 0.00183, 45)
        );

    // also check whether minimum result degree filter is enforced
    MinEngagementFilter minEngagementFilter = new MinEngagementFilter(45, bipartiteGraph,
        new NullStatsReceiver());
    ArrayList<RelatedTweetFilter> filters = new ArrayList<RelatedTweetFilter>(1);
    filters.add(minEngagementFilter);
    RelatedTweetFilterChain filterChain = new RelatedTweetFilterChain(filters);

    RandomMultiGraphNeighborsResponse filteredResponse =
        randomMultiGraphNeighbors.getRandomMultiGraphNeighbors(
          neighborRequest, random, filterChain);
    List<NeighborInfo> filteredResults =
        Lists.newArrayList(filteredResponse.getNeighborNodes());

    assertEquals(expectedFilteredResults, filteredResults);
  }
}
