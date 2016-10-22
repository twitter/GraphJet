package com.twitter.graphjet.algorithms.counting;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.algorithms.RecommendationInfo;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.NodeMetadataMultiSegmentIterator;
import com.twitter.graphjet.stats.StatsReceiver;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import java.util.List;

public class TopSecondDegreeByCountForUser extends
    TopSecondDegreeByCount<TopSecondDegreeUserByCountRequest, TopSecondDegreeByCountResponse> {

  public TopSecondDegreeByCountForUser(
      NodeMetadataLeftIndexedMultiSegmentBipartiteGraph leftIndexedBipartiteGraph,
      int expectedNodesToHit,
      StatsReceiver statsReceiver) {
    super(leftIndexedBipartiteGraph, expectedNodesToHit, statsReceiver);
  }

  @Override
  protected void updateRightNodeInfo(
      long leftNode,
      long rightNode,
      byte edgeType,
      double weight,
      NodeMetadataMultiSegmentIterator edgeIterator,
      int maxSocialProofTypeSize,
      Long2ObjectMap<NodeInfo> collectedRightNodeInfo) {
    NodeInfo nodeInfo;

    if (!collectedRightNodeInfo.containsKey(rightNode)) {
      nodeInfo = new NodeInfo(rightNode, 0.0, maxSocialProofTypeSize);
      collectedRightNodeInfo.put(rightNode, nodeInfo);
    } else {
      nodeInfo = collectedRightNodeInfo.get(rightNode);
    }

    nodeInfo.addToWeight(weight);
    nodeInfo.addToSocialProof(leftNode, edgeType, weight);
  }

  @Override
  public TopSecondDegreeByCountResponse generateRecommendationFromNodeInfo(
      TopSecondDegreeUserByCountRequest request,
      List<NodeInfo> filteredNodeInfos) {
    List<RecommendationInfo> userRecommendations =
        TopSecondDegreeByCountUserRecsGenerator.generateUserRecs(
            request,
            filteredNodeInfos);

    LOG.info(getLogMessage(request)
        + ", numUserResults = " + userRecommendations.size()
        + ", totalResults = " + userRecommendations.size());
    return new TopSecondDegreeByCountResponse(userRecommendations, topSecondDegreeByCountStats);
  }
}
