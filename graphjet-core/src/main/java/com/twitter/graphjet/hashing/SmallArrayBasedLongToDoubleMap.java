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
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

/**
 * This class provides a map from long to double. It uses three primitive arrays to store long keys,
 * double values and long metadata. It only offers apis to get all keys or values or metadata at
 * once, and it does not support getting a specific key/value pair. The main purpose of this
 * implementation is to store a very small number of pairs. When a key/value pair is inserted into
 * the map, it scans through the keys array linearly and makes a decision whether to append the pair
 * or not. When the size of the map is equal to ADD_KEYS_TO_SET_THRESHOLD, it adds all keys and
 * metadata to a map and starts to use the map for dedupping.
 */
public class SmallArrayBasedLongToDoubleMap {

  /**
   * Holder class of a pair of key and metadata, which is used during dedupping when the size of the
   * map grows beyond ADD_KEYS_TO_SET_THRESHOLD.
   */
  private static final class Pair {
    private long key;
    private long metadata;

    public Pair(long key, long metadata) {
      this.key = key;
      this.metadata = metadata;
    }

    public long getKey() {
      return key;
    }

    public long getMetadata() {
      return metadata;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }

      if (obj == this) {
        return true;
      }

      if (obj.getClass() != getClass()) {
        return false;
      }

      Pair other = (Pair) obj;

      return (key == other.key) && (metadata == other.metadata);
    }

    @Override
    public int hashCode() {
      return (int)(key & metadata);
    }
  }

  private static final int ADD_KEYS_TO_SET_THRESHOLD = 8;
  private long[] keys;
  private double[] values;
  private long[] metadataArray;
  private int capacity;
  private int size;
  private int uniqueKeysSize;
  private LongSet keySet;
  private ObjectSet<Pair> keyMetadataPairSet;

  /**
   * Create a new empty array map.
   */
  public SmallArrayBasedLongToDoubleMap() {
    this.capacity = 4;
    this.size = 0;
    this.uniqueKeysSize = 0;
    this.keys = new long[capacity];
    this.values = new double[capacity];
    this.metadataArray = new long[capacity];
    this.keySet = null;
    this.keyMetadataPairSet = null;
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
   * Return the underlying primitive array of metadata.
   *
   * @return the underlying primitive array of metadata.
   */
  public long[] metadata() {
    return this.metadataArray;
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
   * Return the number of unique keys in the map.
   *
   * @return the number of unique keys in the map.
   */
  public int uniqueKeysSize() {
    return this.uniqueKeysSize;
  }

  /**
   * Add a tuple3 to the map.
   *
   * @param key the key.
   * @param value the value.
   * @param metadata the metadata.
   * @return true if no value present for the given pair of key and metadata, and false otherwise.
   */
  public boolean put(long key, double value, long metadata) {
    boolean isUniqueKey = true;

    // If the size of the array is less than ADD_KEYS_TO_SET_THRESHOLD, check against each element
    // in the array for dedupping.
    if (size < ADD_KEYS_TO_SET_THRESHOLD) {
      for (int i = 0; i < size; i++) {
        if (key == keys[i]) {
          isUniqueKey = false;
          if (metadata == metadataArray[i]) {
            return false;
          }
        }
      }
    } else {
      // If the size of the array is no less than ADD_KEYS_TO_SET_THRESHOLD, check against
      // keyMetadataPairSet for dedupping.
      if (keySet == null) {
        keySet = new LongOpenHashSet(keys, 0.75f /* load factor */);
        keyMetadataPairSet = new ObjectOpenHashSet<>();
        for (int i = 0; i < size; i++) {
          keyMetadataPairSet.add(new Pair(keys[i], metadataArray[i]));
        }
      }
      Pair pair = new Pair(key, metadata);

      if (keyMetadataPairSet.contains(pair)) {
        return false;
      } else {
        isUniqueKey = keySet.add(key);
        keyMetadataPairSet.add(pair);
      }
    }

    if (size == capacity) {
      capacity = 2 * capacity;
      copy(capacity, size);
    }

    if (isUniqueKey) uniqueKeysSize++;

    keys[size] = key;
    values[size] = value;
    metadataArray[size] = metadata;
    size++;
    return true;
  }

  /**
   * Sort keys, values and metadataArray in the order of decreasing values.
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
        long metadata1 = metadataArray[i1];
        keys[i1] = keys[i2];
        values[i1] = values[i2];
        metadataArray[i1] = metadataArray[i2];
        keys[i2] = key1;
        values[i2] = value1;
        metadataArray[i2] = metadata1;
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
    int newCapacity = Math.min(inputCapacity, size);

    if (newCapacity < capacity) {
      capacity = newCapacity;
      size = newCapacity;
      uniqueKeysSize = Math.min(newCapacity, uniqueKeysSize);
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
   * Copy keys, values and metadataArray to new arrays.
   *
   * @param newLength the length of new arrays.
   * @param length the number of entries to be copied to new arrays.
   */
  private void copy(int newLength, int length) {
    long[] newKeys = new long[newLength];
    double[] newValues = new double[newLength];
    long[] newMetadataArray = new long[newLength];
    System.arraycopy(keys, 0, newKeys, 0, length);
    System.arraycopy(values, 0, newValues, 0, length);
    System.arraycopy(metadataArray, 0, newMetadataArray, 0, length);
    keys = newKeys;
    values = newValues;
    metadataArray = newMetadataArray;
  }
}
