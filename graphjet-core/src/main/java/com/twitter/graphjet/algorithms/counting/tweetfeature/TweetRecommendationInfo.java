/**
 * Copyright 2018 Twitter. All rights reserved.
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

package com.twitter.graphjet.algorithms.counting.tweetfeature;

import java.util.Map;

import com.twitter.graphjet.algorithms.ConnectingUsersWithMetadata;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.counting.TopSecondDegreeByCountRecommendationInfo;

public class TweetRecommendationInfo extends TopSecondDegreeByCountRecommendationInfo {
  public int[] tweetFeature;

  public TweetRecommendationInfo(
      long recommendation,
      double weight,
      Map<Byte, ConnectingUsersWithMetadata> socialProof,
      int[] tweetFeature
  ) {
    super(recommendation, weight, socialProof);
    super.recommendationType = RecommendationType.TWEET;
    this.tweetFeature = tweetFeature;
  }

  public int[] getTweetFeature() {
    return tweetFeature;
  }
}
