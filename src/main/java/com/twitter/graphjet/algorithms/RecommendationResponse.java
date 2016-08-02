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
