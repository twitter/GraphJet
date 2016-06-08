package com.twitter.graphjet.hashing;

/**
 * A type-specific map that stores a map from an int key to a pair of int values.
 */
public interface IntToIntPairHashMap {

  /**
   * Inserts a key into the map. If the key exists, it's values are replaced by the new values.
   *
   * @param key          is the key to insert in the map
   * @param firstValue   is the first value to store for the key
   * @param secondValue  is the second value to store for the key
   * @return true if the key already existed in the map, false otherwise.
   */
  boolean put(int key, int firstValue, int secondValue);

  /**
   * Retrieves the first stored value for a given key.
   *
   * @param key is the key to look for in the map.
   * @return the first stored value for this key if present, and a default return value otherwise.
   */
  int getFirstValue(int key);

  /**
   * Retrieves the second stored value for a given key.
   *
   * @param key is the key to look for in the map.
   * @return the second stored value for this key if present, and a default return value otherwise.
   */
  int getSecondValue(int key);

  /**
   * Returns both values for a key encoded in a single long for efficiency, with the first 32 bits
   * being the first value and the second 32 bits being the second value.
   *
   * @param key is the key to look for in the map.
   * @return the two stored values for this key encoded as a long (if present), and a default
   * return value otherwise.
   */
  long getBothValues(int key);

  /**
   * Increments the first value for a key that already exists in the map.
   *
   * @param key    is the key whose values are being updated
   * @return the updated first value if the update succeeded, a default return value if not
   *         (such as when the key didn't already exist)
   */
  int incrementFirstValue(int key);

  /**
   * Increments the second value for a key that already exists in the map.
   *
   * @param key    is the key whose values are being updated
   * @param delta  is the change in the values associated with the key
   * @return the updated second value if the update succeeded, a default return value if not
   *         (such as when the key didn't already exist)
   */
  int incrementSecondValue(int key, int delta);

  /**
   * Clears the state of the map and re-initializes for use. This method is NOT thread-safe!
   */
  void clear();
}
