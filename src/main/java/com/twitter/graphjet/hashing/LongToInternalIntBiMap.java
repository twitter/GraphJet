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

/**
 * This interface specifies the contract for a bi-directional long -> int map. This differs from
 * usual maps in that the given long keys are mapped _internally_ to some ints where the mapping is
 * decided internally, and CANNOT be controlled by the client. Also, the map is bi-directional but
 * note that only long keys can be inserted into the map. Hence, long keys are referred to as keys
 * and int values are referred to as values.
 */
public interface LongToInternalIntBiMap {

  /**
   * Retrieves the internal int mapping for a long key.
   *
   * @param key is the long key to look for in the map.
   * @return the mapped internal value for this key, if present, or a default return value.
   */
  int get(long key);

  /**
   * Inserts a long key and retrieves the internal mapped int value.
   *
   * @param key is the long key to insert in the map.
   * @return the mapped internal value for this key.
   */
  int put(long key);

  /**
   * Retrieves the long key for a given value.
   *
   * @param value is the int to look for in the map.
   * @return the corresponding key for this value, if present, or a default return value.
   */
  long getKey(int value);

  /**
   * Clears the state of the map and re-initializes for use. This method is NOT thread-safe!
   */
  void clear();
}
