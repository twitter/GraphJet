package com.twitter.graphjet.algorithms;

import java.util.Map;

import com.google.common.base.Objects;

import it.unimi.dsi.fastutil.longs.LongList;

public class TweetRecommendationInfo implements RecommendationInfo {
  private final long recommendation;
  private final RecommendationType recommendationType;
  private final double weight;
  private final Map<Byte, LongList> socialProof;

  /**
   * This class specifies the tweet recommendation.
   */
  public TweetRecommendationInfo(long recommendation, double weight,
                                 Map<Byte, LongList> socialProof) {
    this.recommendation = recommendation;
    this.recommendationType = RecommendationType.TWEET;
    this.weight = weight;
    this.socialProof = socialProof;
  }

  public long getRecommendation() {
    return recommendation;
  }

  public RecommendationType getRecommendationType() {
    return recommendationType;
  }

  public double getWeight() {
    return weight;
  }

  public Map<Byte, LongList> getSocialProof() {
    return socialProof;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(recommendation, recommendationType, weight, socialProof);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }

    TweetRecommendationInfo other = (TweetRecommendationInfo) obj;

    return
      Objects.equal(getRecommendation(), other.getRecommendation())
        && Objects.equal(getRecommendationType(), other.getRecommendationType())
        && Objects.equal(getWeight(), other.getWeight())
        && Objects.equal(getSocialProof(), other.getSocialProof());
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("recommendation", recommendation)
      .add("recommendationType", recommendationType)
      .add("weight", weight)
      .add("socialProof", socialProof)
      .toString();
  }
}
