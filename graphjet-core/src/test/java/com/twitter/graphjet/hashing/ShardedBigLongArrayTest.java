package com.twitter.graphjet.hashing;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.twitter.graphjet.stats.NullStatsReceiver;

public class ShardedBigLongArrayTest {
  @Test
  public void testSequentialReadWrites() {
    int maxNumNodes = 1 << 16;
    int shardSize = 1 << 10;
    long nullEntry = -1L;
    BigLongArray shardedBigLongArray = new ShardedBigLongArray(
      maxNumNodes / 16, shardSize, nullEntry, new NullStatsReceiver());

    for (int i = 0; i < maxNumNodes; i++) {
      long entry = i * 2;
      assertEquals(nullEntry, shardedBigLongArray.getEntry(i));
      shardedBigLongArray.addEntry(entry, i);
      assertEquals(entry, shardedBigLongArray.getEntry(i));
    }

    for (int i = 0; i < maxNumNodes; i++) {
      assertEquals(nullEntry, shardedBigLongArray.getEntry(maxNumNodes + i));
    }
  }


  @Test
  public void testIntegerRandomReadWrites() {
    int maxNumNodes = 1 << 16;
    int shardSize = 1 << 10;
    long nullEntry = -1L;
    List<Integer> indexList = Lists.newArrayListWithCapacity(maxNumNodes);
    BigLongArray shardedBigLongArray = new ShardedBigLongArray(
      maxNumNodes / 16, shardSize, nullEntry, new NullStatsReceiver());

    for (int i = 0; i < maxNumNodes; i++) {
      indexList.add(i);
    }

    Collections.shuffle(indexList);
    for (Integer index : indexList) {
      long entry = index * 2;
      assertEquals(nullEntry, shardedBigLongArray.getEntry(index));
      shardedBigLongArray.addEntry(entry, index);
      assertEquals(entry, shardedBigLongArray.getEntry(index));
    }

    for (int i = 0; i < maxNumNodes; i++) {
      assertEquals(i * 2, shardedBigLongArray.getEntry(i));
    }

    for (int i = 0; i < maxNumNodes; i++) {
      assertEquals(nullEntry, shardedBigLongArray.getEntry(maxNumNodes + i));
    }
  }

  @Test
  public void testLongRandomReadWrites() {
    int maxNumNodes = 1 << 16;
    int shardSize = 1 << 10;
    long nullEntry = -1L;
    List<Integer> indexList = Lists.newArrayListWithCapacity(maxNumNodes);
    BigLongArray shardedBigLongArray = new ShardedBigLongArray(
      maxNumNodes / 16, shardSize, nullEntry, new NullStatsReceiver());

    for (int i = 0; i < maxNumNodes; i++) {
      indexList.add(i);
    }

    Collections.shuffle(indexList);
    for (Integer index : indexList) {
      long entry = index * Integer.MAX_VALUE;
      assertEquals(nullEntry, shardedBigLongArray.getEntry(index));
      shardedBigLongArray.addEntry(entry, index);
      assertEquals(entry, shardedBigLongArray.getEntry(index));
    }

    for (int i = 0; i < maxNumNodes; i++) {
      assertEquals(i * Integer.MAX_VALUE, shardedBigLongArray.getEntry(i));
    }

    for (int i = 0; i < maxNumNodes; i++) {
      assertEquals(nullEntry, shardedBigLongArray.getEntry(maxNumNodes + i));
    }
  }
}
