package com.twitter.graphjet.adapter.cassovary;

import java.util.Random;

import com.twitter.cassovary.graph.DirectedGraph;
import com.twitter.cassovary.graph.Node;
import com.twitter.cassovary.graph.GraphDir;
import com.twitter.graphjet.directed.api.OutIndexedDirectedGraph;
import com.twitter.graphjet.bipartite.api.EdgeIterator;

import scala.collection.Seq;
import scala.Option;


public class CassovaryOutIndexedDirectedGraph implements OutIndexedDirectedGraph {
  // This is the Cassovary graph that's being wrapped.
  final private DirectedGraph<Node> graph;

  public CassovaryOutIndexedDirectedGraph(DirectedGraph<Node> graph) {
    if (!graph.isDirStored(GraphDir.OutDir()) || graph.isBiDirectional()) {
      // If the graph isn't out-indexed or if the graph is bidirectional, we should be using a different wrapper class.
      throw new IncompatibleCassovaryGraphException();
    }
    this.graph = graph;
  }

  public int getOutDegree(long node) {
    return graph.getNodeById((int) node).get().outboundCount();
  }

  public EdgeIterator getOutEdges(long node) {
    Option<Node> opt = graph.getNodeById((int) node);
    if (opt.isEmpty()) {
      return new EmptyEdgeIterator();
    }

    return new SeqEdgeIteratorWrapper(opt.get().outboundNodes());
  }

  public EdgeIterator getRandomOutEdges(long node, int numSamples, Random random) {
    Option<Node> opt = graph.getNodeById((int) node);
    if (opt.isEmpty()) {
      return new EmptyEdgeIterator();
    }

    return new SeqEdgeIteratorWrapper(opt.get().randomOutboundNodeSet(
        numSamples, scala.util.Random.javaRandomToRandom(random)));
  }

  private class SeqEdgeIteratorWrapper implements EdgeIterator {
    final private Seq seq;
    private int index = 0;

    public SeqEdgeIteratorWrapper(Seq seq) {
      this.seq = seq;
      index = 0;
    }

    public byte currentEdgeType() {
      // Always return 0 since Cassovary edges aren't typed.
      return 0;
    }

    public boolean hasNext() {
      return index < seq.length();
    }

    public long nextLong() {
      return (long) (int) seq.apply(index++);
    }

    public Long next() {
      return (Long) seq.apply(index++);
    }

    public int skip(int n) {
      // TODO: This doesn't work yet.
      return 0;
    }
  }

  private class EmptyEdgeIterator implements EdgeIterator {
    public byte currentEdgeType() {
      return 0;
    }

    public boolean hasNext() {
      return false;
    }

    public long nextLong() {
      return 0;
    }

    public Long next() {
      return 0L;
    }

    public int skip(int n) {
      return 0;
    }
  }
}
