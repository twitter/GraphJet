package com.twitter.graphjet.algorithms.socialproof;

import com.twitter.graphjet.algorithms.RecommendationRequest;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class SocialProofRequest extends RecommendationRequest {
  private static final LongSet EMPTY_SET = new LongArraySet();

  private final Long2DoubleMap leftSeedNodesWithWeight;
  private final LongSet inputTweets;

  /**
   * Create a social proof request.
   *
   * @param tweets              is the set of input tweets to query social proof.
   * @param weightedSeedNodes   is the set of seed users.
   * @param socialProofTypes    is the social proof types to return.
   */
  public SocialProofRequest(
    LongSet tweets,
    Long2DoubleMap weightedSeedNodes,
    byte[] socialProofTypes
  ) {
    super(0, EMPTY_SET, socialProofTypes);
    this.leftSeedNodesWithWeight = weightedSeedNodes;
    this.inputTweets = tweets;
  }

  public Long2DoubleMap getLeftSeedNodesWithWeight() {
    return leftSeedNodesWithWeight;
  }

  public LongSet getInputTweets() {
    return this.inputTweets;
  }

}
