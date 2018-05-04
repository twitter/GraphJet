package com.twitter.graphjet.bipartite;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.segment.IdentityEdgeTypeMask;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;

public class RightNodeMetadataLeftIndexedMultiSegmentBipartiteGraphTest {
  private static int FEATURE_SIZE = 5;
  private static int NUM_INTEGER_TO_UNPACK_SHORT = 2;

  private void addEdges(RightNodeMetadataLeftIndexedMultiSegmentBipartiteGraph graph) {
    int[][] array = new int[1][];
    array[0] = new int[FEATURE_SIZE];
    array[0][0] = 0;
    array[0][1] = 0;
    array[0][2] = 2;
    array[0][3] = 3;
    array[0][4] = 0;


    graph.addEdge(1, 11, (byte) 1, array);
    graph.addEdge(1, 12, (byte) 2, array);
    graph.addEdge(4, 41, (byte) 3, array);
    graph.addEdge(2, 21, (byte) 7, array);
    graph.addEdge(4, 42, (byte) 1, array);
    graph.addEdge(3, 31, (byte) 2, array);
    graph.addEdge(2, 22, (byte) 3, array);
    graph.addEdge(1, 13, (byte) 7, array);
    graph.addEdge(4, 43, (byte) 1, array);
    graph.addEdge(5, 11, (byte) 2, array);
    graph.addEdge(5, 12, (byte) 3, array);
    graph.addEdge(5, 13, (byte) 7, array);
    graph.addEdge(5, 14, (byte) 1, array);
    graph.addEdge(5, 15, (byte) 2, array);
    graph.addEdge(5, 16, (byte) 3, array);
    graph.addEdge(5, 17, (byte) 7, array);
    graph.addEdge(5, 18, (byte) 4, array);
    // violates the max num nodes assumption
  }

  /**
   * Build a random left-regular bipartite graph of given left and right sizes.
   *
   * @param leftSize   is the left hand size of the bipartite graph
   * @param rightSize  is the right hand size of the bipartite graph
   * @return a random bipartite graph
   */
  public static RightNodeMetadataLeftIndexedMultiSegmentBipartiteGraph buildRandomMultiSegmentBipartiteGraph(
    int maxNumSegments,
    int maxNumEdgesPerSegment,
    int leftSize,
    int rightSize
  ) {
    return new RightNodeMetadataLeftIndexedPowerLawMultiSegmentBipartiteGraph(
        maxNumSegments,
        maxNumEdgesPerSegment,
        leftSize / 2,
        10,
        2.0,
        rightSize / 2,
        1,
        new IdentityEdgeTypeMask(),
        new NullStatsReceiver()
      );
  }

  private void testGraph(RightNodeMetadataLeftIndexedMultiSegmentBipartiteGraph multiSegmentPowerLawBipartiteGraph) {
    assertEquals(3, multiSegmentPowerLawBipartiteGraph.getLeftNodeDegree(1));
    assertEquals(2, multiSegmentPowerLawBipartiteGraph.getLeftNodeDegree(2));
    assertEquals(1, multiSegmentPowerLawBipartiteGraph.getLeftNodeDegree(3));
    assertEquals(3, multiSegmentPowerLawBipartiteGraph.getLeftNodeDegree(4));

    Long2ObjectArrayMap long2IntArrayMap = new Long2ObjectArrayMap<IntArrayList>();
    int[] array11 = {1, 1, 0, 0, 2, 3, 0}; // one like, one retweet
    int[] array12 = {1, 0, 0, 1, 2, 3, 0}; // one retweet, one reply
    int[] array13 = {0, 0, 2, 0, 2, 3, 0}; // two quotes
    int[] array21 = {0, 0, 1, 0, 2, 3, 0}; // one quote
    int[] array22 = {0, 0, 0, 1, 2, 3, 0}; // one reply
    int[] array31 = {1, 0, 0, 0, 2, 3, 0}; // one retweet
    long2IntArrayMap.put(11, new IntArrayList(array11));
    long2IntArrayMap.put(12, new IntArrayList(array12));
    long2IntArrayMap.put(13, new IntArrayList(array13));
    long2IntArrayMap.put(21, new IntArrayList(array21));
    long2IntArrayMap.put(22, new IntArrayList(array22));
    long2IntArrayMap.put(31, new IntArrayList(array31));


    for (int i = 1; i <= 3; i++) {
      EdgeIterator edgeIterator = multiSegmentPowerLawBipartiteGraph.getLeftNodeEdges(i);

      while (edgeIterator.hasNext()) {
        long rightNode = edgeIterator.nextLong();
        int[] metadata = new int[FEATURE_SIZE + NUM_INTEGER_TO_UNPACK_SHORT];
        ((RightNodeMetadataMultiSegmentIterator) edgeIterator).fetchFeatureArrayForNode(rightNode, 0, metadata, NUM_INTEGER_TO_UNPACK_SHORT);
        assertEquals(long2IntArrayMap.get(rightNode), new IntArrayList(metadata));
      }
    }
  }


  @Test
  public void testMultiSegmentConstruction() throws Exception {
    int maxNumSegments = 10;
    int maxNumEdgesPerSegment = 3;
    int leftSize = 100;
    int rightSize = 1000;

    RightNodeMetadataLeftIndexedMultiSegmentBipartiteGraph multiSegmentPowerLawBipartiteGraph =
      buildRandomMultiSegmentBipartiteGraph(
        maxNumSegments,
        maxNumEdgesPerSegment,
        leftSize,
        rightSize
      );

    addEdges(multiSegmentPowerLawBipartiteGraph);
    testGraph(multiSegmentPowerLawBipartiteGraph);
  }
}
