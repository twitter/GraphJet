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

package com.twitter.graphjet.algorithms.filter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.algorithms.filters.ExactUserSocialProofSizeFilter;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.NullStatsReceiver;

public class ExactUserSocialProofSizeFilterTest {

  @Test
  public void testNoSocialProof() throws Exception {
    long resultNode = 0L;
    byte[] socialProofTypes = {(byte) 0, (byte) 1};
    SmallArrayBasedLongToDoubleMap[] socialProofs = {null, null};

    // Expect no social proof, Should not be filtered
    int expectedProofCount = 0;
    ExactUserSocialProofSizeFilter filter = new ExactUserSocialProofSizeFilter(
      expectedProofCount,
      socialProofTypes,
      new NullStatsReceiver());
    assertEquals(false, filter.filterResult(resultNode, socialProofs));

    // Expect one social proof, Should be filtered
    expectedProofCount = 1;
    filter = new ExactUserSocialProofSizeFilter(
      expectedProofCount,
      socialProofTypes,
      new NullStatsReceiver());
    assertEquals(true, filter.filterResult(resultNode, socialProofs));
  }

  @Test
  public void testSomeSocialProofs() throws Exception {
    long resultNode = 0L;

    SmallArrayBasedLongToDoubleMap socialProofForType1 = new SmallArrayBasedLongToDoubleMap();
    socialProofForType1.put(1, 1.0, 0L);
    socialProofForType1.put(2, 1.0, 0L);

    SmallArrayBasedLongToDoubleMap socialProofForType2 = new SmallArrayBasedLongToDoubleMap();
    socialProofForType2.put(1, 1.0, 0L);

    SmallArrayBasedLongToDoubleMap[] socialProofs = {socialProofForType1, socialProofForType2};

    // Expect 1 social proof, actually has 3. Should be filtered
    int expectedProofCount = 1;
    byte[] validSocialProofTypes = {(byte) 0, (byte) 1};

    ExactUserSocialProofSizeFilter filter = new ExactUserSocialProofSizeFilter(
      expectedProofCount,
      validSocialProofTypes,
      new NullStatsReceiver());
    assertEquals(true, filter.filterResult(resultNode, socialProofs));

    // Expect 3 social proofs, has 3 proofs. Should not be filtered.
    expectedProofCount = 3;
    validSocialProofTypes = new byte[] {(byte) 0, (byte) 1};

    filter = new ExactUserSocialProofSizeFilter(
      expectedProofCount,
      validSocialProofTypes,
      new NullStatsReceiver());
    assertEquals(false, filter.filterResult(resultNode, socialProofs));

    // Expect 1 social proof from type 0. Has 2. Should be filtered
    expectedProofCount = 1;
    validSocialProofTypes = new byte[] {(byte) 0};
    socialProofs = new SmallArrayBasedLongToDoubleMap[] { socialProofForType1 };

    filter = new ExactUserSocialProofSizeFilter(
        expectedProofCount,
        validSocialProofTypes,
        new NullStatsReceiver());
    assertEquals(true, filter.filterResult(resultNode, socialProofs));

    // Expect 2 social proofs from type 0. Has 1. Should not be filtered
    expectedProofCount = 2;
    validSocialProofTypes = new byte[] {(byte) 0};
    filter = new ExactUserSocialProofSizeFilter(
        expectedProofCount,
        validSocialProofTypes,
        new NullStatsReceiver());
    assertEquals(false, filter.filterResult(resultNode, socialProofs));

  }
}
