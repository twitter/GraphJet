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

package com.twitter.graphjet.demo;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Heap for keeping track of top <i>k</i> nodes based on scores.
 */
public class TopNodes {
  private final int k;
  private PriorityQueue<NodeValueEntry> queue;

  /**
   * Creates a heap for keeping track of top <i>k</i> nodes based on scores.
   *
   * @param k number of nodes to keep track of
   */
  public TopNodes(int k) {
    this.k = k;
    this.queue = new PriorityQueue<>(this.k);
  }

  /**
   * Records this node with a particular score if it is in the top <i>k</i>. Otherwise, do nothing.
   * Note that we defer creation of an object until we know the node is in the top <i>k</i> to
   * avoid unnecessary object creation.
   *
   * @param nodeId  node id
   * @param score   score
   */
  public void offer(long nodeId, double score) {
    if (queue.size() < k) {
      queue.add(new NodeValueEntry(nodeId, score));
    } else {
      NodeValueEntry peek = queue.peek();
      if (score > peek.getValue()) {
        queue.poll();
        queue.add(new NodeValueEntry(nodeId, score));
      }
    }
  }

  /**
   * Returns the top <i>k</i> nodes encountered by this heap.
   *
   * @return the top <i>k</i> nodes encountered by this heap.
   */
  public List<NodeValueEntry> getNodes() {
    NodeValueEntry e;
    final List<NodeValueEntry> entries = new ArrayList<>(queue.size());
    while ((e = queue.poll()) != null) {
      entries.add(e);
    }

    return Lists.reverse(entries);
  }
}
