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

import com.twitter.graphjet.bipartite.LeftIndexedPowerLawMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.api.EdgeTypeMask;
import com.twitter.graphjet.directed.api.DynamicDirectedGraph;
import com.twitter.graphjet.directed.api.OutIndexedDirectedGraph;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * Implementation of a dynamic, out-indexed directed graph. This class is implemented as a wrapper around a
 * {@link LeftIndexedPowerLawMultiSegmentBipartiteGraph} and delegates method calls to the underlying class. We treat a
 * directed graph as a left-indexed bipartite graph where the left and right nodes have the same domain of ids.
 */
public class OutIndexedPowerLawMultiSegmentDirectedGraph implements OutIndexedDirectedGraph, DynamicDirectedGraph {
  protected final LeftIndexedPowerLawMultiSegmentBipartiteGraph graph;

  /**
   * Constructor for a dynamic, out-indexed directed graph.
   *
   * @param maxNumSegments         the maximum number of segments in the graph, after which the oldest segment will
   *                               be dropped
   * @param maxNumEdgesPerSegment  the maximum number of edges in each segment, after which a new segment will be
   *                               created
   * @param expectedNumNodes       the expected number of nodes in each segment
   * @param expectedMaxDegree      the expected maximum degree for a node (soft upper bound)
   * @param powerLawExponent       the exponent of the power law characterizing the left degree distributions, see
   *                               {@link com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool}
   * @param edgeTypeMask           the bit mask for edge types
   * @param statsReceiver          object for tracking internal stats
   */
  public OutIndexedPowerLawMultiSegmentDirectedGraph(
      int maxNumSegments,
      int maxNumEdgesPerSegment,
      int expectedNumNodes,
      int expectedMaxDegree,
      double powerLawExponent,
      EdgeTypeMask edgeTypeMask,
      StatsReceiver statsReceiver) {
    this.graph = new LeftIndexedPowerLawMultiSegmentBipartiteGraph(maxNumSegments, maxNumEdgesPerSegment,
        expectedNumNodes, expectedMaxDegree, powerLawExponent, expectedNumNodes, edgeTypeMask, statsReceiver);
  }

  @Override
  public int getOutDegree(long node) {
    return graph.getLeftNodeDegree(node);
  }

  @Override
  public EdgeIterator getOutEdges(long node) {
    return graph.getLeftNodeEdges(node);
  }

  @Override
  public EdgeIterator getRandomOutEdges(long node, int numSamples, Random random) {
    return graph.getRandomLeftNodeEdges(node, numSamples, random);
  }

  @Override
  public void addEdge(long srcNode, long destNode, byte edgeType) {
    graph.addEdge(srcNode, destNode, edgeType);
  }

  @Override
  public void removeEdge(long srcNode, long destNode) {
    graph.removeEdge(srcNode, destNode);
  }
}
