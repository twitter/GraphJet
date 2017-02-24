package com.twitter.graphjet.algorithms;


import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * Only allow nodes whose total number of user social proofs is exactly equal to
 * what is specified, no more, no less
 */
public class ExactUserSocialProofSizeFilter extends ResultFilter {
  private int exactNumUserSocialProof;
  private byte[] validSocialProofType;

  public ExactUserSocialProofSizeFilter(
      int exactNumUserSocialProof,
      byte[] validSocialProofType,
      StatsReceiver statsReceiver) {
    super(statsReceiver);
    this.exactNumUserSocialProof = exactNumUserSocialProof;
    this.validSocialProofType = validSocialProofType;
  }

  @Override
  public void resetFilter(RecommendationRequest request) {}

  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    int totalNumProofs = 0;
    for (Byte validType: validSocialProofType) {
      if (socialProofs[validType] != null) {
        totalNumProofs += socialProofs[validType].size();
      }
    }
    return totalNumProofs != exactNumUserSocialProof;
  }
}
