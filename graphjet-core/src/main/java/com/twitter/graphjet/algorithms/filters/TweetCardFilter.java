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
