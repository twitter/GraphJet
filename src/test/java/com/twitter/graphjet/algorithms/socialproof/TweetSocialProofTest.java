package com.twitter.graphjet.algorithms.socialproof;


import java.util.*;

import com.google.common.collect.Lists;

import org.junit.Test;

import com.twitter.graphjet.algorithms.BipartiteGraphTestHelper;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedMultiSegmentBipartiteGraph;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleArrayMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;

import static org.junit.Assert.*;

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

  @Test
  public void testComputeRecommendations() throws Exception {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph bipartiteGraph =
      BipartiteGraphTestHelper.
        buildSmallTestNodeMetadataLeftIndexedMultiSegmentBipartiteGraph();

    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(new long[]{2, 3}, new double[]{1.0, 0.5});
    LongSet tweets = new LongArraySet(new long[]{2, 3, 4, 5});

    byte[] validSocialProofs = new byte[]{0, 1, 2, 3, 4};
    long randomSeed = 918324701982347L;
    Random random = new Random(randomSeed);

    SocialProofRequest socialProofRequest = new SocialProofRequest(
      tweets,
      seedsMap,
      validSocialProofs
    );

    SocialProofResponse socialProofResponse = new TweetSocialProof(
      bipartiteGraph
    ).computeRecommendations(socialProofRequest, random);

    List<RecommendationInfo> socialProofResults =
      Lists.newArrayList(socialProofResponse.getRankedRecommendations());

    for (RecommendationInfo recommendationInfo : socialProofResults) {
      SocialProofResult socialProofResult = (SocialProofResult) recommendationInfo;
      Long tweetId = socialProofResult.getNode();
      Byte2ObjectMap<LongSet> socialProofs = socialProofResult.getSocialProof();

      // Test case for tweet 3 and 4
      if (tweetId == 3 || tweetId == 4) {
        assertEquals(socialProofs.isEmpty(), true);
      }

      // Test case for tweet 2 and 5
      if (tweetId == 2) {
        assertEquals(socialProofs.get((byte) 0).size(), 1);
        assertEquals(socialProofs.get((byte) 0).contains(3), true);
      } else if (tweetId == 5) {
        assertEquals(socialProofs.get((byte) 0).size(), 2);
        assertEquals(socialProofs.get((byte) 0).contains(2), true);
        assertEquals(socialProofs.get((byte) 0).contains(3), true);
      }
    }
  }
}
