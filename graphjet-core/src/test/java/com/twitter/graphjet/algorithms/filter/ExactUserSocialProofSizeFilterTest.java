package com.twitter.graphjet.algorithms.filter;

import org.junit.Test;

import com.twitter.graphjet.algorithms.ExactUserSocialProofSizeFilter;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.NullStatsReceiver;

import static org.junit.Assert.assertEquals;

public class ExactUserSocialProofSizeFilterTest {

  @Test
  public void testNoSocialProof() throws Exception {
    long resultNode = 0L;
    byte[] socialProofTypes = {(byte) 1, (byte) 2};
    SmallArrayBasedLongToDoubleMap[] socialProofs = {};

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
    socialProofForType1.put(1, 1.0);
    socialProofForType1.put(2, 1.0);

    SmallArrayBasedLongToDoubleMap socialProofForType2 = new SmallArrayBasedLongToDoubleMap();
    socialProofForType2.put(1, 1.0);

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
