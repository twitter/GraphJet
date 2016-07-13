package com.twitter.graphjet.bipartite.api;

import it.unimi.dsi.fastutil.longs.LongIterator;

/** A type-specific Iterator; provides an additional method to avoid (un)boxing, and
 * the possibility to skip elements.
 */
public interface EdgeIterator extends LongIterator {
  /**
   * Returns the current edge type.
   *
   * @return the current edge type.
   */
  byte currentEdgeType();

  /**
   * Returns the next element as a primitive type.
   *
   * @return the next element in the iteration.
   */
  long nextLong();

  /** Skips the given number of elements.
   *
   * <P>The effect of this call is exactly the same as that of
   * calling {@link #next()} for <code>n</code> times (possibly stopping
   * if {@link #hasNext()} becomes false).
   *
   * @param n the number of elements to skip.
   * @return the number of elements actually skipped.
   */
  int skip(int n);
}
