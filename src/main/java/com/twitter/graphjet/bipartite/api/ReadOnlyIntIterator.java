package com.twitter.graphjet.bipartite.api;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * Abstracts out the notion of a read-only {@link IntIterator}.
 */
public abstract class ReadOnlyIntIterator implements IntIterator {

  @Override
  public void remove() {
    throw new UnsupportedOperationException("This is a read-only iterator!");
  }
}
