package com.twitter.graphjet.algorithms;

import com.google.common.util.concurrent.AtomicDouble;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.directed.api.OutIndexedDirectedGraph;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.ArrayList;

/**
 * PageRank is an algorithm designed to measure the importance of nodes in a graph.
 *
 */
public class PageRank {
  final private OutIndexedDirectedGraph bigraph;
  final private LongOpenHashSet vertices;
  final private long max;
  final private double dampingFactor;
  final private int nodeCount;
  final private double tolerance;

  public PageRank(OutIndexedDirectedGraph bigraph, LongOpenHashSet vertices, long max, double dampingFactor, double tolerance) {
    this.bigraph = bigraph;
    this.vertices = vertices;
    this.max = max;
    this.dampingFactor = dampingFactor;
    this.nodeCount = vertices.size();
    this.tolerance = tolerance;
  }

  public double deltaOfArrays(double a[], double b[]) {
    double ret = 0.0;
    for (int i = 0; i < a.length; ++i) {
      ret += Math.abs(a[i] - b[i]);
    }
    return ret;
  }

  public double[] iterate(double[] prevPR, double dampingAmount, ArrayList<Long> noOuts, AtomicDouble error) {
    double afterPR[] = new double[(int) (max + 1)];
    AtomicDouble dangleSum = new AtomicDouble();
    noOuts.forEach(v -> dangleSum.addAndGet(prevPR[(int) (long) v]));
    dangleSum.set(dangleSum.get() / nodeCount);

    vertices.forEach(v -> {
      int outDegree = bigraph.getOutDegree(v);
      double outWeight = dampingFactor * prevPR[(int) (long) v] / outDegree;
      EdgeIterator edges = bigraph.getOutEdges(v);
      while (edges.hasNext()) {
        int nbr = (int) edges.nextLong();
        afterPR[nbr] += outWeight;
      }
      afterPR[(int) (long) v] += dampingAmount + dangleSum.get();
    });
    error.set(deltaOfArrays(prevPR, afterPR));
    return afterPR;
  }

  public double[] initializePR(int nodeCount) {
    double prevPR[] = new double[(int) (max + 1)];
    vertices.forEach(v -> prevPR[(int) (long) v] = 1.0 / nodeCount);
    return prevPR;
  }

  public double[] run(int maxIteration) {
    ArrayList<Long> noOuts = new ArrayList<>();
    vertices.forEach(v -> {
      if (bigraph.getOutDegree(v) == 0) {
        noOuts.add(v);
      }
    });
    double dampingAmount = (1.0 - dampingFactor) / nodeCount;
    double prevPR[] = initializePR(nodeCount);

    int i = 0;
    AtomicDouble error = new AtomicDouble(Double.MAX_VALUE);
    while (i < maxIteration && error.get() > tolerance) {
      prevPR = iterate(prevPR, dampingAmount, noOuts, error);
      i++;
    }
    System.out.println("# of iterations " + i);
    return prevPR;
  }
}