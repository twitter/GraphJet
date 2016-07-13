package com.twitter.graphjet.algorithms;

import java.util.Random;

/**
 * This interface completely specifies the requirements of a valid recommendation algorithm.
 *
 * @param <S>  is the type of request sent to the algorithm
 * @param <T>  is the type of response received from the algorithm
 */
public interface RecommendationAlgorithm<S extends RecommendationRequest,
                                         T extends RecommendationResponse> {
  /**
   * This is the main entry point for computing recommendations.
   *
   * @param request  is the request for the algorithm
   * @param random   is used for all random choices within the algorithm
   * @return the populated recommendation response
   */
  T computeRecommendations(S request, Random random);
}
