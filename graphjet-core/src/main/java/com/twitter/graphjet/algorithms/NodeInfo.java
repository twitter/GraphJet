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
 * This class contains information about a node that's a potential recommendation.
 */
public class NodeInfo implements Comparable<NodeInfo> {
  private final long value;
  private int[][] nodeMetadata;
  private double weight;
  private int numVisits;
  private SmallArrayBasedLongToDoubleMap[] socialProofs;
  private static final int[][] EMPTY_NODE_META_DATA = new int[1][1];

  /**
   * Creates an instance for a node.
   *
   * @param value        is the node value
   * @param nodeMetadata is the metadata arrays associated with each node, such as hashtags and urls
   * @param weight       is the initial weight
   * @param maxSocialProofTypeSize is the max social proof types to keep
   */
  public NodeInfo(long value, int[][] nodeMetadata, double weight, int maxSocialProofTypeSize) {
    this.value = value;
    this.nodeMetadata = nodeMetadata;
    this.weight = weight;
    this.numVisits = 1;
    this.socialProofs = new SmallArrayBasedLongToDoubleMap[maxSocialProofTypeSize];
  }

  /**
   * Creates an instance for a node and sets empty node meta data in the node.
   *
   * @param value  is the node value
   * @param weight is the initial weight
   * @param maxSocialProofTypeSize is the max social proof types to keep
   */
  public NodeInfo(long value, double weight, int maxSocialProofTypeSize) {
    this.value = value;
    this.nodeMetadata = EMPTY_NODE_META_DATA;
    this.weight = weight;
    this.numVisits = 1;
    this.socialProofs = new SmallArrayBasedLongToDoubleMap[maxSocialProofTypeSize];
  }

  public long getValue() {
    return value;
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
   * Attempts to add the given node as social proof. Note that the node itself may or may not be
   * added depending on the nodeWeight and the current status of the social proof, with the idea
   * being to maintain the "best" social proof.
   *
   * @param node        is the node to attempt to add
   * @param edgeType    is the edge type between the social proof and the recommendation
   * @param nodeWeight  is the nodeWeight of the node
   * @return true of the node was added, false if not
   */
  public boolean addToSocialProof(long node, byte edgeType, double nodeWeight) {
    if (socialProofs[edgeType] == null) {
      socialProofs[edgeType] = new SmallArrayBasedLongToDoubleMap();
    }

    return socialProofs[edgeType].put(node, nodeWeight);
  }

  public int[] getNodeMetadata(int nodeMetadataType) {
    return nodeMetadata[nodeMetadataType];
  }

  public int compareTo(NodeInfo otherNodeInfo) {
    return Double.compare(this.weight, otherNodeInfo.getWeight());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value, weight);
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
      Objects.equal(getValue(), other.getValue())
        && Objects.equal(getWeight(), other.getWeight());
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("value", value)
      .add("weight", weight)
      .add("socialProofs", socialProofs)
      .toString();
  }
}
