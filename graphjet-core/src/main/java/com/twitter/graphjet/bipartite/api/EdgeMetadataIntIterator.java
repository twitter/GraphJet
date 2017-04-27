package com.twitter.graphjet.bipartite.api;

import it.unimi.dsi.fastutil.ints.IntIterator;

public interface EdgeMetadataIntIterator extends IntIterator {
  public abstract long currentMetadata();
}
