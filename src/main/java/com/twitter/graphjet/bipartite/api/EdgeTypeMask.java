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


package com.twitter.graphjet.bipartite.api;

/**
 * The bit mask is used to encode edge types in the top bits of an integer.
 */
public interface EdgeTypeMask {
  /**
   * Encode the edge type into the top bits of the integer node id.
   *
   * @param node the original node id
   * @param edgeType edge type
   * @return the node id with bitmask
   */
  int encode(int node, byte edgeType);

  /**
   * Retrieve the edge type from the integer node id.
   *
   * @param node the node id with bitmask
   * @return edge type
   */
  byte edgeType(int node);

  /**
   * Restore the original node id by removing the meta data saved in top bits.
   *
   * @param node the node id with bitmask
   * @return node id without the bitmask
   */
  int restore(int node);
}
