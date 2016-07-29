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
