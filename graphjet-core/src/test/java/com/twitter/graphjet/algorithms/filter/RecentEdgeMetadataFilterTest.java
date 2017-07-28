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
