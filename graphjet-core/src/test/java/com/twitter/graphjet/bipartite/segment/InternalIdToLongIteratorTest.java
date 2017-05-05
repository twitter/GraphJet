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


package com.twitter.graphjet.bipartite.segment;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.bipartite.api.WithEdgeMetadataIntIterator;
import com.twitter.graphjet.bipartite.api.ReadOnlyIntIterator;
import com.twitter.graphjet.hashing.ArrayBasedLongToInternalIntBiMap;
import com.twitter.graphjet.hashing.LongToInternalIntBiMap;
import com.twitter.graphjet.stats.NullStatsReceiver;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class InternalIdToLongIteratorTest {
  private final StatsReceiver nullStatsReceiver = new NullStatsReceiver();

  // create a helper class wrapping IntIterator which implements WithEdgeMetadataIntIteratorImpl, so
  // that testcase could use WithEdgeMetadataIntIteratorImpl in the resetWithIntIterator function.
  public class WithEdgeMetadataIntIteratorImpl
    extends ReadOnlyIntIterator implements WithEdgeMetadataIntIterator {
    private IntIterator intIterator;

    public WithEdgeMetadataIntIteratorImpl(IntIterator intIterator) {
      this.intIterator = intIterator;
    }

    @Override
    public long currentMetadata() {
      return 0L;
    }

    @Override
    public int nextInt() {
      return intIterator.nextInt();
    }

    @Override
    public int skip(int var1) {
      return intIterator.skip(var1);
    }

    @Override
    public Integer next() {
      return nextInt();
    }

    @Override
    public boolean hasNext() {
      return intIterator.hasNext();
    }
  }

  @Test
  public void testInternalIdToLongIterator() {
    LongToInternalIntBiMap nodesToIndexBiMap =
        new ArrayBasedLongToInternalIntBiMap(10, 0.5, -1, -1L, nullStatsReceiver);
    int n = 7;
    int[] expectedIndices = new int[n];
    long[] expectedEntries = new long[n];
    for (int i = 0; i < n; i++) {
      expectedIndices[i] = nodesToIndexBiMap.put(i);
      expectedEntries[i] = (long) i;
    }
    IntIterator intIterator = new IntArrayList(expectedIndices).iterator();

    InternalIdToLongIterator internalIdToLongIterator =
        new InternalIdToLongIterator(nodesToIndexBiMap, new IdentityEdgeTypeMask());

    internalIdToLongIterator.resetWithIntIterator(new WithEdgeMetadataIntIteratorImpl(intIterator));
    assertEquals(new LongArrayList(expectedEntries), new LongArrayList(internalIdToLongIterator));
  }
}
