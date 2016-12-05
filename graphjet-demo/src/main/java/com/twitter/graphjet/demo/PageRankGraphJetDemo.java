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

import com.twitter.graphjet.algorithms.MultiThreadedPageRank;
import com.twitter.graphjet.algorithms.PageRank;
import com.twitter.graphjet.bipartite.segment.IdentityEdgeTypeMask;
import com.twitter.graphjet.directed.OutIndexedPowerLawMultiSegmentDirectedGraph;
import com.twitter.graphjet.stats.NullStatsReceiver;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

/**
 * Simple benchmark program that loads a graph and runs PageRank over it.
 */
public class PageRankGraphJetDemo {
  private static class PageRankGraphJetDemoArgs {
    @Option(name = "-inputFile", metaVar = "[value]",
        usage = "input data", required = true)
    String inputFile;

    @Option(name = "-maxSegments", metaVar = "[value]",
        usage = "maximum number of segments")
    int maxSegments = 20;

    @Option(name = "-maxEdgesPerSegment", metaVar = "[value]",
        usage = "maximum number of edges in each segment")
    int maxEdgesPerSegment = 10000000;

    @Option(name = "-numNodes", metaVar = "[value]",
        usage = "expected number of nodes in each segment")
    int numNodes = 1000000;

    @Option(name = "-expectedMaxDegree", metaVar = "[value]",
        usage = "expected maximum degree")
    int expectedMaxDegree = 5000000;

    @Option(name = "-powerLawExponent", metaVar = "[value]",
        usage = "power Law exponent")
    float powerLawExponent = 2.0f;

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

  private static final byte EDGE_TYPE = (byte) 1;

  public static void main(String[] argv) throws Exception {
    final PageRankGraphJetDemoArgs args = new PageRankGraphJetDemoArgs();
    CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(90));

    try {
      parser.parseArgument(argv);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      return;
    }

    String graphPath = args.inputFile;

    OutIndexedPowerLawMultiSegmentDirectedGraph graph =
        new OutIndexedPowerLawMultiSegmentDirectedGraph(args.maxSegments, args.maxEdgesPerSegment,
            args.numNodes, args.expectedMaxDegree, args.powerLawExponent,
            new IdentityEdgeTypeMask(),
            new NullStatsReceiver());

    final LongOpenHashSet nodes = new LongOpenHashSet();   // Note, *not* thread safe.
    final AtomicLong fileEdgeCounter = new AtomicLong();
    final AtomicLong maxNodeId = new AtomicLong();

    System.out.println("Loading graph from file...");
    long loadStart = System.currentTimeMillis();

    Files.walk(Paths.get(graphPath)).forEach(filePath -> {
      if (Files.isRegularFile(filePath)) {
        try {
          InputStream inputStream = Files.newInputStream(filePath);
          GZIPInputStream gzip = new GZIPInputStream(inputStream);
          BufferedReader br = new BufferedReader(new InputStreamReader(gzip));
          String line;
          while((line = br.readLine()) != null) {
            if (line.startsWith("#")) continue;

            String[] tokens = line.split("\\s+");
            if (tokens.length > 1) {
              final long from = Long.parseLong(tokens[0]);
              final long to = Long.parseLong(tokens[1]);
              graph.addEdge(from, to, EDGE_TYPE);
              fileEdgeCounter.incrementAndGet();

              // Print logging output every 10 million edges.
              if (fileEdgeCounter.get() % 10000000 == 0 ) {
                System.out.println(String.format("%d million edges read, elapsed time %.2f seconds",
                    fileEdgeCounter.get()/1000000, (System.currentTimeMillis() - loadStart)/1000.0));
              }

              // Note, LongOpenHashSet not thread safe so we need to synchronize manually.
              synchronized(nodes) {
                if (!nodes.contains(from)) {
                  nodes.add(from);
                }
                if (!nodes.contains(to)) {
                  nodes.add(to);
                }
              }

              maxNodeId.getAndUpdate(x -> Math.max(x, from));
              maxNodeId.getAndUpdate(x -> Math.max(x, to));
            }
          }
        } catch (Exception e) {
          // Catch all exceptions and quit.
          e.printStackTrace();
          System.exit(-1);
        }
      }
    });

    long loadEnd = System.currentTimeMillis();
    System.out.println(String.format("Read %d vertices, %d edges loaded in %d ms",
        nodes.size(), fileEdgeCounter.get(), (loadEnd-loadStart)));
    System.out.println(String.format("Average: %.0f edges per second",
        fileEdgeCounter.get()/((float) (loadEnd-loadStart))*1000));

    System.out.println("Verifying loaded graph...");
    long startTime = System.currentTimeMillis();
    AtomicLong graphEdgeCounter = new AtomicLong();
    nodes.forEach(v -> graphEdgeCounter.addAndGet(graph.getOutDegree(v)));
    System.out.println(graphEdgeCounter.get() + " edges traversed in " +
        (System.currentTimeMillis() - startTime) + "ms");

    if (fileEdgeCounter.get() != graphEdgeCounter.get()) {
      System.err.println(String.format("Error, edge counts don't match! Expected: %d, Actual: %d",
          fileEdgeCounter.get(), graphEdgeCounter.get()));
      System.exit(-1);
    }

    double prVector[] = null;
    long total = 0;
    for (int i = 0; i < args.trials; i++) {
      startTime = System.currentTimeMillis();
      System.out.print("Trial " + i + ": Running PageRank for " +
          args.iterations + " iterations... ");

      long endTime;
      if (args.threads == 1) {
        System.out.print("single-threaded: ");
        PageRank pr = new PageRank(graph, nodes, maxNodeId.get(), 0.85, args.iterations, 1e-15);
        pr.run();
        prVector = pr.getPageRankVector();
        endTime = System.currentTimeMillis();
      } else {
        System.out.print(String.format("multi-threaded (%d threads): ", args.threads));
        MultiThreadedPageRank pr = new MultiThreadedPageRank(graph,
            new LongArrayList(nodes), maxNodeId.get(), 0.85, args.iterations, 1e-15, args.threads);
        pr.run();
        endTime = System.currentTimeMillis();
        com.google.common.util.concurrent.AtomicDoubleArray prValues = pr.getPageRankVector();
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
      it.unimi.dsi.fastutil.longs.LongIterator nodeIter = nodes.iterator();
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
