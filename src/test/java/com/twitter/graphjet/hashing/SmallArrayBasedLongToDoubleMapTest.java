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

      map.put(key, value);
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
}
