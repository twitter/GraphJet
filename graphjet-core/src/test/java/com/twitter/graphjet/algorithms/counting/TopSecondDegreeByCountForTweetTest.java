/**
 * Copyright 2018 Twitter. All rights reserved.
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.algorithms.BipartiteGraphTestHelper;
import com.twitter.graphjet.algorithms.ConnectingUsersWithMetadata;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationStats;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.counting.tweet.TweetMetadataRecommendationInfo;
import com.twitter.graphjet.algorithms.filters.RequestedSetFilter;
import com.twitter.graphjet.algorithms.filters.ResultFilter;
import com.twitter.graphjet.algorithms.filters.ResultFilterChain;
import com.twitter.graphjet.algorithms.counting.tweet.TopSecondDegreeByCountForTweet;
import com.twitter.graphjet.algorithms.counting.tweet.TopSecondDegreeByCountRequestForTweet;
import com.twitter.graphjet.algorithms.counting.tweet.TweetRecommendationInfo;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.stats.NullStatsReceiver;

import static com.twitter.graphjet.algorithms.RecommendationRequest.FAVORITE_SOCIAL_PROOF_TYPE;
import static com.twitter.graphjet.algorithms.RecommendationRequest.RETWEET_SOCIAL_PROOF_TYPE;
import static com.twitter.graphjet.algorithms.RecommendationRequest.UNFAVORITE_SOCIAL_PROOF_TYPE;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.longs.Long2DoubleArrayMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class TopSecondDegreeByCountForTweetTest {

  @Test
  public void testTopSecondDegreeByCountWithSmallGraph() {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph bipartiteGraph =
      BipartiteGraphTestHelper.buildSmallTestNodeMetadataLeftIndexedMultiSegmentBipartiteGraph();
    long queryNode = 1;
    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(new long[]{2, 3}, new double[]{1.0, 0.5});
    LongSet toBeFiltered = new LongOpenHashSet(new long[]{2, 3, 4, 5});
    Set<RecommendationType> recommendationTypes = new HashSet<>();
    recommendationTypes.add(RecommendationType.HASHTAG);
    recommendationTypes.add(RecommendationType.URL);
    recommendationTypes.add(RecommendationType.TWEET);
    Map<RecommendationType, Integer> maxNumResults = new HashMap<>();
    maxNumResults.put(RecommendationType.HASHTAG, 3);
    maxNumResults.put(RecommendationType.URL, 3);
    maxNumResults.put(RecommendationType.TWEET, 3);
    Map<RecommendationType, Integer> minUserSocialProofSizes = new HashMap<>();
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
    long maxRightNodeAgeInMillis = Long.MAX_VALUE;
    long maxEdgeAgeInMillis = Long.MAX_VALUE;
    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.<ResultFilter>newArrayList(
      new RequestedSetFilter(new NullStatsReceiver())
    ));
    Set<byte[]> socialProofTypeUnions = new HashSet<>();

    TopSecondDegreeByCountRequestForTweet request = new TopSecondDegreeByCountRequestForTweet(
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
      maxRightNodeAgeInMillis,
      maxEdgeAgeInMillis,
      resultFilterChain,
      socialProofTypeUnions
    );

    TopSecondDegreeByCountResponse response = new TopSecondDegreeByCountForTweet(
      bipartiteGraph,
      expectedNodesToHit,
      new NullStatsReceiver()
    ).computeRecommendations(request, random);

    LongList metadata1 = new LongArrayList(new long[]{0});
    LongList metadata2 = new LongArrayList(new long[]{0, 0});
    ArrayList<HashMap<Byte, ConnectingUsersWithMetadata>> socialProof = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      socialProof.add(new HashMap<>());
    }
    socialProof.get(0).put((byte) 0, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{2, 3}), metadata2));
    socialProof.get(1).put((byte) 0, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{2}), metadata1));
    socialProof.get(2).put((byte) 0, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{3}), metadata1));

    final List<RecommendationInfo> expectedTopResults = new ArrayList<RecommendationInfo>();
    expectedTopResults.add(new TweetRecommendationInfo(10, 1.5, socialProof.get(0)));
    expectedTopResults.add(new TweetRecommendationInfo(6, 1.0, socialProof.get(1)));
    expectedTopResults.add(new TweetRecommendationInfo(8, 0.5, socialProof.get(2)));

    List<RecommendationInfo> topSecondDegreeByCountResults =
      Lists.newArrayList(response.getRankedRecommendations());

    final RecommendationStats expectedTopSecondDegreeByCountStats =
      new RecommendationStats(4, 9, 20, 2, 3, 2);
    RecommendationStats topSecondDegreeByCountStats =
      response.getTopSecondDegreeByCountStats();

    assertEquals(expectedTopSecondDegreeByCountStats, topSecondDegreeByCountStats);
    assertEquals(expectedTopResults, topSecondDegreeByCountResults);
  }

  @Test
  public void testTopSecondDegreeByCountWithSocialProofTypeUnionsWithSmallGraph() {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph bipartiteGraph =
      BipartiteGraphTestHelper.buildSmallTestNodeMetadataLeftIndexedMultiSegmentBipartiteGraphWithEdgeTypes();
    long queryNode = 1;
    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(new long[]{1, 2, 3}, new double[]{1.5, 1.0, 0.5});
    LongSet toBeFiltered = new LongOpenHashSet(new long[]{});
    Set<RecommendationType> recommendationTypes = new HashSet<>();
    recommendationTypes.add(RecommendationType.TWEET);
    Map<RecommendationType, Integer> maxNumResults = new HashMap<>();
    maxNumResults.put(RecommendationType.TWEET, 3);
    Map<RecommendationType, Integer> minUserSocialProofSizes = new HashMap<>();
    minUserSocialProofSizes.put(RecommendationType.TWEET, 2);

    int maxUserSocialProofSize = 3;
    int maxTweetSocialProofSize = 10;
    int maxSocialProofTypeSize = 5;
    byte[] validSocialProofs = new byte[]{0, 1, 2, 3, 4};
    Set<byte[]> socialProofTypeUnions = new HashSet<>();
    socialProofTypeUnions.add(new byte[]{0, 3});
    int expectedNodesToHit = 100;
    long maxRightNodeAgeInMillis = Long.MAX_VALUE;
    long maxEdgeAgeInMillis = Long.MAX_VALUE;
    long randomSeed = 918324701982347L;

    Random random = new Random(randomSeed);
    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.<ResultFilter>newArrayList(
      new RequestedSetFilter(new NullStatsReceiver())
    ));

    TopSecondDegreeByCountRequestForTweet request = new TopSecondDegreeByCountRequestForTweet(
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
      maxRightNodeAgeInMillis,
      maxEdgeAgeInMillis,
      resultFilterChain,
      socialProofTypeUnions
    );

    TopSecondDegreeByCountResponse response = new TopSecondDegreeByCountForTweet(
      bipartiteGraph,
      expectedNodesToHit,
      new NullStatsReceiver()
    ).computeRecommendations(request, random);

    LongList metadata1 = new LongArrayList(new long[]{0});
    LongList metadata3 = new LongArrayList(new long[]{0, 0, 0});
    ArrayList<HashMap<Byte, ConnectingUsersWithMetadata>> socialProof = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      socialProof.add(new HashMap<>());
    }
    socialProof.get(0).put((byte) 1, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{1, 2, 3}), metadata3));
    socialProof.get(1).put((byte) 0, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{2}), metadata1));
    socialProof.get(1).put((byte) 3, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{1}), metadata1));

    final List<RecommendationInfo> expectedTopResults = new ArrayList<>();
    expectedTopResults.add(new TweetRecommendationInfo(3, 3.0, socialProof.get(0)));
    expectedTopResults.add(new TweetRecommendationInfo(5, 2.5, socialProof.get(1)));

    List<RecommendationInfo> topSecondDegreeByCountResults =
      Lists.newArrayList(response.getRankedRecommendations());

    final RecommendationStats expectedTopSecondDegreeByCountStats =
      new RecommendationStats(5, 6, 17, 2, 4, 0);
    RecommendationStats topSecondDegreeByCountStats =
      response.getTopSecondDegreeByCountStats();

    assertEquals(expectedTopSecondDegreeByCountStats, topSecondDegreeByCountStats);
    assertEquals(expectedTopResults, topSecondDegreeByCountResults);
  }

  /**
   * Test a small graph that contain favorite and unfavorite edges to make sure they are
   * removed correctly.
   */
  @Test
  public void testTopSecondDegreeByCountWithSmallGraphWithEdgeRemoval() {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph bipartiteGraph =
      BipartiteGraphTestHelper.buildTestNodeMetadataLeftIndexedMultiSegmentBipartiteGraphWithUnfavorite();

    long queryNode = 1;
    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(
      new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
      new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14});
    byte[] validSocialProofs = new byte[]{FAVORITE_SOCIAL_PROOF_TYPE, RETWEET_SOCIAL_PROOF_TYPE};

    LongSet toBeFiltered = new LongOpenHashSet(new long[]{});
    Set<RecommendationType> recommendationTypes = new HashSet<>();
    recommendationTypes.add(RecommendationType.TWEET);

    Map<RecommendationType, Integer> maxNumResults = new HashMap<>();
    maxNumResults.put(RecommendationType.TWEET, 10);
    Map<RecommendationType, Integer> minUserSocialProofSizes = new HashMap<>();
    minUserSocialProofSizes.put(RecommendationType.TWEET, 1);

    int maxUserSocialProofSize = 10;
    int maxTweetSocialProofSize = 10;
    int maxSocialProofTypeSize = 10;
    int expectedNodesToHit = 100;
    Random random = new Random(918324701982347L);
    long maxRightNodeAgeInMillis = Long.MAX_VALUE;
    long maxEdgeAgeInMillis = Long.MAX_VALUE;
    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.newArrayList());
    Set<byte[]> socialProofTypeUnions = new HashSet<>();

    TopSecondDegreeByCountRequestForTweet request = new TopSecondDegreeByCountRequestForTweet(
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
      maxRightNodeAgeInMillis,
      maxEdgeAgeInMillis,
      resultFilterChain,
      socialProofTypeUnions
    );

    TopSecondDegreeByCountResponse response = new TopSecondDegreeByCountForTweet(
      bipartiteGraph,
      expectedNodesToHit,
      new NullStatsReceiver()
    ).computeRecommendations(request, random);

    LongList metadata1 = new LongArrayList(new long[]{0});
    LongList metadata2 = new LongArrayList(new long[]{0, 0});

    HashMap<Byte, ConnectingUsersWithMetadata> tweet1SocialProof = new HashMap<>();
    tweet1SocialProof.put(
        FAVORITE_SOCIAL_PROOF_TYPE, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{1}), metadata1));

    HashMap<Byte, ConnectingUsersWithMetadata> tweet2SocialProof = new HashMap<>();
    tweet2SocialProof.put(
        RETWEET_SOCIAL_PROOF_TYPE, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{2}), metadata1));

    HashMap<Byte, ConnectingUsersWithMetadata> tweet5SocialProof = new HashMap<>();
    tweet5SocialProof.put(
      FAVORITE_SOCIAL_PROOF_TYPE, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{5}), metadata1));

    HashMap<Byte, ConnectingUsersWithMetadata> tweet7SocialProof = new HashMap<>();
    tweet7SocialProof.put(
      FAVORITE_SOCIAL_PROOF_TYPE, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{7}), metadata1));

    HashMap<Byte, ConnectingUsersWithMetadata> tweet8SocialProof = new HashMap<>();
    tweet8SocialProof.put(
      FAVORITE_SOCIAL_PROOF_TYPE, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{9, 8}), metadata2));

    HashMap<Byte, ConnectingUsersWithMetadata> tweet9SocialProof = new HashMap<>();
    tweet9SocialProof.put(
      FAVORITE_SOCIAL_PROOF_TYPE, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{10, 9}), metadata2));

    HashMap<Byte, ConnectingUsersWithMetadata> tweet10SocialProof = new HashMap<>();
    tweet10SocialProof.put(
      FAVORITE_SOCIAL_PROOF_TYPE, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{10}), metadata1));
    tweet10SocialProof.put(
      RETWEET_SOCIAL_PROOF_TYPE, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{11}), metadata1));

    HashMap<Byte, ConnectingUsersWithMetadata> tweet11SocialProof = new HashMap<>();
    tweet11SocialProof.put(
      RETWEET_SOCIAL_PROOF_TYPE, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{11}), metadata1));

    HashMap<Byte, ConnectingUsersWithMetadata> tweet12SocialProof = new HashMap<>();
    tweet12SocialProof.put(
      RETWEET_SOCIAL_PROOF_TYPE, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{12}), metadata1));

    HashMap<Byte, ConnectingUsersWithMetadata> tweet13SocialProof = new HashMap<>();
    tweet13SocialProof.put(
      FAVORITE_SOCIAL_PROOF_TYPE, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{13}), metadata1));
    tweet13SocialProof.put(
      RETWEET_SOCIAL_PROOF_TYPE, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{14}), metadata1));

    List<RecommendationInfo> topSecondDegreeByCountResults =
      Lists.newArrayList(response.getRankedRecommendations());
    List<RecommendationInfo> expectedResults = new ArrayList<>();

    expectedResults.add(new TweetRecommendationInfo(13, 13+14, tweet13SocialProof));
    expectedResults.add(new TweetRecommendationInfo(10, 10+11, tweet10SocialProof));
    expectedResults.add(new TweetRecommendationInfo(9, 9+10, tweet9SocialProof));
    expectedResults.add(new TweetRecommendationInfo(8, 8+9, tweet8SocialProof));
    expectedResults.add(new TweetRecommendationInfo(12, 12, tweet12SocialProof));
    expectedResults.add(new TweetRecommendationInfo(11, 11, tweet11SocialProof));
    expectedResults.add(new TweetRecommendationInfo(7, 7, tweet7SocialProof));
    expectedResults.add(new TweetRecommendationInfo(5, 5, tweet5SocialProof));
    expectedResults.add(new TweetRecommendationInfo(2, 2, tweet2SocialProof));
    expectedResults.add(new TweetRecommendationInfo(1, 1, tweet1SocialProof));

    assertEquals(expectedResults, topSecondDegreeByCountResults);
  }

  /**
   * Test a small graph that contain favorite and unfavorite edges to make sure they are
   * removed correctly.
   */
  @Test
  public void testTopSecondDegreeMetadataByCountWithSmallGraphWithEdgeRemoval() {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph bipartiteGraph =
      BipartiteGraphTestHelper.buildTestNodeMetadataLeftIndexedMultiSegmentBipartiteGraphWithUnfavorite();

    long queryNode = 1;
    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(
      new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14},
      new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14});
    byte[] validSocialProofs = new byte[]{FAVORITE_SOCIAL_PROOF_TYPE, RETWEET_SOCIAL_PROOF_TYPE};

    LongSet toBeFiltered = new LongOpenHashSet(new long[]{});
    Set<RecommendationType> recommendationTypes = new HashSet<>();
    recommendationTypes.add(RecommendationType.HASHTAG);

    Map<RecommendationType, Integer> maxNumResults = new HashMap<>();
    maxNumResults.put(RecommendationType.HASHTAG, 10);
    Map<RecommendationType, Integer> minUserSocialProofSizes = new HashMap<>();
    minUserSocialProofSizes.put(RecommendationType.HASHTAG, 1);

    int maxUserSocialProofSize = 10;
    int maxTweetSocialProofSize = 10;
    int maxSocialProofTypeSize = 10;
    int expectedNodesToHit = 100;
    Random random = new Random(918324701982347L);
    long maxRightNodeAgeInMillis = Long.MAX_VALUE;
    long maxEdgeAgeInMillis = Long.MAX_VALUE;
    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.newArrayList());
    Set<byte[]> socialProofTypeUnions = new HashSet<>();

    TopSecondDegreeByCountRequestForTweet request = new TopSecondDegreeByCountRequestForTweet(
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
        maxRightNodeAgeInMillis,
        maxEdgeAgeInMillis,
        resultFilterChain,
        socialProofTypeUnions
    );

    TopSecondDegreeByCountResponse response = new TopSecondDegreeByCountForTweet(
        bipartiteGraph,
        expectedNodesToHit,
        new NullStatsReceiver()
    ).computeRecommendations(request, random);

    HashMap<Byte,  Map<Long, LongList>> tweet2SocialProof = new HashMap<>();
    Map<Long, LongList> proof2 = new HashMap<>();
    proof2.put(2L, new LongArrayList());
    proof2.get(2L).add(2);
    tweet2SocialProof.put(RETWEET_SOCIAL_PROOF_TYPE, proof2);

    HashMap<Byte,  Map<Long, LongList>> tweet10SocialProof = new HashMap<>();
    Map<Long, LongList> proof10 = new HashMap<>();
    Map<Long, LongList> proof10_2 = new HashMap<>();
    proof10.put(11L, new LongArrayList());
    proof10.get(11L).add(10);
    tweet10SocialProof.put(RETWEET_SOCIAL_PROOF_TYPE, proof10);
    proof10_2.put(10L, new LongArrayList());
    proof10_2.get(10L).add(10);
    tweet10SocialProof.put(FAVORITE_SOCIAL_PROOF_TYPE, proof10_2);

    HashMap<Byte,  Map<Long, LongList>> tweet11SocialProof = new HashMap<>();
    Map<Long, LongList> proof11 = new HashMap<>();
    proof11.put(11L, new LongArrayList());
    proof11.get(11L).add(11);
    tweet11SocialProof.put(RETWEET_SOCIAL_PROOF_TYPE, proof11);

    HashMap<Byte,  Map<Long, LongList>> tweet12SocialProof = new HashMap<>();
    Map<Long, LongList> proof12 = new HashMap<>();
    proof12.put(12L, new LongArrayList());
    proof12.get(12L).add(12);
    tweet12SocialProof.put(RETWEET_SOCIAL_PROOF_TYPE, proof12);

    HashMap<Byte,  Map<Long, LongList>> tweet13SocialProof = new HashMap<>();
    Map<Long, LongList> proof13 = new HashMap<>();
    Map<Long, LongList> proof13_2 = new HashMap<>();
    proof13.put(14L, new LongArrayList());
    proof13.get(14L).add(13);
    tweet13SocialProof.put(RETWEET_SOCIAL_PROOF_TYPE, proof13);
    proof13_2.put(13L, new LongArrayList());
    proof13_2.get(13L).add(13);
    tweet13SocialProof.put(FAVORITE_SOCIAL_PROOF_TYPE, proof13_2);

    List<RecommendationInfo> topSecondDegreeByCountResults = Lists.newArrayList(response.getRankedRecommendations());
    List<RecommendationInfo> expectedResults = new ArrayList<>();

    expectedResults.add(new TweetMetadataRecommendationInfo(13, RecommendationType.HASHTAG, 13+14, tweet13SocialProof));
    expectedResults.add(new TweetMetadataRecommendationInfo(10, RecommendationType.HASHTAG, 10+11, tweet10SocialProof));
    expectedResults.add(new TweetMetadataRecommendationInfo(12, RecommendationType.HASHTAG, 12, tweet12SocialProof));
    expectedResults.add(new TweetMetadataRecommendationInfo(11, RecommendationType.HASHTAG, 11, tweet11SocialProof));
    expectedResults.add(new TweetMetadataRecommendationInfo(2, RecommendationType.HASHTAG, 2, tweet2SocialProof));

    assertEquals(expectedResults, topSecondDegreeByCountResults);
  }

  @Test
  public void testTopSecondDegreeByCountWithRandomGraph() {
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
    long maxRightNodeAgeInMillis = Long.MAX_VALUE;
    long maxEdgeAgeInMillis = Long.MAX_VALUE;

    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.<ResultFilter>newArrayList(
      new RequestedSetFilter(new NullStatsReceiver())
    ));

    Set<byte[]> socialProofTypeUnions = new HashSet<>();

    TopSecondDegreeByCountRequestForTweet request = new TopSecondDegreeByCountRequestForTweet(
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
      maxRightNodeAgeInMillis,
      maxEdgeAgeInMillis,
      resultFilterChain,
      socialProofTypeUnions
    );

    TopSecondDegreeByCountResponse response = new TopSecondDegreeByCountForTweet(
      bipartiteGraph,
      expectedNodesToHit,
      new NullStatsReceiver()
    ).computeRecommendations(request, random);

    LongList metadata1 = new LongArrayList(new long[]{0});
    LongList metadata2 = new LongArrayList(new long[]{0, 0});
    ArrayList<HashMap<Byte, ConnectingUsersWithMetadata>> socialProof = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      socialProof.add(new HashMap<>());
    }
    socialProof.get(0).put(
      (byte) 0, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{990, 978}), metadata2));
    socialProof.get(1).put(
      (byte) 0, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{990, 978}), metadata2));
    socialProof.get(2).put((byte) 0, new ConnectingUsersWithMetadata(new LongArrayList(new long[]{990}), metadata1));

    final List<RecommendationInfo> expectedTopResults = new ArrayList<RecommendationInfo>();
    expectedTopResults.add(
      new TweetRecommendationInfo(16428, 1.0, socialProof.get(0))
    );
    expectedTopResults.add(
      new TweetRecommendationInfo(3891, 1.0, socialProof.get(1))
    );
    expectedTopResults.add(
      new TweetRecommendationInfo(19301, 0.6, socialProof.get(2))
    );

    List<RecommendationInfo> topSecondDegreeByCountResults =
      Lists.newArrayList(response.getRankedRecommendations());

    final RecommendationStats expectedTopSecondDegreeByCountStats =
      new RecommendationStats(0, 398, 798, 2, 3, 0);
    RecommendationStats topSecondDegreeByCountStats =
      response.getTopSecondDegreeByCountStats();

    assertEquals(expectedTopSecondDegreeByCountStats, topSecondDegreeByCountStats);
    assertEquals(expectedTopResults, topSecondDegreeByCountResults);
  }
}
