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


package com.twitter.graphjet.bipartite;

import com.twitter.graphjet.bipartite.segment.LeftIndexedBipartiteGraphSegment;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

/**
 * This class encapsulates ALL the state that will be accessed by a reader, and since the class
 * has _immutable_ final members, we can guarantee visibility to other threads without
 * synchronization/using volatile. Note that the objects at the level of this class are truly
 * immutable, and if there is a change in the segment map or the id's we'd need to publish a
 * new object. We re-emphasize that data within segments might actually change, and making those
 * changes thread-safe needs some synchronization/memory barriers within the segments themselves
 * but this class guarantees clients that the map will not change.
 *
 * From 'Java Concurrency in practice' by Brian Goetz, p. 349:
 *
 * "Initialization safety guarantees that for properly constructed objects, all
 *  threads will see the correct values of final fields that were set by the con-
 *  structor, regardless of how the object is published. Further, any variables
 *  that can be reached through a final field of a properly constructed object
 *  (such as the elements of a final array or the contents of a HashMap refer-
 *  enced by a final field) are also guaranteed to be visible to other threads."
 */
public class MultiSegmentReaderAccessibleInfo<T extends LeftIndexedBipartiteGraphSegment> {
  /**
   * This keeps track of all the segments we have: note that a segment id is unique and old
   * segments are just dropped so the size of this map is constant.
   *
   * The same map is used for accessing the edges as follows:
   * - For iterating through edges or getting total degree: just iterate through all the segments
   * - For getting a random sample of edges: first, iterate through segments and build an alias
   *   table for sampling segments. Then sample in each segment using the table.
   * TODO (aneesh): this alias table generation can be optimized as degrees in old segments remain
   *                constant.
   *
   * NOTE: while in the middle of the above operations, the segments might change (the old one
   *       might be dropped, and the node might be in a new one) but the operations are still
   *       valid -- if a segment is visible while building the alias table then the edges can
   *       still be accessed.
   */
  protected final Int2ObjectMap<T> segments;
  protected final int oldestSegmentId;
  protected final int liveSegmentId;

  /**
   * A new instance is immediately visible to the readers due to publication safety.
   *
   * @param segments               contains all the present segments
   * @param oldestSegmentId        is the id of the oldest segment
   * @param liveSegmentId          is the id of the live segment
   */
  public MultiSegmentReaderAccessibleInfo(
      Int2ObjectMap<T> segments,
      int oldestSegmentId,
      int liveSegmentId) {
    this.segments = segments;
    this.oldestSegmentId = oldestSegmentId;
    this.liveSegmentId = liveSegmentId;
  }

  public Int2ObjectMap<T> getSegments() {
    return segments;
  }
}
