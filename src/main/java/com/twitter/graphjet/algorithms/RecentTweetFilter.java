package com.twitter.graphjet.algorithms;

import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * This filter removes old tweets based on age of the tweet from tweetID.
 * This filter applies only to tweetId as resultNode!!!
 */
public class RecentTweetFilter extends ResultFilter {
  /**
   * constant, custom epoch (we don't use the unix epoch)
   */
  private static final long TWEPOCH = 1288834974657L;
  private final long keepWithInLastXMillis;
  private long cutoff;

  public RecentTweetFilter(long keepWithInLastXMillis, StatsReceiver statsReceiver) {
    super(statsReceiver);
    this.keepWithInLastXMillis = keepWithInLastXMillis;
  }

  @Override
  public void resetFilter(RecommendationRequest request) {
    // cutoff time is reset at query time
    cutoff = System.currentTimeMillis() - keepWithInLastXMillis;
  }

  /**
   * filter magic
   *
   * @param resultNode is the result node to be checked
   * @param socialProofs is the socialProofs of different types associated with the node
   * @return true if the node should be filtered out, and false if it should not be
   */
  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    // assume resultNode is the tweetID
    // remove tweet if older (less) than cutoff time
    return originalTimeStampFromTweetId(resultNode) < cutoff;
  }

  /*
   * See
   * https://confluence.twitter.biz/display/PLATENG/Snowflake
   */
  public static long timeStampFromTweetId(long id) {
    return (id >> 22) + TWEPOCH;
  }

  private static long originalTimeStampFromTweetId(long id) {
    return timeStampFromTweetId(id & TweetIDMask.MASK);
  }
}
