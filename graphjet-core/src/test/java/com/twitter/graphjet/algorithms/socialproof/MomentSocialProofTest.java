/**
 * Copyright 2017 Twitter. All rights reserved.
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
import com.twitter.graphjet.bipartite.LeftIndexedMultiSegmentBipartiteGraph;

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
public class MomentSocialProofTest {

  @Test
  public void testComputeRecommendations() throws Exception {
    LeftIndexedMultiSegmentBipartiteGraph bipartiteGraph = BipartiteGraphTestHelper.
      buildSmallTestLeftIndexedPowerLawMultiSegmentBipartiteGraphWithEdgeTypes();

    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(new long[]{2, 3}, new double[]{1.0, 0.5});
    LongSet moments = new LongArraySet(new long[]{2, 3, 4, 5});

    byte[] validSocialProofs = new byte[]{0, 1, 2};
    long randomSeed = 918324701982347L;
    Random random = new Random(randomSeed);

    SocialProofRequest socialProofRequest = new SocialProofRequest(
      moments,
      seedsMap,
      validSocialProofs
    );

    SocialProofResponse socialProofResponse = new MomentSocialProofGenerator(
      bipartiteGraph
    ).computeRecommendations(socialProofRequest, random);

    List<RecommendationInfo> socialProofResults =
        Lists.newArrayList(socialProofResponse.getRankedRecommendations());

    for (RecommendationInfo recommendationInfo: socialProofResults) {
      SocialProofResult socialProofResult = (SocialProofResult) recommendationInfo;
      Long momentId = socialProofResult.getNode();
      Byte2ObjectMap<LongSet> socialProofs = socialProofResult.getSocialProof();

      if (momentId == 2 || momentId == 4) {
        assertEquals(socialProofs.isEmpty(), true);
      } else if (momentId == 3) {
        assertEquals(socialProofs.get((byte) 1).size(), 2);
        assertEquals(socialProofs.get((byte) 1).contains(2), true);
        assertEquals(socialProofs.get((byte) 1).contains(3), true);
      } else if (momentId == 5) {
        assertEquals(socialProofs.get((byte) 0).size(), 1);
        assertEquals(socialProofs.get((byte) 0).contains(2), true);
      }
    }
  }
}
