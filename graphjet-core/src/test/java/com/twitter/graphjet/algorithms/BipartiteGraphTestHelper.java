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


package com.twitter.graphjet.algorithms;

import java.util.Random;

import com.twitter.graphjet.bipartite.LeftIndexedPowerLawMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph;
import com.twitter.graphjet.bipartite.segment.HigherBitsEdgeTypeMask;
import com.twitter.graphjet.bipartite.segment.IdentityEdgeTypeMask;
import com.twitter.graphjet.bipartite.segment.LeftRegularBipartiteGraphSegment;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

public final class BipartiteGraphTestHelper {
  // Utility class
  private BipartiteGraphTestHelper() { }

  /**
   * Build a random bipartite graph of given left and right sizes.
   *
   * @param leftSize   is the left hand size of the bipartite graph
   * @param rightSize  is the right hand size of the bipartite graph
   * @param random     is the random number generator to use for constructing the graph
   * @return a random bipartite graph
   */
  public static StaticBipartiteGraph buildRandomBipartiteGraph(
      int leftSize, int rightSize, double edgeProbability, Random random) {
    Long2ObjectMap<LongList> leftSideGraph = new Long2ObjectOpenHashMap<LongList>(leftSize);
    Long2ObjectMap<LongList> rightSideGraph = new Long2ObjectOpenHashMap<LongList>(rightSize);
    int averageLeftDegree = (int) (rightSize * edgeProbability);
    int averageRightDegree = (int) (leftSize * edgeProbability);
    for (int i = 0; i < leftSize; i++) {
      leftSideGraph.put(i, new LongArrayList(averageLeftDegree));
      for (int j = 0; j < rightSize; j++) {
        if (random.nextDouble() < edgeProbability) {
          leftSideGraph.get(i).add(j);
          if (rightSideGraph.containsKey(j)) {
            rightSideGraph.get(j).add(i);
          } else {
            LongList rightSideList = new LongArrayList(averageRightDegree);
            rightSideList.add(i);
            rightSideGraph.put(j, rightSideList);
          }
        }
      }
    }

    return new StaticBipartiteGraph(leftSideGraph, rightSideGraph);
  }

  /**
   * Build a small test bipartite graph.
   *
   * @return a small test bipartite graph
   */
  public static StaticBipartiteGraph buildSmallTestBipartiteGraph() {
    Long2ObjectMap<LongList> leftSideGraph = new Long2ObjectOpenHashMap<LongList>(3);
    leftSideGraph.put(1, new LongArrayList(new long[]{2, 3, 4, 5}));
    leftSideGraph.put(2, new LongArrayList(new long[]{5, 6, 10}));
    leftSideGraph.put(3, new LongArrayList(new long[]{7, 8, 5, 9, 2, 10, 11, 1}));

    Long2ObjectMap<LongList> rightSideGraph = new Long2ObjectOpenHashMap<LongList>(10);
    rightSideGraph.put(1, new LongArrayList(new long[]{3}));
    rightSideGraph.put(2, new LongArrayList(new long[]{1, 3}));
    rightSideGraph.put(3, new LongArrayList(new long[]{1}));
    rightSideGraph.put(4, new LongArrayList(new long[]{1}));
    rightSideGraph.put(5, new LongArrayList(new long[]{1, 2, 3}));
    rightSideGraph.put(6, new LongArrayList(new long[]{2}));
    rightSideGraph.put(7, new LongArrayList(new long[]{3}));
    rightSideGraph.put(8, new LongArrayList(new long[]{3}));
    rightSideGraph.put(9, new LongArrayList(new long[]{3}));
    rightSideGraph.put(10, new LongArrayList(new long[]{2, 3}));
    rightSideGraph.put(11, new LongArrayList(new long[]{3}));

    return new StaticBipartiteGraph(leftSideGraph, rightSideGraph);
  }

  /**
   * Build a small test bipartite graph.
   *
   * @return a small test bipartite graph
   */
  public static StaticLeftIndexedBipartiteGraph buildSmallTestLeftIndexedBipartiteGraph() {
    Long2ObjectMap<LongList> leftSideGraph = new Long2ObjectOpenHashMap<LongList>(3);
    leftSideGraph.put(1, new LongArrayList(new long[]{2, 3, 4, 5}));
    leftSideGraph.put(2, new LongArrayList(new long[]{5, 6, 10}));
    leftSideGraph.put(3, new LongArrayList(new long[]{7, 8, 5, 9, 2, 10, 11, 1}));

    return new StaticLeftIndexedBipartiteGraph(leftSideGraph);
  }

  /**
   * Build a small test bipartite graph segment.
   *
   * @return a small test {@link LeftRegularBipartiteGraphSegment}
   */
  public static LeftRegularBipartiteGraphSegment buildSmallTestBipartiteGraphSegment() {
    LeftRegularBipartiteGraphSegment leftRegularBipartiteGraphSegment =
        new LeftRegularBipartiteGraphSegment(3, 10, 10, 3, 2.0, Integer.MAX_VALUE,
            new IdentityEdgeTypeMask(),
            new NullStatsReceiver());
    leftRegularBipartiteGraphSegment.addEdge(1, 2, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(1, 3, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(1, 4, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(1, 5, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(2, 5, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(2, 6, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(2, 10, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(3, 7, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(3, 8, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(3, 5, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(3, 9, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(3, 2, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(3, 10, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(3, 11, (byte) 0);
    leftRegularBipartiteGraphSegment.addEdge(3, 1, (byte) 0);

    return leftRegularBipartiteGraphSegment;
  }

  /**
   * Build a small test NodeMetadataLeftIndexedMultiSegmentBipartiteGraph.
   *
   * @return a small test {@link NodeMetadataLeftIndexedMultiSegmentBipartiteGraph}
   */
  public static NodeMetadataLeftIndexedMultiSegmentBipartiteGraph
    buildSmallTestNodeMetadataLeftIndexedMultiSegmentBipartiteGraph() {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph nodeMetadataGraph =
      new NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph(
        3,
        10,
        10,
        10,
        2.0,
        100,
        2,
        new IdentityEdgeTypeMask(),
        new NullStatsReceiver()
      );
    int[][] leftNodeMetadata = new int[][]{};
    nodeMetadataGraph.addEdge(1, 2, (byte) 0, leftNodeMetadata,
      new int[][]{new int[]{67}, new int[]{302}}
    );
    nodeMetadataGraph.addEdge(1, 3, (byte) 0, leftNodeMetadata,
      new int[][]{new int[]{37, 67}, new int[]{100}}
    );
    nodeMetadataGraph.addEdge(1, 4, (byte) 0, leftNodeMetadata,
      new int[][]{null, new int[]{700}}
    );
    nodeMetadataGraph.addEdge(1, 5, (byte) 0, leftNodeMetadata,
      new int[][]{new int[]{11}, new int[]{900, 901, 902, 903, 904}}
    );
    nodeMetadataGraph.addEdge(2, 5, (byte) 0, leftNodeMetadata,
      new int[][]{new int[]{11}, new int[]{900, 901, 902, 903, 904}}
    );
    nodeMetadataGraph.addEdge(2, 6, (byte) 0, leftNodeMetadata,
      new int[][]{new int[]{10, 11, 12, 33, 24, 19}, new int[]{400, 401, 402, 403, 404}}
    );
    nodeMetadataGraph.addEdge(2, 10, (byte) 0, leftNodeMetadata,
      null
    );
    nodeMetadataGraph.addEdge(3, 7, (byte) 0, leftNodeMetadata,
      new int[][]{new int[]{23, 24}, null}
    );
    nodeMetadataGraph.addEdge(3, 8, (byte) 0, leftNodeMetadata,
      new int[][]{null, new int[]{700}}
    );
    nodeMetadataGraph.addEdge(3, 5, (byte) 0, leftNodeMetadata,
      new int[][]{new int[]{11}, new int[]{900, 901, 902, 903, 904}}
    );
    nodeMetadataGraph.addEdge(3, 9, (byte) 0, leftNodeMetadata,
      new int[][]{null, new int[]{102, 103, 101}}
    );
    nodeMetadataGraph.addEdge(3, 2, (byte) 0, leftNodeMetadata,
      new int[][]{new int[]{67}, new int[]{302}}
    );
    nodeMetadataGraph.addEdge(3, 10, (byte) 0, leftNodeMetadata,
      new int[][]{new int[]{43}, null}
    );
    nodeMetadataGraph.addEdge(3, 11, (byte) 0, leftNodeMetadata, new int[][]{null, null});
    nodeMetadataGraph.addEdge(3, 1, (byte) 0, leftNodeMetadata, new int[][]{null, null});

    return nodeMetadataGraph;
  }

  /**
   * Build a small test NodeMetadataLeftIndexedMultiSegmentBipartiteGraph.
   *
   * @return a small test {@link NodeMetadataLeftIndexedMultiSegmentBipartiteGraph}
   */
  public static NodeMetadataLeftIndexedMultiSegmentBipartiteGraph
    buildSmallTestNodeMetadataLeftIndexedMultiSegmentBipartiteGraphWithEdgeTypes() {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph nodeMetadataGraph =
      new NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph(
        2,
        10,
        2,
        6,
        2.0,
        6,
        2,
        new HigherBitsEdgeTypeMask(),
        new NullStatsReceiver()
      );
    int[][] leftNodeMetadata = new int[][]{};
    int[][] rightNodeMetadata = new int[][]{};
    nodeMetadataGraph.addEdge(1, 2, (byte) 0, leftNodeMetadata, rightNodeMetadata);

    nodeMetadataGraph.addEdge(1, 3, (byte) 1, leftNodeMetadata, rightNodeMetadata);
    nodeMetadataGraph.addEdge(2, 3, (byte) 1, leftNodeMetadata, rightNodeMetadata);
    nodeMetadataGraph.addEdge(3, 3, (byte) 1, leftNodeMetadata, rightNodeMetadata);

    nodeMetadataGraph.addEdge(1, 4, (byte) 2, leftNodeMetadata, rightNodeMetadata);

    nodeMetadataGraph.addEdge(1, 5, (byte) 3, leftNodeMetadata, rightNodeMetadata);
    nodeMetadataGraph.addEdge(2, 5, (byte) 0, leftNodeMetadata, rightNodeMetadata);

    nodeMetadataGraph.addEdge(2, 6, (byte) 0, leftNodeMetadata, rightNodeMetadata);
    nodeMetadataGraph.addEdge(2, 6, (byte) 1, leftNodeMetadata, rightNodeMetadata);

    nodeMetadataGraph.addEdge(1, 7, (byte) 0, leftNodeMetadata, rightNodeMetadata);
    nodeMetadataGraph.addEdge(2, 7, (byte) 1, leftNodeMetadata, rightNodeMetadata);

    return nodeMetadataGraph;
  }

  /**
   * Build a small test LeftIndexedPowerLawMultiSegmentBipartiteGraph.
   *
   * @return a small test {@link LeftIndexedPowerLawMultiSegmentBipartiteGraph}
   */
  public static LeftIndexedPowerLawMultiSegmentBipartiteGraph
  buildSmallTestLeftIndexedPowerLawMultiSegmentBipartiteGraphWithEdgeTypes() {
    LeftIndexedPowerLawMultiSegmentBipartiteGraph nodeMetadataGraph =
      new LeftIndexedPowerLawMultiSegmentBipartiteGraph(
        2,
        10,
        2,
        6,
        2.0,
        6,
        new HigherBitsEdgeTypeMask(),
        new NullStatsReceiver()
      );
    nodeMetadataGraph.addEdge(1, 2, (byte) 0);

    nodeMetadataGraph.addEdge(1, 3, (byte) 1);
    nodeMetadataGraph.addEdge(2, 3, (byte) 1);
    nodeMetadataGraph.addEdge(3, 3, (byte) 1);

    nodeMetadataGraph.addEdge(1, 4, (byte) 2);

    nodeMetadataGraph.addEdge(1, 5, (byte) 3);
    nodeMetadataGraph.addEdge(2, 5, (byte) 0);

    nodeMetadataGraph.addEdge(2, 6, (byte) 0);
    nodeMetadataGraph.addEdge(2, 6, (byte) 1);

    nodeMetadataGraph.addEdge(1, 7, (byte) 0);
    nodeMetadataGraph.addEdge(2, 7, (byte) 1);

    return nodeMetadataGraph;
  }

  /**
   * Build a random NodeMetadataLeftIndexedMultiSegmentBipartiteGraph of given left size.
   *
   * @param leftSize   is the left hand size of the bipartite graph
   * @param rightSize  is the right hand size of the bipartite graph
   * @param edgeProbability is the edge probability between two different nodes
   * @param random     is the random number generator to use for constructing the graph
   * @return a random bipartite graph
   */
  public static NodeMetadataLeftIndexedMultiSegmentBipartiteGraph
    buildRandomNodeMetadataLeftIndexedMultiSegmentBipartiteGraph(
    int leftSize, int rightSize, double edgeProbability, Random random
  ) {
    NodeMetadataLeftIndexedMultiSegmentBipartiteGraph nodeMetadataGraph =
      new NodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph(
        5,
        1000,
        leftSize,
        (int) (rightSize * edgeProbability),
        2.0,
        rightSize,
        2,
        new IdentityEdgeTypeMask(),
        new NullStatsReceiver()
      );

    int[][] leftNodeMetadata = new int[][]{};
    int[][] rightNodeMetadata = new int[][]{};

    for (int i = 0; i < leftSize; i++) {
      for (int j = 0; j < rightSize; j++) {
        if (random.nextDouble() < edgeProbability) {
          nodeMetadataGraph.addEdge(i, j, (byte) 0, leftNodeMetadata, rightNodeMetadata);
        }
      }
    }

    return nodeMetadataGraph;
  }
}
