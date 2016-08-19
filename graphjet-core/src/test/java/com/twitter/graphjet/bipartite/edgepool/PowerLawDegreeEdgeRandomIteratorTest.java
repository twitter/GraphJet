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


package com.twitter.graphjet.bipartite.edgepool;

import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import static com.twitter.graphjet.bipartite.edgepool.PowerLawDegreeEdgePoolTest.addEdgesToPool;

public class PowerLawDegreeEdgeRandomIteratorTest {

  @Test
  public void testPowerLawDegreeEdgeIterator() throws Exception {
    int maxNumNodes = 4;
    int maxDegree = 6;
    PowerLawDegreeEdgePool powerLawDegreeEdgePool =
        new PowerLawDegreeEdgePool(maxNumNodes, maxDegree, 2.0, new NullStatsReceiver());

    addEdgesToPool(powerLawDegreeEdgePool);

    PowerLawDegreeEdgeRandomIterator powerLawDegreeEdgeRandomIterator =
        new PowerLawDegreeEdgeRandomIterator(powerLawDegreeEdgePool);

    Random random = new Random(90238490238409L);
    int numSamples = 5;

    powerLawDegreeEdgeRandomIterator.resetForNode(1, numSamples, random);
    assertEquals(new IntArrayList(new int[]{13, 13, 11, 15, 14}),
        new IntArrayList(powerLawDegreeEdgeRandomIterator));
    powerLawDegreeEdgeRandomIterator.resetForNode(2, numSamples, random);
    assertEquals(new IntArrayList(new int[]{22, 22, 21, 23, 22}),
        new IntArrayList(powerLawDegreeEdgeRandomIterator));
    powerLawDegreeEdgeRandomIterator.resetForNode(3, numSamples, random);
    assertEquals(new IntArrayList(new int[]{31, 31, 31, 31, 31}),
        new IntArrayList(powerLawDegreeEdgeRandomIterator));
    powerLawDegreeEdgeRandomIterator.resetForNode(4, numSamples, random);
    assertEquals(new IntArrayList(new int[]{43, 41, 43, 41, 42}),
        new IntArrayList(powerLawDegreeEdgeRandomIterator));
    powerLawDegreeEdgeRandomIterator.resetForNode(5, numSamples, random);
    assertEquals(new IntArrayList(new int[]{51, 51, 51, 51, 51}),
        new IntArrayList(powerLawDegreeEdgeRandomIterator));

    // Test a larger sample
    powerLawDegreeEdgeRandomIterator.resetForNode(4, 900, random);
    Long2IntMap occurrenceCounts = new Long2IntOpenHashMap(3);
    for (int sample : new IntArrayList(powerLawDegreeEdgeRandomIterator)) {
      occurrenceCounts.put(sample, occurrenceCounts.get(sample) + 1);
    }
    assertEquals(301, occurrenceCounts.get(41));
    assertEquals(296, occurrenceCounts.get(42));
    assertEquals(303, occurrenceCounts.get(43));
  }
}
