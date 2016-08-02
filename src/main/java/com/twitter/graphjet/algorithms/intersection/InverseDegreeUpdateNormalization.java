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

/**
 * Defines update and normalization functions resulting in the salsa-like inverse degree formula:
 * sim(u,v) = \sum_{w \in N(u) \cap N(v)} 1 / |N(w)|
 * where N(u) = neighbors of nodes u.
 */
public class InverseDegreeUpdateNormalization extends RelatedTweetUpdateNormalization {

  /**
   * Inverse degree update weight
   *
   * @param leftNodeDegree is the degree of the leftNeighbor
   * @return 1.0 / leftNodeDegree
   */
  @Override
  public double computeLeftNeighborContribution(int leftNodeDegree) {
    return 1.0 / leftNodeDegree;
  }

  /**
   * Identity normalization
   *
   * @param cooccurrence is the degree of the leftNeighbor
   * @param similarNodeDegree
   * @param nodeDegree is the degree of the query node
   * @return 1.0
   */
  @Override
  public double computeScoreNormalization(double cooccurrence, int similarNodeDegree,
      int nodeDegree) {
    return 1.0;
  }

}
