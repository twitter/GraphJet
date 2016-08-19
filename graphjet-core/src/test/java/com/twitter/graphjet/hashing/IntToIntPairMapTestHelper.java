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

import it.unimi.dsi.fastutil.ints.IntOpenHashBigSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public final class IntToIntPairMapTestHelper {

  private IntToIntPairMapTestHelper() {
    // Utility class
  }

  /**
   * Holds simple key test information.
   */
  public static class KeyTestInfo {
    public final int[] keysAndValues;
    public final int[] nonKeys;

    public KeyTestInfo(int[] keysAndValues, int[] nonKeys) {
      this.keysAndValues = keysAndValues;
      this.nonKeys = nonKeys;
    }
  }

  public static void testKeyRetrievals(IntToIntPairHashMap map, KeyTestInfo keyTestInfo) {
    // Put the keysAndValues in and check if we can retrieve them right away
    for (int i = 0; i < keyTestInfo.keysAndValues.length / 3; i++) {
      int key = keyTestInfo.keysAndValues[i * 3];
      int firstValue = keyTestInfo.keysAndValues[i * 3 + 1];
      int secondValue = keyTestInfo.keysAndValues[i * 3 + 2];
      map.put(key, firstValue, secondValue);
      assertEquals(firstValue, map.getFirstValue(key));
      assertEquals(secondValue, map.getSecondValue(key));
    }
    // Repeat! Put should always return the same value, no matter how many times we call it...
    for (int i = 0; i < keyTestInfo.keysAndValues.length / 3; i++) {
      int key = keyTestInfo.keysAndValues[i * 3];
      int firstValue = keyTestInfo.keysAndValues[i * 3 + 1];
      int secondValue = keyTestInfo.keysAndValues[i * 3 + 2];
      map.put(key, firstValue, secondValue);
    }
    // now retrieve these
    for (int i = 0; i < keyTestInfo.keysAndValues.length / 3; i++) {
      int key = keyTestInfo.keysAndValues[i * 3];
      int firstValue = keyTestInfo.keysAndValues[i * 3 + 1];
      int secondValue = keyTestInfo.keysAndValues[i * 3 + 2];
      assertEquals(firstValue, map.getFirstValue(key));
      assertEquals(secondValue, map.getSecondValue(key));
      // test increments
      map.incrementFirstValue(key);
      assertEquals(firstValue + 1, map.getFirstValue(key));
      map.incrementSecondValue(key, 1);
      assertEquals(secondValue + 1, map.getSecondValue(key));
    }
    // check that there are no false matches
    for (int nonKey : keyTestInfo.nonKeys) {
      assertEquals(-1, map.getFirstValue(nonKey));
      assertEquals(-1, map.getSecondValue(nonKey));
      assertEquals(-1, map.getBothValues(nonKey));
    }
  }

  public static KeyTestInfo generateSimpleKeys(int maxNumKeys) {
    int[] keysAndValues = new int[maxNumKeys * 3];
    int[] nonKeys = new int[maxNumKeys];
    for (int i = 0; i < maxNumKeys; i++) {
      keysAndValues[i * 3] = i;
      keysAndValues[i * 3 + 1] = i * 3 + 1;
      keysAndValues[i * 3 + 2] = i * 3 + 2;
      nonKeys[i] = i + maxNumKeys;
    }
    return new KeyTestInfo(keysAndValues, nonKeys);
  }

  public static KeyTestInfo generateRandomKeys(Random random, int maxNumKeys) {
    int maxKeyOrValue = maxNumKeys << 2;
    int[] keysAndValues = new int[maxNumKeys * 3];
    int[] nonKeys = new int[maxNumKeys];
    IntSet keySet = new IntOpenHashBigSet(maxNumKeys);
    for (int i = 0; i < maxNumKeys; i++) {
      int entry;
      do {
        entry = random.nextInt(maxKeyOrValue);
      } while (keySet.contains(entry));
      keysAndValues[i * 3] = entry;
      keysAndValues[i * 3 + 1] = random.nextInt(maxKeyOrValue);
      keysAndValues[i * 3 + 2] = random.nextInt(maxKeyOrValue);
      keySet.add(entry);
    }
    for (int i = 0; i < maxNumKeys; i++) {
      int nonKey;
      do {
        nonKey = random.nextInt(maxKeyOrValue);
      } while (keySet.contains(nonKey));
      nonKeys[i] = nonKey;
    }
    return new KeyTestInfo(keysAndValues, nonKeys);
  }
}
