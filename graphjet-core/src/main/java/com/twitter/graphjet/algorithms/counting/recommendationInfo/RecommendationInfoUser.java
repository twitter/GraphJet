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

package com.twitter.graphjet.algorithms.counting.recommendationInfo;

import java.util.Map;

import com.google.common.base.Objects;
import com.twitter.graphjet.algorithms.RecommendationType;
import it.unimi.dsi.fastutil.longs.LongList;

/**
 * This class specifies user recommendations based on user follow, mention, and mediatag
 */
public class RecommendationInfoUser implements RecommendationInfo {
  private final long recommendation;
  private final RecommendationType recommendationType;
  private final double weight;
  private final Map<Byte, LongList> socialProof;

  public RecommendationInfoUser(long recommendation, double weight, Map<Byte, LongList> socialProof) {
    this.recommendation = recommendation;
    this.recommendationType = RecommendationType.USER;
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

    RecommendationInfoTweet other = (RecommendationInfoTweet) obj;

    return Objects.equal(getRecommendation(), other.getRecommendation())
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
