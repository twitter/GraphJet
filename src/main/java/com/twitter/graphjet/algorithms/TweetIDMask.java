package com.twitter.graphjet.algorithms;

/**
 * The bit mask used to manipulate tweet ids to encode further information.
 */
public final class TweetIDMask {

  // Utility class
  private TweetIDMask() {
  }

  /**
   * Before 2024, the top 3 bits of tweet id will remain 0.
   */
  public static final long MASK =
      Long.parseLong("0001111111111111111111111111111111111111111111111111111111111111", 2);
  public static final long METAMASK = 7L << 61;
  public static final long TWEET = 0L << 61;
  public static final long SUMMARY = 1L << 61;
  public static final long PHOTO = 2L << 61;
  public static final long PLAYER = 3L << 61;
  public static final long PROMOTION = 4L << 61;
  public static final long UNUSED1 = 5L << 61;
  public static final long UNUSED2 = 6L << 61;
  public static final long UNUSED3 = 7L << 61;

  public static long tweet(long tweet) {
    return tweet;
  }

  public static long summary(long tweet) {
    return tweet | SUMMARY;
  }

  public static long photo(long tweet) {
    return tweet | PHOTO;
  }

  public static long player(long tweet) {
    return tweet | PLAYER;
  }

  public static long promotion(long tweet) {
    return tweet | PROMOTION;
  }

  /**
   * restore the original tweet id by removing the meta data saved in top bits.
   * @param node the tweet id with bitmask
   * @return tweet id without the bitmask
   */
  public static long restore(long node) {
    return node & MASK;
  }
}
