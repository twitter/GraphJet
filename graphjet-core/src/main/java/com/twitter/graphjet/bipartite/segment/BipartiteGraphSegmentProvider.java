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


package com.twitter.graphjet.bipartite.segment;

import com.twitter.graphjet.bipartite.api.EdgeTypeMask;
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * Abstracts the notion of generating left-indexed bipartite graph segments.
 */
public abstract class BipartiteGraphSegmentProvider<T extends LeftIndexedBipartiteGraphSegment> {
  protected final EdgeTypeMask edgeTypeMask;
  protected final StatsReceiver statsReceiver;

  /**
   * Stores the statsReceiver that future segments would use.
   *
   * @param statsReceiver  tracks the internal stats
   */
  public BipartiteGraphSegmentProvider(EdgeTypeMask edgeTypeMask, StatsReceiver statsReceiver) {
    this(edgeTypeMask, statsReceiver, "BipartiteGraphSegment");
  }

  /**
   * Stores the statsReceiver that future segments would use.
   *
   * @param statsReceiver  tracks the internal stats
   * @param scopeString    sets the scope of the statsReceiver
   */
  public BipartiteGraphSegmentProvider(
      EdgeTypeMask edgeTypeMask,
      StatsReceiver statsReceiver,
      String scopeString) {
    this.edgeTypeMask = edgeTypeMask;
    this.statsReceiver = statsReceiver.scope(scopeString);
  }

  /**
   * Generate a new segment of type <code>T</code>. This is guaranteed to be thread-safe.
   *
   * @return the new segment
   */
  public abstract T generateNewSegment(int segmentId, int maxNumEdges);
}
