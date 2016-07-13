package com.twitter.graphjet.hashing;

import java.util.Random;

import org.junit.Test;

import com.twitter.graphjet.stats.NullStatsReceiver;
import com.twitter.graphjet.stats.StatsReceiver;

public class ArrayBasedLongToInternalIntBiMapTest {
  private final StatsReceiver nullStatsReceiver = new NullStatsReceiver();

  @Test
  public void testSimpleKeyInsertion() throws Exception {
    int maxNumKeys = (int) (0.75 * (1 << 20)); // 1M
    double loadFactor = 0.75;
    ArrayBasedLongToInternalIntBiMap map = new ArrayBasedLongToInternalIntBiMap(
        maxNumKeys / 4, loadFactor, -1, -1L, nullStatsReceiver);
    InternalIdMapTestHelper.KeyTestInfo keyTestInfo =
        InternalIdMapTestHelper.generateSimpleKeys(maxNumKeys);
    InternalIdMapTestHelper.testKeyRetrievals(map, maxNumKeys, keyTestInfo);
  }

  @Test
  public void testRandomKeyInsertion() throws Exception {
    Random random = new Random(75623987456293L);
    int maxNumKeys = (int) (0.75 * (1 << 21)); // 2M
    double loadFactor = 0.75;
    for (int i = 0; i < 4; i++) {
      int factor = 1 << (i + 1);
      ArrayBasedLongToInternalIntBiMap map =
          new ArrayBasedLongToInternalIntBiMap(
              maxNumKeys / factor, loadFactor, -1, -1L, nullStatsReceiver);
      InternalIdMapTestHelper.KeyTestInfo keyTestInfo =
          InternalIdMapTestHelper.generateRandomKeys(random, maxNumKeys);
      InternalIdMapTestHelper.testKeyRetrievals(map, maxNumKeys, keyTestInfo);
    }
  }

  @Test
  public void testConcurrentReadWrites() {
    int maxNumKeys = (int) (0.75 * (1 << 8)); // this should be small
    double loadFactor = 0.75;
    ArrayBasedLongToInternalIntBiMap map = new ArrayBasedLongToInternalIntBiMap(
        maxNumKeys / 8, loadFactor, -1, -1L, nullStatsReceiver);

    InternalIdMapTestHelper.KeyTestInfo keyTestInfo =
        InternalIdMapTestHelper.generateSimpleKeys(maxNumKeys);
    InternalIdMapConcurrentTestHelper.testConcurrentReadWrites(map, keyTestInfo);
  }

  @Test
  public void testRandomConcurrentReadWrites() {
    int maxNumKeys = (int) (0.75 * (1 << 20)); // 1M
    double loadFactor = 0.75;
    ArrayBasedLongToInternalIntBiMap map = new ArrayBasedLongToInternalIntBiMap(
        maxNumKeys / 8, loadFactor, -1, -1L, nullStatsReceiver);

    // Sets up a concurrent read-write situation with the given pool and edges
    Random random = new Random(89234758923475L);
    InternalIdMapConcurrentTestHelper.testRandomConcurrentReadWriteThreads(
        map, -1, 600, maxNumKeys, random);
  }
}
