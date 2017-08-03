package com.twitter.graphjet.algorithms;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for class {@link TweetIDMask}.
 *
 * @see TweetIDMask
 **/
public class TweetIDMaskTest {

  @Test
  public void testPlayer() {
    assertEquals(6917529027641081857L, TweetIDMask.player(1L));
  }

  @Test
  public void testTweet() {
    assertEquals(1048L, TweetIDMask.tweet(1048L));
  }

  @Test
  public void testPromotion() {
    assertEquals((-9223372036854774760L), TweetIDMask.promotion(1048L));
  }

  @Test
  public void testSummary() {
    assertEquals((-6917529027641081856L), TweetIDMask.summary((-9223372036854775808L)));
  }

  @Test
  public void testPhoto() {
    assertEquals((-31L), TweetIDMask.photo((-31L)));
  }

}
