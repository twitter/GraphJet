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


package com.twitter.graphjet.algorithms.filters;

import com.twitter.graphjet.algorithms.TweetIDMask;
import com.twitter.graphjet.stats.StatsReceiver;

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
