package com.twitter.graphjet.adapter.cassovary;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.twitter.cassovary.graph.DirectedGraph;
import com.twitter.cassovary.graph.Node;
import com.twitter.cassovary.graph.TestGraphs;

import com.twitter.graphjet.bipartite.api.EdgeIterator;

public class CassovaryOutIndexedDirectedGraphTest {
  @Test
  public void testG6() throws Exception {
    DirectedGraph<Node> graph = com.twitter.cassovary.graph.TestGraphs.g6_onlyout();
    System.out.println(graph.toString());

    CassovaryOutIndexedDirectedGraph g = new CassovaryOutIndexedDirectedGraph(TestGraphs.g6_onlyout());
    assertEquals(3, g.getOutDegree(10));
    assertEquals(2, g.getOutDegree(11));
    assertEquals(1, g.getOutDegree(12));
    assertEquals(2, g.getOutDegree(13));
    assertEquals(1, g.getOutDegree(14));
    assertEquals(2, g.getOutDegree(15));

    EdgeIterator iter = g.getOutEdges(10);
    assertEquals(true, iter.hasNext());
    assertEquals(true, iter.hasNext());
    assertEquals(true, iter.hasNext());
    // Note, calling hasNext() multiple times shouldn't make a difference.
    assertEquals(11, iter.nextLong());
    assertEquals(true, iter.hasNext());
    assertEquals(12, iter.nextLong());
    assertEquals(true, iter.hasNext());
    assertEquals(13, iter.nextLong());
    assertEquals(false, iter.hasNext());

    iter = g.getOutEdges(11);
    assertEquals(12, iter.nextLong());
    assertEquals(14, iter.nextLong());
    assertEquals(false, iter.hasNext());

    iter = g.getOutEdges(15);
    assertEquals(true, iter.hasNext());
    assertEquals(10, iter.nextLong());
    assertEquals(11, iter.nextLong());
    assertEquals(false, iter.hasNext());

    // Try fetching edges from a node that doesn't exist.
    iter = g.getOutEdges(0);
    assertEquals(false, iter.hasNext());
  }

  @SuppressWarnings("unused")
  @Test(expected=IncompatibleCassovaryGraphException.class)
  public void testIncompatibleGraph1() throws Exception {
    CassovaryOutIndexedDirectedGraph g = new CassovaryOutIndexedDirectedGraph(TestGraphs.g6_onlyin());
  }

  @SuppressWarnings("unused")
  @Test(expected=IncompatibleCassovaryGraphException.class)
  public void testIncompatibleGraph2() throws Exception {
    CassovaryOutIndexedDirectedGraph g = new CassovaryOutIndexedDirectedGraph(TestGraphs.g6());
  }
}
