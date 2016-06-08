package com.twitter.graphjet.algorithms;

import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This interface specifies a request received by a {@link RecommendationAlgorithm}.
 */
public abstract class RecommendationRequest {
  private final long queryNode;
  private final LongSet toBeFiltered;
  private final byte[] socialProofTypes;

  protected RecommendationRequest(
    long queryNode,
    LongSet toBeFiltered,
    byte[] socialProofTypes
  ) {
    this.queryNode = queryNode;
    this.toBeFiltered = toBeFiltered;
    this.socialProofTypes = socialProofTypes;
  }

  public long getQueryNode() {
    return queryNode;
  }

  /**
   * Return the set of RHS nodes to be filtered from the output
   */
  public LongSet getToBeFiltered() {
    return toBeFiltered;
  }

  /**
   * Return the social proof types requested by the clients
   */
  public byte[] getSocialProofTypes() {
    return socialProofTypes;
  }
}
