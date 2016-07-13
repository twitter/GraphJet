package com.twitter.graphjet.algorithms;

import com.twitter.graphjet.bipartite.api.EdgeIterator;

import it.unimi.dsi.fastutil.longs.LongIterator;

public class MockEdgeIterator implements EdgeIterator {
  private LongIterator edgeIterator;

  MockEdgeIterator(LongIterator iterator) {
    edgeIterator = iterator;
  }

  @Override
  public byte currentEdgeType() {
    return 0;
  }

  @Override
  public long nextLong() {
    return edgeIterator.nextLong();
  }

  @Override
  public int skip(int n) {
    return edgeIterator.skip(n);
  }

  @Override
  public boolean hasNext() {
    return edgeIterator.hasNext();
  }

  @Override
  public void remove() {
  }

  @Override
  public Long next() {
    return edgeIterator.next();
  }
}
