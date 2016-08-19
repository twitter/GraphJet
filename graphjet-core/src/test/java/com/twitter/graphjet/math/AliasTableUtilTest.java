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

import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import static com.twitter.graphjet.math.AliasTableUtil.constructAliasTable;
import static com.twitter.graphjet.math.AliasTableUtil.getRandomSampleFromAliasTable;

public class AliasTableUtilTest {
  private static final double PRECISION = 0.01;

  @Test
  public void testTableConstruction() {
    long seed = 895743857349L;
    int listSize = 100;
    int numSamples = 10000;
    int maxWeight = 1000;

    Random random = new Random(seed);

    // Check a simple list first
    int[] simpleWeightsAliasTable = IntArrayAliasTable.generateAliasTableArray(listSize);
    int sumWeights = 0;
    for (int i = 0; i < listSize; i++) {
      IntArrayAliasTable.setEntry(simpleWeightsAliasTable, i, i);
      int weight = i + 1;
      IntArrayAliasTable.setWeight(simpleWeightsAliasTable, i, weight);
      sumWeights += weight;
    }

    IntArrayAliasTable.setAliasTableSize(simpleWeightsAliasTable, listSize);
    IntArrayAliasTable.setAliasTableAverageWeight(simpleWeightsAliasTable, sumWeights / listSize);

    constructAndCheckAliasTable(simpleWeightsAliasTable, random, numSamples);

    // Now check a random list
    int[] randomWeightsAliasTable = IntArrayAliasTable.generateAliasTableArray(listSize);
    sumWeights = 0;
    for (int i = 0; i < listSize; i++) {
      IntArrayAliasTable.setEntry(randomWeightsAliasTable, i, i);
      int weight = random.nextInt(maxWeight);
      IntArrayAliasTable.setWeight(randomWeightsAliasTable, i, weight);
      sumWeights += weight;
    }

    IntArrayAliasTable.setAliasTableSize(randomWeightsAliasTable, listSize);
    IntArrayAliasTable.setAliasTableAverageWeight(randomWeightsAliasTable, sumWeights / listSize);

    constructAndCheckAliasTable(randomWeightsAliasTable, random, numSamples);
  }

  private void constructAndCheckAliasTable(
      int[] aliasTableArray, Random random, int numSamples) {
    Int2DoubleMap expectedWeights =
        new Int2DoubleOpenHashMap(IntArrayAliasTable.getAliasTableSize(aliasTableArray));
    double totalWeight = IntArrayAliasTable.getAliasTableAverageWeight(aliasTableArray)
        * IntArrayAliasTable.getAliasTableSize(aliasTableArray);
    for (int i = 0; i < IntArrayAliasTable.getAliasTableSize(aliasTableArray); i++) {
      expectedWeights.put(
          IntArrayAliasTable.getEntry(aliasTableArray, i),
          IntArrayAliasTable.getWeight(aliasTableArray, i) / totalWeight);
    }

    constructAliasTable(aliasTableArray);

    // Generate some samples
    Int2IntMap sampledData =
        new Int2IntOpenHashMap(IntArrayAliasTable.getAliasTableSize(aliasTableArray));
    for (int i = 0; i < numSamples; i++) {
      int sample = getRandomSampleFromAliasTable(aliasTableArray, random);
      sampledData.put(sample, sampledData.get(sample) + 1);
    }

    // Test that sampled data matches the actual weight
    for (int i : sampledData.keySet()) {
      assertEquals(expectedWeights.get(i), sampledData.get(i) / (double) numSamples, PRECISION);
    }
  }
}
