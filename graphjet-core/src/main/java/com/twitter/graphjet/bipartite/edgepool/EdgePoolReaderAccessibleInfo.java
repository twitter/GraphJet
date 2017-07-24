package com.twitter.graphjet.bipartite.edgepool;

import com.twitter.graphjet.hashing.BigIntArray;
import com.twitter.graphjet.hashing.BigLongArray;
import com.twitter.graphjet.hashing.IntToIntPairHashMap;

public interface EdgePoolReaderAccessibleInfo {

  public BigIntArray getEdges();

  public BigLongArray getMetadata();

  public IntToIntPairHashMap getNodeInfo();
}
