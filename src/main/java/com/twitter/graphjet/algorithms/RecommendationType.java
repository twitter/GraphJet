package com.twitter.graphjet.algorithms;

public enum RecommendationType {
  HASHTAG(0),       // hashtag metadata type
  URL(1),           // url metadata type
  METADATASIZE(2),  // the size of supported metadata types
  TWEET(3);         // tweet, not a metadata type

  private final int value;

  private RecommendationType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  private static final RecommendationType[] VALUES = {HASHTAG, URL, METADATASIZE, TWEET};

  public static RecommendationType at(int index) {
    return VALUES[index];
  }
}
