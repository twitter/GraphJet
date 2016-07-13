package com.twitter.graphjet.bipartite.edgepool;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import static com.twitter.graphjet.bipartite.edgepool.RegularDegreeEdgePoolTest.addEdgesToPool;

public class RegularDegreeEdgeIteratorTest {

  @Test
  public void testRegularDegreeEdgeIterator() throws Exception {
    int maxNumNodes = 5;
    int maxDegree = 3;
    RegularDegreeEdgePool regularDegreeEdgePool =
        new RegularDegreeEdgePool(maxNumNodes, maxDegree, new NullStatsReceiver());
    addEdgesToPool(regularDegreeEdgePool);

    RegularDegreeEdgeIterator regularDegreeEdgeIterator =
        new RegularDegreeEdgeIterator(regularDegreeEdgePool);

    regularDegreeEdgeIterator.resetForNode(1);
    assertEquals(new IntArrayList(new int[]{11, 12, 13}),
                 new IntArrayList(regularDegreeEdgeIterator));
    regularDegreeEdgeIterator.resetForNode(2);
    assertEquals(new IntArrayList(new int[]{21, 22}),
                 new IntArrayList(regularDegreeEdgeIterator));
    regularDegreeEdgeIterator.resetForNode(3);
    assertEquals(new IntArrayList(new int[]{31}),
                 new IntArrayList(regularDegreeEdgeIterator));
    regularDegreeEdgeIterator.resetForNode(4);
    assertEquals(new IntArrayList(new int[]{41, 42, 43}),
                 new IntArrayList(regularDegreeEdgeIterator));
  }
}
