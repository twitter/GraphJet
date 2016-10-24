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


package com.twitter.graphjet.algorithms.salsa;

import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationResponse;

/**
 * A simple wrapper around {@link RecommendationResponse} that also allows returning
 * {@link SalsaStats} from the Salsa computation.
 */
public class SalsaResponse extends RecommendationResponse {
  private final SalsaStats salsaStats;

  public SalsaResponse(
      Iterable<RecommendationInfo> rankedRecommendations,
      SalsaStats salsaStats) {
    super(rankedRecommendations);
    this.salsaStats = salsaStats;
  }

  public SalsaStats getSalsaStats() {
    return salsaStats;
  }
}
