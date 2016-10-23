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

package com.twitter.graphjet.algorithms;

import com.google.common.util.concurrent.AtomicDouble;
import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.directed.api.OutIndexedDirectedGraph;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * Implementation of PageRank. This implementation initializes an array of doubles to hold PageRank values, where the
 * node id is used as the index into the array. Therefore, the algorithm needs to know the max node id as well as the
 * set of nodes, in the case where the node ids are not assigned sequentially.
 */
public class PageRank {
  final private OutIndexedDirectedGraph graph;
  final private LongOpenHashSet nodes;
  final private long maxNodeId;
  final private double dampingFactor;
  final private int nodeCount;
  final private int maxIterations;
  final private double tolerance;

  final private AtomicDouble normL1 = new AtomicDouble(Double.MAX_VALUE);
  private double[] prVector = null;

  /**
   * Constructs this object for running PageRank over a directed graph.
   *
   * @param graph          the directed graph
   * @param nodes          nodes in the graph
   * @param maxNodesId     maximum node id
   * @param dampingFactor  damping factor
   * @param maxIterations  maximum number of iterations to run
   * @param tolerance      L1 norm threshold for convergence
   */
  public PageRank(OutIndexedDirectedGraph graph, LongOpenHashSet nodes, long maxNodesId, double dampingFactor,
                  int maxIterations, double tolerance) {
    this.graph = graph;
    this.nodes = nodes;
    this.maxNodeId = maxNodesId;
    this.dampingFactor = dampingFactor;
    this.nodeCount = nodes.size();
    this.maxIterations = maxIterations;
    this.tolerance = tolerance;
  }

  private double computeL1Norm(double a[], double b[]) {
    double ret = 0.0;
    for (int i = 0; i < a.length; ++i) {
      ret += Math.abs(a[i] - b[i]);
    }
    return ret;
  }

  private double[] iterate(double[] prevPR, double dampingAmount, LongArrayList noOuts) {
    double nextPR[] = new double[(int) (maxNodeId + 1)]; // PageRank vector after the iteration.

    // First compute how much mass is trapped at the dangling nodes.
    AtomicDouble dangleSum = new AtomicDouble();
    LongIterator iter = noOuts.iterator();
    while (iter.hasNext()) {
      dangleSum.addAndGet(prevPR[(int) iter.nextLong()]);
    }
    dangleSum.set(dampingFactor * dangleSum.get() / nodeCount);

    // Distribute PageRank mass.
    iter = nodes.iterator();
    while (iter.hasNext()) {
      long v = iter.nextLong();
      int outDegree = graph.getOutDegree(v);
      if (outDegree > 0) {
        double outWeight = dampingFactor * prevPR[(int) v] / outDegree;
        EdgeIterator edges = graph.getOutEdges(v);
        while (edges.hasNext()) {
          int nbr = (int) edges.nextLong();
          nextPR[nbr] += outWeight;
        }
      }

      nextPR[(int) v] += dampingAmount + dangleSum.get();
    }

    normL1.set(computeL1Norm(prevPR, nextPR));
    return nextPR;
  }

  private double[] initializePageRankVector(int nodeCount) {
    double pr[] = new double[(int) (maxNodeId + 1)];
    nodes.forEach(v -> pr[(int) (long) v] = 1.0 / nodeCount);
    return pr;
  }

  /**
   * Runs PageRank, either until the max number of iterations has been reached or the L1 norm of the difference between
   * PageRank vectors drops below the tolerance
   *
   * @return number of iterations that was actually run
   */
  public int run() {
    LongArrayList noOuts = new LongArrayList();
    LongIterator iter = nodes.iterator();
    while (iter.hasNext()) {
      long v = iter.nextLong();
      if (graph.getOutDegree(v) == 0) {
        noOuts.add(v);
      }
    }

    double dampingAmount = (1.0 - dampingFactor) / nodeCount;
    double pr[] = initializePageRankVector(nodeCount);

    int i = 0;
    while (i < this.maxIterations && normL1.get() > tolerance) {
      pr = iterate(pr, dampingAmount, noOuts);
      i++;
    }

    prVector = pr;
    return i;
  }

  /**
   * Returns the final L1 norm value after PageRank has been run.
   *
   * @return the final L1 norm value after PageRank has been run
   */
  public double getL1Norm() {
    return normL1.get();
  }

  /**
   * Returns the PageRank vector, or null if PageRank has not yet been run.
   *
   * @return the PageRank vector, or null if PageRank has not yet been run
   */
  public double[] getPageRankVector() {
    return prVector;
  }
}