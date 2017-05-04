/**
 * Copyright 2017 Twitter. All rights reserved.
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
 * Allows manipulating a data structure as a long array. A client can store entries in an
 * arbitrary position in this array, and increment positions as they normally would for a regular
 * one-dimensional array. All operations here are expected to be constant time lookups.
 */
public interface BigLongArray {
  /**
   * Adds an entry to the array at a specific desired position. Note that this would over-write any
   * existing value.
   *
   * @param entry     is the entry to add
   * @param position  is the position where to put the entry
   */
  void addEntry(long entry, int position);

  /**
   * Fetches the stored entry at a position.
   *
   * @param position  is the position to look at
   * @return the stored entry.
   */
  long getEntry(int position);

  /**
   * Increments the stored entry at a position by delta.
   *
   * @param position  is the position to look at
   * @param delta  is the change in the value associated with the position
   * @return null entry if the position is not already filled, or the new value otherwise.
   */
  long incrementEntry(int position, long delta);

  /**
   * Batch add array elements in {@link BigLongArray}.
   *
   * @param src the source array
   * @param srcPos the starting position in the source array
   * @param desPos the starting position in {@link BigLongArray}
   * @param length the number of array elements to be copied
   * @param updateStats whether to update internal stats or not
   */
  void arrayCopy(long[] src, int srcPos, int desPos, int length, boolean updateStats);

  /**
   * The fill percentage is the percentage of memory allocated that is being occupied. This should
   * be very cheap to get and will be exported as a stat counter.
   *
   * @return the fill percentage
   */
  double getFillPercentage();

  /**
   * Resets all the memory. Doesn't actually free it, but resets it.
   */
  void reset();
}
