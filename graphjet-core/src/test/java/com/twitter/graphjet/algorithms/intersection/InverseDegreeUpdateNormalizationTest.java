package com.twitter.graphjet.algorithms.intersection;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for class {@link InverseDegreeUpdateNormalization}.
 *
 * @see InverseDegreeUpdateNormalization
 **/
public class InverseDegreeUpdateNormalizationTest {

  @Test
  public void testComputeScoreNormalization() {
    InverseDegreeUpdateNormalization inverseDegreeUpdateNormalization = new InverseDegreeUpdateNormalization();

    assertEquals(1.0, inverseDegreeUpdateNormalization.computeScoreNormalization(2.0, 2, 4), 0.01);
  }

  @Test
  public void testComputeLeftNeighborContribution() {
    InverseDegreeUpdateNormalization inverseDegreeUpdateNormalization = new InverseDegreeUpdateNormalization();

    assertEquals(0.5, inverseDegreeUpdateNormalization.computeLeftNeighborContribution(2), 0.01);
  }

}