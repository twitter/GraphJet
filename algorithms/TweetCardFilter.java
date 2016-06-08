package com.twitter.graphjet.algorithms;

import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

/**
 * card type filter
 */
public class TweetCardFilter extends ResultFilter {
  private final boolean tweet;
  private final boolean summary;
  private final boolean photo;
  private final boolean player;
  private final boolean promotion;

  /**
   * construct card type filter
   * @param tweet true if and only if keeping plain tweet
   * @param summary true if and only if keeping summary tweet
   * @param photo true if and only if keeping photo tweet
   * @param player true if and only if keeping player tweet
   * @param promotion true if and only if keeping promotion tweet
   */
  public TweetCardFilter(boolean tweet,
                         boolean summary,
                         boolean photo,
                         boolean player,
                         boolean promotion,
                         StatsReceiver statsReceiver) {
    super(statsReceiver);
    this.tweet = tweet;
    this.summary = summary;
    this.photo = photo;
    this.player = player;
    this.promotion = promotion;
  }

  @Override
  public void resetFilter(RecommendationRequest request) {

  }

  /**
   * discard card tweets
   *
   * @param resultNode is the result node to be checked
   * @param socialProofs is the socialProofs of different types associated with the node
   * @return true if the node should be discarded, and false if it should not be
   */
  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    long bits = resultNode & TweetIDMask.METAMASK;
    boolean keep = (tweet && (bits == TweetIDMask.TWEET))
        || (summary && (bits == TweetIDMask.SUMMARY))
        || (photo && (bits == TweetIDMask.PHOTO))
        || (player && (bits == TweetIDMask.PLAYER))
        || (promotion && (bits == TweetIDMask.PROMOTION));
    return !keep;
  }

}
