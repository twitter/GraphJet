package com.twitter.graphjet.demo;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class TopNodes {
  private final int k;
  private PriorityQueue<NodeValueEntry> queue;

  public TopNodes(int k) {
    this.k = k;
    this.queue = new PriorityQueue<>(this.k);
  }

  /**
   * Don't take object for efficiency reasons.
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

  public List<NodeValueEntry> getNodes() {
    NodeValueEntry e;
    List<NodeValueEntry> entries = new ArrayList<>(queue.size());
    while ((e = queue.poll()) != null) {
      entries.add(e);
    }

    return Lists.reverse(entries);
  }
}
