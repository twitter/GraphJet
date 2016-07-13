package com.twitter.graphjet.bipartite.edgepool;

import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import static com.twitter.graphjet.bipartite.edgepool.RegularDegreeEdgePoolTest.addEdgesToPool;

public class RegularDegreeEdgeRandomIteratorTest {

  @Test
  public void testRegularDegreeEdgeRandomIterator() throws Exception {
    Random random = new Random(90238490238409L);
    int maxNumNodes = 5;
    int maxDegree = 3;
    RegularDegreeEdgePool regularDegreeEdgePool =
        new RegularDegreeEdgePool(maxNumNodes, maxDegree, new NullStatsReceiver());
    addEdgesToPool(regularDegreeEdgePool);

    RegularDegreeEdgeRandomIterator regularDegreeEdgeRandomIterator =
        new RegularDegreeEdgeRandomIterator(regularDegreeEdgePool);

    regularDegreeEdgeRandomIterator.resetForNode(1, 5, random);
    assertEquals(new IntArrayList(new int[]{12, 11, 13, 11, 11}),
                 new IntArrayList(regularDegreeEdgeRandomIterator));
    regularDegreeEdgeRandomIterator.resetForNode(2, 5, random);
    assertEquals(new IntArrayList(new int[]{22, 22, 22, 21, 21}),
                 new IntArrayList(regularDegreeEdgeRandomIterator));
    regularDegreeEdgeRandomIterator.resetForNode(3, 5, random);
    assertEquals(new IntArrayList(new int[]{31, 31, 31, 31, 31}),
                 new IntArrayList(regularDegreeEdgeRandomIterator));
    regularDegreeEdgeRandomIterator.resetForNode(4, 5, random);
    assertEquals(new IntArrayList(new int[]{43, 41, 43, 41, 42}),
                 new IntArrayList(regularDegreeEdgeRandomIterator));
  }
}
