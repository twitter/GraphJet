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
