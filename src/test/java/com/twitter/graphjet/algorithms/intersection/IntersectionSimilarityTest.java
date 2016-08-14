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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.algorithms.BipartiteGraphTestHelper;
import com.twitter.graphjet.algorithms.SimilarityInfo;
import com.twitter.graphjet.algorithms.SimilarityResponse;
import com.twitter.graphjet.algorithms.filters.MinEngagementFilter;
import com.twitter.graphjet.algorithms.filters.RelatedTweetFilter;
import com.twitter.graphjet.algorithms.filters.RelatedTweetFilterChain;
import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class IntersectionSimilarityTest {
  @Test
  public void testIntersectionSimilarity() throws Exception {
    BipartiteGraph bipartiteGraph = BipartiteGraphTestHelper.buildSmallTestBipartiteGraphSegment();
    long queryNode = 2;
    int numResults = 2;
    LongSet seedSet = new LongOpenHashSet(new long[]{3});
    int maxNumNeighbors = 100;
    int minNeighborDegree = 2;
    int maxNumSamplesPerNeighbor = 100;
    int minCooccurrence = 1;
    int minDegree = 2;
    double maxLowerMultiplicativeDeviation = 5.0;
    double maxUpperMultiplicativeDeviation = 5.0;
    long randomSeed = 5298057403198457L;

    IntersectionSimilarityRequest intersectionSimilarityRequest = new IntersectionSimilarityRequest(
        queryNode,
        numResults,
        seedSet,
        maxNumNeighbors,
        minNeighborDegree,
        maxNumSamplesPerNeighbor,
        minCooccurrence,
        minDegree,
        maxLowerMultiplicativeDeviation,
        maxUpperMultiplicativeDeviation,
        false);

    final List<SimilarityInfo> expectedCosineSimilarityResults =
        Lists.newArrayList(
            new SimilarityInfo(2, 2.1213203435596424, 3, 2),
            new SimilarityInfo(3, 2.0, 2, 1)
        );

    // Should be in sorted order of weight
    RelatedTweetUpdateNormalization cosineUpdateNormalization = new CosineUpdateNormalization();
    IntersectionSimilarity cosineSimilarity = new IntersectionSimilarity(bipartiteGraph,
        cosineUpdateNormalization, new NullStatsReceiver());
    Random random = new Random(randomSeed);
    SimilarityResponse similarityResponse =
        cosineSimilarity.getSimilarNodes(intersectionSimilarityRequest, random);
    List<SimilarityInfo> cosineSimilarityResults =
        Lists.newArrayList(similarityResponse.getRankedSimilarNodes());

    assertEquals(expectedCosineSimilarityResults, cosineSimilarityResults);

    final List<SimilarityInfo> expectedFilteredCosineSimilarityResults =
        Lists.newArrayList(
            new SimilarityInfo(5, 1.7320508075688776, 3, 3)
        );

    // also check whether minimum result degree filter is enforced
    MinEngagementFilter minEngagementFilter = new MinEngagementFilter(3, bipartiteGraph,
        new NullStatsReceiver());
    ArrayList<RelatedTweetFilter> filters = new ArrayList<RelatedTweetFilter>(1);
    filters.add(minEngagementFilter);
    RelatedTweetFilterChain filterChain = new RelatedTweetFilterChain(filters);

    SimilarityResponse filteredSimilarityResponse =
        cosineSimilarity.getSimilarNodes(intersectionSimilarityRequest, random, filterChain);
    List<SimilarityInfo> filteredCosineSimilarityResults =
        Lists.newArrayList(filteredSimilarityResponse.getRankedSimilarNodes());

    assertEquals(expectedFilteredCosineSimilarityResults, filteredCosineSimilarityResults);
  }
}
