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


package com.twitter.graphjet.algorithms.filter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.algorithms.filters.SocialProofTypesFilter;
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
    clickSocialProof.put(123L, 1.0, 0L);
    SmallArrayBasedLongToDoubleMap favoriteSocialProof = new SmallArrayBasedLongToDoubleMap();
    favoriteSocialProof.put(234L, 1.0, 0L);
    SmallArrayBasedLongToDoubleMap retweetSocialProof = new SmallArrayBasedLongToDoubleMap();
    retweetSocialProof.put(345L, 1.0, 0L);

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
    clickSocialProof.put(123L, 1.0, 0L);
    SmallArrayBasedLongToDoubleMap favoriteSocialProof = new SmallArrayBasedLongToDoubleMap();
    favoriteSocialProof.put(234L, 1.0, 0L);
    SmallArrayBasedLongToDoubleMap retweetSocialProof = new SmallArrayBasedLongToDoubleMap();
    retweetSocialProof.put(345L, 1.0, 0L);

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
