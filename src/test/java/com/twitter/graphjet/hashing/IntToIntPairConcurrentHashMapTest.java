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

import com.twitter.graphjet.stats.NullStatsReceiver;
import com.twitter.graphjet.stats.StatsReceiver;

public class IntToIntPairConcurrentHashMapTest {
  private final StatsReceiver nullStatsReceiver = new NullStatsReceiver();

  @Test
  public void testSimpleKeyInsertion() throws Exception {
    int expectedNumKeys = (int) (0.75 * (1 << 20)); // 1M
    double loadFactor = 0.75;
    IntToIntPairHashMap map =
        new IntToIntPairConcurrentHashMap(expectedNumKeys / 4, loadFactor, -1, nullStatsReceiver);
    IntToIntPairMapTestHelper.KeyTestInfo keysAndValues =
        IntToIntPairMapTestHelper.generateSimpleKeys(expectedNumKeys);
    IntToIntPairMapTestHelper.testKeyRetrievals(map, keysAndValues);
  }

  @Test
  public void testRandomKeyInsertion() throws Exception {
    Random random = new Random(7985674567766598L);
    int maxNumKeys = (int) (0.75 * (1 << 21)); // 2M
    double loadFactor = 0.75;
    for (int i = 0; i < 4; i++) {
      IntToIntPairHashMap map =
          new IntToIntPairConcurrentHashMap(maxNumKeys, loadFactor, -1, nullStatsReceiver);
      IntToIntPairMapTestHelper.KeyTestInfo keyTestInfo =
          IntToIntPairMapTestHelper.generateRandomKeys(random, maxNumKeys);
      IntToIntPairMapTestHelper.testKeyRetrievals(map, keyTestInfo);
    }
  }

  @Test
  public void testConcurrentReadWrites() {
    int maxNumKeys = (int) (0.75 * (1 << 8)); // this should be small
    double loadFactor = 0.75;
    IntToIntPairHashMap map =
        new IntToIntPairConcurrentHashMap(maxNumKeys, loadFactor, -1, nullStatsReceiver);

    IntToIntPairMapTestHelper.KeyTestInfo keyTestInfo =
        IntToIntPairMapTestHelper.generateSimpleKeys(maxNumKeys);
    IntToIntPairMapConcurrentTestHelper.testConcurrentReadWrites(map, keyTestInfo);
  }

  @Test
  public void testRandomConcurrentReadWrites() {
    int maxNumKeys = (int) (0.75 * (1 << 20)); // 1M
    double loadFactor = 0.75;
    IntToIntPairHashMap map =
        new IntToIntPairConcurrentHashMap(maxNumKeys, loadFactor, -1, nullStatsReceiver);

    // Sets up a concurrent read-write situation with the given map
    Random random = new Random(89234758923475L);
    IntToIntPairMapConcurrentTestHelper.testRandomConcurrentReadWriteThreads(
        map, -1, 600, maxNumKeys, random);
  }
}
