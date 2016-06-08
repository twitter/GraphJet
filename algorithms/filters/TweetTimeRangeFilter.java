package com.twitter.graphjet.algorithms.filters;

import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.graphjet.algorithms.TweetIDMask;

public class TweetTimeRangeFilter extends RelatedTweetFilter {
  /**
   * constant, custom epoch (we don't use the unix epoch)
   */
  private static final long TWEPOCH = 1288834974657L;
  private final long after;
  private final long before;

  public TweetTimeRangeFilter(long after, long before, StatsReceiver statsReceiver) {
    super(statsReceiver);
    this.after = after;
    this.before = before;
  }

  /**
   * filter tweet outside time range
   *
   * @param tweet is the result node to be checked
   * @return true if the tweet should be discarded
   */
  @Override
  public boolean filter(long tweet) {
    long tweetTime = originalTimeStampFromTweetId(tweet);
    return tweetTime < after || tweetTime > before;
  }

  /*
   * See
   * https://confluence.twitter.biz/display/PLATENG/Snowflake
   */
  public static long timeStampFromTweetId(long id) {
    return (id >> 22) + TWEPOCH;
  }

  public static long originalTimeStampFromTweetId(long id) {
    return timeStampFromTweetId(id & TweetIDMask.MASK);
  }
}
