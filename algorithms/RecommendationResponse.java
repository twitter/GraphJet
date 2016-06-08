package com.twitter.graphjet.algorithms;

/**
 * This class encapsulates the required response from a {@link RecommendationAlgorithm}.
 */
public class RecommendationResponse {
  private final Iterable<RecommendationInfo> rankedRecommendations;

  public RecommendationResponse(Iterable<RecommendationInfo> rankedRecommendations) {
    this.rankedRecommendations = rankedRecommendations;
  }

  public Iterable<RecommendationInfo> getRankedRecommendations() {
    return rankedRecommendations;
  }
}
