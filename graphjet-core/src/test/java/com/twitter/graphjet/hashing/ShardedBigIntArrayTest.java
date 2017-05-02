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

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.ints.IntIterator;

public class ShardedBigIntArrayTest {

  protected static final org.slf4j.Logger LOG = LoggerFactory.getLogger("graph");

  @Test
  public void testSequentialReadWrites() {
    int maxNumNodes = 1 << 16;
    int shardSize = 1 << 10;
    int nullEntry = -1;
    ShardedBigIntArray shardedBigIntArray = new ShardedBigIntArray(
        maxNumNodes / 16, shardSize, 0, nullEntry, new NullStatsReceiver());

    for (int i = 0; i < maxNumNodes; i++) {
      int entry = i * 2;
      assertEquals(nullEntry, shardedBigIntArray.getEntry(i));
      shardedBigIntArray.addEntry(entry, i);
      assertEquals(entry, shardedBigIntArray.getEntry(i));
    }

    for (int i = 0; i < maxNumNodes; i++) {
      assertEquals(nullEntry, shardedBigIntArray.getEntry(maxNumNodes + i));
    }
  }


  @Test
  public void testRandomReadWrites() {
    int maxNumNodes = 1 << 16;
    int shardSize = 1 << 10;
    int nullEntry = -1;
    List<Integer> indexList = Lists.newArrayListWithCapacity(maxNumNodes);
    ShardedBigIntArray shardedBigIntArray = new ShardedBigIntArray(
        maxNumNodes / 16, shardSize, 0, nullEntry, new NullStatsReceiver());

    for (int i = 0; i < maxNumNodes; i++) {
      indexList.add(i);
    }

    Collections.shuffle(indexList);
    for (Integer index : indexList) {
      int entry = index * 2;
      assertEquals(nullEntry, shardedBigIntArray.getEntry(index));
      shardedBigIntArray.addEntry(entry, index);
      assertEquals(entry, shardedBigIntArray.getEntry(index));
    }

    for (int i = 0; i < maxNumNodes; i++) {
      assertEquals(i * 2, shardedBigIntArray.getEntry(i));
    }

    for (int i = 0; i < maxNumNodes; i++) {
      assertEquals(nullEntry, shardedBigIntArray.getEntry(maxNumNodes + i));
    }
  }

  @Test
  public void testSequentialReadWritesWithMetadataOne() {
    int maxNumNodes = 1 << 18;
    int shardSize = 1 << 10;
    int nullEntry = -1;
    ShardedBigIntArray shardedBigIntArray = new ShardedBigIntArray(
      maxNumNodes / 16, shardSize, 2, nullEntry, new NullStatsReceiver());

    for (int i = 0; i < maxNumNodes; i++) {
      int entry = i * 2;
      int[] metadata = {entry + 1, entry + 2};
      assertEquals(nullEntry, shardedBigIntArray.getEntry(i));
      shardedBigIntArray.addEntry(entry, i, metadata);
      assertEquals(entry, shardedBigIntArray.getEntry(i));
      IntIterator metadataIterator = shardedBigIntArray.getMetadata(i);
      assertEquals(metadata[0], metadataIterator.nextInt());
      assertEquals(metadata[1], metadataIterator.nextInt());
    }

    for (int i = 0; i < maxNumNodes; i++) {
      assertEquals(nullEntry, shardedBigIntArray.getEntry(maxNumNodes + i));
    }
  }

  @Test
  public void testSequentialReadWritesWithMetadataTwo() {
    int maxNumNodes = 1 << 18;
    int shardSize = 1 << 10;
    int nullEntry = -1;
    ShardedBigIntArray shardedBigIntArray = new ShardedBigIntArray(
      maxNumNodes / 16, shardSize, 3, nullEntry, new NullStatsReceiver());

    for (int i = 0; i < maxNumNodes; i++) {
      int entry = i * 2;
      int[] metadata = {entry + 1, entry + 2, entry + 3};
      assertEquals(nullEntry, shardedBigIntArray.getEntry(i));
      shardedBigIntArray.addEntry(entry, i, metadata);
      assertEquals(entry, shardedBigIntArray.getEntry(i));
      IntIterator metadataIterator = shardedBigIntArray.getMetadata(i);
      assertEquals(metadata[0], metadataIterator.nextInt());
      assertEquals(metadata[1], metadataIterator.nextInt());
      assertEquals(metadata[2], metadataIterator.nextInt());
    }

    for (int i = 0; i < maxNumNodes; i++) {
      assertEquals(nullEntry, shardedBigIntArray.getEntry(maxNumNodes + i));
    }
  }

  @Test
  public void testRandomReadWritesWithMetadataOne() {
    int maxNumNodes = 1 << 16;
    int shardSize = 1 << 10;
    int nullEntry = -1;
    List<Integer> indexList = Lists.newArrayListWithCapacity(maxNumNodes);
    ShardedBigIntArray shardedBigIntArray = new ShardedBigIntArray(
      maxNumNodes / 16, shardSize, 2, nullEntry, new NullStatsReceiver());

    for (int i = 0; i < maxNumNodes; i++) {
      indexList.add(i);
    }

    Collections.shuffle(indexList);
    for (Integer index : indexList) {
      int entry = index * 2;
      int[] metadata = {entry + 1, entry + 2};
      assertEquals(nullEntry, shardedBigIntArray.getEntry(index));
      shardedBigIntArray.addEntry(entry, index, metadata);
      assertEquals(entry, shardedBigIntArray.getEntry(index));
      IntIterator metadataIterator = shardedBigIntArray.getMetadata(index);
      assertEquals(metadata[0], metadataIterator.nextInt());
      assertEquals(metadata[1], metadataIterator.nextInt());
    }

    for (int i = 0; i < maxNumNodes; i++) {
      assertEquals(i * 2, shardedBigIntArray.getEntry(i));
    }

    for (int i = 0; i < maxNumNodes; i++) {
      assertEquals(nullEntry, shardedBigIntArray.getEntry(maxNumNodes + i));
    }
  }
}
