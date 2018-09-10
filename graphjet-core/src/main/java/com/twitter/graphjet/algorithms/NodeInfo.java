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


package com.twitter.graphjet.algorithms;

import com.google.common.base.Objects;

import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

/**
 * This class contains information about a node that's a potential recommendation, by collecting the
 * engagements (social proofs) from other nodes. An example usage is to aggregate the
 * different social proof engagements on a tweet from different users.
 */
public class NodeInfo implements Comparable<NodeInfo> {
  private final long nodeId;
  private int[][] nodeMetadata;
  private double weight;
  private int numVisits;
  private SmallArrayBasedLongToDoubleMap[] socialProofs;
  private static final int[][] EMPTY_NODE_META_DATA = new int[1][1];

  /**
   * Creates an instance for a node.
   *
   * @param nodeId        is the node nodeId
   * @param nodeMetadata is the metadata arrays associated with each node, such as hashtags and urls
   * @param weight       is the initial weight
   * @param maxSocialProofTypeSize is the max social proof types to keep
   */
  public NodeInfo(long nodeId, int[][] nodeMetadata, double weight, int maxSocialProofTypeSize) {
    this.nodeId = nodeId;
    this.nodeMetadata = nodeMetadata;
    this.weight = weight;
    this.numVisits = 1;
    this.socialProofs = new SmallArrayBasedLongToDoubleMap[maxSocialProofTypeSize];
  }

  /**
   * Creates an instance for a node and sets empty node meta data in the node.
   *
   * @param nodeId  is the node nodeId
   * @param weight is the initial weight
   * @param maxSocialProofTypeSize is the max social proof types to keep
   */
  public NodeInfo(long nodeId, double weight, int maxSocialProofTypeSize) {
    this.nodeId = nodeId;
    this.nodeMetadata = EMPTY_NODE_META_DATA;
    this.weight = weight;
    this.numVisits = 1;
    this.socialProofs = new SmallArrayBasedLongToDoubleMap[maxSocialProofTypeSize];
  }

  public long getNodeId() {
    return nodeId;
  }

  public double getWeight() {
    return weight;
  }

  public void setWeight(double weight) {
    this.weight = weight;
  }

  public void addToWeight(double increment) {
    this.weight += increment;
    numVisits++;
  }

  public int getNumVisits() {
    return numVisits;
  }

  /**
   * Returns an array of different type social proofs for this node.
   *
   * @return a map between social proof type and social proofs
   */
  public SmallArrayBasedLongToDoubleMap[] getSocialProofs() {
    return socialProofs;
  }

  /**
   * Attempts to add the given socialProofId as social proof. Note that the socialProofId itself may or may not be
   * added depending on the socialProofWeight and the current status of the social proof, with the idea
   * being to maintain the "best" social proof.
   *
   * @param socialProofId     is the socialProofId to attempt to add
   * @param edgeType          is the edge type between the social proof and the recommendation
   * @param edgeMetadata      is the edge metadata between the social proof and the recommendation
   * @param socialProofWeight is the socialProofWeight of the socialProofId
   * @return true of the socialProofId was added, false if not
   */
  public boolean addToSocialProof(long socialProofId, byte edgeType, long edgeMetadata, double socialProofWeight) {
    if (socialProofs[edgeType] == null) {
      socialProofs[edgeType] = new SmallArrayBasedLongToDoubleMap();
    }

    socialProofs[edgeType].put(socialProofId, socialProofWeight, edgeMetadata);
    return true;
  }

  public int[] getNodeMetadata(int nodeMetadataType) {
    return nodeMetadata[nodeMetadataType];
  }

  public int compareTo(NodeInfo otherNodeInfo) {
    return Double.compare(this.weight, otherNodeInfo.getWeight());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(nodeId, weight);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }

    NodeInfo other = (NodeInfo) obj;

    return
      Objects.equal(getNodeId(), other.getNodeId())
        && Objects.equal(getWeight(), other.getWeight());
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("nodeId", nodeId)
      .add("weight", weight)
      .add("socialProofs", socialProofs)
      .toString();
  }
}
