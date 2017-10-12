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

import it.unimi.dsi.fastutil.bytes.*;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.*;

/**
 * NodeMetadataSocialProofGenerator shares similar logic with
 * {@link com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCount}.
 * In the request, clients specify a seed user set (left nodes) and a node metadata set
 * (right node's metadata).
 * NodeMetadataSocialProofGenerator finds the intersection between the seed users (left nodes)
 * and the given node metadata id set. It accomplishes this by traversing each outgoing edge from
 * the seed set and each right node's metadata from those edges.
 * Only node metadatas with at least one social proof will be returned to clients.
 */
public abstract class NodeMetadataSocialProofGenerator implements
  RecommendationAlgorithm<NodeMetadataSocialProofRequest, SocialProofResponse> {

  private static final int MAX_EDGES_PER_NODE = 500;
  private static final Byte2ObjectMap<Long2ObjectMap<LongSet>> EMPTY_SOCIALPROOF_MAP =
    new Byte2ObjectArrayMap<>();

  private NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph graph;
  // NodeMetadata (Int) -> Engagement (Byte) -> User (Long) -> Tweets (LongSet)
  private final Int2ObjectMap<Byte2ObjectMap<Long2ObjectMap<LongSet>>> socialProofs;
  // NodeMetadata (Int) -> Sum of social proof edges (Double)
  private final Int2DoubleMap socialProofWeights;
  protected RecommendationType recommendationType;
  protected IDMask idMask;

  public NodeMetadataSocialProofGenerator(
    NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph graph,
    TweetIDMask mask,
    RecommendationType recommendationType
  ) {
    this.graph = graph;
    this.idMask = mask;
    this.recommendationType = recommendationType;
    // Variables socialProofs and socialProofWeights are re-used for each request.
    // We chose to use an ArrayMap since, on average, we will request social proof for very few
    // (<10) metadatas in a single request.
    this.socialProofs = new Int2ObjectArrayMap<>();
    this.socialProofWeights = new Int2DoubleArrayMap();
  }

  private void updateSocialProofWeight(int metadataId, double weight) {
    // We sum the weights of incoming leftNodes as the weight of the rightNode.
    socialProofWeights.put(
      metadataId,
      weight + socialProofWeights.get(metadataId)
    );
  }

  private void addSocialProof(int metadataId, byte edgeType, long leftNode, long rightNode) {
    if (!socialProofs.containsKey(metadataId)) {
      // We chose to use an ArrayMap here since we will at most have 5 edge types that we request
      // social proof for.
      socialProofs.put(metadataId, new Byte2ObjectArrayMap<>());
      socialProofWeights.put(metadataId, 0);
    }
    Byte2ObjectMap<Long2ObjectMap<LongSet>> socialProofMap = socialProofs.get(metadataId);

    // Get the user to tweets map variable by the engagement type.
    if (!socialProofMap.containsKey(edgeType)) {
      // We chose to use an OpenHashMap since a single edge type may have dozens or even hundreds
      // of user seed ids associated.
      socialProofMap.put(edgeType, new Long2ObjectOpenHashMap<>());
    }
    Long2ObjectMap<LongSet> userToTweetsMap = socialProofMap.get(edgeType);

    // Add the connecting user to the user map.
    if (!userToTweetsMap.containsKey(leftNode)) {
      // We chose to use an OpenHashSet since a single user may engage with dozens or even
      // hundreds of tweet ids.
      userToTweetsMap.put(leftNode, new LongOpenHashSet());
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
   * @param request contains a set of input metadata ids and a set of seed users.
   */
  private void collectRecommendations(NodeMetadataSocialProofRequest request) {
    socialProofs.clear();
    socialProofWeights.clear();
    IntSet inputNodeMetadataIds = request.getNodeMetadataIds();
    ByteSet socialProofTypes = new ByteArraySet(request.getSocialProofTypes());
    byte nodeMetadataType = (byte) this.recommendationType.getValue();

    // Iterate through the set of seed users with weights.
    for (Long2DoubleMap.Entry entry: request.getLeftSeedNodesWithWeight().long2DoubleEntrySet()) {
      long leftNode = entry.getLongKey();
      double weight = entry.getDoubleValue();
      NodeMetadataMultiSegmentIterator edgeIterator =
        (NodeMetadataMultiSegmentIterator) graph.getLeftNodeEdges(leftNode);
      if (edgeIterator == null) continue;

      // For each seed user node, we traverse all of its outgoing edges, up to the MAX_EDGES_PER_NODE.
      int numEdgePerNode = 0;
      while (edgeIterator.hasNext() && numEdgePerNode++ < MAX_EDGES_PER_NODE) {
        long rightNode = idMask.restore(edgeIterator.nextLong());
        byte edgeType = edgeIterator.currentEdgeType();
        if (!socialProofTypes.contains(edgeType)) continue;

        IntArrayIterator metadataIterator =
          (IntArrayIterator) edgeIterator.getRightNodeMetadata(nodeMetadataType);
        if (metadataIterator == null) continue;

        // For each of the accepted edges, we traverse the metadata ids for the specified node
        // metadata type (either RecommendationType.HASHTAG or RecommendationType.URL).
        while (metadataIterator.hasNext()) {
          int metadataId = metadataIterator.nextInt();
          // If the current id is in the set of inputIds, we find and store its social proof.
          if (inputNodeMetadataIds.contains(metadataId)) {
            addSocialProof(metadataId, edgeType, leftNode, rightNode);
            updateSocialProofWeight(metadataId, weight);
          }
        }
      }
    }
  }

  @Override
  public SocialProofResponse computeRecommendations(NodeMetadataSocialProofRequest request, Random rand) {
    collectRecommendations(request);

    List<RecommendationInfo> socialProofList = new LinkedList<>();
    for (Integer id: request.getNodeMetadataIds()) {
      // Return only ids with at least one social proof.
      if (socialProofs.containsKey(id)) {
        socialProofList.add(new NodeMetadataSocialProofResult(
          id,
          // The EMPTY_SOCIALPROOF_MAP will never be used, since we check if (socialProofs.containsKey(id)).
          socialProofs.getOrDefault(id, EMPTY_SOCIALPROOF_MAP),
          // The weight of 0.0 will never be used, since we insert the socialProofWeights entry
          // when we insert the corresponding socialProofs entry.
          socialProofWeights.getOrDefault(id, 0.0),
          recommendationType));
      }
    }

    return new SocialProofResponse(socialProofList);
  }
}
