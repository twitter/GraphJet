package com.twitter.graphjet.algorithms;

import com.google.common.base.Objects;

/**
 * This interface specifies the required information from each of the recommendations returned by
 * a {@link SimilarityAlgorithm}.
 */
public class SimilarityInfo implements Comparable<SimilarityInfo> {
  private final long similarNode;
  private final double weight;
  private final int cooccurrence;
  private final int degree;

  /**
   * Constructor for Similarity Info
   */
  public SimilarityInfo(long similarNode, double weight, int cooccurrence, int degree) {
    this.similarNode = similarNode;
    this.weight = weight;
    this.cooccurrence = cooccurrence;
    this.degree = degree;
  }

  public long getSimilarNode() {
    return similarNode;
  }

  public double getWeight() {
    return weight;
  }

  public int getCooccurrence() {
    return cooccurrence;
  }

  public int getDegree() {
    return degree;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(similarNode, weight, cooccurrence, degree);
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

    SimilarityInfo other = (SimilarityInfo) obj;

    return
        Objects.equal(getSimilarNode(), other.getSimilarNode())
            && Objects.equal(getWeight(), other.getWeight())
            && Objects.equal(getCooccurrence(), other.getCooccurrence())
            && Objects.equal(getDegree(), other.getDegree());
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("similarNode", similarNode)
        .add("weight", weight)
        .add("coocurrence", cooccurrence)
        .add("degree", degree)
        .toString();
  }

  @Override
  public int compareTo(SimilarityInfo otherSimilarityInfo) {
    return Double.compare(this.weight, otherSimilarityInfo.getWeight());
  }
}
