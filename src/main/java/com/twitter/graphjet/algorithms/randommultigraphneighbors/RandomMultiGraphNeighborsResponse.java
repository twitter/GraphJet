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


package com.twitter.graphjet.algorithms.randommultigraphneighbors;

/**
 * This class encapsulates the required response from a {@link RandomMultiGraphNeighbors}.
 */
public class RandomMultiGraphNeighborsResponse {
  private final Iterable<NeighborInfo> neighborNodes;

  /**
   * Create a new response.
   *
   * @param neighborNodes             a list of neighbor nodes
   */
  public RandomMultiGraphNeighborsResponse(Iterable<NeighborInfo> neighborNodes) {
    this.neighborNodes = neighborNodes;
  }

  public Iterable<NeighborInfo> getNeighborNodes() {
    return neighborNodes;
  }
}
