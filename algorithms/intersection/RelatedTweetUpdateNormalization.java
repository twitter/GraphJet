package com.twitter.graphjet.algorithms.intersection;

/**
 * Abstracts out the update weights for related tweets.
 */
public abstract class RelatedTweetUpdateNormalization {

  /**
   * Provides an interface for clients to compute related tweet update weights
   *
   * @param leftNodeDegree is the degree of the leftNode
   * @return a transformation of the leftNode degree
   */
  public abstract double computeLeftNeighborContribution(int leftNodeDegree);

  /**
   * Provides an interface for clients to compute related tweet score normalization
   *
   * @param cooccurrence is the computed cooccurrence
   * @param similarNodeDegree is the degree of the candidate tweet
   * @param nodeDegree is the degree of the query tweet
   * @return a transformation of the leftNode degree
   */
  public abstract double computeScoreNormalization(double cooccurrence, int similarNodeDegree,
      int nodeDegree);
}
