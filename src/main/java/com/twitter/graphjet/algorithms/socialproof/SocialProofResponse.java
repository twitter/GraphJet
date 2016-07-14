package com.twitter.graphjet.algorithms.socialproof;

import java.util.List;

import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationResponse;

/**
 * The response of {@link TweetSocialProof}
 *
 * @see SocialProofResult
 */
public class SocialProofResponse extends RecommendationResponse {

  public SocialProofResponse(List<RecommendationInfo> tweetsWithSocialProof) {
    super(tweetsWithSocialProof);
  }

}
