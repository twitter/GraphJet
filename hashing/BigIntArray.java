package com.twitter.graphjet.hashing;

/**
 * Allows manipulating a data structure as an integer array. A client can store entries in an
 * arbitrary position in this array, and increment positions as they normally would for a regular
 * one-dimensional array. All operations here are expected to be constant time lookups.
 */
public interface BigIntArray {
  /**
   * Adds an entry to the array at a specific desired position. Note that this would over-write any
   * existing value.
   *
   * @param entry     is the entry to add
   * @param position  is the position where to put the entry
   */
  void addEntry(int entry, int position);

  /**
   * Fetches the stored entry at a position.
   *
   * @param position  is the position to look at
   * @return the stored entry.
   */
  int getEntry(int position);

  /**
   * Increments the stored entry at a position by delta.
   *
   * @param position  is the position to look at
   * @param delta  is the change in the value associated with the position
   * @return null entry if the position is not already filled, or the new value otherwise.
   */
  int incrementEntry(int position, int delta);

  /**
   * Batch add array elements in {@link BigIntArray}.
   *
   * @param src the source array
   * @param srcPos the starting position in the source array
   * @param desPos the starting position in {@link BigIntArray}
   * @param length the number of array elements to be copied
   * @param updateStats whether to update internal stats or not
   */
  void arrayCopy(int[] src, int srcPos, int desPos, int length, boolean updateStats);

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
