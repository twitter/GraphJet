package com.twitter.graphjet.algorithms.intersection;

import com.twitter.graphjet.algorithms.intersection.JaccardUpdateNormalization;
import org.junit.Assert;
import org.junit.Test;

public class JaccardUpdateNormalizationTest {
  
  @Test
  public void computeLeftNeighborContributionInputZeroOutputPositive() {

    // Arrange
    final JaccardUpdateNormalization objectUnderTest = new JaccardUpdateNormalization();
    final int leftNodeDegree = 0;

    // Act
    final double retval = objectUnderTest.computeLeftNeighborContribution(leftNodeDegree);

    // Assert result
    Assert.assertEquals(1.0, retval, 0.0);
  }

  @Test
  public void computeScoreNormalizationInputPositiveZeroZeroOutputNegativeInfinity() {

    // Arrange
    final JaccardUpdateNormalization objectUnderTest = new JaccardUpdateNormalization();
    final double cooccurrence = 0x0.1p-1022 /* 1.39067e-309 */;
    final int similarNodeDegree = 0;
    final int nodeDegree = 0;

    // Act
    final double retval =
        objectUnderTest.computeScoreNormalization(cooccurrence, similarNodeDegree, nodeDegree);

    // Assert result
    Assert.assertEquals(Double.NEGATIVE_INFINITY, retval, 0.0);
  }
}
