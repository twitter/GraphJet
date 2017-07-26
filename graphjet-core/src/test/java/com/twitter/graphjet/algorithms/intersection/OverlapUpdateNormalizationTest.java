package com.twitter.graphjet.algorithms.intersection;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for class {@link OverlapUpdateNormalization}.
 *
 * @date 2017-07-19
 * @see OverlapUpdateNormalization
 *
 **/
public class OverlapUpdateNormalizationTest{

  @Test
  public void testComputeScoreNormalization() {
    OverlapUpdateNormalization overlapUpdateNormalization = new OverlapUpdateNormalization();

    assertNotNull(overlapUpdateNormalization.computeScoreNormalization(0.0, 0, 0));
    assertNotEquals(0, overlapUpdateNormalization.computeScoreNormalization(0.0, 0, 0));
  }

  @Test
  public void testComputeLeftNeighborContribution() {
    OverlapUpdateNormalization overlapUpdateNormalization = new OverlapUpdateNormalization();

    assertNotNull(overlapUpdateNormalization.computeLeftNeighborContribution(0));
    assertNotEquals(0, overlapUpdateNormalization.computeLeftNeighborContribution(0));
  }

}