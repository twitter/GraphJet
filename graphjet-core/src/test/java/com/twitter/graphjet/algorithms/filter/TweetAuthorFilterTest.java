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
import com.twitter.graphjet.algorithms.TweetAuthorFilter;
import com.twitter.graphjet.bipartite.LeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.segment.MockEdgeTypeMask;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class TweetAuthorFilterTest {
  @Test
  public void testEmptyAuthor() {
    LeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph = null;
    LongSet tweetAuthors = new LongArraySet();
    TweetAuthorFilter authorFilter = new TweetAuthorFilter(leftIndexedBipartiteGraph, tweetAuthors, new NullStatsReceiver());
    assertEquals(false, authorFilter.filterResult(0L, new SmallArrayBasedLongToDoubleMap[] {}));
  }

  @Test
  public void testOneAuthor() {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph =
      new NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph(
        3,
        10,
        10,
        10,
        2.0,
        100,
        2,
        new MockEdgeTypeMask((byte)RecommendationRequest.AUTHOR_SOCIAL_PROOF_TYPE),
        new NullStatsReceiver());
    int[][] dummyNodeMetadata = new int[][]{};

    leftIndexedBipartiteGraph.addEdge(1, 10, (byte)RecommendationRequest.AUTHOR_SOCIAL_PROOF_TYPE,
      0L, dummyNodeMetadata, dummyNodeMetadata);
    leftIndexedBipartiteGraph.addEdge(2, 20, (byte)RecommendationRequest.AUTHOR_SOCIAL_PROOF_TYPE,
      0L, dummyNodeMetadata, dummyNodeMetadata);

    SmallArrayBasedLongToDoubleMap[] socialProofs = {};
    LongSet tweetAuthors = new LongArraySet();

    // Only look for tweets from author 1
    tweetAuthors.add(1L);
    TweetAuthorFilter authorFilter = new TweetAuthorFilter(leftIndexedBipartiteGraph, tweetAuthors, new NullStatsReceiver());

    // Tweets authored by tweetAuthors should not be filtered. Node 10 is authored by 1.
    assertEquals(false, authorFilter.filterResult(10, socialProofs));
    // Tweets not authored by tweetAuthors should be filtered. Node 20 is not authored by 1
    assertEquals(true, authorFilter.filterResult(20, socialProofs));
  }
}
