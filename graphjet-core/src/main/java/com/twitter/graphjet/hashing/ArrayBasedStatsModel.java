package com.twitter.graphjet.hashing;

import com.twitter.graphjet.stats.Counter;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * @author Harsh Shah
 */
public class ArrayBasedStatsModel {

  private final StatsReceiver scopedStatsReceiver;
  private final Counter numStoredKeysCounter;
  private final Counter numFixedLengthMapsCounter;
  private final Counter totalAllocatedArrayBytesCounter;

  public ArrayBasedStatsModel(StatsReceiver scopedStatsReceiver,
                              Counter numStoredKeysCounter,
                              Counter numFixedLengthMapsCounter,
                              Counter totalAllocatedArrayBytesCounter) {
    this.scopedStatsReceiver = scopedStatsReceiver;
    this.numStoredKeysCounter = numStoredKeysCounter;
    this.numFixedLengthMapsCounter = numFixedLengthMapsCounter;
    this.totalAllocatedArrayBytesCounter = totalAllocatedArrayBytesCounter;
  }

  public StatsReceiver getScopedStatsReceiver() {
    return scopedStatsReceiver;
  }

  public Counter getNumStoredKeysCounter() {
    return numStoredKeysCounter;
  }

  public Counter getNumFixedLengthMapsCounter() {
    return numFixedLengthMapsCounter;
  }

  public Counter getTotalAllocatedArrayBytesCounter() {
    return totalAllocatedArrayBytesCounter;
  }
}
