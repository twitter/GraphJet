package com.twitter.graphjet.algorithms.socialproof;

import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationType;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This class wraps a social proof recommendation result for one tweet.
 * The {@link SocialProofResponse} wraps a list of SocialProofResult object.
 */
public class SocialProofResult implements RecommendationInfo {

  private final long node;
  private final Byte2ObjectMap<LongSet> socialProof;
  private final double weight;

  public SocialProofResult(
    Long node,
    Byte2ObjectMap<LongSet> socialProof,
    double weight
  ) {
    this.node = node;
    this.socialProof = socialProof;
    this.weight = weight;
  }

  @Override
  public RecommendationType getRecommendationType() {
    return RecommendationType.TWEET;
  }

  @Override
  public double getWeight() {
    return this.weight;
  }

  public Byte2ObjectMap<LongSet> getSocialProof() {
    return this.socialProof;
  }

  public long getNode() {
    return this.node;
  }

  /**
   * Calculate the total number of interactions for current tweet on given set of users.
   *
   * @return the number of interactions.
   */
  public int getSocialProofSize() {
    int socialProofSize = 0;
    for (LongSet connectingUsers: socialProof.values()) {
      socialProofSize += connectingUsers.size();
    }
    return socialProofSize;
  }

}
