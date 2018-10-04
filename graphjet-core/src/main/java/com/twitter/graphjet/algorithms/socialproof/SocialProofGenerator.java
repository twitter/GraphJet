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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.twitter.graphjet.algorithms.IDMask;
import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.NodeInfoHelper;
import com.twitter.graphjet.algorithms.RecommendationAlgorithm;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.bipartite.LeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.ByteArraySet;
import it.unimi.dsi.fastutil.bytes.ByteSet;
import it.unimi.dsi.fastutil.longs.Long2ByteArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;

import static com.twitter.graphjet.algorithms.RecommendationRequest.FAVORITE_SOCIAL_PROOF_TYPE;
import static com.twitter.graphjet.algorithms.RecommendationRequest.UNFAVORITE_SOCIAL_PROOF_TYPE;

/**
 * SocialProofGenerator shares similar logic with
 * {@link com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCount}.
 * In the request, clients specify a seed user set (left nodes) and an entity set (right nodes).
 * SocialProofGenerator finds the intersection between the seed users' (left node) edges and the
 * given entity set.
 * Only entities with at least one social proof will be returned to clients.
 */
public abstract class SocialProofGenerator implements
  RecommendationAlgorithm<SocialProofRequest, SocialProofResponse> {

  private static final int MAX_EDGES_PER_NODE = 500;
  private static int NUM_OF_SOCIAL_PROOF_TYPES = RecommendationRequest.MAX_SOCIAL_PROOF_TYPE_SIZE;

  private LeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph;
  protected RecommendationType recommendationType;
  protected IDMask idMask;

  private final Long2ObjectMap<NodeInfo> visitedRightNodes;
  private final Long2ByteMap seenEdgesPerNode;

  public SocialProofGenerator(
    LeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph
  ) {
    // We re-use these data containers to avoid redundant allocations across requests
    this.visitedRightNodes = new Long2ObjectOpenHashMap<>();
    this.seenEdgesPerNode = new Long2ByteArrayMap();

    this.leftIndexedBipartiteGraph = leftIndexedBipartiteGraph;
  }

  private void reset() {
    seenEdgesPerNode.clear();
    visitedRightNodes.clear();
  }

  /**
   * Given a nodeInfo containing the social proofs and weight information regarding a rightNode,
   * convert and store these data in a SocialProofResult object, to comply with the class interface.
   *
   * @param nodeInfo Contains all the social proofs on a particular right node, along with the
   * accumulated node weight
   */
  private SocialProofResult makeSocialProofResult(NodeInfo nodeInfo) {
    Byte2ObjectArrayMap<LongSet> socialProofsMap = new Byte2ObjectArrayMap<>();

    for (int socialProofType = 0; socialProofType < NUM_OF_SOCIAL_PROOF_TYPES; socialProofType++) {
      SmallArrayBasedLongToDoubleMap socialProofsByType = nodeInfo.getSocialProofs()[socialProofType];
      if (socialProofsByType == null || socialProofsByType.size() == 0) {
        continue;
      }
      LongSet rightNodeIds = new LongArraySet(
        Arrays.copyOfRange(socialProofsByType.keys(), 0, socialProofsByType.size()));
      socialProofsMap.put((byte)socialProofType, rightNodeIds);
    }

    return new SocialProofResult(
      nodeInfo.getNodeId(),
      socialProofsMap,
      nodeInfo.getWeight(),
      recommendationType
    );
  }

  /**
   * Generate the social proof recommendations based on the nodeInfo collected previously.
   */
  private SocialProofResponse generateRecommendationFromNodeInfo() {
    List<RecommendationInfo> results = new LinkedList<>();
    for (Map.Entry<Long, NodeInfo> entry: this.visitedRightNodes.entrySet()) {
      results.add(makeSocialProofResult(entry.getValue()));
    }
    return new SocialProofResponse(results);
  }

  /**
   * Remove edges that were unfavorited first, and then generate the social proof recommendations
   * based on the filtered nodeInfo.
   */
  private SocialProofResponse removeUnfavoritesAndGenerateRecommendationsFromNodeInfo() {
    List<RecommendationInfo> results = new LinkedList<>();

    for (Map.Entry<Long, NodeInfo> entry: this.visitedRightNodes.entrySet()) {
      NodeInfo nodeInfo = entry.getValue();
      // Remove unfavorited edges from the node, and skip the node if it has no social proof left
      boolean isNodeModified = NodeInfoHelper.removeUnfavoritedSocialProofs(nodeInfo);
      if (isNodeModified && !NodeInfoHelper.nodeInfoHasValidSocialProofs(nodeInfo)) {
        continue;
      }
      results.add(makeSocialProofResult(nodeInfo));
    }
    return new SocialProofResponse(results);
  }

  private void updateVisitedRightNodes(long leftNode, long rightNode, byte edgeType, double weight) {
    NodeInfo nodeInfo;
    if (!this.visitedRightNodes.containsKey(rightNode)) {
      nodeInfo = new NodeInfo(rightNode, 0.0, NUM_OF_SOCIAL_PROOF_TYPES);
      this.visitedRightNodes.put(rightNode, nodeInfo);
    } else {
      nodeInfo = this.visitedRightNodes.get(rightNode);
    }

    nodeInfo.addToWeight(weight);
    nodeInfo.addToSocialProof(leftNode, edgeType, 0, weight);
  }

  /**
   * Collect social proofs for a given {@link SocialProofRequest}.
   *
   * @param leftSeedNodesWithWeight Engagement edges from these left nodes are iterated and collected
   * @param rightNodeIds            Right nodes for which we want to generate social proofs
   * @param validSocialProofTypes   Social proof types that we are interested in
   */
  private void collectRightNodeInfo(
    Long2DoubleMap leftSeedNodesWithWeight, LongSet rightNodeIds, byte[] validSocialProofTypes) {
    ByteSet socialProofTypeSet = new ByteArraySet(validSocialProofTypes);

    // Iterate through the set of left node seeds with weights.
    // For each left node, go through its edges and collect the engagements on the right nodes
    for (Long2DoubleMap.Entry entry: leftSeedNodesWithWeight.long2DoubleEntrySet()) {
      long leftNode = entry.getLongKey();
      EdgeIterator edgeIterator = leftIndexedBipartiteGraph.getLeftNodeEdges(leftNode);
      if (edgeIterator == null) {
        continue;
      }

      int numEdgePerNode = 0;
      double weight = entry.getDoubleValue();
      seenEdgesPerNode.clear();

      // Sequentially iterate through the latest MAX_EDGES_PER_NODE edges per node
      while (edgeIterator.hasNext() && numEdgePerNode++ < MAX_EDGES_PER_NODE) {
        long rightNode = idMask.restore(edgeIterator.nextLong());
        byte edgeType = edgeIterator.currentEdgeType();

        boolean hasSeenRightNodeFromEdge =
          seenEdgesPerNode.containsKey(rightNode) && seenEdgesPerNode.get(rightNode) == edgeType;

        boolean isValidEngagement = rightNodeIds.contains(rightNode) &&
          socialProofTypeSet.contains(edgeType);

        if (hasSeenRightNodeFromEdge || !isValidEngagement) {
          continue;
        }
        updateVisitedRightNodes(leftNode, rightNode, edgeType, weight);
      }
    }
  }

  /**
   * When the incoming request asks for Favorite as one of the social proofs, we must check for
   * Unfavorite edges as well, and remove the corresponding unfavorited edges.
   */
  private boolean shouldRemoveUnfavoritedEdges(SocialProofRequest request) {
    for (byte socialProofType: request.getSocialProofTypes()) {
      if (socialProofType == FAVORITE_SOCIAL_PROOF_TYPE ||
          socialProofType == UNFAVORITE_SOCIAL_PROOF_TYPE) {
        return true;
      }
    }
    return false;
  }

  /**
   * Given an array of social proof types, return a new array with
   * {@link RecommendationRequest#UNFAVORITE_SOCIAL_PROOF_TYPE} type appended to the end
   */
  private byte[] appendUnfavoriteType(byte[] oldSocialProofTypes) {
    byte[] socialProofTypesWithUnfav =
      Arrays.copyOf(oldSocialProofTypes, oldSocialProofTypes.length + 1);
    socialProofTypesWithUnfav[socialProofTypesWithUnfav.length - 1] = UNFAVORITE_SOCIAL_PROOF_TYPE;

    return socialProofTypesWithUnfav;
  }

  @Override
  public SocialProofResponse computeRecommendations(SocialProofRequest request, Random rand) {
    reset();

    Long2DoubleMap leftSeedNodesWithWeight = request.getLeftSeedNodesWithWeight();
    LongSet rightNodeIds = request.getRightNodeIds();

    if (shouldRemoveUnfavoritedEdges(request)) {
      collectRightNodeInfo(
        leftSeedNodesWithWeight, rightNodeIds, appendUnfavoriteType(request.getSocialProofTypes()));
      return removeUnfavoritesAndGenerateRecommendationsFromNodeInfo();
    } else {
      collectRightNodeInfo(leftSeedNodesWithWeight, rightNodeIds, request.getSocialProofTypes());
      return generateRecommendationFromNodeInfo();
    }
  }
}
