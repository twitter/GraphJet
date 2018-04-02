package com.twitter.graphjet.bipartite;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.bipartite.segment.IdentityEdgeTypeMask;
import com.twitter.graphjet.stats.NullStatsReceiver;

public class RightNodeMetadataLeftIndexedMultiSegmentBipartiteGraphTest {

  private void addEdges(RightNodeMetadataLeftIndexedMultiSegmentBipartiteGraph graph) {
    int[][] array = new int[1][];
    array[0] = new int[3];
    array[0][0] = 1;
    array[0][1] = 2;
    array[0][2] = 3;


    graph.addEdge(1, 11, (byte) 0, array);
    graph.addEdge(1, 12, (byte) 0, array);
    graph.addEdge(4, 41, (byte) 0, array);
    graph.addEdge(2, 21, (byte) 0, array);
    graph.addEdge(4, 42, (byte) 0, array);
    graph.addEdge(3, 31, (byte) 0, array);
    graph.addEdge(2, 22, (byte) 0, array);
    graph.addEdge(1, 13, (byte) 0, array);
    graph.addEdge(4, 43, (byte) 0, array);
    graph.addEdge(5, 11, (byte) 0, array);
    graph.addEdge(5, 12, (byte) 0, array);
    graph.addEdge(5, 13, (byte) 0, array);
    graph.addEdge(5, 14, (byte) 0, array);
    graph.addEdge(5, 15, (byte) 0, array);
    graph.addEdge(5, 16, (byte) 0, array);
    graph.addEdge(5, 17, (byte) 0, array);
    graph.addEdge(5, 18, (byte) 0, array);
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
  }


  @Test
  public void testMultiSegmentConstruction() throws Exception {
    int maxNumSegments = 10;
    int maxNumEdgesPerSegment = 1500;
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
