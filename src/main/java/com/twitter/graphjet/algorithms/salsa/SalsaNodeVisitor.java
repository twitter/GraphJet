package com.twitter.graphjet.algorithms.salsa;

import com.twitter.graphjet.algorithms.NodeInfo;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

/**
 * This class encapsulates node visiting logic, and the related updates
 * to the internal state.
 */
public class SalsaNodeVisitor {
  /**
   * This class encapsulates a visit to a node.
   */
  public abstract static class NodeVisitor {
    protected final Long2ObjectMap<NodeInfo> visitedRightNodes;
    protected SalsaRequest salsaRequest;

    public NodeVisitor(Long2ObjectMap<NodeInfo> visitedRightNodes) {
      this.visitedRightNodes = visitedRightNodes;
    }

    /**
     * Visits the given rightNode, updating the internal state about visits. In this implementation,
     * we store more information about the node being visited
     *
     * @param leftNode   is the left node that visits the rightNode
     * @param rightNode  is the right node being visited
     * @param edgeType   is the edge type between leftNode and rightNode, e.g. click, favorite,
     *                   retweet, reply
     * @param weight     is the weight of the edge
     * @return the number of visits to this node so far, including the current one
     */
    public abstract int visitRightNode(long leftNode, long rightNode, byte edgeType, double weight);

    protected int simpleRightNodeVisitor(long rightNode) {
      int numVisits = 1;
      if (visitedRightNodes.containsKey(rightNode)) {
        visitedRightNodes.get(rightNode).addToWeight(1.0);
        numVisits = (int) visitedRightNodes.get(rightNode).getWeight();
      } else {
        visitedRightNodes.put(rightNode,
            new NodeInfo(
              rightNode,
              1.0,
              salsaRequest.getMaxSocialProofTypeSize()
            )
        );
      }
      return numVisits;
    }

    public void resetWithRequest(SalsaRequest incomingSalsaRequest) {
      this.salsaRequest = incomingSalsaRequest;
    }
  }

  /**
   * A simple visitor that just updates the visit counters and doesn't incorporate the starting
   * point/weight info.
   */
  public static class SimpleNodeVisitor extends NodeVisitor {
    public SimpleNodeVisitor(Long2ObjectMap<NodeInfo> visitedRightNodes) {
      super(visitedRightNodes);
    }

    @Override
    public int visitRightNode(long leftNode, long rightNode, byte edgeType, double weight) {
      return simpleRightNodeVisitor(rightNode);
    }
  }

  /**
   * A weighted visitor that updated the visit counters according to weights
   */
  public static class WeightedNodeVisitor extends NodeVisitor {
    public WeightedNodeVisitor(Long2ObjectMap<NodeInfo> visitedRightNodes) {
      super(visitedRightNodes);
    }

    @Override
    public int visitRightNode(long leftNode, long rightNode, byte edgeType, double weight) {
      int numVisits = 1;
      if (visitedRightNodes.containsKey(rightNode)) {
        visitedRightNodes.get(rightNode).addToWeight(weight);
        numVisits = visitedRightNodes.get(rightNode).getNumVisits();
      } else {
        visitedRightNodes.put(rightNode,
            new NodeInfo(
              rightNode,
              weight,
              salsaRequest.getMaxSocialProofTypeSize()
            )
        );
      }
      return numVisits;
    }
  }

  /**
   * A visitor that both updates the visit counters and adds the starting point as social proof.
   */
  public static class NodeVisitorWithSocialProof extends NodeVisitor {

    public NodeVisitorWithSocialProof(Long2ObjectMap<NodeInfo> visitedRightNodes) {
      super(visitedRightNodes);
    }

    @Override
    public int visitRightNode(long leftNode, long rightNode, byte edgeType, double weight) {
      int numVisits = simpleRightNodeVisitor(rightNode);
      if (leftNode != salsaRequest.getQueryNode()) {
        visitedRightNodes.get(rightNode).addToSocialProof(leftNode, edgeType, weight);
      }
      return numVisits;
    }
  }


  /**
   * A visitor that both updates the visit counters and adds the starting point as social proof.
   */
  public static class WeightedNodeVisitorWithSocialProof extends WeightedNodeVisitor {

    public WeightedNodeVisitorWithSocialProof(Long2ObjectMap<NodeInfo> visitedRightNodes) {
      super(visitedRightNodes);
    }

    @Override
    public int visitRightNode(long leftNode, long rightNode, byte edgeType, double weight) {
      int numVisits = super.visitRightNode(leftNode, rightNode, edgeType, weight);
      if (leftNode != salsaRequest.getQueryNode()) {
        visitedRightNodes.get(rightNode).addToSocialProof(leftNode, edgeType, weight);
      }
      return numVisits;
    }
  }
}
