package com.twitter.graphjet.algorithms.intersection;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for class {@link ScaledUpdateNormalization}.
 *
 * @date 2017-07-19
 * @see ScaledUpdateNormalization
 *
 **/
public class ScaledUpdateNormalizationTest{

  @Test
  public void testComputeLeftNeighborContribution() {
    ScaledUpdateNormalization scaledUpdateNormalization = new ScaledUpdateNormalization();

    assertEquals(0.5, scaledUpdateNormalization.computeLeftNeighborContribution(4), 0.01);
  }

  @Test
  public void testComputeScoreNormalization() {
    assertEquals(0.2886751345948129,
            new ScaledUpdateNormalization().computeScoreNormalization(2, 4, 6), 0.01
    );
  }

}