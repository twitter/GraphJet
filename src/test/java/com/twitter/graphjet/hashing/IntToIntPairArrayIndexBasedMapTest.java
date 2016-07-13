package com.twitter.graphjet.hashing;

import java.util.Random;

import org.junit.Test;

import com.twitter.graphjet.stats.NullStatsReceiver;
import com.twitter.graphjet.stats.StatsReceiver;

public class IntToIntPairArrayIndexBasedMapTest {
  private final StatsReceiver nullStatsReceiver = new NullStatsReceiver();

  @Test
  public void testSimpleKeyInsertion() throws Exception {
    int expectedNumKeys = (int) (0.75 * (1 << 20)); // 1M
    IntToIntPairHashMap map =
        new IntToIntPairArrayIndexBasedMap(expectedNumKeys / 4, -1, nullStatsReceiver);
    IntToIntPairMapTestHelper.KeyTestInfo keysAndValues =
        IntToIntPairMapTestHelper.generateSimpleKeys(expectedNumKeys);
    IntToIntPairMapTestHelper.testKeyRetrievals(map, keysAndValues);
  }

  @Test
  public void testRandomKeyInsertion() throws Exception {
    Random random = new Random(7985674567766598L);
    int maxNumKeys = (int) (0.75 * (1 << 21)); // 2M
    for (int i = 0; i < 4; i++) {
      IntToIntPairHashMap map =
          new IntToIntPairArrayIndexBasedMap(maxNumKeys, -1, nullStatsReceiver);
      IntToIntPairMapTestHelper.KeyTestInfo keyTestInfo =
          IntToIntPairMapTestHelper.generateRandomKeys(random, maxNumKeys);
      IntToIntPairMapTestHelper.testKeyRetrievals(map, keyTestInfo);
    }
  }

  @Test
  public void testConcurrentReadWrites() {
    int maxNumKeys = (int) (0.75 * (1 << 8)); // this should be small
    IntToIntPairHashMap map = new IntToIntPairArrayIndexBasedMap(maxNumKeys, -1, nullStatsReceiver);

    IntToIntPairMapTestHelper.KeyTestInfo keyTestInfo =
        IntToIntPairMapTestHelper.generateSimpleKeys(maxNumKeys);
    IntToIntPairMapConcurrentTestHelper.testConcurrentReadWrites(map, keyTestInfo);
  }

  @Test
  public void testRandomConcurrentReadWrites() {
    int maxNumKeys = (int) (0.75 * (1 << 20)); // 1M
    IntToIntPairHashMap map = new IntToIntPairArrayIndexBasedMap(maxNumKeys, -1, nullStatsReceiver);

    // Sets up a concurrent read-write situation with the given map
    Random random = new Random(89234758923475L);
    IntToIntPairMapConcurrentTestHelper.testRandomConcurrentReadWriteThreads(
        map, -1, 600, maxNumKeys, random);
  }
}
