/**
 * Copyright 2017 Twitter. All rights reserved.
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

/**
 * This filter keeps the nodes with the exact number of user social proofs equal to the parameter of the query,
 * no more, no less.
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
        totalNumProofs += socialProofs[validType].uniqueKeysSize();
      }
    }
    return totalNumProofs != exactNumUserSocialProof;
  }
}
