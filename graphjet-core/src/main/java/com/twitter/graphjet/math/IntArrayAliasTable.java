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


package com.twitter.graphjet.math;

import java.util.Arrays;

/**
 * An entry in the alias table is three numbers: the original inputList index, a weight and a
 * jump pointer into the inputList. Here, we use  the convention that weights are [0,1] for fast
 * comparison with a uniform random number.
 *
 * NOTE: This assumes that the aliasTable entry order is ensured to be the same as the inputList
 * order!
 */
public final class IntArrayAliasTable {
  // Reserve space at the front of the table to store some global information
  private static final int PADDING = 2;

  private IntArrayAliasTable() {
    // Utility class
  }

  public static int[] generateAliasTableArray(int numEntries) {
    return new int[PADDING + (numEntries * 3)];
  }

  public static void clearAliasTableArray(int[] aliasTableArray) {
    Arrays.fill(aliasTableArray, 0);
  }

  public static int getAliasTableSize(int[] aliasTableArray) {
    return aliasTableArray[0];
  }

  public static void setAliasTableSize(int[] aliasTableArray, int size) {
    aliasTableArray[0] = size;
  }

  public static int getAliasTableAverageWeight(int[] aliasTableArray) {
    return aliasTableArray[1];
  }

  public static void setAliasTableAverageWeight(int[] aliasTableArray, int averageWeight) {
    aliasTableArray[1] = averageWeight;
  }

  public static void setEntry(int[] aliasTableArray, int index, int entry) {
    aliasTableArray[PADDING + (index * 3)] = entry;
  }

  public static int getEntry(int[] aliasTableArray, int index) {
    return aliasTableArray[PADDING + (index * 3)];
  }

  public static int getWeight(int[] aliasTableArray, int index) {
    return aliasTableArray[PADDING + (index * 3) + 1];
  }

  public static void setWeight(int[] aliasTableArray, int index, int weight) {
    aliasTableArray[PADDING + (index * 3) + 1] = weight;
  }

  public static int getNextEntry(int[] aliasTableArray, int index) {
    return aliasTableArray[PADDING + (index * 3) + 2];
  }

  public static void setNextEntry(int[] aliasTableArray, int index, int nextEntry) {
    aliasTableArray[PADDING + (index * 3) + 2] = nextEntry;
  }
}
