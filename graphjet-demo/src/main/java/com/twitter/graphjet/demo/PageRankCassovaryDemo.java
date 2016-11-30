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

package com.twitter.graphjet.demo;

import com.google.common.util.concurrent.AtomicDoubleArray;
import com.twitter.cassovary.graph.DirectedGraph;
import com.twitter.cassovary.graph.Node;
import com.twitter.cassovary.graph.StoredGraphDir;
import com.twitter.cassovary.util.NodeNumberer;
import com.twitter.cassovary.util.io.ListOfEdgesGraphReader;
import com.twitter.graphjet.adapter.cassovary.CassovaryOutIndexedDirectedGraph;
import com.twitter.graphjet.algorithms.MultiThreadedPageRank;
import com.twitter.graphjet.algorithms.PageRank;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple benchmark program that loads a Cassovary graph and runs GraphJet's implementation of
 * PageRank over it.
 */
public class PageRankCassovaryDemo {
  private static class PageRankCassovaryDemoArgs {
    @Option(name = "-inputDir", metaVar = "[path]",
        usage = "input directory", required = true)
    String inputDir;

    @Option(name = "-inputFilePrefix", metaVar = "[string]",
        usage = "prefix of input files", required = true)
    String inputFile;

    @Option(name = "-dumpTopK", metaVar = "[value]",
        usage = "dump top k nodes to stdout")
    int k = 0;

    @Option(name = "-iterations", metaVar = "[value]",
        usage = "number of iterations to run per trial")
    int iterations = 10;

    @Option(name = "-trials", metaVar = "[value]",
        usage = "number of trials to run")
    int trials = 10;

    @Option(name = "-threads", metaVar = "[value]",
        usage = "number of threads")
    int threads = 1;
  }

  public static void main(String[] argv) throws Exception {
    final PageRankCassovaryDemoArgs args = new PageRankCassovaryDemoArgs();
    CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(90));

    try {
      parser.parseArgument(argv);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      return;
    }

    DirectedGraph<Node> cgraph = ListOfEdgesGraphReader.forIntIds(args.inputDir, args.inputFile,
        new NodeNumberer.IntIdentity(), false, false, '\t', StoredGraphDir.OnlyOut(), true)
          .toSharedArrayBasedDirectedGraph(scala.Option.apply(null));

    // Wrap the Cassovary graph.
    CassovaryOutIndexedDirectedGraph graph = new CassovaryOutIndexedDirectedGraph(cgraph);

    // Extract the nodes and the max node id.
    LongOpenHashSet nodes = new LongOpenHashSet();
    long maxNodeId = 0;

    scala.collection.Iterator<Node> iter = cgraph.iterator();
    while (iter.hasNext()) {
      Node n = iter.next();
      nodes.add(n.id());
      if (n.id() > maxNodeId) {
        maxNodeId = n.id();
      }
    }

    System.out.println("Verifying loaded graph...");
    long startTime = System.currentTimeMillis();
    AtomicLong graphEdgeCounter = new AtomicLong();
    nodes.forEach(v -> graphEdgeCounter.addAndGet(graph.getOutDegree(v)));
    System.out.println(graphEdgeCounter.get() + " edges traversed in " +
        (System.currentTimeMillis() - startTime) + "ms");

    double prVector[] = null;
    long total = 0;
    for (int i = 0; i < args.trials; i++) {
      startTime = System.currentTimeMillis();
      System.out.print("Trial " + i + ": Running PageRank for " +
          args.iterations + " iterations - ");

      long endTime;
      if (args.threads == 1) {
        System.out.print("single-threaded: ");
        PageRank pr = new PageRank(graph, nodes, maxNodeId, 0.85, args.iterations, 1e-15);
        pr.run();
        prVector = pr.getPageRankVector();
        endTime = System.currentTimeMillis();
      } else {
        System.out.print(String.format("multi-threaded (%d threads): ", args.threads));
        MultiThreadedPageRank pr = new MultiThreadedPageRank(graph,
            new LongArrayList(nodes), maxNodeId, 0.85, args.iterations, 1e-15, args.threads);
        pr.run();
        endTime = System.currentTimeMillis();
        AtomicDoubleArray prValues = pr.getPageRankVector();
        // We need to convert the AtomicDoubleArray into an ordinary double array.
        // No need to do this more than once.
        if (prVector == null) {
          prVector = new double[prValues.length()];
          for (int n = 0; n < prValues.length(); n++) {
            prVector[n] = prValues.get(n);
          }
        }
      }

      System.out.println("Complete! Elapsed time = " + (endTime-startTime) + " ms");
      total += endTime-startTime;
    }
    System.out.println("Averaged over " + args.trials + " trials: " + total/args.trials + " ms");

    // Extract the top k.
    if (args.k != 0) {
      TopNodes top = new TopNodes(args.k);
      LongIterator nodeIter = nodes.iterator();
      while (nodeIter.hasNext()) {
        long nodeId = nodeIter.nextLong();
        top.offer(nodeId, prVector[(int) nodeId]);
      }

      for (NodeValueEntry entry : top.getNodes()) {
        System.out.println(entry.getNode() + " " + entry.getValue());
      }
    }
  }
}
