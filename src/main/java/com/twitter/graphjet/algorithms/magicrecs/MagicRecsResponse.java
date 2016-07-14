package com.twitter.graphjet.algorithms.magicrecs;

import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationResponse;
import com.twitter.graphjet.algorithms.RecommendationStats;

/**
 * A simple wrapper around {@link RecommendationResponse} that also allows returning
 * {@link RecommendationStats} from the MagicRecs computation.
 */
public class MagicRecsResponse extends RecommendationResponse {
  private final RecommendationStats magicRecsStats;

  public MagicRecsResponse(
    Iterable<RecommendationInfo> rankedRecommendations,
    RecommendationStats magicRecsStats) {
    super(rankedRecommendations);
    this.magicRecsStats = magicRecsStats;
  }

  public RecommendationStats getMagicRecsStats() {
    return magicRecsStats;
  }
}
