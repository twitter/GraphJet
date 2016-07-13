package com.twitter.graphjet.bipartite.edgepool;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import static com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePoolTest.addEdgesToPool;

public class PowerLawDegreeEdgeIteratorTest {

  @Test
  public void testPowerLawDegreeEdgeIterator() throws Exception {
    int maxNumNodes = 4;
    int maxDegree = 6;
    PowerLawDegreeEdgePool powerLawDegreeEdgePool =
        new PowerLawDegreeEdgePool(maxNumNodes, maxDegree, 2.0, new NullStatsReceiver());

    addEdgesToPool(powerLawDegreeEdgePool);

    PowerLawDegreeEdgeIterator powerLawDegreeEdgeIterator =
        new PowerLawDegreeEdgeIterator(powerLawDegreeEdgePool);

    powerLawDegreeEdgeIterator.resetForNode(1);
    assertEquals(new IntArrayList(new int[]{11, 12, 13, 14, 15, 16, 17}),
        new IntArrayList(powerLawDegreeEdgeIterator));
    powerLawDegreeEdgeIterator.resetForNode(2);
    assertEquals(new IntArrayList(new int[]{21, 22, 23}),
        new IntArrayList(powerLawDegreeEdgeIterator));
    powerLawDegreeEdgeIterator.resetForNode(3);
    assertEquals(new IntArrayList(new int[]{31}),
        new IntArrayList(powerLawDegreeEdgeIterator));
    powerLawDegreeEdgeIterator.resetForNode(4);
    assertEquals(new IntArrayList(new int[]{41, 42, 43}),
        new IntArrayList(powerLawDegreeEdgeIterator));
    powerLawDegreeEdgeIterator.resetForNode(5);
    assertEquals(new IntArrayList(new int[]{51}),
        new IntArrayList(powerLawDegreeEdgeIterator));
  }
}
