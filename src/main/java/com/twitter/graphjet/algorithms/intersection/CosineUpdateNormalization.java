package com.twitter.graphjet.algorithms.intersection;

/**
 * Defines update and normalization functions resulting in the cosine formula, i.e.
 * sim(u,v) = |N(u) \cap N(v)| / (\sqrt{|N(u)|} * \sqrt{|N(v)|})
 * where N(u) = neighbors of nodes u.
 */
public class CosineUpdateNormalization extends RelatedTweetUpdateNormalization {

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
   * Cosine normalization
   *
   * @param cooccurrence is the degree of the leftNeighbor
   * @param similarNodeDegree
   * @param nodeDegree is the degree of the query node
   * @return 1.0 / Math.sqrt(similarNodeDegree)
   */
  @Override
  public double computeScoreNormalization(double cooccurrence, int similarNodeDegree,
      int nodeDegree) {
    return 1.0 / Math.sqrt(similarNodeDegree);
  }
}
