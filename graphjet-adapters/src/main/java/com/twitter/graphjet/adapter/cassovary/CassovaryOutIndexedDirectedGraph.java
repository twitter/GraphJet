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

import com.twitter.cassovary.graph.DirectedGraph;
import com.twitter.cassovary.graph.GraphDir;
import com.twitter.cassovary.graph.Node;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.directed.api.OutIndexedDirectedGraph;
import scala.Option;
import scala.collection.Seq;

import java.util.Random;

/**
 * A GraphJet wrapper for an out-indexed Cassovary graph. Implements the GraphJet API by
 * delegating methods to the underlying Cassovary API.
 */
public class CassovaryOutIndexedDirectedGraph implements OutIndexedDirectedGraph {
  // This is the Cassovary graph that's being wrapped.
  final private DirectedGraph<Node> graph;

  /**
   * Constructs a GraphJet wrapper for an out-indexed Cassovary graph.
   *
   * @param graph the Cassovary graph
   */
  public CassovaryOutIndexedDirectedGraph(DirectedGraph<Node> graph) {
    if (!graph.isDirStored(GraphDir.OutDir()) || graph.isBiDirectional()) {
      // If the graph isn't out-indexed or if the graph is bidirectional, we should be using a
      // different wrapper class.
      throw new IncompatibleCassovaryGraphException();
    }
    this.graph = graph;
  }

  @Override
  public int getOutDegree(long node) {
    return graph.getNodeById((int) node).get().outboundCount();
  }

  @Override
  public EdgeIterator getOutEdges(long node) {
    Option<Node> opt = graph.getNodeById((int) node);
    if (opt.isEmpty()) {
      return new EmptyEdgeIterator();
    }

    // Note that outboundNodes returns a CSeq, whereas randomOutboundNodeSet returns a Seq, so we
    // need different wrapper classes.
    return new CSeqEdgeIteratorWrapper(opt.get().outboundNodes());
  }

  @Override
  public EdgeIterator getRandomOutEdges(long node, int numSamples, Random random) {
    Option<Node> opt = graph.getNodeById((int) node);
    if (opt.isEmpty()) {
      return new EmptyEdgeIterator();
    }

    // Note that randomOutboundNodeSet returns a Seq, whereas outboundNodes returns a CSeq, so we
    // need different wrapper classes.
    return new SeqEdgeIteratorWrapper(opt.get().randomOutboundNodeSet(
        numSamples, scala.util.Random.javaRandomToRandom(random)));
  }

  /**
   * Wrapper for a Scala Seq as an EdgeIterator.
   */
  private class SeqEdgeIteratorWrapper implements EdgeIterator {
    final private Seq seq;
    private int index = 0;

    public SeqEdgeIteratorWrapper(Seq seq) {
      this.seq = seq;
      index = 0;
    }

    @Override
    public byte currentEdgeType() {
      // Always return 0 since Cassovary edges aren't typed.
      return 0;
    }

    @Override
    public long currentMetadata() {
      // Always return 0 since Cassovary edges do not have metadata.
      return 0L;
    }

    @Override
    public boolean hasNext() {
      return index < seq.length();
    }

    @Override
    public long nextLong() {
      return (long) (int) seq.apply(index++);
    }

    @Override
    public Long next() {
      return (Long) seq.apply(index++);
    }

    @Override
    public int skip(int n) {
      if (index + n < seq.length()) {
        index += n;
        return n;
      }

      int skipped = seq.length() - index;
      index = seq.length();
      return skipped;
    }
  }

  /**
   * Wrapper for CSeq as an EdgeIterator.
   */
  private class CSeqEdgeIteratorWrapper implements EdgeIterator {
    final private com.twitter.cassovary.collections.CSeq seq;
    private int index = 0;

    public CSeqEdgeIteratorWrapper(com.twitter.cassovary.collections.CSeq seq) {
      this.seq = seq;
      index = 0;
    }

    @Override
    public byte currentEdgeType() {
      // Always return 0 since Cassovary edges aren't typed.
      return 0;
    }

    @Override
    public long currentMetadata() {
      // Always return 0 since Cassovary edges do not have edge metadata.
      return 0L;
    }

    @Override
    public boolean hasNext() {
      return index < seq.length();
    }

    @Override
    public long nextLong() {
      return (long) (int) seq.apply(index++);
    }

    @Override
    public Long next() {
      return (Long) seq.apply(index++);
    }

    @Override
    public int skip(int n) {
      if (index + n < seq.length()) {
        index += n;
        return n;
      }

      int skipped = seq.length() - index;
      index = seq.length();
      return skipped;
    }
  }

  /**
   * An empty EdgeIterator.
   */
  private class EmptyEdgeIterator implements EdgeIterator {
    @Override
    public byte currentEdgeType() {
      return 0;
    }

    @Override
    public long currentMetadata() {
      return 0L;
    }

    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public long nextLong() {
      return 0;
    }

    @Override
    public Long next() {
      return 0L;
    }

    @Override
    public int skip(int n) {
      return 0;
    }
  }
}
