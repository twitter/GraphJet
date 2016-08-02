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

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * Abstracts out the notion of a reusable {@link IntIterator} in the context of providing iterative
 * access over edges of nodes that are ints.
 */
public interface ReusableNodeIntIterator {

  /**
   * Resets the iterator state for the given node.
   *
   * @param node  the node that this iterator now points to
   * @return the iterator
   */
  IntIterator resetForNode(int node);
}
