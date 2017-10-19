package com.twitter.graphjet.algorithms.salsa;

import com.google.common.base.Preconditions;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for class {@link SalsaRequestBuilder}.
 *
 * @date 2017-09-18
 * @see SalsaRequestBuilder
 **/
public class SalsaRequestBuilderTest {
  @Test
  public void testBuildAllDesiredPropertiesAreSetAutomatically() {
    SalsaRequestBuilder salsaRequestBuilder = new SalsaRequestBuilder(211L);
    salsaRequestBuilder.build();
    SalsaRequest salsaRequest = salsaRequestBuilder.build();

    assertEquals(0.3, salsaRequest.getResetProbability(), 0.01);
    assertEquals(1, salsaRequest.getMaxSocialProofTypeSize());
    assertFalse(salsaRequest.removeCustomizedBitsNodes());
    assertEquals(0.9, salsaRequest.getQueryNodeWeightFraction(), 0.01);
    assertEquals(1, salsaRequest.getNumRandomWalks());
    assertEquals(0, salsaRequest.getMaxSocialProofSize());
    assertEquals(211L, salsaRequest.getQueryNode());
    assertEquals(1, salsaRequest.getMaxRandomWalkLength());
    assertEquals(10, salsaRequest.getMaxNumResults());
  }

  @Test
  public void testWithMaxRandomWalkLengthThrowsIllegalArgumentException() {
    SalsaRequestBuilder salsaRequestBuilder = new SalsaRequestBuilder(211L);

    try {
      salsaRequestBuilder.withMaxRandomWalkLength(100);
      fail("Expecting exception: IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals(Preconditions.class.getName(), e.getStackTrace()[0].getClassName());
    }
  }
}