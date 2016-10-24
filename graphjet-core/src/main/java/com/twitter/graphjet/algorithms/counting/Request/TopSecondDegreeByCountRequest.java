package com.twitter.graphjet.algorithms.counting.request;

import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.ResultFilterChain;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * Parent class of all requests using TopSecondDegreeByCountForTweet algorithm
 */
public abstract class TopSecondDegreeByCountRequest extends RecommendationRequest {

  private final Long2DoubleMap leftSeedNodesWithWeight;
  private final int maxSocialProofTypeSize;
  private final ResultFilterChain resultFilterChain;

  /**
   * Parent class constructor for requests to TopSecondDegreeByCountForTweet
   * @param queryNode                 User Id of the requester
   * @param leftSeedNodesWithWeight   Weighted seed users
   * @param toBeFiltered              Excluded User Ids
   * @param maxSocialProofTypeSize    Number of social proof types
   * @param socialProofTypes          Social proof types, masked into a byte array
   * @param resultFilterChain         Filter chain to be applied after recommendation computation
   */
  public TopSecondDegreeByCountRequest(
      long queryNode,
      Long2DoubleMap leftSeedNodesWithWeight,
      LongSet toBeFiltered,
      int maxSocialProofTypeSize,
      byte[] socialProofTypes,
      ResultFilterChain resultFilterChain) {
    super(queryNode, toBeFiltered, socialProofTypes);
    this.leftSeedNodesWithWeight = leftSeedNodesWithWeight;
    this.maxSocialProofTypeSize = maxSocialProofTypeSize;
    this.resultFilterChain = resultFilterChain;
  }

  public Long2DoubleMap getLeftSeedNodesWithWeight() {
    return leftSeedNodesWithWeight;
  }

  public int getMaxSocialProofTypeSize() {
    return maxSocialProofTypeSize;
  }

  public void resetFilters() {
    if (resultFilterChain != null) {
      resultFilterChain.resetFilters(this);
    }
  }

  /**
   * filter the given result
   * @param result is the node to check for filtering
   * @param socialProofs is the socialProofs of different types associated with the node
   * @return true if the node should be discarded, false otherwise
   */
  public boolean filterResult(Long result, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    return resultFilterChain != null && resultFilterChain.filterResult(result, socialProofs);
  }
}
