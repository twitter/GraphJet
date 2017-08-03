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


package com.twitter.graphjet.algorithms.filters;

import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.StatsReceiver;

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
    for (int i = 0; i < size; i++) {
      if (socialProofs[socialProofTypes[i]] != null) {
        return false;
      }
    }
    return true;
  }
}
