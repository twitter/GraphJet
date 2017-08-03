package com.twitter.graphjet.bipartite.segment;

import com.google.common.base.Preconditions;
import com.twitter.graphjet.stats.NullStatsReceiver;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for class {@link LeftIndexedBipartiteGraphSegment}.
 *
 * @see LeftIndexedBipartiteGraphSegment
 **/
public class LeftIndexedBipartiteGraphSegmentTest {

  @Test
  public void testCreateThrowsIllegalArgumentException() {
    IdentityEdgeTypeMask identityEdgeTypeMask = new IdentityEdgeTypeMask();
    NullStatsReceiver statsReceiver = new NullStatsReceiver();
    LeftIndexedPowerLawBipartiteGraphSegment leftIndexedPowerLawBipartiteGraphSegment = null;

    try {
      leftIndexedPowerLawBipartiteGraphSegment = new LeftIndexedPowerLawBipartiteGraphSegment(679,
              679,
              108.010504583,
              (-1066),
              679,
              identityEdgeTypeMask,
              statsReceiver);
      fail("Expecting exception: IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals(Preconditions.class.getName(), e.getStackTrace()[0].getClassName());
    }
  }

  @Test
  public void testInitializeRightInternalIdToLongIterator() {
    MockEdgeTypeMask mockEdgeTypeMask = new MockEdgeTypeMask((byte) (-64));
    NullStatsReceiver statsReceiver = new NullStatsReceiver();
    LeftIndexedPowerLawBipartiteGraphSegment leftIndexedPowerLawBipartiteGraphSegment =
            new LeftIndexedPowerLawBipartiteGraphSegment(163,
                    4455,
                    163,
                    1052,
                    163,
                    mockEdgeTypeMask,
                    statsReceiver);
    leftIndexedPowerLawBipartiteGraphSegment.initializeRightInternalIdToLongIterator();

    assertEquals(163, leftIndexedPowerLawBipartiteGraphSegment.getMaxNumEdges());
    assertEquals(0, leftIndexedPowerLawBipartiteGraphSegment.getCurrentNumEdges());
  }

}