package com.twitter.graphjet.algorithms.magicrecs;

import java.util.Map;
import java.util.Set;

import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.ResultFilterChain;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongSet;

public class MagicRecsRequest extends RecommendationRequest {
  private final Long2DoubleMap leftSeedNodesWithWeight;
  private final Set<RecommendationType> recommendationTypes;
  private final Map<RecommendationType, Integer> maxNumResultsByType;
  private final int maxSocialProofTypeSize;
  private final int maxUserSocialProofSize;
  private final int maxTweetSocialProofSize;
  private final Map<RecommendationType, Integer> minUserSocialProofSizes;
  private final ResultFilterChain resultFilterChain;

  /**
   * The constructor of MagicRecsRequest.
   * @param queryNode                 is the query node for running MagicRecs
   * @param leftSeedNodesWithWeight   is the set of seed nodes and their weights to use for
   *                                  MagicRecs.
   * @param toBeFiltered              is the set of RHS nodes to be filtered from the output
   * @param recommendationTypes       is the list of recommendation types requested by clients
   * @param maxNumResultsByType       is the maximum number of results requested by clients per type
   * @param maxSocialProofTypeSize    is the maximum size of social proof types in the graph.
   * @param maxUserSocialProofSize    is the maximum size of user social proof per type to return.
   *                                  Set this to 0 to return no social proof
   * @param maxTweetSocialProofSize   is the maximum size of tweet social proof per user to return.
   * @param minUserSocialProofSizes   is the minimum size of user social proof per recommendation
   *                                  type to return
   * @param socialProofTypes          is the social proof types to return
   * @param resultFilterChain         is the chain of filters to be applied
   */
  public MagicRecsRequest(
    long queryNode,
    Long2DoubleMap leftSeedNodesWithWeight,
    LongSet toBeFiltered,
    Set<RecommendationType> recommendationTypes,
    Map<RecommendationType, Integer> maxNumResultsByType,
    int maxSocialProofTypeSize,
    int maxUserSocialProofSize,
    int maxTweetSocialProofSize,
    Map<RecommendationType, Integer> minUserSocialProofSizes,
    byte[] socialProofTypes,
    ResultFilterChain resultFilterChain
  ) {
    super(queryNode, toBeFiltered, socialProofTypes);
    this.leftSeedNodesWithWeight = leftSeedNodesWithWeight;
    this.recommendationTypes = recommendationTypes;
    this.maxNumResultsByType = maxNumResultsByType;
    this.maxSocialProofTypeSize = maxSocialProofTypeSize;
    this.maxUserSocialProofSize = maxUserSocialProofSize;
    this.maxTweetSocialProofSize = maxTweetSocialProofSize;
    this.minUserSocialProofSizes = minUserSocialProofSizes;
    this.resultFilterChain = resultFilterChain;
  }

  public Long2DoubleMap getLeftSeedNodesWithWeight() {
    return leftSeedNodesWithWeight;
  }

  public Set<RecommendationType> getRecommendationTypes() {
    return recommendationTypes;
  }

  public Map<RecommendationType, Integer> getMaxNumResultsByType() {
    return maxNumResultsByType;
  }

  public int getMaxSocialProofTypeSize() {
    return maxSocialProofTypeSize;
  }

  public int getMaxUserSocialProofSize() {
    return maxUserSocialProofSize;
  }

  public int getMaxTweetSocialProofSize() {
    return maxTweetSocialProofSize;
  }

  public Map<RecommendationType, Integer> getMinUserSocialProofSizes() {
    return minUserSocialProofSizes;
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
