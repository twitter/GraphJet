package com.twitter.graphjet.bipartite.api;

public interface RightNodeMetadataDynamicBipartiteGraph {
  /**
   * Adding an edge is assumed to be an O(1) operation.
   *
   * @param leftNode          is the left hand side node in the bipartite graph.
   * @param rightNode         is the right hand side node in the bipartite graph.
   * @param edgeType          is the edge type relationship between leftNode and rightNode.
   * @param rightNodeMetadata is the metadata associated with the right hand side node. A two
   *                          dimensional array stores the metadata, and each entry in the first
   *                          dimension array represents a unique metadata type id, starting from 0,
   *                          and each entry in the second dimension array represents a unique
   *                          metadata id. The library assumes the metadata type ids are consecutive
   *                          and small, for example, less than 10. Though a large number of
   *                          metadata types does not affect correctness, the memory usage in the
   *                          library could be further optimized. If a node does not have a
   *                          particular metadata type, client needs to initialize the corresponding
   *                          second dimension array to null.
   */
  void addEdge(
    long leftNode,
    long rightNode,
    byte edgeType,
    int[][] rightNodeMetadata
  );
}
