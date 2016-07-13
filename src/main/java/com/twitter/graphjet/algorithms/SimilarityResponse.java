package com.twitter.graphjet.algorithms;

/**
 * This class encapsulates the required response from a {@link SimilarityAlgorithm}.
 */
public class SimilarityResponse {
  private final Iterable<SimilarityInfo> rankedSimilarNodes;
  private final int queryNodeDegree;

  public SimilarityResponse(Iterable<SimilarityInfo> rankedSimilarNodes, int queryNodeDegree) {
    this.rankedSimilarNodes = rankedSimilarNodes;
    this.queryNodeDegree = queryNodeDegree;
  }

  public Iterable<SimilarityInfo> getRankedSimilarNodes() {
    return rankedSimilarNodes;
  }

  public int getDegree() {
    return queryNodeDegree;
  }
}
