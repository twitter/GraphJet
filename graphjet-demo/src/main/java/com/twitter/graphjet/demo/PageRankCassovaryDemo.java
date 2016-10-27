package com.twitter.graphjet.demo;

import com.twitter.cassovary.graph.DirectedGraph;
import com.twitter.cassovary.graph.Node;
import com.twitter.cassovary.graph.StoredGraphDir;
import com.twitter.cassovary.util.NodeNumberer;
import com.twitter.cassovary.util.io.ListOfEdgesGraphReader;
import com.twitter.graphjet.adapter.cassovary.CassovaryOutIndexedDirectedGraph;
import com.twitter.graphjet.algorithms.PageRank;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

public class PageRankCassovaryDemo {
  private static class PageRankCassovaryDemoArgs {
    @Option(name = "-inputDir", metaVar = "[value]", usage = "maximum number of segments", required = true)
    String inputDir;

    @Option(name = "-inputFile", metaVar = "[value]", usage = "maximum number of segments", required = true)
    String inputFile;

    @Option(name = "-dumpTopK", metaVar = "[value]", usage = "Dump top k nodes to stdout")
    int k = 0;
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
        new NodeNumberer.IntIdentity(), false, false, '\t', StoredGraphDir.OnlyOut())
          .toSharedArrayBasedDirectedGraph(scala.Option.apply(null));

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

    double prVector[] = null;
    long numRuns = 10;
    long total = 0;
    for (int i = 0; i < numRuns; ++i) {
      long startTime = System.currentTimeMillis();
      System.out.print("Trial " + i + ": Running PageRank for 10 iterations... ");

      PageRank pr = new PageRank(graph, nodes, maxNodeId, 0.85, 10, 1e-15);
      pr.run();
      prVector = pr.getPageRankVector();
      long endTime = System.currentTimeMillis();

      System.out.println("Complete! Elapsed time = " + (endTime-startTime) + " ms");
      total += endTime-startTime;
    }
    System.out.println("Averaged over " + numRuns + " trials: " + total/numRuns + " ms");

    if (args.k != 0) {
      TopNodes top = new TopNodes(args.k);
      LongIterator nodeIter = nodes.iterator();
      while (nodeIter.hasNext()) {
        long nodeId = nodeIter.nextLong();
        System.out.println(nodeId);
        top.offer(nodeId, prVector[(int) nodeId]);
      }

      for (NodeValueEntry entry : top.getNodes()) {
        System.out.println(entry.getNode() + " " + entry.getValue());
      }
    }
  }
}
