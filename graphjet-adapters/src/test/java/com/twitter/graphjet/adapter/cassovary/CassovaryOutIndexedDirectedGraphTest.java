/**
 * Copyright 2016 Twitter. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

  @Test
  public void testSkipping() throws Exception {
    CassovaryOutIndexedDirectedGraph g = new CassovaryOutIndexedDirectedGraph(TestGraphs.g6_onlyout());
    // node 10 -> 11, 12, 13

    EdgeIterator iter = g.getOutEdges(10);
    assertEquals(true, iter.hasNext());
    assertEquals(11, iter.nextLong());
    assertEquals(true, iter.hasNext());
    assertEquals(1, iter.skip(1));
    assertEquals(true, iter.hasNext());
    assertEquals(13, iter.nextLong());
    assertEquals(false, iter.hasNext());

    iter = g.getOutEdges(10);
    assertEquals(true, iter.hasNext());
    assertEquals(1, iter.skip(1));
    assertEquals(true, iter.hasNext());
    assertEquals(12, iter.nextLong());
    assertEquals(1, iter.skip(1));
    assertEquals(false, iter.hasNext());

    iter = g.getOutEdges(10);
    assertEquals(true, iter.hasNext());
    assertEquals(3, iter.skip(10));
    assertEquals(false, iter.hasNext());

    iter = g.getOutEdges(10);
    assertEquals(true, iter.hasNext());
    assertEquals(11, iter.nextLong());
    assertEquals(true, iter.hasNext());
    assertEquals(2, iter.skip(10));
    assertEquals(false, iter.hasNext());

    iter = g.getOutEdges(10);
    assertEquals(true, iter.hasNext());
    assertEquals(2, iter.skip(2));
    assertEquals(true, iter.hasNext());
    assertEquals(13, iter.nextLong());
    assertEquals(false, iter.hasNext());
  }
}
