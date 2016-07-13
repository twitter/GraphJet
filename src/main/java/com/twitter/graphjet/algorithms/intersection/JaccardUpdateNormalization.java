package com.twitter.graphjet.algorithms.intersection;

/**
 * Defines update and normalization functions resulting in the jaccard formula, i.e.
 * sim(u,v) = |N(u) \cap N(v)| / |N(u) \cup N(v)|
 * where N(u) = neighbors of nodes u.
 */
public class JaccardUpdateNormalization extends RelatedTweetUpdateNormalization {

  /**
   * Identity update weight
   *
   * @param leftNodeDegree is the degree of the leftNeighbor
   * @return 1.0
   */
  @Override
  public double computeLeftNeighborContribution(int leftNodeDegree) {
    return 1.0;
  }

  /**
   * Jaccard normalization
   *
   * @param cooccurrence is the degree of the leftNeighbor
   * @param similarNodeDegree
   * @param nodeDegree is the degree of the query node
   * @return 1.0 / (similarNodeDegree + nodeDegree - cooccurrence)
   */
  @Override
  public double computeScoreNormalization(double cooccurrence, int similarNodeDegree,
      int nodeDegree) {
    return 1.0 / (similarNodeDegree + nodeDegree - cooccurrence);
  }

}
