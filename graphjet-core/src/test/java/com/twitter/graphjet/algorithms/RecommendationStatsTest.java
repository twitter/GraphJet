package com.twitter.graphjet.algorithms;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for class {@link RecommendationStats}.
 *
 * @see RecommendationStats
 **/
public class RecommendationStatsTest {

  @Test
  public void testReset() {
    RecommendationStats recommendationStats = new RecommendationStats();
    recommendationStats.maxVisitsPerRightNode = (-1392);
    RecommendationStats recommendationStatsTwo = new RecommendationStats(-1392, -2951, 0, 0, 0, 0);
    recommendationStatsTwo.reset();

    assertEquals(Integer.MAX_VALUE, recommendationStatsTwo.getMinVisitsPerRightNode());
    assertFalse(recommendationStats.equals(recommendationStatsTwo));
  }

  @Test
  public void testCreateTaking6Arguments() {
    RecommendationStats recommendationStats = new RecommendationStats();
    RecommendationStats recommendationStatsTwo = new RecommendationStats(1620,
            1620,
            1620,
            (-13),
            (-7639),
            (-13)
    );

    assertEquals(0, recommendationStats.getNumRightNodesFiltered());
    assertEquals(1620, recommendationStatsTwo.getNumDirectNeighbors());
    assertEquals(1620, recommendationStatsTwo.getNumRightNodesReached());
    assertEquals((-13), recommendationStatsTwo.getNumRightNodesFiltered());
    assertEquals(1620, recommendationStatsTwo.getNumRHSVisits());
    assertFalse(recommendationStats.equals(recommendationStatsTwo));
    assertEquals((-13), recommendationStatsTwo.getMinVisitsPerRightNode());
    assertEquals((-7639), recommendationStatsTwo.getMaxVisitsPerRightNode());
  }

  @Test
  public void testEqualsWithNull() {
    RecommendationStats recommendationStats = new RecommendationStats();

    assertFalse(recommendationStats.equals(null));
  }

  @Test
  public void testEqualsReturningFalse() {
    RecommendationStats recommendationStats = new RecommendationStats();
    Object object = new Object();

    assertFalse(recommendationStats.equals(object));
  }

  @Test
  public void testEqualsWitSameObject() {
    RecommendationStats recommendationStats = new RecommendationStats();

    assertTrue(recommendationStats.equals(recommendationStats));
  }

  @Test
  public void testUpdateVisitStatsPerRightNode() {
    RecommendationStats recommendationStats = new RecommendationStats();
    recommendationStats.updateVisitStatsPerRightNode(0);
    RecommendationStats recommendationStatsTwo = new RecommendationStats();

    assertFalse(recommendationStats.equals(recommendationStatsTwo));
    assertEquals(0, recommendationStats.getMinVisitsPerRightNode());
  }

}