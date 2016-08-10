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


package com.twitter.graphjet.algorithms.randommultigraphneighbors;

import com.google.common.base.Objects;

/**
 * This interface specifies the required information from each of the neighbors returned by
 * a {@link RandomMultiGraphNeighbors}.
 */
public class NeighborInfo implements Comparable<NeighborInfo> {
  private final long neighborNode;
  private final double score;
  private final int degree;

  /**
   * Constructor for Neighbor Info
   *
   * @param neighborNode                the neighbor node
   * @param score                       the score of the neighbor node returned by the algorithm
   * @param degree                      the degree of the neighbor node in the graph
   */
  public NeighborInfo(long neighborNode, double score, int degree) {
    this.neighborNode = neighborNode;
    this.score = score;
    this.degree = degree;
  }

  public long getNeighborNode() {
    return neighborNode;
  }

  public double getScore() {
    return score;
  }

  public int getDegree() {
    return degree;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(neighborNode, score, degree);
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

    NeighborInfo other = (NeighborInfo) obj;

    return
        Objects.equal(getNeighborNode(), other.getNeighborNode())
            && Objects.equal(getScore(), other.getScore())
            && Objects.equal(getDegree(), other.getDegree());
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("neighbor", neighborNode)
        .add("score", score)
        .add("degree", degree)
        .toString();
  }

  @Override
  public int compareTo(NeighborInfo otherNeighborInfo) {
    return Double.compare(this.score, otherNeighborInfo.getScore());
  }
}
