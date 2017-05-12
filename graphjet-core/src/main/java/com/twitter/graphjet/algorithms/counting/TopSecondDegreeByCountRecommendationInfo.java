/**
 * Copyright 2017 Twitter. All rights reserved.
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

import java.util.Map;

import com.google.common.base.Objects;

import com.twitter.graphjet.algorithms.ConnectingUsersWithMetadata;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationType;

/**
 * Recommendation based on user-entity interactions, such as creation and like.
 */
public abstract class TopSecondDegreeByCountRecommendationInfo implements RecommendationInfo {
  private final long recommendation;
  private final double weight;
  private final Map<Byte, ConnectingUsersWithMetadata> socialProof;
  protected RecommendationType recommendationType;

  public TopSecondDegreeByCountRecommendationInfo(
    long recommendation,
    double weight,
    Map<Byte, ConnectingUsersWithMetadata> socialProof
  ) {
    this.recommendation = recommendation;
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

  public Map<Byte, ConnectingUsersWithMetadata> getSocialProof() {
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

    TopSecondDegreeByCountRecommendationInfo other = (TopSecondDegreeByCountRecommendationInfo) obj;

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
