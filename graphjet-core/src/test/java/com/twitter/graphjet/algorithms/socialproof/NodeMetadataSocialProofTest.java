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
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2DoubleArrayMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;

import static org.junit.Assert.*;

/**
 * Unit test for the node metadata social proof finder.
 *
 * Build the graph using BipartiteGraphTestHelper, and test the proof finder logic for url metadata
 * and hashtag metadata stored in the metadata of the right nodes.
 */
public class NodeMetadataSocialProofTest {

  @Test
  public void testComputeRecommendations() throws Exception {
    NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph graph = BipartiteGraphTestHelper.
      buildSmallTestNodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraphWithEdgeTypes();

    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(new long[]{2, 3}, new double[]{1.0, 0.5});
    IntSet urlIds = new IntOpenHashSet(new int[]{200, 300});
    IntSet hashtagIds = new IntOpenHashSet(new int[]{100, 101, 102});
    Byte2ObjectMap<IntSet> nodeMetadataTypeToIdsMap = new Byte2ObjectArrayMap<>();
    nodeMetadataTypeToIdsMap.put((byte) RecommendationType.HASHTAG.getValue(), hashtagIds);
    nodeMetadataTypeToIdsMap.put((byte) RecommendationType.URL.getValue(), urlIds);

    byte[] validSocialProofs = new byte[]{1, 2, 3, 4};
    long randomSeed = 918324701982347L;
    Random random = new Random(randomSeed);

    NodeMetadataSocialProofRequest request = new NodeMetadataSocialProofRequest(
        nodeMetadataTypeToIdsMap,
        seedsMap,
        validSocialProofs
    );

    SocialProofResponse socialProofResponse = new NodeMetadataSocialProofGenerator(
      graph
    ).computeRecommendations(request, random);

    List<RecommendationInfo> socialProofResults =
      Lists.newArrayList(socialProofResponse.getRankedRecommendations());

    for (RecommendationInfo recommendationInfo: socialProofResults) {
      NodeMetadataSocialProofResult socialProofResult = (NodeMetadataSocialProofResult) recommendationInfo;
      int nodeMetadataId = socialProofResult.getNodeMetadataId();
      Byte2ObjectMap<Long2ObjectMap<LongSet>> socialProofs = socialProofResult.getSocialProof();

      if (nodeMetadataId == 100) {
        assertEquals(socialProofResult.getSocialProofSize(), 1);
        assertEquals(socialProofs.get((byte) 1).get(2).contains(3), true);
      } else if (nodeMetadataId == 101) {
        assertEquals(socialProofs.isEmpty(), true);
      } else if (nodeMetadataId == 102) {
        assertEquals(socialProofs.isEmpty(), true);
      } else if (nodeMetadataId == 300) {
        assertEquals(socialProofs.isEmpty(), true);
      } else if (nodeMetadataId == 200) {
        assertEquals(socialProofResult.getSocialProofSize(), 3);
        assertEquals(socialProofs.get((byte) 1).get(2).contains(4), true);
        assertEquals(socialProofs.get((byte) 1).get(2).contains(6), true);
        assertEquals(socialProofs.get((byte) 4).get(3).contains(4), true);
      }
    }
  }
}
