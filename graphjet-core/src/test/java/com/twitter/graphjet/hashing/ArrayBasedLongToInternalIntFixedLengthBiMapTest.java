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


package com.twitter.graphjet.hashing;

import java.util.Random;

import com.google.common.base.Preconditions;
import org.junit.Test;

import com.twitter.graphjet.stats.NullStatsReceiver;
import com.twitter.graphjet.stats.StatsReceiver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ArrayBasedLongToInternalIntFixedLengthBiMapTest {
  private final StatsReceiver nullStatsReceiver = new NullStatsReceiver();

  @Test
  public void testSimpleKeyInsertion() throws Exception {
    int maxNumKeys = (int) (0.75 * (1 << 20)); // 1M
    double loadFactor = 0.75;
    ArrayBasedLongToInternalIntFixedLengthBiMap map =
        new ArrayBasedLongToInternalIntFixedLengthBiMap(
            maxNumKeys, loadFactor, -1, -1L, nullStatsReceiver);
    InternalIdMapTestHelper.KeyTestInfo keyTestInfo =
        InternalIdMapTestHelper.generateSimpleKeys(maxNumKeys);
    InternalIdMapTestHelper.testKeyRetrievals(map, maxNumKeys, keyTestInfo);
  }

  @Test
  public void testRandomKeyInsertion() throws Exception {
    Random random = new Random(89457098347123125L);
    int maxNumKeys = (int) (0.75 * (1 << 21)); // 2M
    double loadFactor = 0.75;
    for (int i = 0; i < 4; i++) {
      ArrayBasedLongToInternalIntFixedLengthBiMap map =
              new ArrayBasedLongToInternalIntFixedLengthBiMap(
                      maxNumKeys, loadFactor, -1, -1L, nullStatsReceiver);
      InternalIdMapTestHelper.KeyTestInfo keyTestInfo =
              InternalIdMapTestHelper.generateRandomKeys(random, maxNumKeys);
      InternalIdMapTestHelper.testKeyRetrievals(map, maxNumKeys, keyTestInfo);
    }
  }

  @Test
  public void testConcurrentReadWrites() {
    int maxNumKeys = (int) (0.75 * (1 << 8)); // this should be small
    double loadFactor = 0.75;
    ArrayBasedLongToInternalIntFixedLengthBiMap map =
        new ArrayBasedLongToInternalIntFixedLengthBiMap(
            maxNumKeys, loadFactor, -1, -1L, nullStatsReceiver);

    InternalIdMapTestHelper.KeyTestInfo keyTestInfo =
        InternalIdMapTestHelper.generateSimpleKeys(maxNumKeys);
    InternalIdMapConcurrentTestHelper.testConcurrentReadWrites(map, keyTestInfo);
  }

  @Test
  public void testRandomConcurrentReadWrites() {
    int maxNumKeys = (int) (0.75 * (1 << 20)); // 1M
    double loadFactor = 0.75;
    ArrayBasedLongToInternalIntFixedLengthBiMap map =
        new ArrayBasedLongToInternalIntFixedLengthBiMap(
            maxNumKeys, loadFactor, -1, -1L, nullStatsReceiver);

    // Sets up a concurrent read-write situation with the given map
    Random random = new Random(89234758923475L);
    InternalIdMapConcurrentTestHelper.testRandomConcurrentReadWriteThreads(
        map, -1, 600, maxNumKeys, random);
  }

  @Test
  public void testClear() {
    ArrayBasedLongToInternalIntFixedLengthBiMap arrayBasedLongToInternalIntFixedLengthBiMap =
            new ArrayBasedLongToInternalIntFixedLengthBiMap((-6206),
                    2154.072652232,
                    1,
                    1,
                    new NullStatsReceiver());

    arrayBasedLongToInternalIntFixedLengthBiMap.put(1L);

    assertEquals(1, arrayBasedLongToInternalIntFixedLengthBiMap.getNumStoredKeys());
    assertEquals(16, arrayBasedLongToInternalIntFixedLengthBiMap.getBackingArrayLength());

    arrayBasedLongToInternalIntFixedLengthBiMap.clear();

    assertEquals(0, arrayBasedLongToInternalIntFixedLengthBiMap.getNumStoredKeys());
    assertEquals(16, arrayBasedLongToInternalIntFixedLengthBiMap.getBackingArrayLength());
  }

  @Test
  public void testGetKeyWithNegative() {
    NullStatsReceiver nullStatsReceiver = new NullStatsReceiver();
    ArrayBasedLongToInternalIntFixedLengthBiMap arrayBasedLongToInternalIntFixedLengthBiMap =
            new ArrayBasedLongToInternalIntFixedLengthBiMap((-1768), 0.0, 14, (-1768), nullStatsReceiver);

    try {
      arrayBasedLongToInternalIntFixedLengthBiMap.getKey((-1768));
      fail("Expecting exception: IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      assertEquals(ArrayBasedLongToInternalIntFixedLengthBiMap.class.getName(), e.getStackTrace()[0].getClassName());
    }
  }

  @Test
  public void testPutThrowsRuntimeException() {
    NullStatsReceiver nullStatsReceiver = new NullStatsReceiver();
    ArrayBasedLongToInternalIntFixedLengthBiMap arrayBasedLongToInternalIntFixedLengthBiMap =
            new ArrayBasedLongToInternalIntFixedLengthBiMap(204, 0.0, 204, 204, nullStatsReceiver);

    try {
      arrayBasedLongToInternalIntFixedLengthBiMap.put(0L);
      fail("Expecting exception: RuntimeException");
    } catch (RuntimeException e) {
      assertEquals(ArrayBasedLongToInternalIntFixedLengthBiMap.class.getName(), e.getStackTrace()[0].getClassName());
    }
  }

  @Test
  public void testFailsToCreateThrowsIllegalArgumentException() {
    NullStatsReceiver nullStatsReceiver = new NullStatsReceiver();
    ArrayBasedLongToInternalIntFixedLengthBiMap arrayBasedLongToInternalIntFixedLengthBiMap = null;

    try {
      arrayBasedLongToInternalIntFixedLengthBiMap =
              new ArrayBasedLongToInternalIntFixedLengthBiMap(610, 610, 0, 610, nullStatsReceiver);
      fail("Expecting exception: IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals(Preconditions.class.getName(), e.getStackTrace()[0].getClassName());
    }
  }

  @Test
  public void testGetKeyThrowsIndexOutOfBoundsException() {
    ArrayBasedLongToInternalIntFixedLengthBiMap arrayBasedLongToInternalIntFixedLengthBiMap =
            new ArrayBasedLongToInternalIntFixedLengthBiMap(0, 0, 22, 0L, new NullStatsReceiver());

    try {
      arrayBasedLongToInternalIntFixedLengthBiMap.getKey(722);
      fail("Expecting exception: IndexOutOfBoundsException");
    } catch (IndexOutOfBoundsException e) {
      assertEquals(ArrayBasedLongToInternalIntFixedLengthBiMap.class.getName(), e.getStackTrace()[0].getClassName());
    }
  }

}
