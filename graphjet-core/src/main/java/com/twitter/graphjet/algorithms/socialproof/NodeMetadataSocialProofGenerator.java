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

import java.util.ArrayList;
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
 * In the request clients specify a seed user set (left nodes), a node metadata set
 * (right node's metadata), as well as the RecommendationTypes of the node metadata set.
 * This allows for clients to request social proof for all supported RecommendationTypes within
 * a right node's metadata in a single request.
 *
 * NodeMetadataSocialProofGenerator finds the intersection between the seed users (left nodes)
 * and the given node metadata id set. It accomplishes this by traversing each outgoing edge from
 * the seed set and each right node's metadata from those edges.
 * Only node metadata with at least one social proof will be returned to clients.
 */
public class NodeMetadataSocialProofGenerator implements
  RecommendationAlgorithm<NodeMetadataSocialProofRequest, SocialProofResponse> {

  private static final int MAX_EDGES_PER_NODE = 500;
  private static final Byte2ObjectMap<Long2ObjectMap<LongSet>> EMPTY_SOCIAL_PROOF_MAP =
    new Byte2ObjectArrayMap<>();

  private NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph graph;
  // The list of social proof mappings for each RecommendationType requested.
  // The social proof mapping is as follows:
  // NodeMetadata (Int) -> Engagement (Byte) -> User (Long) -> Tweets (LongSet)
  private final List<Int2ObjectMap<Byte2ObjectMap<Long2ObjectMap<LongSet>>>> socialProofs;
  // The list of social proof weight mappings for each RecommendationType requested.
  // The social proof weight mapping is as follows:
  // NodeMetadata (Int) -> Sum of social proof edges (Double)
  private final List<Int2DoubleMap> socialProofWeights;
  protected IDMask idMask;

  public NodeMetadataSocialProofGenerator(
    NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph graph
  ) {
    this.graph = graph;
    this.idMask = new TweetIDMask();
    // Variables such as socialProofs and socialProofWeights, are re-used for each request.
    // We choose to use an ArrayList of size RecommendationType.METADATASIZE.
    // We support up to 256 metadata types since each node's metadatatype is represented as a byte.
    // This can be observed when we retrieve the metadataIterator for a given right node since
    // we iterate over the metadata ids for a single metadata type at a time.
    this.socialProofs = new ArrayList<>(RecommendationType.METADATASIZE.getValue());
    this.socialProofWeights = new ArrayList<>(RecommendationType.METADATASIZE.getValue());
    for (int i = 0; i < RecommendationType.METADATASIZE.getValue(); i++) {
      // We choose to use an OpenHashMap to future proof the potential performance degradation.
      // Additionally, the memory is re-used, so we are less concerned about the memory overhead
      // of OpenHashMap compared with ArrayMap.
      socialProofs.add(i, new Int2ObjectOpenHashMap<>());
      socialProofWeights.add(i, new Int2DoubleOpenHashMap());
    }
  }

  private void addSocialProofWeight(byte nodeMetaDataType, int metadataId, double weight) {
    Int2DoubleMap socialProofToWeightMap = socialProofWeights.get(nodeMetaDataType);

    if (!socialProofToWeightMap.containsKey(metadataId)) {
      socialProofToWeightMap.put(metadataId, 0);
    }
    // We sum the weights of incoming leftNodes as the weight of the rightNode.
    socialProofToWeightMap.put(
      metadataId,
      weight + socialProofToWeightMap.get(metadataId)
    );
  }

  private void addSocialProof(
    byte nodeMetaDataType,
    int metadataId,
    byte edgeType,
    long leftNode,
    long rightNode
  ) {
    Int2ObjectMap<Byte2ObjectMap<Long2ObjectMap<LongSet>>> nodeMetadataToSocialProofMap =
      socialProofs.get(nodeMetaDataType);

    if (!nodeMetadataToSocialProofMap.containsKey(metadataId)) {
      // We choose ArrayMap over OpenHashMap since the request will have at most a few edge types,
      // usually less than five.
      nodeMetadataToSocialProofMap.put(metadataId, new Byte2ObjectArrayMap<>());
    }
    Byte2ObjectMap<Long2ObjectMap<LongSet>> socialProofMap =
      nodeMetadataToSocialProofMap.get(metadataId);

    // Get the user to tweets map variable by the engagement type.
    if (!socialProofMap.containsKey(edgeType)) {
      // We choose to use an OpenHashMap since a single edge type may have dozens or even hundreds
      // of user seed ids associated.
      socialProofMap.put(edgeType, new Long2ObjectOpenHashMap<>());
    }
    Long2ObjectMap<LongSet> userToTweetsMap = socialProofMap.get(edgeType);

    // Add the connecting user to the user map.
    if (!userToTweetsMap.containsKey(leftNode)) {
      // We choose to use an OpenHashSet since a single user may engage with dozens or even
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
    Byte2ObjectMap<IntSet> inputNodeMetadataTypeToIdsMap = request.getNodeMetadataTypeToIdsMap();
    ByteSet socialProofTypes = new ByteArraySet(request.getSocialProofTypes());

    // Iterate through the set of seed users with weights.
    for (Long2DoubleMap.Entry seedWithWeight: request.getLeftSeedNodesWithWeight().long2DoubleEntrySet()) {
      long leftNode = seedWithWeight.getLongKey();
      double weight = seedWithWeight.getDoubleValue();
      NodeMetadataMultiSegmentIterator edgeIterator =
        (NodeMetadataMultiSegmentIterator) graph.getLeftNodeEdges(leftNode);
      if (edgeIterator == null) continue;

      // For each seed user node, we traverse all of its outgoing edges, up to the MAX_EDGES_PER_NODE.
      int numEdgePerNode = 0;
      while (edgeIterator.hasNext() && numEdgePerNode++ < MAX_EDGES_PER_NODE) {
        long rightNode = idMask.restore(edgeIterator.nextLong());
        byte edgeType = edgeIterator.currentEdgeType();
        if (!socialProofTypes.contains(edgeType)) continue;

        // For each of the accepted edges, we traverse the metadata ids for all of the specified node
        // metadata types in the request.
        for (byte nodeMetadataType: request.getNodeMetadataTypeToIdsMap().keySet()) {
          IntSet inputNodeMetadataIds = inputNodeMetadataTypeToIdsMap.get(nodeMetadataType);
          IntArrayIterator metadataIterator =
            (IntArrayIterator) edgeIterator.getRightNodeMetadata(nodeMetadataType);
          if (metadataIterator == null) continue;

          while (metadataIterator.hasNext()) {
            int metadataId = metadataIterator.nextInt();
            // If the current id is in the set of inputNodeMetadataIds,
            // we find and store its social proof.
            if (!inputNodeMetadataIds.contains(metadataId)) continue;
            addSocialProof(nodeMetadataType, metadataId, edgeType, leftNode, rightNode);
            addSocialProofWeight(nodeMetadataType, metadataId, weight);
          }
        }
      }
    }
  }

  private void resetSocialProofs() {
    for (Int2ObjectMap nodeMetadataMap: socialProofs) {
      nodeMetadataMap.clear();
    }
    for (Int2DoubleMap socialProofToWeightMap: socialProofWeights) {
      socialProofToWeightMap.clear();
    }
  }

  @Override
  public SocialProofResponse computeRecommendations(NodeMetadataSocialProofRequest request, Random rand) {
    resetSocialProofs();
    collectRecommendations(request);

    List<RecommendationInfo> socialProofList = new LinkedList<>();
    for (byte inputNodeMetadataType : request.getNodeMetadataTypeToIdsMap().keySet()) {
      for (int inputNodeMetadataId : request.getNodeMetadataTypeToIdsMap().get(inputNodeMetadataType)) {
        Int2ObjectMap<Byte2ObjectMap<Long2ObjectMap<LongSet>>> nodeMetadataToSocialProofMap =
          socialProofs.get(inputNodeMetadataType);
        Int2DoubleMap socialProofToWeightMap = socialProofWeights.get(inputNodeMetadataType);
        // Return only ids with at least one social proof.
        if (!nodeMetadataToSocialProofMap.containsKey(inputNodeMetadataId)) continue;
        socialProofList.add(new NodeMetadataSocialProofResult(
          inputNodeMetadataId,
          // The EMPTY_SOCIALPROOF_MAP will never be used, since we check if
          // (nodeMetadataMap.containsKey(id)).
          nodeMetadataToSocialProofMap.getOrDefault(inputNodeMetadataId, EMPTY_SOCIAL_PROOF_MAP),
          // The weight of 0.0 will never be used, since we insert the socialProofWeights entry
          // when we insert the corresponding socialProofs entry.
          socialProofToWeightMap.getOrDefault(inputNodeMetadataId, 0.0),
          RecommendationType.at(inputNodeMetadataType)));
      }
    }

    return new SocialProofResponse(socialProofList);
  }
}
