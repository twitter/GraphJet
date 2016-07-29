/**
 * Copyright 2016 Twitter. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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
