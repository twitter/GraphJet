package com.twitter.graphjet.algorithms.intersection;

/**
 * Defines update and normalization functions resulting in a scaled similarity formula:
 * sim(u,v) = \sum_{w \in N(u) \cap N(v)} 1 / lg(|N(w)|) /
 *                  (\sqrt{|N(v)|} * \sqrt{1 + ||N(v)| - |N(u)|})
 * where N(u) = neighbors of nodes u.
 */
public class ScaledUpdateNormalization extends RelatedTweetUpdateNormalization {

  private static final double LN2 = Math.log(2);
  /**
   * Scaled update weight
   *
   * @param leftNodeDegree is the degree of the leftNeighbor
   * @return LN2 / Math.log(leftNodeDegree);
   */
  @Override
  public double computeLeftNeighborContribution(int leftNodeDegree) {
    return LN2 / Math.log(leftNodeDegree);
  }

  /**
   * Scaled normalization
   *
   * @param cooccurrence is the degree of the leftNeighbor
   * @param similarNodeDegree
   * @param nodeDegree is the degree of the query node
   * @return 1.0 / (Math.sqrt(similarNodeDegree) *
   *                Math.sqrt(Math.abs(similarNodeDegree - nodeDegree) + 1.0))
   */
  @Override
  public double computeScoreNormalization(double cooccurrence, int similarNodeDegree,
      int nodeDegree) {
    return 1.0 / (Math.sqrt(similarNodeDegree)
        * Math.sqrt(Math.abs(similarNodeDegree - nodeDegree) + 1.0));
  }
}
