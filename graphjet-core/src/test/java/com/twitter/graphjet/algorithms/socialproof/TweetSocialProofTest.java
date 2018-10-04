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

package com.twitter.graphjet.algorithms.socialproof;

import java.util.*;

import org.junit.Test;

import com.twitter.graphjet.algorithms.BipartiteGraphTestHelper;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.bipartite.LeftIndexedPowerLawMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedMultiSegmentBipartiteGraph;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleArrayMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;

import static org.junit.Assert.*;

import static com.twitter.graphjet.algorithms.RecommendationRequest.AUTHOR_SOCIAL_PROOF_TYPE;
import static com.twitter.graphjet.algorithms.RecommendationRequest.CLICK_SOCIAL_PROOF_TYPE;
import static com.twitter.graphjet.algorithms.RecommendationRequest.FAVORITE_SOCIAL_PROOF_TYPE;
import static com.twitter.graphjet.algorithms.RecommendationRequest.IS_MENTIONED_SOCIAL_PROOF_TYPE;
import static com.twitter.graphjet.algorithms.RecommendationRequest.REPLY_SOCIAL_PROOF_TYPE;
import static com.twitter.graphjet.algorithms.RecommendationRequest.RETWEET_SOCIAL_PROOF_TYPE;
import static com.twitter.graphjet.algorithms.RecommendationRequest.UNFAVORITE_SOCIAL_PROOF_TYPE;

/**
 * Unit test for social proof finder.
 *
 * Build graph using BipartiteGraphTestHelper, and test the proof finder logic with
 * one type of edges.
 *
 * Issue: the BipartiteGraphTestHelper does not support more than one type of edges
 * so far.
 */
public class TweetSocialProofTest {

  private long user1 = 1;
  private long user2 = 2;
  private long user3 = 3;
  private long user4 = 4;
  private long user5 = 5;
  private long user6 = 6;
  private long user7 = 7;
  private long user8 = 8;
  private long user9 = 9;
  private long user10 = 10;
  private long user11 = 11;
  private long user12 = 12;
  private long user13 = 13;
  private long user14 = 14;

  private long tweet1 = 1;
  private long tweet2 = 2;
  private long tweet3 = 3;
  private long tweet4 = 4;
  private long tweet5 = 5;
  private long tweet6 = 6;
  private long tweet7 = 7;
  private long tweet8 = 8;
  private long tweet9 = 9;
  private long tweet10 = 10;
  private long tweet11 = 11;
  private long tweet12 = 12;
  private long tweet13 = 13;

  private void assertEqualSocialProofResults(SocialProofResult expected, SocialProofResult actual) {
    assertEquals(expected.getNode(), actual.getNode());
    assertEquals(expected.getSocialProof(), actual.getSocialProof());
    assertEquals(expected.getSocialProofSize(), actual.getSocialProofSize());
    assertEquals(0, Double.compare(expected.getWeight(), actual.getWeight()));
  }

  @Test
  public void testTweetSocialProofs() {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph bipartiteGraph =
      BipartiteGraphTestHelper.buildSmallTestNodeMetadataLeftIndexedMultiSegmentBipartiteGraph();

    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(
      new long[] {user2, user3}, new double[] {1.0, 0.5});
    LongSet tweets = new LongArraySet(new long[] {tweet2, tweet3, tweet4, tweet5});

    byte[] validSocialProofTypes = new byte[] {
      CLICK_SOCIAL_PROOF_TYPE,
      FAVORITE_SOCIAL_PROOF_TYPE,
      RETWEET_SOCIAL_PROOF_TYPE,
      REPLY_SOCIAL_PROOF_TYPE,
      AUTHOR_SOCIAL_PROOF_TYPE,
    };

    SocialProofRequest socialProofRequest = new SocialProofRequest(
      tweets,
      seedsMap,
      validSocialProofTypes
    );
    HashMap<Long, SocialProofResult> results = new HashMap<>();

    new TweetSocialProofGenerator(bipartiteGraph)
      .computeRecommendations(socialProofRequest, new Random(0))
      .getRankedRecommendations().forEach( recInfo ->
        results.put(((SocialProofResult)recInfo).getNode(), (SocialProofResult)recInfo));

    assertEquals(results.size(), 2);

    Byte2ObjectMap<LongSet> expectedProofs;
    SocialProofResult expected;

    // Test social proofs for tweet 2
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(CLICK_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user3}));
    expected = new SocialProofResult(tweet2, expectedProofs, 0.5, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet2));

    // Test social proofs for tweet 5
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(CLICK_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user2, user3}));
    expected = new SocialProofResult(tweet5, expectedProofs, 1.5, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet5));
  }

  @Test
  public void testTweetSocialProofs2() {
    // Run on another test graph
    LeftIndexedPowerLawMultiSegmentBipartiteGraph bipartiteGraph =
      BipartiteGraphTestHelper.buildSmallTestLeftIndexedPowerLawMultiSegmentBipartiteGraphWithEdgeTypes();

    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(
      new long[] {user1, user2}, new double[] {1.0, 0.5});
    LongSet tweets = new LongArraySet(new long[] {tweet2, tweet3, tweet4, tweet5, tweet6, tweet7, tweet8});

    byte[] validSocialProofTypes = new byte[] {
      FAVORITE_SOCIAL_PROOF_TYPE,
      RETWEET_SOCIAL_PROOF_TYPE
    };

    SocialProofRequest socialProofRequest = new SocialProofRequest(
      tweets,
      seedsMap,
      validSocialProofTypes
    );
    HashMap<Long, SocialProofResult> results = new HashMap<>();

    new TweetSocialProofGenerator(bipartiteGraph)
      .computeRecommendations(socialProofRequest, new Random(0))
      .getRankedRecommendations().forEach( recInfo ->
      results.put(((SocialProofResult)recInfo).getNode(), (SocialProofResult)recInfo));

    assertEquals(5, results.size());

    Byte2ObjectMap<LongSet> expectedProofs;
    SocialProofResult expected;

    // Test social proofs for tweet 3
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user1, user2}));
    expected = new SocialProofResult(tweet3, expectedProofs, 1.5, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet3));

    // Test social proofs for tweet 4
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(RETWEET_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user1}));
    expected = new SocialProofResult(tweet4, expectedProofs, 1, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet4));

    // Test social proofs for tweet 6
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user2}));
    expected = new SocialProofResult(tweet6, expectedProofs, 0.5, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet6));

    // Test social proofs for tweet 7
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user2}));
    expected = new SocialProofResult(tweet7, expectedProofs, 0.5, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet7));

    // Test social proofs for tweet 8
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user1}));
    expectedProofs.put(RETWEET_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user2}));
    expected = new SocialProofResult(tweet8, expectedProofs, 1.5, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet8));

  }

  @Test
  public void testTweetSocialProofsWithInvalidType() {
    // Test cases where the requested social proof types yield no results
    LeftIndexedPowerLawMultiSegmentBipartiteGraph bipartiteGraph =
      BipartiteGraphTestHelper.buildSmallTestLeftIndexedPowerLawMultiSegmentBipartiteGraphWithEdgeTypes();

    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(
      new long[] {user1, user2}, new double[] {1.0, 0.5});
    LongSet tweets = new LongArraySet(new long[] {tweet2, tweet3, tweet4, tweet5, tweet6, tweet7});

    // In the graph there are no valid social proofs corresponding to these types
    byte[] validSocialProofTypes = new byte[] {
      AUTHOR_SOCIAL_PROOF_TYPE,
      IS_MENTIONED_SOCIAL_PROOF_TYPE
    };

    SocialProofRequest socialProofRequest = new SocialProofRequest(
      tweets,
      seedsMap,
      validSocialProofTypes
    );
    HashMap<Long, SocialProofResult> results = new HashMap<>();

    new TweetSocialProofGenerator(bipartiteGraph)
      .computeRecommendations(socialProofRequest, new Random(0))
      .getRankedRecommendations().forEach( recInfo ->
      results.put(((SocialProofResult)recInfo).getNode(), (SocialProofResult)recInfo));

    assertTrue(results.isEmpty());
  }

  @Test
  public void testTweetSocialProofsWithUnfavorites() {
    // Test graph with favorite edges that are potentially unfavorited later
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph graph =
      BipartiteGraphTestHelper.buildTestNodeMetadataLeftIndexedMultiSegmentBipartiteGraphWithUnfavorite();

    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(
      new long[] {user1, user2, user3, user4, user5, user6, user7,
        user8, user9, user10, user11, user12, user13, user14},
      new double[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1});
    LongSet tweets = new LongArraySet(
      new long[] {tweet1, tweet2, tweet3, tweet4, tweet5, tweet6, tweet7,
        tweet8, tweet9, tweet10, tweet11, tweet12, tweet13});

    byte[] validSocialProofTypes = new byte[] {FAVORITE_SOCIAL_PROOF_TYPE};

    SocialProofRequest socialProofRequest = new SocialProofRequest(
      tweets, seedsMap, validSocialProofTypes);
    HashMap<Long, SocialProofResult> results = new HashMap<>();
    new TweetSocialProofGenerator(graph)
      .computeRecommendations(socialProofRequest, new Random(0))
      .getRankedRecommendations().forEach( recInfo ->
      results.put(((SocialProofResult)recInfo).getNode(), (SocialProofResult)recInfo));

    assertEquals(7, results.size());

    Byte2ObjectMap<LongSet> expectedProofs;
    SocialProofResult expected;

    // Test social proofs for tweet 1
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user1}));
    expected = new SocialProofResult(tweet1, expectedProofs, 1, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet1));

    // Test social proofs for tweet 5
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user5}));
    expected = new SocialProofResult(tweet5, expectedProofs, 1, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet5));

    // Test social proofs for tweet 7
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user7}));
    expected = new SocialProofResult(tweet7, expectedProofs, 1, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet7));

    // Test social proofs for tweet 8
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user8, user9}));
    expected = new SocialProofResult(tweet8, expectedProofs, 2, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet8));

    // Test social proofs for tweet 9
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user9, user10}));
    expected = new SocialProofResult(tweet9, expectedProofs, 2, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet9));

    // Test social proofs for tweet 10
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user10}));
    expected = new SocialProofResult(tweet10, expectedProofs, 1, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet10));

    // Test social proofs for tweet 13
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user13}));
    expected = new SocialProofResult(tweet13, expectedProofs, 1, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet13));
  }

  @Test
  public void testTweetSocialProofWithInvalidUnfavorites() {
    // Test cases where unfavorite social proof is the only social proof type. Nothing will return
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph graph =
      BipartiteGraphTestHelper.buildTestNodeMetadataLeftIndexedMultiSegmentBipartiteGraphWithUnfavorite();

    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(
      new long[] {user1, user2, user3, user4, user5, user6, user7,
        user8, user9, user10, user11, user12, user13, user14},
      new double[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1});
    LongSet tweets = new LongArraySet(
      new long[] {tweet1, tweet2, tweet3, tweet4, tweet5, tweet6, tweet7,
        tweet8, tweet9, tweet10, tweet11, tweet12, tweet13});

    byte[] validSocialProofTypes = new byte[] {UNFAVORITE_SOCIAL_PROOF_TYPE};

    SocialProofRequest socialProofRequest = new SocialProofRequest(
      tweets, seedsMap, validSocialProofTypes);
    HashMap<Long, SocialProofResult> results = new HashMap<>();
    new TweetSocialProofGenerator(graph)
      .computeRecommendations(socialProofRequest, new Random(0))
      .getRankedRecommendations().forEach( recInfo ->
      results.put(((SocialProofResult)recInfo).getNode(), (SocialProofResult)recInfo));

    assertEquals(0, results.size());
  }

  @Test
  public void testTweetSocialProofWithRetweetAndUnfavorites() {
    // Test cases where unfavorite tweets are also retweeted
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph graph =
      BipartiteGraphTestHelper.buildTestNodeMetadataLeftIndexedMultiSegmentBipartiteGraphWithUnfavorite();

    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(
      new long[] {user1, user2, user3, user4, user5, user6, user7,
        user8, user9, user10, user11, user12, user13, user14},
      new double[] {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1});
    LongSet tweets = new LongArraySet(
      new long[] {tweet1, tweet2, tweet3, tweet4, tweet5, tweet6, tweet7,
        tweet8, tweet9, tweet10, tweet11, tweet12, tweet13});

    byte[] validSocialProofTypes = new byte[] {FAVORITE_SOCIAL_PROOF_TYPE, RETWEET_SOCIAL_PROOF_TYPE};

    SocialProofRequest socialProofRequest = new SocialProofRequest(
      tweets, seedsMap, validSocialProofTypes);
    HashMap<Long, SocialProofResult> results = new HashMap<>();
    new TweetSocialProofGenerator(graph)
      .computeRecommendations(socialProofRequest, new Random(0))
      .getRankedRecommendations().forEach( recInfo ->
      results.put(((SocialProofResult)recInfo).getNode(), (SocialProofResult)recInfo));

    assertEquals(10, results.size());

    Byte2ObjectMap<LongSet> expectedProofs;
    SocialProofResult expected;

    // Test social proofs for tweet 1
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user1}));
    expected = new SocialProofResult(tweet1, expectedProofs, 1, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet1));

    // Test social proofs for tweet 2
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(RETWEET_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user2}));
    expected = new SocialProofResult(tweet2, expectedProofs, 1, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet2));

    // Test social proofs for tweet 5
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user5}));
    expected = new SocialProofResult(tweet5, expectedProofs, 1, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet5));

    // Test social proofs for tweet 7
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user7}));
    expected = new SocialProofResult(tweet7, expectedProofs, 1, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet7));

    // Test social proofs for tweet 8
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user8, user9}));
    expected = new SocialProofResult(tweet8, expectedProofs, 2, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet8));

    // Test social proofs for tweet 9
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user9, user10}));
    expected = new SocialProofResult(tweet9, expectedProofs, 2, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet9));

    // Test social proofs for tweet 10
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user10}));
    expectedProofs.put(RETWEET_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user11}));
    expected = new SocialProofResult(tweet10, expectedProofs, 2, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet10));

    // Test social proofs for tweet 11
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(RETWEET_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user11}));
    expected = new SocialProofResult(tweet11, expectedProofs, 1, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet11));

    // Test social proofs for tweet 12
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(RETWEET_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user12}));
    expected = new SocialProofResult(tweet12, expectedProofs, 1, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet12));

    // Test social proofs for tweet 13
    expectedProofs = new Byte2ObjectArrayMap<>();
    expectedProofs.put(FAVORITE_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user13}));
    expectedProofs.put(RETWEET_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user14}));
    expected = new SocialProofResult(tweet13, expectedProofs, 2, RecommendationType.TWEET);
    assertEqualSocialProofResults(expected, results.get(tweet13));
  }
}
