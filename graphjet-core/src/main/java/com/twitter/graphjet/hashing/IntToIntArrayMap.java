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

import com.twitter.graphjet.bipartite.api.ReusableNodeIntIterator;

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * This interface specifies the contract for an int -> int[] map.
 */
public interface IntToIntArrayMap {
  /**
   * Retrieves the IntIterator for an int key.
   *
   * @param key is the int key to look for in the map.
   * @return the int array for this key, if present, or a null object.
   */
  IntIterator get(int key);

  /**
   * Retrieves the IntIterator for an int key.
   *
   * @param key is the int key to look for in the map.
   * @param reusableNodeIntIterator  is the iterator to reset and reuse for returning int array
   * @return the reusableNodeIntIterator iterator that has been reset to point to the int array of
   *         this key
   */
  IntIterator get(int key, ReusableNodeIntIterator reusableNodeIntIterator);

  /**
   * Retrieves the number of value items associated with a given key.
   *
   * @param key is the int key to look for in the map.
   * @return the length of value array for this key, if present, or 0.
   */
  int getArrayLength(int key);

  /**
   * Inserts an int key along with its int array value.
   * If the specified key already exists in the map, it will discard the value and return false
   * immediately without modifying the map.
   * If the specified value is null, it will return false immediately without modifying the map.
   *
   * @param key is the int key to insert in the map.
   * @param value the int array value for this key.
   * @return a boolean flag indicating whether operation succeeded or not
   */
  boolean put(int key, int[] value);


  boolean incrementFeatureValue(int key, byte featureType);
}
