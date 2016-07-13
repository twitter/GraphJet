package com.twitter.graphjet.algorithms;

import java.util.Random;

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.api.LeftIndexedBipartiteGraph;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

public class StaticLeftIndexedBipartiteGraph implements LeftIndexedBipartiteGraph {
  private final Long2ObjectMap<LongList> leftSideGraph;

  public StaticLeftIndexedBipartiteGraph(Long2ObjectMap<LongList> leftSideGraph) {
    this.leftSideGraph = leftSideGraph;
  }

  @Override public int getLeftNodeDegree(long leftNode) {
    if (!leftSideGraph.containsKey(leftNode)) {
      return 0;
    }
    return leftSideGraph.get(leftNode).size();
  }

  @Override public EdgeIterator getLeftNodeEdges(long leftNode) {
    return new MockEdgeIterator(leftSideGraph.get(leftNode).iterator());
  }

  @Override
  public EdgeIterator getRandomLeftNodeEdges(long leftNode, int numSamples, Random random) {
    LongList samples = new LongArrayList(numSamples);
    for (int i = 0; i < numSamples; i++) {
      LongList edges = leftSideGraph.get(leftNode);
      samples.add(edges.get(random.nextInt(edges.size())));
    }
    return new MockEdgeIterator(samples.iterator());
  }
}
