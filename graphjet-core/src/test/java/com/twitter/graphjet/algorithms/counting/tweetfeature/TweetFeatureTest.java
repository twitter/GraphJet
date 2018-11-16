package com.twitter.graphjet.algorithms.counting.tweetfeature;

import com.twitter.graphjet.algorithms.counting.tweetfeature.TweetFeature;
import org.junit.Assert;
import org.junit.Test;

public class TweetFeatureTest {

    @Test
    public void atInputPositiveOutputNotNull() {

      // Arrange
      final int index = 1;

      // Act
      final TweetFeature retval = TweetFeature.at(index);

      // Assert result
      Assert.assertEquals(TweetFeature.TWEET_FEATURE_SIZE, retval);
    }

    @Test
    public void atInputZeroOutputNotNull2() {

      // Arrange
      final int index = 0;

      // Act
      final TweetFeature retval = TweetFeature.at(index);

      // Assert result
      Assert.assertEquals(TweetFeature.TWEET_FEATURE, retval);
    
  }

} 
