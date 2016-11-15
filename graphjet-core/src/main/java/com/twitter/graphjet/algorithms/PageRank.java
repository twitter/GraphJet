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

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.directed.api.OutIndexedDirectedGraph;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * <p>
 * Implementation of PageRank. This implementation initializes an array of doubles to hold the
 * PageRank vector, where the node id is used as the index into the array. In other words, we use
 * a dense vector representation: this is ideal if all the nodes are assigned sequential (and
 * consecutive) node ids, but potentially wasteful if the node ids are sparsely assigned. Either
 * way, the implementation needs to know the max node id in order to properly instantiate the
 * PageRank vector. Furthermore, the implementation needs to be given the set of node ids that
 * comprise the graph, because this information is not currently accessible from the graph APIs.
 * </p>
 *
 * <p>
 * One limitation of using an array to store the PageRank vector is that we are limited by the size
 * of Java arrays, capped at {@code Integer.MAX_VALUE}. Since GraphJet nodes are longs, this means
 * there are valid graphs that this algorithm can't run on. For these, the current implementation
 * throws an {@code UnsupportedOperationException}.
 * </p>
 *
 * <p>
 * This implementation has a somewhat unconventional consistency model: the nodes under
 * consideration are fixed throughout the computation (i.e., across iterations), but the edges are
 * dynamic. That is, if an edge is added, the next iteration will traverse it.
 * </p>
 */
public class PageRank {
  final private OutIndexedDirectedGraph graph;
  final private LongOpenHashSet nodes;
  final private long maxNodeId;
  final private double dampingFactor;
  final private int nodeCount;
  final private int maxIterations;
  final private double tolerance;

  private double normL1 = Double.MAX_VALUE;
  private double[] prVector = null;

  /**
   * Constructs this object for running PageRank over a directed graph.
   *
   * @param graph          the directed graph
   * @param nodes          nodes in the graph
   * @param maxNodeId      maximum node id
   * @param dampingFactor  damping factor
   * @param maxIterations  maximum number of iterations to run
   * @param tolerance      L1 norm threshold for convergence
   */
  public PageRank(OutIndexedDirectedGraph graph, LongOpenHashSet nodes, long maxNodeId,
                  double dampingFactor, int maxIterations, double tolerance) {
    if (maxNodeId > Integer.MAX_VALUE) {
      throw new UnsupportedOperationException("maxNodeId exceeds Integer.MAX_VALUE!");
    }

    this.graph = graph;
    this.nodes = nodes;
    this.maxNodeId = maxNodeId;
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

  private void iterate(double dampingAmount, LongArrayList noOuts) {
    double nextPR[] = new double[(int) (maxNodeId + 1)]; // PageRank vector after the iteration.

    // First compute how much mass is trapped at the dangling nodes.
    double dangleSum = 0.0;
    LongIterator iter = noOuts.iterator();
    while (iter.hasNext()) {
      dangleSum += prVector[(int) iter.nextLong()];
    }
    dangleSum = dampingFactor * dangleSum / nodeCount;

    // Distribute PageRank mass.
    iter = nodes.iterator();
    while (iter.hasNext()) {
      long v = iter.nextLong();
      int outDegree = graph.getOutDegree(v);
      if (outDegree > 0) {
        double outWeight = dampingFactor * prVector[(int) v] / outDegree;
        EdgeIterator edges = graph.getOutEdges(v);
        while (edges.hasNext()) {
          int nbr = (int) edges.nextLong();
          nextPR[nbr] += outWeight;
        }
      }

      nextPR[(int) v] += dampingAmount + dangleSum;
    }

    normL1 = computeL1Norm(prVector, nextPR);
    prVector = nextPR;
  }

  /**
   * Runs PageRank, either until the max number of iterations has been reached or the L1 norm of
   * the difference between PageRank vectors drops below the tolerance.
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
    prVector = new double[(int) (maxNodeId + 1)];
    nodes.forEach(v -> prVector[(int) (long) v] = 1.0 / nodeCount);

    int i = 0;
    while (i < this.maxIterations && normL1 > tolerance) {
      iterate(dampingAmount, noOuts);
      i++;
    }

    return i;
  }

  /**
   * Returns the final L1 norm value after PageRank has been run.
   *
   * @return the final L1 norm value after PageRank has been run
   */
  public double getL1Norm() {
    return normL1;
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