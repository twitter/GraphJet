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


package com.twitter.graphjet.algorithms.socialproof;


import java.util.*;

import com.google.common.collect.Lists;

import org.junit.Test;

import com.twitter.graphjet.algorithms.BipartiteGraphTestHelper;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationType;
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
import static com.twitter.graphjet.algorithms.RecommendationRequest.REPLY_SOCIAL_PROOF_TYPE;
import static com.twitter.graphjet.algorithms.RecommendationRequest.RETWEET_SOCIAL_PROOF_TYPE;

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

  private long user2 = 2;
  private long user3 = 3;

  private long tweet2 = 2;
  private long tweet3 = 3;
  private long tweet4 = 4;
  private long tweet5 = 5;

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
    Random random = new Random(918324701982347L);

    SocialProofRequest socialProofRequest = new SocialProofRequest(
      tweets,
      seedsMap,
      validSocialProofTypes
    );
    HashMap<Long, SocialProofResult> results = new HashMap<>();

    new TweetSocialProofGenerator(bipartiteGraph)
      .computeRecommendations(socialProofRequest, random)
      .getRankedRecommendations().forEach( recInfo ->
        results.put(((SocialProofResult)recInfo).getNode(), (SocialProofResult)recInfo));

    assertEquals(results.size(), 2);

    // Test social proofs for tweet 2
    SocialProofResult actual2 = results.get(tweet2);
    Byte2ObjectMap<LongSet> expectedProofs2 = new Byte2ObjectArrayMap<>();
    expectedProofs2.put(CLICK_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user3}));
    SocialProofResult expected2 =
        new SocialProofResult(tweet2, expectedProofs2, 0.5, RecommendationType.TWEET);
    assertEquals(expected2.getNode(), actual2.getNode());
    assertEquals(expected2.getSocialProof(), actual2.getSocialProof());
    assertEquals(expected2.getSocialProofSize(), actual2.getSocialProofSize());
    assertEquals(0, Double.compare(expected2.getWeight(), actual2.getWeight()));

    // Test social proofs for tweet 5
    SocialProofResult actual5 = results.get(tweet5);
    Byte2ObjectMap<LongSet> expectedProofs5 = new Byte2ObjectArrayMap<>();
    expectedProofs5.put(CLICK_SOCIAL_PROOF_TYPE, new LongArraySet(new long[] {user2, user3}));
    SocialProofResult expected5 =
      new SocialProofResult(tweet5, expectedProofs5, 1.5, RecommendationType.TWEET);
    assertEquals(expected5.getNode(), actual5.getNode());
    assertEquals(expected5.getSocialProof(), actual5.getSocialProof());
    assertEquals(expected5.getSocialProofSize(), actual5.getSocialProofSize());
    assertEquals(0, Double.compare(expected5.getWeight(), actual5.getWeight()));
  }
}
