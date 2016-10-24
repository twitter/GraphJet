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


package com.twitter.graphjet.algorithms.counting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Lists;

import com.twitter.graphjet.algorithms.counting.request.TopSecondDegreeByCountRequestForTweet;
import com.twitter.graphjet.algorithms.counting.request.TopSecondDegreeByCountRequestForUser;
import com.twitter.graphjet.algorithms.counting.response.TopSecondDegreeByCountResponse;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.algorithms.BipartiteGraphTestHelper;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationStats;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.RequestedSetFilter;
import com.twitter.graphjet.algorithms.ResultFilter;
import com.twitter.graphjet.algorithms.ResultFilterChain;
import com.twitter.graphjet.algorithms.counting.recommendationInfo.RecommendationInfoTweet;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.longs.Long2DoubleArrayMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class TopSecondDegreeByCountTest {

  @Test
  public void testTopSecondDegreeByCountWithSmallGraph() throws Exception {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph bipartiteGraph =
      BipartiteGraphTestHelper.buildSmallTestNodeMetadataLeftIndexedMultiSegmentBipartiteGraph();
    long queryNode = 1;
    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(new long[]{2, 3}, new double[]{1.0, 0.5});
    LongSet toBeFiltered = new LongOpenHashSet(new long[]{2, 3, 4, 5});
    Set<RecommendationType> recommendationTypes = new HashSet<RecommendationType>();
    recommendationTypes.add(RecommendationType.HASHTAG);
    recommendationTypes.add(RecommendationType.URL);
    recommendationTypes.add(RecommendationType.TWEET);
    Map<RecommendationType, Integer> maxNumResults = new HashMap<RecommendationType, Integer>();
    maxNumResults.put(RecommendationType.HASHTAG, 3);
    maxNumResults.put(RecommendationType.URL, 3);
    maxNumResults.put(RecommendationType.TWEET, 3);
    Map<RecommendationType, Integer> minUserSocialProofSizes =
      new HashMap<RecommendationType, Integer>();
    minUserSocialProofSizes.put(RecommendationType.HASHTAG, 1);
    minUserSocialProofSizes.put(RecommendationType.URL, 1);
    minUserSocialProofSizes.put(RecommendationType.TWEET, 1);

    int maxUserSocialProofSize = 2;
    int maxTweetSocialProofSize = 10;
    int maxSocialProofTypeSize = 5;
    byte[] validSocialProofs = new byte[]{0, 1, 2, 3, 4};
    int expectedNodesToHit = 100;
    long randomSeed = 918324701982347L;
    Random random = new Random(randomSeed);
    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.<ResultFilter>newArrayList(
      new RequestedSetFilter(new NullStatsReceiver())
    ));
    Set<byte[]> socialProofTypeUnions = new HashSet<>();

    TopSecondDegreeByCountRequestForTweet topSecondDegreeByCountRequestForTweet = new TopSecondDegreeByCountRequestForTweet(
      queryNode,
      seedsMap,
      toBeFiltered,
      recommendationTypes,
      maxNumResults,
      maxSocialProofTypeSize,
      maxUserSocialProofSize,
      maxTweetSocialProofSize,
      minUserSocialProofSizes,
      validSocialProofs,
      resultFilterChain,
      socialProofTypeUnions
    );

    TopSecondDegreeByCountResponse topSecondDegreeByCountResponse = new TopSecondDegreeByCountForTweet(
      bipartiteGraph,
      expectedNodesToHit,
      new NullStatsReceiver()
    ).computeRecommendations(topSecondDegreeByCountRequestForTweet, random);

    ArrayList<HashMap<Byte, LongList>> socialProof = new ArrayList<HashMap<Byte, LongList>>();
    for (int i = 0; i < 3; i++) {
      socialProof.add(new HashMap<Byte, LongList>());
    }
    socialProof.get(0).put((byte) 0, new LongArrayList(new long[]{2, 3}));
    socialProof.get(1).put((byte) 0, new LongArrayList(new long[]{2}));
    socialProof.get(2).put((byte) 0, new LongArrayList(new long[]{3}));

    final List<RecommendationInfo> expectedTopResults = new ArrayList<RecommendationInfo>();
    expectedTopResults.add(new RecommendationInfoTweet(10, 1.5, socialProof.get(0)));
    expectedTopResults.add(new RecommendationInfoTweet(6, 1.0, socialProof.get(1)));
    expectedTopResults.add(new RecommendationInfoTweet(8, 0.5, socialProof.get(2)));

    List<RecommendationInfo> topSecondDegreeByCountResults =
      Lists.newArrayList(topSecondDegreeByCountResponse.getRankedRecommendations());

    final RecommendationStats expectedTopSecondDegreeByCountStats =
      new RecommendationStats(4, 9, 20, 2, 3, 2);
    RecommendationStats topSecondDegreeByCountStats =
      topSecondDegreeByCountResponse.getTopSecondDegreeByCountStats();

    assertEquals(expectedTopSecondDegreeByCountStats, topSecondDegreeByCountStats);
    assertEquals(expectedTopResults, topSecondDegreeByCountResults);
  }

  @Test
  public void testTopSecondDegreeByCountWithSocialProofTypeUnionsWithSmallGraph() throws Exception {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph bipartiteGraph =
      BipartiteGraphTestHelper.buildSmallTestNodeMetadataLeftIndexedMultiSegmentBipartiteGraphWithEdgeTypes();
    long queryNode = 1;
    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(new long[]{1, 2, 3}, new double[]{1.5, 1.0, 0.5});
    LongSet toBeFiltered = new LongOpenHashSet(new long[]{});
    Set<RecommendationType> recommendationTypes = new HashSet<RecommendationType>();
    recommendationTypes.add(RecommendationType.TWEET);
    Map<RecommendationType, Integer> maxNumResults = new HashMap<RecommendationType, Integer>();
    maxNumResults.put(RecommendationType.TWEET, 3);
    Map<RecommendationType, Integer> minUserSocialProofSizes = new HashMap<RecommendationType, Integer>();
    minUserSocialProofSizes.put(RecommendationType.TWEET, 2);

    int maxUserSocialProofSize = 3;
    int maxTweetSocialProofSize = 10;
    int maxSocialProofTypeSize = 5;
    byte[] validSocialProofs = new byte[]{0, 1, 2, 3, 4};
    Set<byte[]> socialProofTypeUnions = new HashSet<>();
    socialProofTypeUnions.add(new byte[]{0, 3});
    int expectedNodesToHit = 100;
    long randomSeed = 918324701982347L;
    Random random = new Random(randomSeed);
    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.<ResultFilter>newArrayList(
      new RequestedSetFilter(new NullStatsReceiver())
    ));

    TopSecondDegreeByCountRequestForTweet topSecondDegreeByCountRequestForTweet = new TopSecondDegreeByCountRequestForTweet(
      queryNode,
      seedsMap,
      toBeFiltered,
      recommendationTypes,
      maxNumResults,
      maxSocialProofTypeSize,
      maxUserSocialProofSize,
      maxTweetSocialProofSize,
      minUserSocialProofSizes,
      validSocialProofs,
      resultFilterChain,
      socialProofTypeUnions
    );

    TopSecondDegreeByCountResponse topSecondDegreeByCountResponse = new TopSecondDegreeByCountForTweet(
      bipartiteGraph,
      expectedNodesToHit,
      new NullStatsReceiver()
    ).computeRecommendations(topSecondDegreeByCountRequestForTweet, random);

    ArrayList<HashMap<Byte, LongList>> socialProof = new ArrayList<HashMap<Byte, LongList>>();
    for (int i = 0; i < 2; i++) {
      socialProof.add(new HashMap<Byte, LongList>());
    }
    socialProof.get(0).put((byte) 1, new LongArrayList(new long[]{1, 2, 3}));
    socialProof.get(1).put((byte) 0, new LongArrayList(new long[]{2}));
    socialProof.get(1).put((byte) 3, new LongArrayList(new long[]{1}));

    final List<RecommendationInfo> expectedTopResults = new ArrayList<RecommendationInfo>();
    expectedTopResults.add(new RecommendationInfoTweet(3, 3.0, socialProof.get(0)));
    expectedTopResults.add(new RecommendationInfoTweet(5, 2.5, socialProof.get(1)));

    List<RecommendationInfo> topSecondDegreeByCountResults =
      Lists.newArrayList(topSecondDegreeByCountResponse.getRankedRecommendations());

    final RecommendationStats expectedTopSecondDegreeByCountStats =
      new RecommendationStats(5, 6, 17, 2, 4, 0);
    RecommendationStats topSecondDegreeByCountStats =
      topSecondDegreeByCountResponse.getTopSecondDegreeByCountStats();

    assertEquals(expectedTopSecondDegreeByCountStats, topSecondDegreeByCountStats);
    assertEquals(expectedTopResults, topSecondDegreeByCountResults);
  }

  @Test
  public void testTopSecondDegreeByCountWithRandomGraph() throws Exception {
    long randomSeed = 918324701982347L;
    Random random = new Random(randomSeed);

    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph bipartiteGraph =
      BipartiteGraphTestHelper
        .buildRandomNodeMetadataLeftIndexedMultiSegmentBipartiteGraph(1000, 20000, 0.01, random);
    long queryNode = 0;

    LongList seedsList = new LongArrayList();
    DoubleList scoresList = new DoubleArrayList();
    for (int i = 1; i < 1000; i++) {
      if (random.nextInt(10) < 1) {
        seedsList.add(i);
        scoresList.add(((double) random.nextInt(10)) / 10.0);
      }
    }

    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(
      seedsList.toLongArray(),
      scoresList.toDoubleArray()
    );
    LongSet toBeFiltered = new LongOpenHashSet(new long[]{2, 3, 4, 5});
    Set<RecommendationType> recommendationTypes = new HashSet<RecommendationType>();
    recommendationTypes.add(RecommendationType.HASHTAG);
    recommendationTypes.add(RecommendationType.URL);
    recommendationTypes.add(RecommendationType.TWEET);
    Map<RecommendationType, Integer> maxNumResults = new HashMap<RecommendationType, Integer>();
    maxNumResults.put(RecommendationType.HASHTAG, 3);
    maxNumResults.put(RecommendationType.URL, 3);
    maxNumResults.put(RecommendationType.TWEET, 3);
    Map<RecommendationType, Integer> minUserSocialProofSizes =
      new HashMap<RecommendationType, Integer>();
    minUserSocialProofSizes.put(RecommendationType.HASHTAG, 1);
    minUserSocialProofSizes.put(RecommendationType.URL, 1);
    minUserSocialProofSizes.put(RecommendationType.TWEET, 1);

    int maxUserSocialProofSize = 2;
    int maxTweetSocialProofSize = 10;
    int maxSocialProofTypeSize = 5;
    byte[] validSocialProofs = new byte[]{0, 1, 2, 3, 4};
    int expectedNodesToHit = 100;

    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.<ResultFilter>newArrayList(
      new RequestedSetFilter(new NullStatsReceiver())
    ));

    Set<byte[]> socialProofTypeUnions = new HashSet<>();

    TopSecondDegreeByCountRequestForTweet topSecondDegreeByCountRequestForTweet = new TopSecondDegreeByCountRequestForTweet(
      queryNode,
      seedsMap,
      toBeFiltered,
      recommendationTypes,
      maxNumResults,
      maxSocialProofTypeSize,
      maxUserSocialProofSize,
      maxTweetSocialProofSize,
      minUserSocialProofSizes,
      validSocialProofs,
      resultFilterChain,
      socialProofTypeUnions
    );

    TopSecondDegreeByCountResponse topSecondDegreeByCountResponse = new TopSecondDegreeByCountForTweet(
      bipartiteGraph,
      expectedNodesToHit,
      new NullStatsReceiver()
    ).computeRecommendations(topSecondDegreeByCountRequestForTweet, random);

    ArrayList<HashMap<Byte, LongList>> socialProof = new ArrayList<HashMap<Byte, LongList>>();
    for (int i = 0; i < 3; i++) {
      socialProof.add(new HashMap<Byte, LongList>());
    }
    socialProof.get(0).put((byte) 0, new LongArrayList(new long[]{990, 978}));
    socialProof.get(1).put((byte) 0, new LongArrayList(new long[]{990, 978}));
    socialProof.get(2).put((byte) 0, new LongArrayList(new long[]{990}));

    final List<RecommendationInfo> expectedTopResults = new ArrayList<RecommendationInfo>();
    expectedTopResults.add(
      new RecommendationInfoTweet(16428, 1.0, socialProof.get(0))
    );
    expectedTopResults.add(
      new RecommendationInfoTweet(3891, 1.0, socialProof.get(1))
    );
    expectedTopResults.add(
      new RecommendationInfoTweet(19301, 0.6, socialProof.get(2))
    );

    List<RecommendationInfo> topSecondDegreeByCountResults =
      Lists.newArrayList(topSecondDegreeByCountResponse.getRankedRecommendations());

    final RecommendationStats expectedTopSecondDegreeByCountStats =
      new RecommendationStats(0, 398, 798, 2, 3, 0);
    RecommendationStats topSecondDegreeByCountStats =
      topSecondDegreeByCountResponse.getTopSecondDegreeByCountStats();

    assertEquals(expectedTopSecondDegreeByCountStats, topSecondDegreeByCountStats);
    assertEquals(expectedTopResults, topSecondDegreeByCountResults);
  }

  @Test
  public void testTopSecondDegreeByCountWithSocialProofUsersWithSmallGraph() throws Exception {

    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph bipartiteGraph =
        BipartiteGraphTestHelper.buildSmallTestNodeMetadataLeftIndexedMultiSegmentBipartiteGraphWithEdgeTypes();

    long queryNode = 1;
    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(new long[]{1, 2, 3}, new double[]{1.5, 1.0, 0.5});
    LongSet toBeFiltered = new LongOpenHashSet(new long[]{});
    int maxNumResults = 3;
    int maxSocialProofSize = 10;
    Map<Byte, Integer> minUserPerSocialProof = new HashMap<>();
    byte[] socialProofTypes = new byte[]{0, 1, 2, 3};
    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.<ResultFilter>newArrayList(
        new RequestedSetFilter(new NullStatsReceiver())));

    int expectedNodesToHit = 100;
    long randomSeed = 918324701982347L;
    Random random = new Random(randomSeed);

    TopSecondDegreeByCountRequestForUser request = new TopSecondDegreeByCountRequestForUser(
        queryNode,
        seedsMap,
        toBeFiltered,
        maxNumResults,
        maxSocialProofSize,
        minUserPerSocialProof,
        socialProofTypes,
        resultFilterChain);

    TopSecondDegreeByCountResponse response = new TopSecondDegreeByCountForUser(
        bipartiteGraph,
        expectedNodesToHit,
        new NullStatsReceiver()
    ).computeRecommendations(request, random);

    ArrayList<HashMap<Byte, LongList>> socialProof = new ArrayList<HashMap<Byte, LongList>>();
    for (int i = 0; i < 2; i++) {
      socialProof.add(new HashMap<>());
    }
    socialProof.get(0).put((byte) 1, new LongArrayList(new long[]{1, 2, 3}));
    socialProof.get(1).put((byte) 0, new LongArrayList(new long[]{2}));
    socialProof.get(1).put((byte) 3, new LongArrayList(new long[]{1}));

    final List<RecommendationInfo> expectedTopResults = new ArrayList<RecommendationInfo>();
    expectedTopResults.add(new RecommendationInfoTweet(3, 3.0, socialProof.get(0)));
    expectedTopResults.add(new RecommendationInfoTweet(5, 2.5, socialProof.get(1)));

    List<RecommendationInfo> topSecondDegreeByCountResults =
        Lists.newArrayList(response.getRankedRecommendations());

    final RecommendationStats expectedTopSecondDegreeByCountStats =
        new RecommendationStats(5, 6, 17, 2, 4, 0);
    RecommendationStats topSecondDegreeByCountStats =
        response.getTopSecondDegreeByCountStats();

    assertEquals(expectedTopSecondDegreeByCountStats, topSecondDegreeByCountStats);
    assertEquals(expectedTopResults, topSecondDegreeByCountResults);
  }
}
