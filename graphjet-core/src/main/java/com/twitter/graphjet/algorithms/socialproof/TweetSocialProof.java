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


package com.twitter.graphjet.algorithms.socialproof;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.twitter.graphjet.algorithms.RecommendationAlgorithm;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.TweetIDMask;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.NodeMetadataMultiSegmentIterator;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.ByteArraySet;
import it.unimi.dsi.fastutil.bytes.ByteSet;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * TweetSocialProof shares similar logic with
 * {@link com.twitter.graphjet.algorithms.counting.tweet.TopSecondDegreeByCountForTweet}. In the
 * request, clients specify a seed user set and a tweet set. TweetSocialProof finds the intersection
 * between the seed user's tweet engagement set and the given tweet set from clients. All tweets
 * with at least one social proof will be returned to clients, and tweets without social proofs will
 * not appear in the list of the results.
 */
public class TweetSocialProof implements
  RecommendationAlgorithm<SocialProofRequest, SocialProofResponse> {

  private static final int MAX_EDGES_PER_NODE = 500;
  private static final Byte2ObjectMap<LongSet> EMPTY_SOCIALPROOF_MAP = new Byte2ObjectArrayMap<>();

  private NodeMetadataLeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph;
  private final Long2ObjectMap<Byte2ObjectMap<LongSet>> tweetsInteractions;
  private final Long2DoubleMap tweetsSocialProofWeights;

  public TweetSocialProof(
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph leftIndexedBiparGraph
  ) {
    this.leftIndexedBipartiteGraph = leftIndexedBiparGraph;
    // Variables tweetsInteractions and tweetsSocialProofWeights are re-used for each request.
    this.tweetsInteractions = new Long2ObjectOpenHashMap<>();
    this.tweetsSocialProofWeights = new Long2DoubleOpenHashMap();
  }

  /**
   * Collect tweet social proofs for a given {@link SocialProofRequest}.
   *
   * @param request contains a set of input tweets and a set of seed users.
   */
  private void collectRecommendations(SocialProofRequest request) {
    tweetsInteractions.clear();
    tweetsSocialProofWeights.clear();
    LongSet inputTweets = request.getInputTweets();
    ByteSet socialProofTypes = new ByteArraySet(request.getSocialProofTypes());

    // Iterate through the set of seed users with weights.
    // For each seed user, we go through his engaged tweets.
    for (Long2DoubleMap.Entry entry
      : request.getLeftSeedNodesWithWeight().long2DoubleEntrySet()) {
      long leftNode = entry.getLongKey();
      double weight = entry.getDoubleValue();
      NodeMetadataMultiSegmentIterator edgeIterator =
        (NodeMetadataMultiSegmentIterator)
          leftIndexedBipartiteGraph.getLeftNodeEdges(leftNode);

      int numEdgePerNode = 0;
      if (edgeIterator != null) {
        // Iterate through all the tweets that are engaged by the current user.
        while (edgeIterator.hasNext() && numEdgePerNode++ < MAX_EDGES_PER_NODE) {
          long rightNode = TweetIDMask.restore(edgeIterator.nextLong());
          byte edgeType = edgeIterator.currentEdgeType();

          // If the set of inputTweets contains the current tweet,
          // we find and store its social proof.
          if (inputTweets.contains(rightNode) && socialProofTypes.contains(edgeType)) {
            // Get the social proof variable of the current tweet.
            if (!tweetsInteractions.containsKey(rightNode)) {
              tweetsInteractions.put(rightNode, new Byte2ObjectArrayMap<>());
              tweetsSocialProofWeights.put(rightNode, 0);
            }
            Byte2ObjectMap<LongSet> interactionMap =
              tweetsInteractions.get(rightNode);

            // Update the weight of the current tweet.
            // We sum the weights of connecting users as the weight of an engaged tweet.
            tweetsSocialProofWeights.put(
              rightNode,
              weight + tweetsSocialProofWeights.get(rightNode)
            );

            // Get the user set variable by the engagement type.
            if (!interactionMap.containsKey(edgeType)) {
              interactionMap.put(edgeType, new LongArraySet());
            }
            LongSet connectingUsers = interactionMap.get(edgeType);

            // Add the connecting user to the user set.
            if (!connectingUsers.contains(leftNode)) {
              connectingUsers.add(leftNode);
            }
          }
        }
      }
    }
  }

  @Override
  public SocialProofResponse computeRecommendations(SocialProofRequest request, Random rand) {
    collectRecommendations(request);

    List<RecommendationInfo> socialProofList = new LinkedList<>();
    for (Long tweetId : request.getInputTweets()) {
      // Return only tweet ids with at least one social proof
      if (tweetsInteractions.containsKey(tweetId)) {
        socialProofList.add(new SocialProofResult(
          tweetId,
          tweetsInteractions.getOrDefault(tweetId, EMPTY_SOCIALPROOF_MAP),
          tweetsSocialProofWeights.getOrDefault(tweetId, 0.0)));
      }
    }

    return new SocialProofResponse(socialProofList);
  }
}
