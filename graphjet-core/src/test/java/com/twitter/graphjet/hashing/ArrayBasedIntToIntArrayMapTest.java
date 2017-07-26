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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import com.google.common.base.Preconditions;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.twitter.graphjet.stats.NullStatsReceiver;
import com.twitter.graphjet.stats.StatsReceiver;

import it.unimi.dsi.fastutil.ints.IntIterator;

public class ArrayBasedIntToIntArrayMapTest {
  private final StatsReceiver nullStatsReceiver = new NullStatsReceiver();

  private Map<Integer, ArrayList<Integer>> generateRandomKeyValuePairs(
    Random random,
    int size,
    int maxArrayLength
  ) {
    Map<Integer, ArrayList<Integer>> map = new TreeMap<Integer, ArrayList<Integer>>();
    for (int i = 0; i < size; i++) {
      int arrayLength = random.nextInt(maxArrayLength);

      if (arrayLength != 0) {
        ArrayList<Integer> array = new ArrayList<Integer>();
        for (int j = 0; j < arrayLength; j++) {
          array.add(random.nextInt());
        }
        map.put(i, array);
      } else {
        map.put(i, null);
      }
    }

    return map;
  }

  private void testArrayBasedIntToIntArrayMap(Random random, int capacity, int maxArrayLength) {
    IntToIntArrayMap intToIntArrayMap = new ArrayBasedIntToIntArrayMap(
      capacity,
      maxArrayLength,
      nullStatsReceiver
    );

    Map<Integer, ArrayList<Integer>> inputMap = generateRandomKeyValuePairs(
      random,
      capacity,
      maxArrayLength
    );
    Iterator<Map.Entry<Integer, ArrayList<Integer>>> it = inputMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Integer, ArrayList<Integer>> pair = it.next();
      Integer key = pair.getKey();
      ArrayList<Integer> value = pair.getValue();

      if (value != null) {
        int[] array = new int[value.size()];
        for (int i = 0; i < value.size(); i++) {
          array[i] = value.get(i);
        }

        intToIntArrayMap.put(key, array);
      }
    }

    for (int i = 0; i < capacity; i++) {
      IntIterator iterator = intToIntArrayMap.get(i);
      int[] actualArray = null;
      if (intToIntArrayMap.getArrayLength(i) > 0) {
        actualArray = new int[intToIntArrayMap.getArrayLength(i)];
      }

      int index = 0;
      while (iterator.hasNext()) {
        actualArray[index] = iterator.nextInt();
        index++;
      }

      ArrayList<Integer> expectedValue = inputMap.get(i);

      if (actualArray != null) {
        int[] expectedArray = new int[expectedValue.size()];
        for (int j = 0; j < expectedValue.size(); j++) {
          expectedArray[j] = expectedValue.get(j);
        }
        assertEquals(true, Arrays.equals(expectedArray, actualArray));
      } else {
        assertEquals(expectedValue, null);
      }
    }
  }

  @Test
  public void smallTestArrayBasedIntToIntArrayMap() {
    testArrayBasedIntToIntArrayMap(new Random(90238490238409L), 200, (1 << 4) - 1);
  }

  @Test
  public void bigTestArrayBasedIntToIntArrayMap() {
    testArrayBasedIntToIntArrayMap(new Random(90238490238410L), 1200, (1 << 3) - 1);
  }

  @Test
  public void hugeTestArrayBasedIntToIntArrayMap() {
    testArrayBasedIntToIntArrayMap(new Random(90238490238411L), 12000, (1 << 3) - 1);
  }

  @Test
  public void testConcurrentReadWrites() {
    int maxNumKeys = (int) (0.75 * (1 << 8)); // this should be small
    int arrayMaxLength = 15;
    IntToIntArrayMap map = new ArrayBasedIntToIntArrayMap(
      maxNumKeys,
      arrayMaxLength,
      nullStatsReceiver
    );

    Random random = new Random(90238490238411L);
    Map<Integer, ArrayList<Integer>> keysToValueMap = generateRandomKeyValuePairs(
      random,
      maxNumKeys,
      arrayMaxLength
    );
    IntToIntArrayMapConcurrentTestHelper.testConcurrentReadWrites(
      map,
      keysToValueMap
    );
  }

  @Test
  public void testRandomConcurrentReadWrites() {
    int maxNumKeys = (int) (0.75 * (1 << 20)); // 1M
    int maxArrayLength = 15;
    IntToIntArrayMap map = new ArrayBasedIntToIntArrayMap(
      maxNumKeys,
      maxArrayLength,
      nullStatsReceiver
    );

    // Sets up a concurrent read-write situation with the given map
    Random random = new Random(89234758923475L);
    Map<Integer, ArrayList<Integer>> keysToValueMap = generateRandomKeyValuePairs(
      random,
      maxNumKeys,
      maxArrayLength
    );
    IntToIntArrayMapConcurrentTestHelper.testRandomConcurrentReadWriteThreads(
      map,
      new ArrayList<Integer>(),
      600,
      keysToValueMap,
      random
    );
  }

  @Test
  public void testFailsCreateTakingThreeArgumentsThrowsIllegalArgumentException() {
    try {
      new ArrayBasedIntToIntArrayMap((-1), (-1), new NullStatsReceiver());
      fail("Expecting exception: IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertEquals(Preconditions.class.getName(), e.getStackTrace()[0].getClassName());
    }
  }

  @Test
  public void testGetArrayLengthReturningZero() {
    ArrayBasedIntToIntArrayMap arrayBasedIntToIntArrayMap =
            new ArrayBasedIntToIntArrayMap(2856, 0, new NullStatsReceiver());

    assertEquals(0, arrayBasedIntToIntArrayMap.getArrayLength(2856));
  }

}