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

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.twitter.graphjet.algorithms.IDMask;
import com.twitter.graphjet.algorithms.RecommendationAlgorithm;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.TweetIDMask;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.NodeMetadataMultiSegmentIterator;
import com.twitter.graphjet.hashing.IntArrayIterator;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.ByteArraySet;
import it.unimi.dsi.fastutil.bytes.ByteSet;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * SocialProofGenerator shares similar logic with {@link com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCount}.
 * In the request, clients specify a seed user set (left nodes) and an entity set
 * (right nodes' metadata).
 * EntitySocialProofGenerator finds the intersection between the seed users' (left node) edges and
 * the given entity set by traversing each right node's metadata.
 * Only entities with at least one social proof will be returned to clients.
 */
public abstract class EntitySocialProofGenerator implements
    RecommendationAlgorithm<EntitySocialProofRequest, SocialProofResponse> {

  private static final int MAX_EDGES_PER_NODE = 500;
  private static final Byte2ObjectMap<Long2ObjectMap<LongSet>> EMPTY_SOCIALPROOF_MAP =
      new Byte2ObjectArrayMap<>();

  private NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph graph;
  // Entity (Int) -> Engagements (Byte) -> User (Long) -> Tweets (LongSet)
  private final Int2ObjectMap<Byte2ObjectMap<Long2ObjectMap<LongSet>>> socialProofs;
  // Entity (Int) -> Sum of social proof edges (Double)
  private final Int2DoubleMap socialProofWeights;
  protected RecommendationType recommendationType;
  protected IDMask idMask;

  public EntitySocialProofGenerator(
      NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph graph,
      TweetIDMask mask,
      RecommendationType recommendationType
  ) {
    this.graph = graph;
    this.idMask = mask;
    this.recommendationType = recommendationType;
    // Variables socialProofs and socialProofWeights are re-used for each request.
    this.socialProofs = new Int2ObjectOpenHashMap<>();
    this.socialProofWeights = new Int2DoubleOpenHashMap();
  }

  private void updateSocialProofWeight(int entity, double weight) {
    // We sum the weights of incoming leftNodes as the weight of the rightNode.
    socialProofWeights.put(
        entity,
        weight + socialProofWeights.get(entity)
    );
  }

  private void addSocialProof(int entity, byte edgeType, long leftNode, long rightNode) {
    if (!socialProofs.containsKey(entity)) {
      socialProofs.put(entity, new Byte2ObjectArrayMap<>());
      socialProofWeights.put(entity, 0);
    }
    Byte2ObjectMap<Long2ObjectMap<LongSet>> socialProofMap = socialProofs.get(entity);

    // Get the user to tweets map variable by the engagement type.
    if (!socialProofMap.containsKey(edgeType)) {
      socialProofMap.put(edgeType, new Long2ObjectArrayMap<>());
    }
    Long2ObjectMap<LongSet> userToTweetsMap = socialProofMap.get(edgeType);

    // Add the connecting user to the user map.
    if (!userToTweetsMap.containsKey(leftNode)) {
      userToTweetsMap.put(leftNode, new LongArraySet());
    }
    LongSet connectingTweets = userToTweetsMap.get(leftNode);

    // Add the connecting tweet to the tweet set.
    if (!connectingTweets.contains(rightNode)) {
      connectingTweets.add(rightNode);
    }
  }

  /**
   * Collect social proofs for a given {@link SocialProofRequest}.
   *
   * @param request contains a set of input ids and a set of seed users.
   */
  private void collectRecommendations(EntitySocialProofRequest request) {
    socialProofs.clear();
    socialProofWeights.clear();
    IntSet inputEntityIds = request.getEntityIds();
    ByteSet socialProofTypes = new ByteArraySet(request.getSocialProofTypes());
    byte entityType = (byte) recommendationType.getValue();

    // Iterate through the set of seed users with weights. For each seed user, we go through his edges.
    for (Long2DoubleMap.Entry entry: request.getLeftSeedNodesWithWeight().long2DoubleEntrySet()) {
      long leftNode = entry.getLongKey();
      double weight = entry.getDoubleValue();
      NodeMetadataMultiSegmentIterator edgeIterator =
        (NodeMetadataMultiSegmentIterator) graph.getLeftNodeEdges(leftNode);
      if (edgeIterator == null) { continue; }

      int numEdgePerNode = 0;
      while (edgeIterator.hasNext() && numEdgePerNode++ < MAX_EDGES_PER_NODE) {
        long rightNode = idMask.restore(edgeIterator.nextLong());
        byte edgeType = edgeIterator.currentEdgeType();
        if (!socialProofTypes.contains(edgeType)) { continue; }

        IntArrayIterator metadataIterator =
          (IntArrayIterator) edgeIterator.getRightNodeMetadata(entityType);
        if (metadataIterator == null) { continue; }

        while (metadataIterator.hasNext()) {
          int entity = metadataIterator.nextInt();
          // If the current id is in the set of inputIds, we find and store its social proof.
          if (inputEntityIds.contains(entity)) {
            addSocialProof(entity, edgeType, leftNode, rightNode);
            updateSocialProofWeight(entity, weight);
          }
        }
      }
    }
  }

  @Override
  public SocialProofResponse computeRecommendations(EntitySocialProofRequest request, Random rand) {
    collectRecommendations(request);

    List<RecommendationInfo> socialProofList = new LinkedList<>();
    for (Integer id: request.getEntityIds()) {
      // Return only ids with at least one social proof
      if (socialProofs.containsKey(id)) {
        socialProofList.add(new EntitySocialProofResult(
            id,
            socialProofs.getOrDefault(id, EMPTY_SOCIALPROOF_MAP),
            socialProofWeights.getOrDefault(id, 0.0),
            recommendationType));
      }
    }

    return new SocialProofResponse(socialProofList);
  }
}
