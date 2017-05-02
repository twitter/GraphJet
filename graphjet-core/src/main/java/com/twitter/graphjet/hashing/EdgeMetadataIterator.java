package com.twitter.graphjet.hashing;

import com.twitter.graphjet.bipartite.api.ReadOnlyIntIterator;
import com.twitter.graphjet.bipartite.api.ReusableEdgeIntIterator;

public class EdgeMetadataIterator extends ReadOnlyIntIterator
                                  implements ReusableEdgeIntIterator {
  private final ShardedBigIntArray bigIntArray;
  private int startIndex;
  private int degree;
  private int currentEdge;

  /**
   * Creates an iterator that can be reused. Note that the client needs to call the resetForNode
   * method before using the iterator.
   *
   * @param bigIntArray is the underlying {@link ShardedBigIntArray}
   */
  public EdgeMetadataIterator(ShardedBigIntArray bigIntArray, int degree) {
    this.bigIntArray = bigIntArray;
    this.degree = degree;
  }

  /**
   * Returns the size of the underlying array
   *
   * @return the size of the underlying array
   */
  public int size() {
    return degree;
  }

  /**
   * Resets the iterator to return ints edge metadata of the input edge. Note that calling this
   * method resets the position of the iterator to the first edge metadata of the edge.
   *
   * @param position is the position that this iterator resets to
   * @return the iterator itself for ease of use
   */
  @Override
  public EdgeMetadataIterator resetForEdge(int position) {
    startIndex = position * (degree + 1) + 1;
    currentEdge = 0;
    return this;
  }

  @Override
  public int nextInt() {
    int shardId = bigIntArray.getShardId(startIndex);
    int offset = bigIntArray.getShardOffset(startIndex);
    startIndex ++;
    currentEdge ++;
   // System.out.println("shard Id " + shardId + " offset " + offset);
    return bigIntArray.getMetadata(shardId, offset);
  }

  @Override
  public int skip(int i) {
    return 0;
  }

  @Override
  public boolean hasNext() {
    return currentEdge < degree;
  }

  @Override
  public Integer next() {
    return nextInt();
  }

}
