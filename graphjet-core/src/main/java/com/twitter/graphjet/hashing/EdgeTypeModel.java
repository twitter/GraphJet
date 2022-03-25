package com.twitter.graphjet.hashing;

/**
 * @author Harsh Shah
 */
public class EdgeTypeModel {

  public static final int INTEGER_TOP_TWO_BYTE_SHIFT = 1 << 16;
  public static final int INTEGER_TOP_TWO_BYTE_OVERFLOW = 0x7fff;
  public static final int FAVORITE_ACTION = 1;
  public static final int RETWEET_ACTION = 2;
  public static final int REPLY_ACTION = 3;
  public static final int QUOTE_ACTION = 7;

  // This method can only be accessed through incrementFeatureValue method.
  public int getFeaturePosition(int position, byte edgeType) {
    if (edgeType == FAVORITE_ACTION || edgeType == RETWEET_ACTION) {  // FAVORITE OR RETWEET
      return position;
    } else { // REPLY OR QUOTE
      return position + 1;
    }
  }

  // This method can only be accessed through incrementFeatureValue method.
  public int getIncrementValue(byte edgeType) {
    if (edgeType == FAVORITE_ACTION || edgeType == REPLY_ACTION) { // FAVORITE OR REPLY
      return 1;
    } else { // RETWEET OR QUOTE
      return INTEGER_TOP_TWO_BYTE_SHIFT;
    }
  }

  // This method can only be accessed through incrementFeatureValue method.
  public int getFeatureValue(int currentEntry, byte edgeType) {
    if (edgeType == FAVORITE_ACTION || edgeType == REPLY_ACTION) { // FAVORITE OR REPLY
      return currentEntry & 0xffff;
    } else { // RETWEET OR QUOTE
      return (currentEntry >> 16) & 0xffff;
    }
  }


}
