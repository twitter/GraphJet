package com.twitter.graphjet.algorithms;

import java.util.Random;

import com.twitter.graphjet.algorithms.filters.RelatedTweetFilterChain;

/**
 * This interface completely specifies the requirements of a valid similarity algorithm.
 *
 * @param <S>  is the type of request sent to the algorithm
 * @param <T>  is the type of response received from the algorithm
 */
public interface SimilarityAlgorithm<S extends SimilarityRequest,
                                     T extends SimilarityResponse> {
  /**
   * This is the main entry point for computing similar results.
   *
   * @param request  is the request for the algorithm
   * @param random   is used for all random choices within the algorithm
   * @return the populated similarity response
   */
  T getSimilarNodes(S request, Random random);

  /**
   * This is the main entry point for computing similar results with filters
   *
   * @param request  is the request for the algorithm
   * @param random   is used for all random choices within the algorithm
   * @param filterChain is used to filter the results
   * @return the populated similarity response
   */
  T getSimilarNodes(S request, Random random, RelatedTweetFilterChain filterChain);
}
