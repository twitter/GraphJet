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

package com.twitter.graphjet.directed;

import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.bipartite.segment.IdentityEdgeTypeMask;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.longs.LongArrayList;

// Note that OutIndexedPowerLawMultiSegmentDirectedGraphTest is simply a lightweight wrapper around
// LeftIndexedPowerLawMultiSegmentBipartiteGraph, so the test cases don't need to be elaborate. We basically
// just need to confirm that we've "wired together" the classes correctly.
public class OutIndexedPowerLawMultiSegmentDirectedGraphTest {

  @Test
  public void testBasicGraph() throws Exception {
    OutIndexedPowerLawMultiSegmentDirectedGraph graph = new
        OutIndexedPowerLawMultiSegmentDirectedGraph(1, 10,
        5, 2, 2.0, new IdentityEdgeTypeMask(), new NullStatsReceiver());

    graph.addEdge(1, 11, (byte) 0);
    graph.addEdge(1, 12, (byte) 0);
    graph.addEdge(4, 41, (byte) 0);
    graph.addEdge(2, 21, (byte) 0);
    graph.addEdge(4, 42, (byte) 0);
    graph.addEdge(3, 31, (byte) 0);
    graph.addEdge(2, 22, (byte) 0);
    graph.addEdge(1, 13, (byte) 0);
    graph.addEdge(4, 43, (byte) 0);
    graph.addEdge(5, 11, (byte) 0);

    assertEquals(3, graph.getOutDegree(1));
    assertEquals(2, graph.getOutDegree(2));
    assertEquals(1, graph.getOutDegree(3));
    assertEquals(3, graph.getOutDegree(4));
    assertEquals(0, graph.getOutDegree(13));   // linked to, but no outgoing edges
    assertEquals(0, graph.getOutDegree(11));   // ditto

    assertEquals(new LongArrayList(new long[]{11, 12, 13}), new LongArrayList(graph.getOutEdges(1)));
    assertEquals(new LongArrayList(new long[]{21, 22}), new LongArrayList(graph.getOutEdges(2)));
    assertEquals(new LongArrayList(new long[]{31}), new LongArrayList(graph.getOutEdges(3)));
    assertEquals(new LongArrayList(new long[]{41, 42, 43}), new LongArrayList(graph.getOutEdges(4)));
    assertEquals(new LongArrayList(new long[]{11}), new LongArrayList(graph.getOutEdges(5)));

    Random random = new Random(90238490238409L);
    int numSamples = 5;

    assertEquals(new LongArrayList(new long[]{13, 12, 11, 13, 13}),
        new LongArrayList(graph.getRandomOutEdges(1, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{22, 22, 22, 21, 22}),
        new LongArrayList(graph.getRandomOutEdges(2, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{31, 31, 31, 31, 31}),
        new LongArrayList(graph.getRandomOutEdges(3, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{42, 43, 42, 41, 41}),
        new LongArrayList(graph.getRandomOutEdges(4, numSamples, random)));
    assertEquals(new LongArrayList(new long[]{11, 11, 11, 11, 11}),
        new LongArrayList(graph.getRandomOutEdges(5, numSamples, random)));
  }
}
