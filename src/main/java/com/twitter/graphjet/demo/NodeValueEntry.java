package com.twitter.graphjet.demo;

/**
 * A container object for holding a node (node id) and an associated value, for insertion into a priority queue, heap,
 * or some similar data structure.
 */
public class NodeValueEntry implements Comparable<NodeValueEntry> {
  private final long node;
  private final int value;

  public NodeValueEntry(long node, int value) {
    this.node = node;
    this.value = value;
  }

  public long getNode() {
    return node;
  }

  public long getValue() {
    return value;
  }

  @Override
  public int compareTo(NodeValueEntry other) {
    if (this.value < other.value) {
      return -1;
    }

    if (this.value > other.value) {
      return 1;
    }

    return this.node < other.node ? -1 : 1;
  }

  @Override
  public String toString() {
    return "(" + this.node + ", " + this.value + ")";
  }
}