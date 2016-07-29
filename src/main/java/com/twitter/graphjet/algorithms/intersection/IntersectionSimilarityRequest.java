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


package com.twitter.graphjet.algorithms.intersection;

import com.twitter.graphjet.algorithms.SimilarityRequest;

import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This class encapsulates an intersection similarity request.
 */
public class IntersectionSimilarityRequest extends SimilarityRequest {
  private final LongSet seedSet;
  private final int maxNumNeighbors;
  private final int minNeighborDegree;
  private final int maxNumSamplesPerNeighbor;
  private final int minCooccurrence;
  private final int minQueryDegree;
  private final double maxLowerMultiplicativeDeviation;
  private final double maxUpperMultiplicativeDeviation;
  private final boolean populateTweetFeatures;

  /**
   * Create a new request
   *
   * @param queryNode                        is the query node for similarity
   * @param seedSet                          is the seed set to use to supplement the query node
   * @param maxNumNeighbors                  is the maximum number of neighbors to consider in the
   *                                         computation for efficiency
   * @param minNeighborDegree                is the minimum degree an LHS node must have to be
   *                                         considered in the computation
   * @param maxNumSamplesPerNeighbor         is the maximum number of samples an LHS neighbor can
   *                                         contribute
   * @param minCooccurrence                  is the percent of LHS nodes a potential result must
   *                                         share before being considered similar
   * @param minQueryDegree                   is the minimum degree of the query tweet
   * @param maxLowerMultiplicativeDeviation  lower bounds the degree deviation of a potential
   *                                         candidate
   * @param maxUpperMultiplicativeDeviation  upper bounds the degree deviation of a potential
   *                                         candidate
   * @param populateTweetFeatures            is a flag which will return graph features when true
   *                                         //TODO: implementation
   */
  public IntersectionSimilarityRequest(
      long queryNode,
      int maxNumResults,
      LongSet seedSet,
      int maxNumNeighbors,
      int minNeighborDegree,
      int maxNumSamplesPerNeighbor,
      int minCooccurrence,
      int minQueryDegree,
      double maxLowerMultiplicativeDeviation,
      double maxUpperMultiplicativeDeviation,
      boolean populateTweetFeatures) {
    super(queryNode, maxNumResults);
    this.seedSet = seedSet;
    this.maxNumNeighbors = maxNumNeighbors;
    this.minNeighborDegree = minNeighborDegree;
    this.maxNumSamplesPerNeighbor = maxNumSamplesPerNeighbor;
    this.minCooccurrence = minCooccurrence;
    this.minQueryDegree = minQueryDegree;
    this.maxLowerMultiplicativeDeviation = maxLowerMultiplicativeDeviation;
    this.maxUpperMultiplicativeDeviation = maxUpperMultiplicativeDeviation;
    this.populateTweetFeatures = populateTweetFeatures;
  }

  public LongSet getSeedSet() {
    return seedSet;
  }

  public int getMaxNumNeighbors() {
    return maxNumNeighbors;
  }

  public int getMinNeighborDegree() {
    return minNeighborDegree;
  }

  public int getMaxNumSamplesPerNeighbor() {
    return maxNumSamplesPerNeighbor;
  }

  public int getMinCooccurrence() {
    return minCooccurrence;
  }

  public int getMinQueryDegree() {
    return minQueryDegree;
  }

  public double getMaxLowerMultiplicativeDeviation() {
    return maxLowerMultiplicativeDeviation;
  }

  public double getMaxUpperMultiplicativeDeviation() {
    return maxUpperMultiplicativeDeviation;
  }

  public boolean getPopulateTweetFeatures() {
    return populateTweetFeatures;
  }
}
