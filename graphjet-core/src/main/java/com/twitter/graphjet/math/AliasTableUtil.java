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

import static com.twitter.graphjet.math.IntArrayAliasTable.getAliasTableAverageWeight;
import static com.twitter.graphjet.math.IntArrayAliasTable.getAliasTableSize;
import static com.twitter.graphjet.math.IntArrayAliasTable.getEntry;
import static com.twitter.graphjet.math.IntArrayAliasTable.getNextEntry;
import static com.twitter.graphjet.math.IntArrayAliasTable.getWeight;
import static com.twitter.graphjet.math.IntArrayAliasTable.setNextEntry;
import static com.twitter.graphjet.math.IntArrayAliasTable.setWeight;

/**
 * Constructs the Walker Alias Table for repeated random weighted sampling with replacement from a
 * weighted list. This is a linear time construction that looks at each element at most twice.
 * The random number generation is constant time.
 */
public final class AliasTableUtil {

  private AliasTableUtil() {
    // Utility class
  }

  /**
   * This function constructs the alias table and runs in <= 2*n time where n is the size of the
   * input list. Note that the index of entries in the alias table correspond exactly to the
   * original list in order to enable storing payloads in the alias table.
   */
  public static void constructAliasTable(int[] aliasTableArray) {
    int aliasTableSize = getAliasTableSize(aliasTableArray);
    int averageWeight = getAliasTableAverageWeight(aliasTableArray);

    // TODO(aneesh): both these arrays can be captured in a single array
    int[] belowAverageIndices = new int[aliasTableSize];
    int numBelowAverage = 0;
    int[] aboveAverageIndices = new int[aliasTableSize];
    int numAboveAverage = 0;

    // First pass through the list
    for (int i = 0; i < aliasTableSize; i++) {
      int weight = getWeight(aliasTableArray, i);
      if (weight == averageWeight) {
        setNextEntry(aliasTableArray, i, -1);
      } else if (weight < averageWeight) {
        if (numAboveAverage > 0) {
          int donatingItemIndex = aboveAverageIndices[--numAboveAverage];
          setNextEntry(aliasTableArray, i, getEntry(aliasTableArray, donatingItemIndex));
          int newWeight =
              getWeight(aliasTableArray, donatingItemIndex) - (averageWeight - weight);
          setWeight(aliasTableArray, donatingItemIndex, newWeight);
          if (newWeight == averageWeight) {
            setNextEntry(aliasTableArray, donatingItemIndex, -1);
          } else if (newWeight < averageWeight) {
            belowAverageIndices[numBelowAverage++] = donatingItemIndex;
          } else {
            aboveAverageIndices[numAboveAverage++] = donatingItemIndex;
          }
        } else {
          belowAverageIndices[numBelowAverage++] = i;
        }
      } else {
        aboveAverageIndices[numAboveAverage++] = i;
      }
    }

    // This is for balancing the below average elements, but due to limited precision there might
    // be some remaining elements
    // TODO (aneesh): convert to for loop
    while (numBelowAverage > 0 && numAboveAverage > 0) {
      int entryIndex = belowAverageIndices[--numBelowAverage];
      int weight = getWeight(aliasTableArray, entryIndex);
      int donatingElementIndex = aboveAverageIndices[--numAboveAverage];
      setNextEntry(aliasTableArray, entryIndex, getEntry(aliasTableArray, donatingElementIndex));
      int newWeight =
          getWeight(aliasTableArray, donatingElementIndex) - (averageWeight - weight);
      setWeight(aliasTableArray, donatingElementIndex, newWeight);
      if (newWeight == averageWeight) {
        setNextEntry(aliasTableArray, donatingElementIndex, -1);
      } else if (newWeight < averageWeight) {
        belowAverageIndices[numBelowAverage++] = donatingElementIndex;
      } else {
        aboveAverageIndices[numAboveAverage++] = donatingElementIndex;
      }
    }

    // Due to limited precision, it is possible we get here, so we do the final cleanup
    for (int i = numBelowAverage - 1; i >= 0; i--) {
      int index = belowAverageIndices[i];
      setWeight(aliasTableArray, index, averageWeight);
      setNextEntry(aliasTableArray, index, -1);
    }
    for (int i = numAboveAverage - 1; i >= 0; i--) {
      int index = aboveAverageIndices[i];
      setWeight(aliasTableArray, index, averageWeight);
      setNextEntry(aliasTableArray, index, -1);
    }
  }

  /**
   * This function allows a client to get a random sample from the input list generated according
   * to the weight of the element in the input list. Remember that this is weighted random sampling
   * with replacement.
   *
   * A crucial property of the alias table is that this function is O(1), and actually requires
   * just two calls to a random number generator! Ask me for a proof if you're curious :)
   *
   * This particular incarnation of the sampling function allows sampling from an arbitrary alias
   * table list type as long as we have random access into the table and can retrieve the required
   * alias table data.
   *
   * @param aliasTableArray  is the alias table to sample using
   * @param random           is used for generating random numbers
   * @return a sampled entry from the alias table
   */
  public static int getRandomSampleFromAliasTable(int[] aliasTableArray, Random random) {
    if (aliasTableArray == null) {
      return -1;
    }
    int randomIndex = random.nextInt(getAliasTableSize(aliasTableArray));
    int randomWeight = random.nextInt(getAliasTableAverageWeight(aliasTableArray));
    if (randomWeight < getWeight(aliasTableArray, randomIndex)) {
      return getEntry(aliasTableArray, randomIndex);
    } else {
      return getNextEntry(aliasTableArray, randomIndex);
    }
  }
}
