package com.twitter.graphjet.algorithms;

/**
 * This interface specifies a request received by a {@link SimilarityAlgorithm}.
 */
public class SimilarityRequest {
  private final long queryNode;
  private final int maxNumResults;

  public SimilarityRequest(long queryNode, int maxNumResults) {
    this.queryNode = queryNode;
    this.maxNumResults = maxNumResults;
  }

  public long getQueryNode() {
    return queryNode;
  }

  public int getMaxNumResults() {
    return maxNumResults;
  }
}
