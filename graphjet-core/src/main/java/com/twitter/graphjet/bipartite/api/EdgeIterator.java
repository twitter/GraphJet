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

/**
 * An optimized iterator for traversing graph edges. Iterator returns primitive values to avoid (un)boxing and also
 * provides a method for skipping elements.
 */
public interface EdgeIterator extends LongIterator {
  /**
   * Returns the current edge type.
   *
   * @return the current edge type.
   */
  byte currentEdgeType();

  /**
   * Returns the current edge metadata.
   *
   * @return the current edge metadata.
   */
  long currentMetadata();

  /**
   * Returns the next element as a primitive type.
   *
   * @return the next element in the iteration.
   */
  long nextLong();

  /**
   * Skips the given number of elements. The effect of this call is exactly the same as that of calling {@link #next()}
   * <i>n</i> times (stopping if {@link #hasNext()} becomes false).
   *
   * @param n the number of elements to skip.
   * @return the number of elements actually skipped.
   */
  int skip(int n);
}
