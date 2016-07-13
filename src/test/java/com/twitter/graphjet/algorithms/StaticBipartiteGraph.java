package com.twitter.graphjet.algorithms;

import java.util.Random;

import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.bipartite.api.EdgeIterator;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

public class StaticBipartiteGraph implements BipartiteGraph {
  private final Long2ObjectMap<LongList> leftSideGraph;
  private final Long2ObjectMap<LongList> rightSideGraph;

  public StaticBipartiteGraph(
      Long2ObjectMap<LongList> leftSideGraph,
      Long2ObjectMap<LongList> rightSideGraph) {
    this.leftSideGraph = leftSideGraph;
    this.rightSideGraph = rightSideGraph;
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

  @Override public EdgeIterator getRightNodeEdges(long rightNode) {
    return new MockEdgeIterator(rightSideGraph.get(rightNode).iterator());
  }

  @Override public int getRightNodeDegree(long rightNode) {
    return rightSideGraph.get(rightNode).size();
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

  @Override
  public EdgeIterator getRandomRightNodeEdges(long rightNode, int numSamples, Random random) {
    LongList samples = new LongArrayList(numSamples);
    for (int i = 0; i < numSamples; i++) {
      LongList edges = rightSideGraph.get(rightNode);
      samples.add(edges.get(random.nextInt(edges.size())));
    }
    return new MockEdgeIterator(samples.iterator());
  }
}
