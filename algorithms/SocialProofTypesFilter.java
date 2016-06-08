package com.twitter.graphjet.algorithms;

import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

public class SocialProofTypesFilter extends ResultFilter {
  private byte[] socialProofTypes;

  /**
   * construct valid social proof types filter
   */
  public SocialProofTypesFilter(StatsReceiver statsReceiver) {
    super(statsReceiver);
  }

  @Override
  public void resetFilter(RecommendationRequest request) {
    socialProofTypes = request.getSocialProofTypes();
  }

  /**
   * discard results without valid social proof types specified by clients
   *
   * @param resultNode is the result node to be checked
   * @param socialProofs is the socialProofs of different types associated with the node
   * @return true if none of the specified socialProofTypes are present in the socialProofs map
   */
  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    int size = socialProofTypes.length;
    boolean keep = false;
    for (int i = 0; i < size; i++) {
      if (socialProofs[socialProofTypes[i]] != null) {
        keep = true;
        break;
      }
    }

    return !keep;
  }
}
