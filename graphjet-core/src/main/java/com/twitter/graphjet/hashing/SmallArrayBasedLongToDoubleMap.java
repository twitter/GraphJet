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

import it.unimi.dsi.fastutil.Arrays;
import it.unimi.dsi.fastutil.Swapper;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

/**
 * This class provides a map from long to double. It uses two primitive arrays to store long keys
 * and double values separately. It only offers apis to get all keys or values at once, and it does
 * not support getting a specific key/value pair. The main purpose of this implementation is to
 * store a very small number of pairs. When a key/value pair is inserted into the map, it scans
 * through the keys array linearly and makes a decision whether to append the pair or not. When the
 * size of the map is equal to ADD_KEYS_TO_SET_THRESHOLD, it adds all keys to a set and starts to
 * use the set for dedupping.
 */
public class SmallArrayBasedLongToDoubleMap {
  private static final int ADD_KEYS_TO_SET_THRESHOLD = 8;
  private long[] keys;
  private double[] values;
  private int capacity;
  private int size;
  private LongSet keySet;

  /**
   * Create a new empty array map.
   */
  public SmallArrayBasedLongToDoubleMap() {
    this.capacity = 4;
    this.size = 0;
    this.keys = new long[capacity];
    this.values = new double[capacity];
    this.keySet = null;
  }

  /**
   * Return the underlying primitive array of keys.
   *
   * @return the underlying primitive array of keys.
   */
  public long[] keys() {
    return this.keys;
  }

  /**
   * Return the underlying primitive array of values.
   *
   * @return the underlying primitive array of values.
   */
  public double[] values() {
    return this.values;
  }

  /**
   * Return the size of the map.
   *
   * @return the size of the map.
   */
  public int size() {
    return this.size;
  }

  /**
   * Add a pair to the map.
   *
   * @param key the key.
   * @param value the value.
   * @return true if no value present for the giving key, and false otherwise.
   */
  public boolean put(long key, double value) {
    if (size < ADD_KEYS_TO_SET_THRESHOLD) {
      for (int i = 0; i < size; i++) {
        if (key == keys[i]) {
          return false;
        }
      }
    } else {
      if (keySet == null) {
        keySet = new LongOpenHashSet(keys, 0.75f /* load factor */);
      }
      if (keySet.contains(key)) {
        return false;
      } else {
        keySet.add(key);
      }
    }

    if (size == capacity) {
      capacity = 2 * capacity;
      copy(capacity, size);
    }

    keys[size] = key;
    values[size] = value;
    size++;

    return true;
  }

  /**
   * Sort both keys and values in the order of decreasing values.
   */
  public void sort() {
    Arrays.quickSort(0, size, new IntComparator() {
      @Override
      // sort both keys and values in the order of decreasing values
      public int compare(int i1, int i2) {
        double val1 = values[i1];
        double val2 = values[i2];
        if (val1 < val2) {
          return 1;
        }  else if (val1 > val2) {
          return -1;
        }
        return 0;
      }

      @Override
      public int compare(Integer integer1, Integer integer2) {
        throw new UnsupportedOperationException();
      }
    }, new Swapper() {
      @Override
      public void swap(int i1, int i2) {
        long key1 = keys[i1];
        double value1 = values[i1];
        keys[i1] = keys[i2];
        values[i1] = values[i2];
        keys[i2] = key1;
        values[i2] = value1;
      }
    });
  }

  /**
   * Trim the capacity of this map instance to min(inputCapacity, size). Clients can use this
   * method to minimize the storage of this map.
   *
   * @param inputCapacity the input capacity that clients want to trim the map to
   * @return true if the capacity of the map is trimmed down, and false otherwise.
   */
  public boolean trim(int inputCapacity) {
    int newCapacity = inputCapacity > size ? size : inputCapacity;

    if (newCapacity < capacity) {
      capacity = newCapacity;
      size = newCapacity;
      copy(newCapacity, newCapacity);
      return true;
    } else {
      return false;
    }
  }

  /**
   * @param key the input capacity that clients want to trim the map to
   * @return true if the key is in the Map.
   *
   * Note: Use this function with caution since it is linear to ADD_KEYS_TO_SET_THRESHOLD.
   */
  public boolean contains(long key) {
    // The size might have reached the ADD_KEYS_TO_SET_THRESHOLD, but we may have not inserted
    // a new key yet. Therefore, we need to also check if the size is equal to that threshold.
    if (size <= ADD_KEYS_TO_SET_THRESHOLD) {
      for (int i = 0; i < size; i++) {
        if (key == keys[i]) {
          return true;
        }
      }
    } else if (keySet.contains(key)) {
      return true;
    }

    return false;
  }

  /**
   * Copy keys and values to new arrays.
   *
   * @param newLength the length of new arrays.
   * @param length the number of entries to be copied to new arrays.
   */
  private void copy(int newLength, int length) {
    long[] newKeys = new long[newLength];
    double[] newValues = new double[newLength];
    System.arraycopy(keys, 0, newKeys, 0, length);
    System.arraycopy(values, 0, newValues, 0, length);
    keys = newKeys;
    values = newValues;
  }
}
