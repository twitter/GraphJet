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
