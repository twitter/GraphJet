package com.twitter.graphjet.algorithms;

/**
 * This interface specifies the required information from each of the recommendations returned by
 * a {@link RecommendationAlgorithm}.
 */
public interface RecommendationInfo {
  /**
   * Return the recommendation type, such as Hashtag, Url and Tweet.
   */
  RecommendationType getRecommendationType();

  /**
   * Return the weight of the recommendation.
   */
  double getWeight();
}
