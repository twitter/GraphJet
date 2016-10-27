package com.twitter.graphjet.demo;

import com.twitter.graphjet.algorithms.PageRank;
import com.twitter.graphjet.bipartite.segment.IdentityEdgeTypeMask;
import com.twitter.graphjet.directed.OutIndexedPowerLawMultiSegmentDirectedGraph;
import com.twitter.graphjet.stats.NullStatsReceiver;
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

public class PageRankGraphJetDemo {
  private static class PageRankGraphJetDemoArgs {
    @Option(name = "-inputFile", metaVar = "[value]", usage = "maximum number of segments", required = true)
    String inputFile;

    @Option(name = "-maxSegments", metaVar = "[value]", usage = "maximum number of segments")
    int maxSegments = 15;

    @Option(name = "-maxEdgesPerSegment", metaVar = "[value]", usage = "maximum number of edges in each segment")
    int maxEdgesPerSegment = 5000000;

    @Option(name = "-leftSize", metaVar = "[value]", usage = "expected number of nodes on left side")
    int leftSize = 5000000;

    @Option(name = "-leftDegree", metaVar = "[value]", usage = "expected maximum degree on left side")
    int leftDegree = 5000000;

    @Option(name = "-leftPowerLawExponent", metaVar = "[value]", usage = "left side Power Law exponent")
    float leftPowerLawExponent = 2.0f;
  }

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

    OutIndexedPowerLawMultiSegmentDirectedGraph bigraph =
        new OutIndexedPowerLawMultiSegmentDirectedGraph(args.maxSegments, args.maxEdgesPerSegment,
            args.leftSize, args.leftDegree, args.leftPowerLawExponent,
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
              bigraph.addEdge(from, to, (byte) 1);
              fileEdgeCounter.incrementAndGet();

              // Print logging output every 10 million edges.
              if (fileEdgeCounter.get() % 10000000 == 0 ) {
                System.out.println(String.format("%d million edges read, elapsed time %.2f seconds",
                    fileEdgeCounter.get()/1000000, (System.currentTimeMillis() - loadStart)/1000.0));
              }

              // Note, LongOpenHashSet not thread safe so we need to synchronize manually
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
    AtomicLong graphEdgeCounter = new AtomicLong();
    nodes.forEach(v -> graphEdgeCounter.addAndGet(bigraph.getOutDegree(v)));

    if (fileEdgeCounter.get() == graphEdgeCounter.get()) {
      System.out.println("Edge count: " + fileEdgeCounter.get());
    } else {
      System.err.println(String.format("Error, edge counts don't match! Expected: %d, Actual: %d",
          fileEdgeCounter.get(), graphEdgeCounter.get()));
      System.exit(-1);
    }
    System.out.println("Count of graph edges verified!");

    long numRuns = 10;
    long total = 0;
    for (int i = 0; i < numRuns; ++i) {
      long startTime = System.currentTimeMillis();
      System.out.print("Trial " + i + ": Running PageRank for 10 iterations... ");

      PageRank pr = new PageRank(bigraph, nodes, maxNodeId.get(), 0.85, 10, 1e-15);
      pr.run();
      double prVector[] = pr.getPageRankVector();
      long endTime = System.currentTimeMillis();

      System.out.println("Complete! Elapsed time = " + (endTime-startTime) + " ms");
      total += endTime-startTime;
    }
    System.out.println("Averaged over " + numRuns + " trials: " + total/numRuns + " ms");
  }
}
