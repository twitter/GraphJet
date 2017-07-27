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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import static com.twitter.graphjet.bipartite.edgepool.RegularDegreeEdgePoolTest.addEdgesToPool;
import static org.junit.Assert.assertFalse;

public class RegularDegreeEdgeIteratorTest {

  @Test
  public void testRegularDegreeEdgeIterator() throws Exception {
    int maxNumNodes = 5;
    int maxDegree = 3;
    RegularDegreeEdgePool regularDegreeEdgePool =
        new RegularDegreeEdgePool(maxNumNodes, maxDegree, new NullStatsReceiver());
    addEdgesToPool(regularDegreeEdgePool);

    RegularDegreeEdgeIterator regularDegreeEdgeIterator =
        new RegularDegreeEdgeIterator(regularDegreeEdgePool);

    regularDegreeEdgeIterator.resetForNode(1);
    assertEquals(new IntArrayList(new int[]{11, 12, 13}),
                 new IntArrayList(regularDegreeEdgeIterator));
    regularDegreeEdgeIterator.resetForNode(2);
    assertEquals(new IntArrayList(new int[]{21, 22}),
                 new IntArrayList(regularDegreeEdgeIterator));
    regularDegreeEdgeIterator.resetForNode(3);
    assertEquals(new IntArrayList(new int[]{31}),
                 new IntArrayList(regularDegreeEdgeIterator));
    regularDegreeEdgeIterator.resetForNode(4);
    assertEquals(new IntArrayList(new int[]{41, 42, 43}),
                 new IntArrayList(regularDegreeEdgeIterator));
  }

  @Test
  public void testResetForNodeReturningRegularDegreeEdgeIteratorWhereHasNextIsFalse() {
    RegularDegreeEdgePool regularDegreeEdgePool = new RegularDegreeEdgePool(1224, 1224, new NullStatsReceiver());
    RegularDegreeEdgeIterator regularDegreeEdgeIterator = new RegularDegreeEdgeIterator(regularDegreeEdgePool);
    RegularDegreeEdgeIterator regularDegreeEdgeIteratorTwo = regularDegreeEdgeIterator.resetForNode(3307);

    assertFalse(regularDegreeEdgeIteratorTwo.hasNext());
  }

  @Test
  public void testSkip() {
    NullStatsReceiver nullStatsReceiver = new NullStatsReceiver();
    RegularDegreeEdgePool regularDegreeEdgePool = new RegularDegreeEdgePool(61, 3311, nullStatsReceiver);
    RegularDegreeEdgeIterator regularDegreeEdgeIterator = new RegularDegreeEdgeIterator(regularDegreeEdgePool);

    assertEquals(0, regularDegreeEdgeIterator.skip(3311));
  }

  @Test
  public void testNext() {
    NullStatsReceiver nullStatsReceiver = new NullStatsReceiver();
    RegularDegreeEdgePool regularDegreeEdgePool = new RegularDegreeEdgePool(61, 3311, nullStatsReceiver);
    RegularDegreeEdgeIterator regularDegreeEdgeIterator = new RegularDegreeEdgeIterator(regularDegreeEdgePool);
    regularDegreeEdgeIterator.next();

    assertFalse(regularDegreeEdgeIterator.hasNext());
  }

}
