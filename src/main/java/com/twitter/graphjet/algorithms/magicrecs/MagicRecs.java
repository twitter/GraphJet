package com.twitter.graphjet.algorithms.magicrecs;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.RecommendationAlgorithm;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.algorithms.RecommendationStats;
import com.twitter.graphjet.algorithms.RecommendationType;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.NodeMetadataMultiSegmentIterator;
import com.twitter.graphjet.hashing.IntArrayIterator;
import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.longs.Long2ByteArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class MagicRecs implements RecommendationAlgorithm<MagicRecsRequest, MagicRecsResponse> {
  private static final Logger LOG = LoggerFactory.getLogger("graph");
  private static final int MAX_EDGES_PER_NODE = 500;

  private final NodeMetadataLeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph;
  private final Long2ObjectMap<NodeInfo> visitedRightNodes;
  private final List<NodeInfo> nodeInfosAfterFiltering;
  private final Long2ByteMap seenEdgesPerNode;
  private final RecommendationStats magicRecsStats;
  private final StatsReceiver statsReceiver;
  private final Counter numRequestsCounter;

  /**
   * This initializes all the state needed to run MagicRecs. Note that the object can be reused for
   * answering many different queries on the same graph, which allows for optimizations such as
   * reusing internally allocated maps etc.
   *
   * @param leftIndexedBipartiteGraph is the
   *                                  {@link NodeMetadataLeftIndexedMultiSegmentBipartiteGraph}
   *                                  to run MagicRecs on
   * @param expectedNodesToHit        is an estimate of how many nodes can be hit in MagicRecs. This
   *                                  is purely for allocating needed memory right up front to make
   *                                  requests fast.
   * @param statsReceiver             tracks the internal stats
   */
  public MagicRecs(
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
    int expectedNodesToHit,
    StatsReceiver statsReceiver
  ) {
    this.leftIndexedBipartiteGraph = leftIndexedBipartiteGraph;
    this.visitedRightNodes = new Long2ObjectOpenHashMap<NodeInfo>(expectedNodesToHit);
    this.nodeInfosAfterFiltering = new ArrayList<NodeInfo>();
    this.seenEdgesPerNode = new Long2ByteArrayMap();
    this.magicRecsStats = new RecommendationStats();
    this.statsReceiver = statsReceiver.scope("MagicRecs");
    this.numRequestsCounter = this.statsReceiver.counter("numRequests");
  }

  private void collectRecommendations(MagicRecsRequest request) {
    for (Long2DoubleMap.Entry entry: request.getLeftSeedNodesWithWeight().long2DoubleEntrySet()) {
      long leftNode = entry.getLongKey();
      double weight = entry.getDoubleValue();
      int numEdgesPerNode = 0;
      NodeMetadataMultiSegmentIterator edgeIterator =
        (NodeMetadataMultiSegmentIterator) leftIndexedBipartiteGraph.getLeftNodeEdges(leftNode);
      seenEdgesPerNode.clear();

      if (edgeIterator != null) {
        // Sequentially iterating through the latest MAX_EDGES_PER_NODE edges per node
        while (edgeIterator.hasNext() && numEdgesPerNode++ < MAX_EDGES_PER_NODE) {
          long rightNode = edgeIterator.nextLong();
          byte edgeType = edgeIterator.currentEdgeType();

          if (seenEdgesPerNode.containsKey(rightNode)
            && seenEdgesPerNode.get(rightNode) == edgeType) {
            // no op
          } else {
            seenEdgesPerNode.put(rightNode, edgeType);

            NodeInfo nodeInfo;
            if (!visitedRightNodes.containsKey(rightNode)) {
              int metadataSize = RecommendationType.METADATASIZE.getValue();

              int[][] nodeMetadata = new int[metadataSize][];

              for (int i = 0; i < metadataSize; i++) {
                IntArrayIterator metadataIterator =
                  (IntArrayIterator) edgeIterator.getRightNodeMetadata((byte) i);

                if (metadataIterator.size() > 0) {
                  int[] metadata = new int[metadataIterator.size()];
                  int j = 0;
                  while (metadataIterator.hasNext()) {
                    metadata[j++] = metadataIterator.nextInt();
                  }
                  nodeMetadata[i] = metadata;
                }
              }

              nodeInfo = new NodeInfo(
                rightNode,
                nodeMetadata,
                0.0,
                request.getMaxSocialProofTypeSize()
              );
              visitedRightNodes.put(rightNode, nodeInfo);
            } else {
              nodeInfo = visitedRightNodes.get(rightNode);
            }

            nodeInfo.addToWeight(weight);
            nodeInfo.addToSocialProof(leftNode, edgeType, weight);
          }
        }
      }
    }
  }

  private void collectRecommendationStats(long queryNode) {
    magicRecsStats.setNumDirectNeighbors(leftIndexedBipartiteGraph.getLeftNodeDegree(queryNode));

    int minVisitsPerRightNode = Integer.MAX_VALUE;
    int maxVisitsPerRightNode = 0;
    int numRHSVisits = 0;

    for (Long2ObjectMap.Entry<NodeInfo> entry: visitedRightNodes.long2ObjectEntrySet()) {
      NodeInfo nodeInfo = entry.getValue();
      int numVisits = nodeInfo.getNumVisits();

      minVisitsPerRightNode = Math.min(minVisitsPerRightNode, numVisits);
      maxVisitsPerRightNode = Math.max(maxVisitsPerRightNode, numVisits);
      numRHSVisits += numVisits;
    }

    magicRecsStats.setMinVisitsPerRightNode(minVisitsPerRightNode);
    magicRecsStats.setMaxVisitsPerRightNode(maxVisitsPerRightNode);
    magicRecsStats.setNumRHSVisits(numRHSVisits);
    magicRecsStats.setNumRightNodesReached(visitedRightNodes.size());
  }

  private void filterNodeInfo(MagicRecsRequest request) {
    int numFilteredNodes = 0;
    for (NodeInfo nodeInfo : visitedRightNodes.values()) {
      if (request.filterResult(nodeInfo.getValue(), nodeInfo.getSocialProofs())) {
        numFilteredNodes++;
        continue;
      }

      nodeInfosAfterFiltering.add(nodeInfo);
    }

    magicRecsStats.setNumRightNodesFiltered(numFilteredNodes);
  }

  private void reset(MagicRecsRequest request) {
    request.resetFilters();
    visitedRightNodes.clear();
    nodeInfosAfterFiltering.clear();
    seenEdgesPerNode.clear();
    magicRecsStats.reset();
  }

  @Override
  public MagicRecsResponse computeRecommendations(
    MagicRecsRequest request,
    Random random
  ) {
    numRequestsCounter.incr();
    reset(request);

    collectRecommendations(request);

    collectRecommendationStats(request.getQueryNode());

    filterNodeInfo(request);

    int numTweetResults = 0;
    int numHashtagResults = 0;
    int numUrlResults = 0;
    List<RecommendationInfo> recommendations = new ArrayList<RecommendationInfo>();

    if (request.getRecommendationTypes().contains(RecommendationType.TWEET)) {
      List<RecommendationInfo> tweetRecommendations =
        MagicRecsTweetRecsGenerator.generateTweetRecs(request, nodeInfosAfterFiltering);
      numTweetResults = tweetRecommendations.size();
      recommendations.addAll(tweetRecommendations);
    }

    if (request.getRecommendationTypes().contains(RecommendationType.HASHTAG)) {
      List<RecommendationInfo> hashtagRecommendations =
        MagicRecsTweetMetadataRecsGenerator.generateTweetMetadataRecs(
          request,
          nodeInfosAfterFiltering,
          RecommendationType.HASHTAG
        );
      numHashtagResults = hashtagRecommendations.size();
      recommendations.addAll(hashtagRecommendations);
    }

    if (request.getRecommendationTypes().contains(RecommendationType.URL)) {
      List<RecommendationInfo> urlRecommendations =
        MagicRecsTweetMetadataRecsGenerator.generateTweetMetadataRecs(
          request,
          nodeInfosAfterFiltering,
          RecommendationType.URL
        );
      numUrlResults = urlRecommendations.size();
      recommendations.addAll(urlRecommendations);
    }

    LOG.info("MagicRecs: after running algorithm for request_id = "
        + request.getQueryNode()
        + ", we get numDirectNeighbors = "
        + magicRecsStats.getNumDirectNeighbors()
        + ", numRHSVisits = "
        + magicRecsStats.getNumRHSVisits()
        + ", numRightNodesReached = "
        + magicRecsStats.getNumRightNodesReached()
        + ", numRightNodesFiltered = "
        + magicRecsStats.getNumRightNodesFiltered()
        + ", minVisitsPerRightNode = "
        + magicRecsStats.getMinVisitsPerRightNode()
        + ", maxVisitsPerRightNode = "
        + magicRecsStats.getMaxVisitsPerRightNode()
        + ", numTweetResults = "
        + numTweetResults
        + ", numHashtagResults = "
        + numHashtagResults
        + ", numUrlResults = "
        + numUrlResults
        + ", totalResults = "
        + (numTweetResults + numHashtagResults + numUrlResults)
    );

    return new MagicRecsResponse(recommendations, magicRecsStats);
  }
}
