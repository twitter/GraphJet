/**
 * Copyright 2017 Twitter. All rights reserved.
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

import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.filters.RecentEdgeMetadataFilter;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.NullStatsReceiver;

public class RecentEdgeMetadataFilterTest {
  private long benchmarkTimeInMillis = 1501276549000L; // July 28, 2017 9:15:49 PM GMT
  private long oneDayInMillis = 1000 * 60 * 60 * 24;

  @Test
  public void testOneEdgeOldEnough() {
    long twoDaysPriorBenchmark = benchmarkTimeInMillis - oneDayInMillis * 2;
    SmallArrayBasedLongToDoubleMap socialProof = new SmallArrayBasedLongToDoubleMap();
    socialProof.put(100L, 1.0, twoDaysPriorBenchmark);
    SmallArrayBasedLongToDoubleMap[] socialProofs = {null, null, null, null, socialProof};

    long elapsedTimeInMillis = System.currentTimeMillis() - benchmarkTimeInMillis;
    RecentEdgeMetadataFilter filter = new RecentEdgeMetadataFilter(
        elapsedTimeInMillis + oneDayInMillis, // one day before benchmark
        (byte)RecommendationRequest.AUTHOR_SOCIAL_PROOF_TYPE,
        new NullStatsReceiver());
    assertEquals(false, filter.filterResult(1L, socialProofs));
  }

  @Test
  public void testOneEdgeTooRecent() {

    long halfDaysPriorBenchmark = benchmarkTimeInMillis - oneDayInMillis / 2;
    SmallArrayBasedLongToDoubleMap socialProof = new SmallArrayBasedLongToDoubleMap();
    socialProof.put(100L, 1.0, halfDaysPriorBenchmark);
    SmallArrayBasedLongToDoubleMap[] socialProofs = {null, null, null, null, socialProof};

    long elapsedTimeInMillis = System.currentTimeMillis() - benchmarkTimeInMillis;
    RecentEdgeMetadataFilter filter = new RecentEdgeMetadataFilter(
        elapsedTimeInMillis + oneDayInMillis, // one day before benchmark
        (byte)RecommendationRequest.AUTHOR_SOCIAL_PROOF_TYPE,
        new NullStatsReceiver());
    assertEquals(true, filter.filterResult(1L, socialProofs));
  }

  @Test
  public void testTwoEdgeTooRecent() {

    long halfDaysPriorBenchmark = benchmarkTimeInMillis - oneDayInMillis / 2;
    long twoDaysPriorBenchmark = benchmarkTimeInMillis - oneDayInMillis * 2;

    SmallArrayBasedLongToDoubleMap socialProof = new SmallArrayBasedLongToDoubleMap();
    socialProof.put(100L, 1.0, halfDaysPriorBenchmark);
    socialProof.put(101L, 1.0, twoDaysPriorBenchmark);
    SmallArrayBasedLongToDoubleMap[] socialProofs = {null, null, null, null, socialProof};

    long elapsedTimeInMillis = System.currentTimeMillis() - benchmarkTimeInMillis;
    RecentEdgeMetadataFilter filter = new RecentEdgeMetadataFilter(
        elapsedTimeInMillis + oneDayInMillis, // one day before benchmark
        (byte)RecommendationRequest.AUTHOR_SOCIAL_PROOF_TYPE,
        new NullStatsReceiver());
    assertEquals(true, filter.filterResult(1L, socialProofs));
  }
}
