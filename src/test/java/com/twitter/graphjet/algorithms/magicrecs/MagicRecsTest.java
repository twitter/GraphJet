package com.twitter.graphjet.algorithms.magicrecs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Lists;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.algorithms.BipartiteGraphTestHelper;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationStats;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.algorithms.RequestedSetFilter;
import com.twitter.graphjet.algorithms.ResultFilter;
import com.twitter.graphjet.algorithms.ResultFilterChain;
import com.twitter.graphjet.algorithms.TweetRecommendationInfo;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.longs.Long2DoubleArrayMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public class MagicRecsTest {

  @Test
  public void testMagicRecsWithSmallGraph() throws Exception {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph bipartiteGraph =
      BipartiteGraphTestHelper.buildSmallTestNodeMetadataLeftIndexedMultiSegmentBipartiteGraph();
    long queryNode = 1;
    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(new long[]{2, 3}, new double[]{1.0, 0.5});
    LongSet toBeFiltered = new LongOpenHashSet(new long[]{2, 3, 4, 5});
    Set<RecommendationType> recommendationTypes = new HashSet<RecommendationType>();
    recommendationTypes.add(RecommendationType.HASHTAG);
    recommendationTypes.add(RecommendationType.URL);
    recommendationTypes.add(RecommendationType.TWEET);
    Map<RecommendationType, Integer> maxNumResults = new HashMap<RecommendationType, Integer>();
    maxNumResults.put(RecommendationType.HASHTAG, 3);
    maxNumResults.put(RecommendationType.URL, 3);
    maxNumResults.put(RecommendationType.TWEET, 3);
    Map<RecommendationType, Integer> minUserSocialProofSizes =
      new HashMap<RecommendationType, Integer>();
    minUserSocialProofSizes.put(RecommendationType.HASHTAG, 1);
    minUserSocialProofSizes.put(RecommendationType.URL, 1);
    minUserSocialProofSizes.put(RecommendationType.TWEET, 1);

    int maxUserSocialProofSize = 2;
    int maxTweetSocialProofSize = 10;
    int maxSocialProofTypeSize = 5;
    byte[] validSocialProofs = new byte[]{0, 1, 2, 3, 4};
    int expectedNodesToHit = 100;
    long randomSeed = 918324701982347L;
    Random random = new Random(randomSeed);
    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.<ResultFilter>newArrayList(
      new RequestedSetFilter(new NullStatsReceiver())
    ));

    MagicRecsRequest magicRecsRequest = new MagicRecsRequest(
      queryNode,
      seedsMap,
      toBeFiltered,
      recommendationTypes,
      maxNumResults,
      maxSocialProofTypeSize,
      maxUserSocialProofSize,
      maxTweetSocialProofSize,
      minUserSocialProofSizes,
      validSocialProofs,
      resultFilterChain
    );

    MagicRecsResponse magicRecsResponse = new MagicRecs(
      bipartiteGraph,
      expectedNodesToHit,
      new NullStatsReceiver()
    ).computeRecommendations(magicRecsRequest, random);

    ArrayList<HashMap<Byte, LongList>> socialProof = new ArrayList<HashMap<Byte, LongList>>();
    for (int i = 0; i < 3; i++) {
      socialProof.add(new HashMap<Byte, LongList>());
    }
    socialProof.get(0).put((byte) 0, new LongArrayList(new long[]{2, 3}));
    socialProof.get(1).put((byte) 0, new LongArrayList(new long[]{2}));
    socialProof.get(2).put((byte) 0, new LongArrayList(new long[]{3}));

    final List<RecommendationInfo> expectedTopResults = new ArrayList<RecommendationInfo>();
    expectedTopResults.add(new TweetRecommendationInfo(10, 1.5, socialProof.get(0)));
    expectedTopResults.add(new TweetRecommendationInfo(6, 1.0, socialProof.get(1)));
    expectedTopResults.add(new TweetRecommendationInfo(8, 0.5, socialProof.get(2)));

    List<RecommendationInfo> magicRecsResults =
      Lists.newArrayList(magicRecsResponse.getRankedRecommendations());

    final RecommendationStats expectedMagicRecsStats = new RecommendationStats(4, 9, 20, 2, 3, 2);
    RecommendationStats magicRecsStats = magicRecsResponse.getMagicRecsStats();


    assertEquals(expectedMagicRecsStats, magicRecsStats);
    assertEquals(expectedTopResults, magicRecsResults);
  }

  @Test
  public void testMagicRecsWithRandomGraph() throws Exception {
    long randomSeed = 918324701982347L;
    Random random = new Random(randomSeed);

    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph bipartiteGraph =
      BipartiteGraphTestHelper
        .buildRandomNodeMetadataLeftIndexedMultiSegmentBipartiteGraph(1000, 20000, 0.01, random);
    long queryNode = 0;

    LongList seedsList = new LongArrayList();
    DoubleList scoresList = new DoubleArrayList();
    for (int i = 1; i < 1000; i++) {
      if (random.nextInt(10) < 1) {
        seedsList.add(i);
        scoresList.add(((double) random.nextInt(10)) / 10.0);
      }
    }

    Long2DoubleMap seedsMap = new Long2DoubleArrayMap(
      seedsList.toLongArray(),
      scoresList.toDoubleArray()
    );
    LongSet toBeFiltered = new LongOpenHashSet(new long[]{2, 3, 4, 5});
    Set<RecommendationType> recommendationTypes = new HashSet<RecommendationType>();
    recommendationTypes.add(RecommendationType.HASHTAG);
    recommendationTypes.add(RecommendationType.URL);
    recommendationTypes.add(RecommendationType.TWEET);
    Map<RecommendationType, Integer> maxNumResults = new HashMap<RecommendationType, Integer>();
    maxNumResults.put(RecommendationType.HASHTAG, 3);
    maxNumResults.put(RecommendationType.URL, 3);
    maxNumResults.put(RecommendationType.TWEET, 3);
    Map<RecommendationType, Integer> minUserSocialProofSizes =
      new HashMap<RecommendationType, Integer>();
    minUserSocialProofSizes.put(RecommendationType.HASHTAG, 1);
    minUserSocialProofSizes.put(RecommendationType.URL, 1);
    minUserSocialProofSizes.put(RecommendationType.TWEET, 1);

    int maxUserSocialProofSize = 2;
    int maxTweetSocialProofSize = 10;
    int maxSocialProofTypeSize = 5;
    byte[] validSocialProofs = new byte[]{0, 1, 2, 3, 4};
    int expectedNodesToHit = 100;

    ResultFilterChain resultFilterChain = new ResultFilterChain(Lists.<ResultFilter>newArrayList(
      new RequestedSetFilter(new NullStatsReceiver())
    ));

    MagicRecsRequest magicRecsRequest = new MagicRecsRequest(
      queryNode,
      seedsMap,
      toBeFiltered,
      recommendationTypes,
      maxNumResults,
      maxSocialProofTypeSize,
      maxUserSocialProofSize,
      maxTweetSocialProofSize,
      minUserSocialProofSizes,
      validSocialProofs,
      resultFilterChain
    );

    MagicRecsResponse magicRecsResponse = new MagicRecs(
      bipartiteGraph,
      expectedNodesToHit,
      new NullStatsReceiver()
    ).computeRecommendations(magicRecsRequest, random);

    ArrayList<HashMap<Byte, LongList>> socialProof = new ArrayList<HashMap<Byte, LongList>>();
    for (int i = 0; i < 3; i++) {
      socialProof.add(new HashMap<Byte, LongList>());
    }
    socialProof.get(0).put((byte) 0, new LongArrayList(new long[]{990, 978}));
    socialProof.get(1).put((byte) 0, new LongArrayList(new long[]{990, 978}));
    socialProof.get(2).put((byte) 0, new LongArrayList(new long[]{990}));

    final List<RecommendationInfo> expectedTopResults = new ArrayList<RecommendationInfo>();
    expectedTopResults.add(
      new TweetRecommendationInfo(16428, 1.0, socialProof.get(0))
    );
    expectedTopResults.add(
      new TweetRecommendationInfo(3891, 1.0, socialProof.get(1))
    );
    expectedTopResults.add(
      new TweetRecommendationInfo(19301, 0.6, socialProof.get(2))
    );

    List<RecommendationInfo> magicRecsResults =
      Lists.newArrayList(magicRecsResponse.getRankedRecommendations());

    final RecommendationStats expectedMagicRecsStats =
      new RecommendationStats(0, 398, 798, 2, 3, 0);
    RecommendationStats magicRecsStats = magicRecsResponse.getMagicRecsStats();

    assertEquals(expectedMagicRecsStats, magicRecsStats);
    assertEquals(expectedTopResults, magicRecsResults);
  }
}
