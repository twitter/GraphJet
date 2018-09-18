/**
 * Copyright 2018 Twitter. All rights reserved.
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
import com.twitter.graphjet.algorithms.filters.TweetAuthorFilter;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.segment.MockEdgeTypeMask;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class TweetAuthorFilterTest {

  /**
   * Constuct a tweet graph where the author->tweet relationship is as follows:
   * 1 -> 10L
   * 2 -> 20L
   * 3 -> 30L
   * 4 -> 40L
   */
  private NodeMetadataLeftIndexedMultiSegmentBipartiteGraph getMockGraph() {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph =
        new NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph(
            3,
            10,
            10,
            10,
            2.0,
            100,
            2,
            new MockEdgeTypeMask(RecommendationRequest.AUTHOR_SOCIAL_PROOF_TYPE),
            new NullStatsReceiver());
    int[][] dummyNodeMetadata = new int[][]{};

    leftIndexedBipartiteGraph.addEdge(1, 10, RecommendationRequest.AUTHOR_SOCIAL_PROOF_TYPE,
        0L, dummyNodeMetadata, dummyNodeMetadata);
    leftIndexedBipartiteGraph.addEdge(2, 20, RecommendationRequest.AUTHOR_SOCIAL_PROOF_TYPE,
        0L, dummyNodeMetadata, dummyNodeMetadata);
    leftIndexedBipartiteGraph.addEdge(3, 30, RecommendationRequest.AUTHOR_SOCIAL_PROOF_TYPE,
        0L, dummyNodeMetadata, dummyNodeMetadata);
    leftIndexedBipartiteGraph.addEdge(4, 40, RecommendationRequest.AUTHOR_SOCIAL_PROOF_TYPE,
        0L, dummyNodeMetadata, dummyNodeMetadata);
    return leftIndexedBipartiteGraph;
  }

  @Test
  public void testEmptyWhitelist() {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph mockGraph = getMockGraph();

    SmallArrayBasedLongToDoubleMap[] socialProofs = {};

    LongSet whitelistAuthors = new LongArraySet();

    LongSet blacklistAuthors = new LongArraySet();

    TweetAuthorFilter authorFilter = new TweetAuthorFilter(
        mockGraph, whitelistAuthors, blacklistAuthors, new NullStatsReceiver());

    // None of the tweets should be filtered.
    assertEquals(false, authorFilter.filterResult(10, socialProofs));
    assertEquals(false, authorFilter.filterResult(20, socialProofs));
    assertEquals(false, authorFilter.filterResult(30, socialProofs));
    assertEquals(false, authorFilter.filterResult(40, socialProofs));
  }

  @Test
  public void testWhitelistOnlyTwoAuthors() {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph mockGraph = getMockGraph();

    SmallArrayBasedLongToDoubleMap[] socialProofs = {};

    LongSet whitelistAuthors = new LongArraySet();
    whitelistAuthors.add(1L);
    whitelistAuthors.add(2L);

    LongSet blacklistAuthors = new LongArraySet();

    TweetAuthorFilter authorFilter = new TweetAuthorFilter(
        mockGraph, whitelistAuthors, blacklistAuthors, new NullStatsReceiver());

    // 10L and 20L are authored by user 1 and 2. Do not filter them
    assertEquals(false, authorFilter.filterResult(10, socialProofs));
    assertEquals(false, authorFilter.filterResult(20, socialProofs));
    // 30L and 40L are authored by user 3 and 4. Filter them
    assertEquals(true, authorFilter.filterResult(30, socialProofs));
    assertEquals(true, authorFilter.filterResult(40, socialProofs));
  }

  @Test
  public void testBlacklistOnlyTwoAuthors() {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph mockGraph = getMockGraph();

    SmallArrayBasedLongToDoubleMap[] socialProofs = {};

    LongSet whitelistAuthors = new LongArraySet();
    LongSet blacklistAuthors = new LongArraySet();
    blacklistAuthors.add(3L);
    blacklistAuthors.add(4L);

    TweetAuthorFilter authorFilter = new TweetAuthorFilter(
        mockGraph, whitelistAuthors, blacklistAuthors, new NullStatsReceiver());

    // 10L and 20L are authored by user 1 and 2. Do not filter them
    assertEquals(false, authorFilter.filterResult(10, socialProofs));
    assertEquals(false, authorFilter.filterResult(20, socialProofs));
    // 30L and 40L are authored by user 3 and 4. Filter them
    assertEquals(true, authorFilter.filterResult(30, socialProofs));
    assertEquals(true, authorFilter.filterResult(40, socialProofs));
  }

  @Test
  public void testBlacklistWhitelistNoOverlap() {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph mockGraph = getMockGraph();

    SmallArrayBasedLongToDoubleMap[] socialProofs = {};

    LongSet whitelistAuthors = new LongArraySet();
    whitelistAuthors.add(1L);
    whitelistAuthors.add(2L);

    LongSet blacklistAuthors = new LongArraySet();
    blacklistAuthors.add(3L);
    blacklistAuthors.add(4L);

    TweetAuthorFilter authorFilter = new TweetAuthorFilter(
        mockGraph, whitelistAuthors, blacklistAuthors, new NullStatsReceiver());

    // Should only return whitelisted 10 and 20. 30 and 40 are filtered by blacklist
    assertEquals(false, authorFilter.filterResult(10, socialProofs));
    assertEquals(false, authorFilter.filterResult(20, socialProofs));
    assertEquals(true, authorFilter.filterResult(30, socialProofs));
    assertEquals(true, authorFilter.filterResult(40, socialProofs));
  }

  @Test
  public void testBlacklistWhitelistFullOverlap() {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph mockGraph = getMockGraph();

    SmallArrayBasedLongToDoubleMap[] socialProofs = {};

    LongSet whitelistAuthors = new LongArraySet();
    whitelistAuthors.add(1L);
    whitelistAuthors.add(2L);
    whitelistAuthors.add(3L);

    LongSet blacklistAuthors = new LongArraySet();
    blacklistAuthors.add(1L);
    blacklistAuthors.add(2L);
    blacklistAuthors.add(3L);

    TweetAuthorFilter authorFilter = new TweetAuthorFilter(
        mockGraph, whitelistAuthors, blacklistAuthors, new NullStatsReceiver());

    // No tweet should be returned. 10, 20, 30 filtered by blacklist, 40 filtered by whitelist
    assertEquals(true, authorFilter.filterResult(10, socialProofs));
    assertEquals(true, authorFilter.filterResult(20, socialProofs));
    assertEquals(true, authorFilter.filterResult(30, socialProofs));
    assertEquals(true, authorFilter.filterResult(40, socialProofs));
  }

  @Test
  public void testBlacklistWhitelistPartialOverlap() {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph mockGraph = getMockGraph();

    SmallArrayBasedLongToDoubleMap[] socialProofs = {};

    LongSet whitelistAuthors = new LongArraySet();
    whitelistAuthors.add(1L);
    whitelistAuthors.add(2L);
    whitelistAuthors.add(3L);

    LongSet blacklistAuthors = new LongArraySet();
    blacklistAuthors.add(2L);
    blacklistAuthors.add(3L);
    blacklistAuthors.add(4L);

    TweetAuthorFilter authorFilter = new TweetAuthorFilter(
        mockGraph, whitelistAuthors, blacklistAuthors, new NullStatsReceiver());

    // Only return 10, since it is whitelisted and not blacklisted
    assertEquals(false, authorFilter.filterResult(10, socialProofs));
    assertEquals(true, authorFilter.filterResult(20, socialProofs));
    assertEquals(true, authorFilter.filterResult(30, socialProofs));
    assertEquals(true, authorFilter.filterResult(40, socialProofs));
  }
}
