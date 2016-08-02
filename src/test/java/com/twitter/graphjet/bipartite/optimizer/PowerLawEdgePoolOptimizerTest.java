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


package com.twitter.graphjet.bipartite.optimizer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.bipartite.edgepool.EdgePool;
import com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePool;
import com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePoolTest;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class PowerLawEdgePoolOptimizerTest {
  @Test
  public void testOptimizeSegment() {
    int maxNumNodes = 4;
    int maxDegree = 6;
    PowerLawDegreeEdgePool powerLawDegreeEdgePool =
      new PowerLawDegreeEdgePool(maxNumNodes, maxDegree, 2.0, new NullStatsReceiver());

    PowerLawDegreeEdgePoolTest.addEdgesToPool(powerLawDegreeEdgePool);

    EdgePool optimizedPool = Optimizer.optimizePowerLawDegreeEdgePool(powerLawDegreeEdgePool);
    assertEquals(new IntArrayList(new int[]{11, 12, 13, 14, 15, 16, 17}),
      new IntArrayList(optimizedPool.getNodeEdges(1)));
    assertEquals(new IntArrayList(new int[]{21, 22, 23}),
      new IntArrayList(optimizedPool.getNodeEdges(2)));
    assertEquals(new IntArrayList(new int[]{31}),
      new IntArrayList(optimizedPool.getNodeEdges(3)));
    assertEquals(new IntArrayList(new int[]{41, 42, 43}),
      new IntArrayList(optimizedPool.getNodeEdges(4)));
    assertEquals(new IntArrayList(new int[]{51}),
      new IntArrayList(optimizedPool.getNodeEdges(5)));
  }
}

