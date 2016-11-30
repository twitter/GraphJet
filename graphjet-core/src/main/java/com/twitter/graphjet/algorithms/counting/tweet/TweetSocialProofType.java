package com.twitter.graphjet.algorithms.counting.tweet;

// This needs to be in sync with the SocialProofType thrift enum in recos_common.thrift.
public enum TweetSocialProofType {
  CLICK(0),
  FAVORITE(1),
  RETWEET(2),
  REPLY(3),
  TWEET(4),
  IS_MENTIONED(5),
  IS_MEDIATAGGED(6),
  QUOTE(7);

  private final byte value;

  TweetSocialProofType(int value) {
    this.value = (byte) value;
  }

  public byte getValue() {
    return this.value;
  }
}
