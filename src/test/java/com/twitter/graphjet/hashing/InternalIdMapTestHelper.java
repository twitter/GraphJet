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

import static org.junit.Assert.assertEquals;

import it.unimi.dsi.fastutil.longs.LongOpenHashBigSet;
import it.unimi.dsi.fastutil.longs.LongSet;

public final class InternalIdMapTestHelper {

  private InternalIdMapTestHelper() {
    // Utility class
  }

  /**
   * Holds simple key test information.
   */
  public static class KeyTestInfo {
    public final long[] keys;
    public final long[] nonKeys;

    public KeyTestInfo(long[] keys, long[] nonKeys) {
      this.keys = keys;
      this.nonKeys = nonKeys;
    }
  }

  public static void testKeyRetrievals(
      LongToInternalIntBiMap map, int maxNumKeys, KeyTestInfo keyTestInfo) {
    int[] keyMaps = new int[maxNumKeys];
    // Put the keysAndValues in and check if we can retrieve them right away
    for (int i = 0; i < maxNumKeys; i++) {
      keyTestInfo.keys[i] = (long) i;
      keyMaps[i] = map.put(keyTestInfo.keys[i]);
      assertEquals((long) i, map.getKey(keyMaps[i]));
    }
    // Repeat! Put should always return the same value, no matter how many times we call it...
    for (int i = 0; i < maxNumKeys; i++) {
      keyTestInfo.keys[i] = (long) i;
      keyMaps[i] = map.put(keyTestInfo.keys[i]);
      assertEquals((long) i, map.getKey(keyMaps[i]));
    }
    // now retrieve these
    for (int i = 0; i < maxNumKeys; i++) {
      assertEquals(keyMaps[i], map.get(keyTestInfo.keys[i]));
    }
    // check that there are no false matches
    for (long nonKey : keyTestInfo.nonKeys) {
      assertEquals(-1, map.get(nonKey));
    }
  }

  public static KeyTestInfo generateSimpleKeys(int maxNumKeys) {
    long[] keys = new long[maxNumKeys];
    long[] nonKeys = new long[maxNumKeys];
    for (int i = 0; i < maxNumKeys; i++) {
      keys[i] = (long) i;
      nonKeys[i] = (long) i + maxNumKeys;
    }
    return new KeyTestInfo(keys, nonKeys);
  }

  public static KeyTestInfo generateRandomKeys(Random random, int maxNumKeys) {
    long[] keys = new long[maxNumKeys];
    long[] nonKeys = new long[maxNumKeys];
    LongSet keySet = new LongOpenHashBigSet(maxNumKeys);
    for (int i = 0; i < maxNumKeys; i++) {
      keys[i] = random.nextLong();
      keySet.add(keys[i]);
    }
    for (int i = 0; i < maxNumKeys; i++) {
      long nonKey;
      do {
        nonKey = random.nextLong();
      } while (keySet.contains(nonKey));
      nonKeys[i] = nonKey;
    }
    return new KeyTestInfo(keys, nonKeys);
  }
}
