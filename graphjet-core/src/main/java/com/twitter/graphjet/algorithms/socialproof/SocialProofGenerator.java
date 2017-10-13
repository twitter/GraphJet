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

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.twitter.graphjet.algorithms.IDMask;
import com.twitter.graphjet.algorithms.RecommendationAlgorithm;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.bipartite.LeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.api.EdgeIterator;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.ByteArraySet;
import it.unimi.dsi.fastutil.bytes.ByteSet;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * SocialProofGenerator shares similar logic with {@link com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCount}.
 * In the request, clients specify a seed user set (left nodes) and an entity set (right nodes).
 * SocialProofGenerator finds the intersection between the seed users' (left node) edges and the given entity set.
 * Only entities with at least one social proof will be returned to clients.
 */
public abstract class SocialProofGenerator implements
  RecommendationAlgorithm<SocialProofRequest, SocialProofResponse> {

  private static final int MAX_EDGES_PER_NODE = 500;
  private static final Byte2ObjectMap<LongSet> EMPTY_SOCIALPROOF_MAP = new Byte2ObjectArrayMap<>();

  private LeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph;
  private final Long2ObjectMap<Byte2ObjectMap<LongSet>> socialProofs;
  private final Long2DoubleMap socialProofWeights;
  protected RecommendationType recommendationType;
  protected IDMask idMask;

  public SocialProofGenerator(
    LeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph
  ) {
    this.leftIndexedBipartiteGraph = leftIndexedBipartiteGraph;
    // Variables socialProofs and socialProofWeights are re-used for each request.
    this.socialProofs = new Long2ObjectOpenHashMap<>();
    this.socialProofWeights = new Long2DoubleOpenHashMap();
  }

  /**
   * Collect social proofs for a given {@link SocialProofRequest}.
   *
   * @param request contains a set of input ids and a set of seed users.
   */
  private void collectRecommendations(SocialProofRequest request) {
    LongSet inputRightNodeIds = request.getRightNodeIds();
    ByteSet socialProofTypes = new ByteArraySet(request.getSocialProofTypes());

    // Iterate through the set of seed users with weights. For each seed user, we go through his edges.
    for (Long2DoubleMap.Entry entry: request.getLeftSeedNodesWithWeight().long2DoubleEntrySet()) {
      long leftNode = entry.getLongKey();
      double weight = entry.getDoubleValue();
      EdgeIterator edgeIterator = leftIndexedBipartiteGraph.getLeftNodeEdges(leftNode);

      int numEdgePerNode = 0;
      if (edgeIterator != null) {
        while (edgeIterator.hasNext() && numEdgePerNode++ < MAX_EDGES_PER_NODE) {
          long rightNode = idMask.restore(edgeIterator.nextLong());
          byte edgeType = edgeIterator.currentEdgeType();

          // If the current id is in the set of inputIds, we find and store its social proof.
          if (inputRightNodeIds.contains(rightNode) && socialProofTypes.contains(edgeType)) {
            if (!socialProofs.containsKey(rightNode)) {
              socialProofs.put(rightNode, new Byte2ObjectArrayMap<>());
              socialProofWeights.put(rightNode, 0);
            }
            Byte2ObjectMap<LongSet> socialProofMap = socialProofs.get(rightNode);

            // We sum the weights of incoming leftNodes as the weight of the rightNode.
            socialProofWeights.put(
              rightNode,
              weight + socialProofWeights.get(rightNode)
            );

            // Get the user set variable by the engagement type.
            if (!socialProofMap.containsKey(edgeType)) {
              socialProofMap.put(edgeType, new LongArraySet());
            }
            LongSet connectingUsers = socialProofMap.get(edgeType);

            // Add the connecting user to the user set.
            if (!connectingUsers.contains(leftNode)) {
              connectingUsers.add(leftNode);
            }
          }
        }
      }
    }
  }

  private void resetSocialProofs() {
    socialProofs.clear();
    socialProofWeights.clear();
  }

  @Override
  public SocialProofResponse computeRecommendations(SocialProofRequest request, Random rand) {
    resetSocialProofs();
    collectRecommendations(request);

    List<RecommendationInfo> socialProofList = new LinkedList<>();
    for (Long id: request.getRightNodeIds()) {
      // Return only ids with at least one social proof
      if (socialProofs.containsKey(id)) {
        socialProofList.add(new SocialProofResult(
          id,
          socialProofs.getOrDefault(id, EMPTY_SOCIALPROOF_MAP),
          socialProofWeights.getOrDefault(id, 0.0),
          recommendationType));
      }
    }

    return new SocialProofResponse(socialProofList);
  }
}
