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


package com.twitter.graphjet.hashing;

import com.twitter.graphjet.bipartite.api.ReadOnlyIntIterator;
import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;

public class IntArrayIterator extends ReadOnlyIntIterator
                              implements ReusableNodeIntIterator {
  private final ArrayBasedIntToIntArrayMap arrayBasedIntToIntArrayMap;
  private int position;
  private int degree;
  private int currentEdge;

  /**
   * Creates an iterator that can be reused. Note that the client needs to call the resetForNode
   * method before using the iterator.
   *
   * @param arrayBasedIntToIntArrayMap is the underlying {@link ArrayBasedIntToIntArrayMap}
   */
  public IntArrayIterator(ArrayBasedIntToIntArrayMap arrayBasedIntToIntArrayMap) {
    this.arrayBasedIntToIntArrayMap = arrayBasedIntToIntArrayMap;
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
   * Resets the iterator to return edges of a node with this information. Note that calling this
   * method resets the position of the iterator to the first edge of the node.
   *
   * @param node is the node that this iterator resets to
   * @return the iterator itself for ease of use
   */
  @Override
  public IntArrayIterator resetForNode(int node) {
    long nodeInfo = arrayBasedIntToIntArrayMap.getNodeInfo(node);
    if (nodeInfo == -1L) {
      this.degree = 0;
    } else {
      this.position = IntToIntPairArrayIndexBasedMap.getFirstValueFromNodeInfo(nodeInfo);
      this.degree = IntToIntPairArrayIndexBasedMap.getSecondValueFromNodeInfo(nodeInfo);
    }

    currentEdge = 0;
    return this;
  }

  @Override
  public int nextInt() {
    return arrayBasedIntToIntArrayMap.getNumberedEdge(position, currentEdge++);
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
