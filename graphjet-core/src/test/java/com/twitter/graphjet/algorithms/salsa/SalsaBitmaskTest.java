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


package com.twitter.graphjet.algorithms.salsa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import com.google.common.collect.Lists;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.filters.RequestedSetFilter;
import com.twitter.graphjet.algorithms.filters.ResultFilter;
import com.twitter.graphjet.algorithms.filters.ResultFilterChain;
import com.twitter.graphjet.algorithms.StaticBipartiteGraph;
import com.twitter.graphjet.algorithms.filters.TweetCardFilter;
import com.twitter.graphjet.algorithms.TweetIDMask;
import com.twitter.graphjet.algorithms.counting.tweet.TweetRecommendationInfo;
import com.twitter.graphjet.algorithms.salsa.fullgraph.Salsa;
import com.twitter.graphjet.bipartite.api.BipartiteGraph;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class SalsaBitmaskTest {

  private static final TweetIDMask TWEET_ID_MASK = new TweetIDMask();
  private static long tweetNode = 11 | TweetIDMask.TWEET;
  private static long summaryNode = 12 | TweetIDMask.SUMMARY;
  private static long photoNode = 13 | TweetIDMask.PHOTO;
  private static long playerNode = 14 | TweetIDMask.PLAYER;
  private static long promotionNode = 15 | TweetIDMask.PROMOTION;
  private StaticBipartiteGraph buildTestGraph() {
    Long2ObjectMap<LongList> leftSideGraph = new Long2ObjectOpenHashMap<LongList>(3);
    leftSideGraph.put(1, new LongArrayList(new long[]{2, 3, 4, 5}));
    leftSideGraph.put(2, new LongArrayList(new long[]{tweetNode, summaryNode, photoNode, playerNode,
        2, 3}));
    leftSideGraph.put(3, new LongArrayList(new long[]{tweetNode, summaryNode, photoNode, playerNode,
        promotionNode, 4, 5}));

    Long2ObjectMap<LongList> rightSideGraph = new Long2ObjectOpenHashMap<LongList>(10);
    rightSideGraph.put(2, new LongArrayList(new long[]{1, 2}));
    rightSideGraph.put(3, new LongArrayList(new long[]{1, 2}));
    rightSideGraph.put(4, new LongArrayList(new long[]{1, 3}));
    rightSideGraph.put(5, new LongArrayList(new long[]{1, 3}));
    rightSideGraph.put(tweetNode, new LongArrayList(new long[]{2, 3}));
    rightSideGraph.put(summaryNode, new LongArrayList(new long[]{2, 3}));
    rightSideGraph.put(photoNode, new LongArrayList(new long[]{2, 3}));
    rightSideGraph.put(playerNode, new LongArrayList(new long[]{2, 3}));
    rightSideGraph.put(promotionNode, new LongArrayList(new long[]{3}));

    return new StaticBipartiteGraph(leftSideGraph, rightSideGraph);

  }

  private void testFilter(TweetCardFilter filter, Long[] expected) {
    Random random = new Random(918324701982347L);
    long queryNode = 1;
    BipartiteGraph bipartiteGraph = buildTestGraph();
    LongSet toBeFiltered = new LongOpenHashSet(new long[]{2, 3, 4, 5});
    int numIterations = 5;
    double resetProbability = 0.3;
    int numResults = 20;
    int numRandomWalks = 1000;
    int maxSocialProofSize = 2;
    int expectedNodesToHit = numRandomWalks * numIterations / 2;
    SalsaStats salsaStats = new SalsaStats(1, 4, 3, 6, 1, 3, 0);


    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.<ResultFilter>newArrayList(
        new RequestedSetFilter(new NullStatsReceiver()),
        filter
    ));

    SalsaRequest salsaRequest =
        new SalsaRequestBuilder(queryNode)
            .withLeftSeedNodes(null)
            .withToBeFiltered(toBeFiltered)
            .withMaxNumResults(numResults)
            .withResetProbability(resetProbability)
            .withMaxRandomWalkLength(numIterations)
            .withNumRandomWalks(numRandomWalks)
            .withMaxSocialProofSize(maxSocialProofSize)
            .withResultFilterChain(resultFilterChain)
            .build();

    SalsaResponse salsaResponse = new Salsa(
        bipartiteGraph,
        expectedNodesToHit,
        new NullStatsReceiver())
        .computeRecommendations(salsaRequest, random);

    Iterator<RecommendationInfo> recs = salsaResponse.getRankedRecommendations().iterator();
    ArrayList<Long> rets = new ArrayList<Long>();
    while (recs.hasNext()) {
      rets.add(((TweetRecommendationInfo) recs.next()).getRecommendation());
    }
    assertArrayEquals(expected, rets.toArray(new Long[]{}));
  }


  @Test
  public void testTweetsOnly() {
    Long[] tweets = new Long[]{TWEET_ID_MASK.restore(tweetNode)};

    testFilter(new TweetCardFilter(true, false, false, false, false, new NullStatsReceiver()),
        tweets);
  }

  @Test
  public void testSummaryOnly() {
    Long[] summary = new Long[]{TWEET_ID_MASK.restore(summaryNode)};

    testFilter(new TweetCardFilter(false, true, false, false, false, new NullStatsReceiver()),
        summary);
  }

  @Test
  public void testPhotoOnly() {
    Long[] photo = new Long[]{TWEET_ID_MASK.restore(photoNode)};

    testFilter(new TweetCardFilter(false, false, true, false, false, new NullStatsReceiver()),
        photo);
  }

  @Test
  public void testPlayerOnly() {
    Long[] player = new Long[]{TWEET_ID_MASK.restore(playerNode)};

    testFilter(new TweetCardFilter(false, false, false, true, false, new NullStatsReceiver()),
        player);
  }

  @Test
  public void testPromotionOnly() {
    Long[] promotion = new Long[]{TWEET_ID_MASK.restore(promotionNode)};

    testFilter(new TweetCardFilter(false, false, false, false, true, new NullStatsReceiver()),
        promotion);
  }
}
