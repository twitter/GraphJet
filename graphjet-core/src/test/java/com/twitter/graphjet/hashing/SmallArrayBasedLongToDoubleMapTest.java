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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;

public class SmallArrayBasedLongToDoubleMapTest {

  private SmallArrayBasedLongToDoubleMap insertRandomKeyValuePairsIntoMap(
    Random random,
    int size,
    int maxKey,
    int maxValue,
    int trimSize
  ) {
    SmallArrayBasedLongToDoubleMap map = new SmallArrayBasedLongToDoubleMap();

    for (int i = 0; i < size; i++) {
      long key = random.nextInt(maxKey);
      double value = (double) random.nextInt(maxValue);

      map.put(key, value, 0L);
      map.sort();
      map.trim(trimSize);
    }

    return map;
  }

  @Test
  public void testManySmallRangeKeys() {
    SmallArrayBasedLongToDoubleMap map =
      insertRandomKeyValuePairsIntoMap(new Random(90238490238409L), 1000, 10, 1000, 3);

    long[] expectedKeys = {2L, 0L, 4L};
    double[] expectedValues = {996.0, 995.0, 994.0};

    assertEquals(new LongArrayList(expectedKeys), new LongArrayList(map.keys()));
    assertEquals(new DoubleArrayList(expectedValues), new DoubleArrayList(map.values()));
  }

  @Test
  public void testFewSmallRangeKeys() {
    SmallArrayBasedLongToDoubleMap map =
      insertRandomKeyValuePairsIntoMap(new Random(90238490238409L), 2, 10, 1000, 3);

    long[] expectedKeys = {6L, 4L};
    double[] expectedValues = {970.0, 326.0};

    assertEquals(new LongArrayList(expectedKeys), new LongArrayList(map.keys()));
    assertEquals(new DoubleArrayList(expectedValues), new DoubleArrayList(map.values()));
  }

  @Test
  public void testManyLargeRangeKeys() {
    SmallArrayBasedLongToDoubleMap map =
      insertRandomKeyValuePairsIntoMap(new Random(90238490238409L), 1000, 100, 1000, 3);

    long[] expectedKeys = {32L, 90L, 20L};
    double[] expectedValues = {996.0, 995.0, 995.0};

    assertEquals(new LongArrayList(expectedKeys), new LongArrayList(map.keys()));
    assertEquals(new DoubleArrayList(expectedValues), new DoubleArrayList(map.values()));
  }

  @Test
  public void testRepeatedKeys() {
    SmallArrayBasedLongToDoubleMap map =
      insertRandomKeyValuePairsIntoMap(new Random(90238490238409L), 1000, 1, 1000, 3);

    long[] expectedKeys = {0L};
    double[] expectedValues = {326.0};

    assertEquals(new LongArrayList(expectedKeys), new LongArrayList(map.keys()));
    assertEquals(new DoubleArrayList(expectedValues), new DoubleArrayList(map.values()));
  }

  @Test
  public void testRepeatedKeyMetadataPairs() {
    SmallArrayBasedLongToDoubleMap map = new SmallArrayBasedLongToDoubleMap();
    for (int i = 1; i <= 100; i++) {
      map.put(i, i * 0.01, 1000 + i);
    }

    for (int i = 1; i <= 100; i++) {
      assertEquals(false, map.put(i, i * 0.01, 1000 + i));
    }

    for (int i = 100; i >= 1; i--) {
      assertEquals(false, map.put(i, i * 0.01, 1000 + i));
    }

    for (int i = 1; i <= 100; i++) {
      assertEquals(true, map.put(i, i * 0.01, 2000 + i));
    }
  }

  @Test
  public void testUniqueKeys() {
    SmallArrayBasedLongToDoubleMap mapOne = new SmallArrayBasedLongToDoubleMap();

    Random random = new Random(90238490238409L);
    for (int i = 1; i <= 300; i++) {
      mapOne.put(random.nextInt(200), i * 0.01, 1000 + i);
    }

    assertEquals(300, mapOne.size());
    assertEquals(156, mapOne.uniqueKeysSize());

    SmallArrayBasedLongToDoubleMap mapTwo = new SmallArrayBasedLongToDoubleMap();

    for (int i = 1; i <= 30; i++) {
      mapTwo.put(random.nextInt(20), i * 0.01, 1000 + i);
    }

    assertEquals(15, mapTwo.uniqueKeysSize());
  }
}
