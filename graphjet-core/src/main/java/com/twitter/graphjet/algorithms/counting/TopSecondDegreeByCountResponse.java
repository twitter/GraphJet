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


package com.twitter.graphjet.algorithms.counting;

import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationResponse;
import com.twitter.graphjet.algorithms.RecommendationStats;

/**
 * A simple wrapper around {@link RecommendationResponse} that also allows returning
 * {@link RecommendationStats} from the TopSecondDegreeByCount computation.
 */
public class TopSecondDegreeByCountResponse extends RecommendationResponse {
  private final RecommendationStats topSecondDegreeByCountStats;

  public TopSecondDegreeByCountResponse(
    Iterable<RecommendationInfo> rankedRecommendations,
    RecommendationStats topSecondDegreeByCountStats) {
    super(rankedRecommendations);
    this.topSecondDegreeByCountStats = topSecondDegreeByCountStats;
  }

  public RecommendationStats getTopSecondDegreeByCountStats() {
    return topSecondDegreeByCountStats;
  }
}
