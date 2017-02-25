package com.twitter.graphjet.algorithms;


import com.twitter.graphjet.bipartite.api.EdgeTypeMask;

/**
 * Mocks a certain edge type.
 */
public class MockEdgeTypeMask implements EdgeTypeMask {
  private final byte mockEdgeType;

  public MockEdgeTypeMask(byte mockEdgeType) {
    this.mockEdgeType = mockEdgeType;
  }

  @Override
  public int encode(int node, byte edgeType) {
    return node;
  }

  @Override
  public byte edgeType(int node) {
    return mockEdgeType;
  }

  @Override
  public int restore(int node) {
    return node;
  }
}
