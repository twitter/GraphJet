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

import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.TweetIDMask;
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

  public static long originalTimeStampFromTweetId(long id) {
    return timeStampFromTweetId(id & TweetIDMask.MASK);
  }
}
