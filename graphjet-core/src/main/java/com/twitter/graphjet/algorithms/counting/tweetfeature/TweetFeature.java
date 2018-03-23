package com.twitter.graphjet.algorithms.counting.tweetfeature;

public enum TweetFeature {
  TWEET_FEATURE(0),       // tweet feature metadata type
  TWEET_FEATURE_SIZE(1);  // tweet feature size

  private final int value;

  private TweetFeature(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  private static final TweetFeature[] VALUES = {TWEET_FEATURE, TWEET_FEATURE_SIZE};

  public static TweetFeature at(int index) {
    return VALUES[index];
  }
}