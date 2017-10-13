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

import com.twitter.graphjet.algorithms.RecommendationRequest;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class NodeMetadataSocialProofRequest extends RecommendationRequest {
  private static final LongSet EMPTY_SET = new LongArraySet();

  private final Long2DoubleMap leftSeedNodesWithWeight;
  private final Byte2ObjectMap<IntSet> nodeMetadataTypeToIdsMap;

  /**
   * Create a social proof request for a right node's metadata.
   *
   * @param nodeMetadataTypeToIdsMap  The map of node metadata type to ids map. Used to specify
   *                                  which node metadata types to retrieve social proof for, and
   *                                  for which ids. These metadata types are derived from
   *                                  the RecommendationType enum.
   * @param weightedSeedNodes         The set of left nodes to be used as social proofs.
   * @param socialProofTypes          The social proof types to return.
   */
  public NodeMetadataSocialProofRequest(
    Byte2ObjectMap<IntSet> nodeMetadataTypeToIdsMap,
    Long2DoubleMap weightedSeedNodes,
    byte[] socialProofTypes
  ) {
    super(0, EMPTY_SET, socialProofTypes);
    this.leftSeedNodesWithWeight = weightedSeedNodes;
    this.nodeMetadataTypeToIdsMap = nodeMetadataTypeToIdsMap;
  }

  public Long2DoubleMap getLeftSeedNodesWithWeight() {
    return leftSeedNodesWithWeight;
  }

  public Byte2ObjectMap<IntSet> getNodeMetadataTypeToIdsMap() {
    return this.nodeMetadataTypeToIdsMap;
  }

}
