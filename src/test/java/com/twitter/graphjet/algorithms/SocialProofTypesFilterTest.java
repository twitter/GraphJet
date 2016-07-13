package com.twitter.graphjet.algorithms;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.algorithms.salsa.SalsaRequest;
import com.twitter.graphjet.algorithms.salsa.SalsaRequestBuilder;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.NullStatsReceiver;

public class SocialProofTypesFilterTest {
  private SocialProofTypesFilter socialProofTypeFilter = new SocialProofTypesFilter(
    new NullStatsReceiver()
  );
  private long queryNode = 12L;

  @Test
  public void testAllSocialProofFilter() throws Exception {
    SalsaRequest salsaRequest = new SalsaRequestBuilder(queryNode)
      .withValidSocialProofTypes(new byte[]{(byte) 0, (byte) 1, (byte) 2, (byte) 3})
      .withMaxSocialProofTypeSize(4)
      .build();

    socialProofTypeFilter.resetFilter(salsaRequest);

    SmallArrayBasedLongToDoubleMap clickSocialProof = new SmallArrayBasedLongToDoubleMap();
    clickSocialProof.put(123L, 1.0);
    SmallArrayBasedLongToDoubleMap favoriteSocialProof = new SmallArrayBasedLongToDoubleMap();
    favoriteSocialProof.put(234L, 1.0);
    SmallArrayBasedLongToDoubleMap retweetSocialProof = new SmallArrayBasedLongToDoubleMap();
    retweetSocialProof.put(345L, 1.0);

    SmallArrayBasedLongToDoubleMap[] socialProofs =
      new SmallArrayBasedLongToDoubleMap[]{
        clickSocialProof, favoriteSocialProof, retweetSocialProof
      };

    assertEquals(false, socialProofTypeFilter.filterResult(123456L, socialProofs));
  }

  @Test
  public void testFavoriteSocialProofFilter() throws Exception {
    SalsaRequest salsaRequest = new SalsaRequestBuilder(queryNode)
      .withValidSocialProofTypes(new byte[]{(byte) 1})
      .withMaxSocialProofTypeSize(4)
      .build();

    socialProofTypeFilter.resetFilter(salsaRequest);

    SmallArrayBasedLongToDoubleMap clickSocialProof = new SmallArrayBasedLongToDoubleMap();
    clickSocialProof.put(123L, 1.0);
    SmallArrayBasedLongToDoubleMap favoriteSocialProof = new SmallArrayBasedLongToDoubleMap();
    favoriteSocialProof.put(234L, 1.0);
    SmallArrayBasedLongToDoubleMap retweetSocialProof = new SmallArrayBasedLongToDoubleMap();
    retweetSocialProof.put(345L, 1.0);

    SmallArrayBasedLongToDoubleMap[] socialProofs =
      new SmallArrayBasedLongToDoubleMap[]{
        clickSocialProof, favoriteSocialProof, retweetSocialProof
      };

    SmallArrayBasedLongToDoubleMap[] missingSocialProofs =
      new SmallArrayBasedLongToDoubleMap[]{clickSocialProof, null, retweetSocialProof};

    assertEquals(false, socialProofTypeFilter.filterResult(123456L, socialProofs));
    assertEquals(true, socialProofTypeFilter.filterResult(123456L, missingSocialProofs));
  }
}
