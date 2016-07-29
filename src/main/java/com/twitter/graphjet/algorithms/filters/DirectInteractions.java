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


package com.twitter.graphjet.algorithms.filters;

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.api.LeftIndexedBipartiteGraph;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This is a utility class holding direct interactions of a left node
 */
public class DirectInteractions {
  private final LeftIndexedBipartiteGraph bipartiteGraph;
  private final LongSet leftNodeEdgeSet;

  public DirectInteractions(LeftIndexedBipartiteGraph bipartiteGraph) {
    this.bipartiteGraph = bipartiteGraph;
    this.leftNodeEdgeSet = new LongOpenHashSet();
  }

  /**
   * populate direct interactions
   *
   * @param queryNode is the left query node
   */
  public void addDirectInteractions(long queryNode) {
    EdgeIterator iterator = bipartiteGraph.getLeftNodeEdges(queryNode);
    if (iterator == null) {
      return;
    }
    leftNodeEdgeSet.clear();
    while (iterator.hasNext()) {
      leftNodeEdgeSet.add(iterator.nextLong());
    }
  }

  /**
   * filter magic
   *
   * @param resultNode is the result node to be checked
   * @return true if the node should be filtered out, and false if it should not be
   */
  public boolean isDirectInteraction(long resultNode) {
    return !leftNodeEdgeSet.isEmpty() && leftNodeEdgeSet.contains(resultNode);
  }
}
