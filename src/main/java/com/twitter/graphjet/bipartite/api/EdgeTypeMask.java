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
 * Bit mask used to encode edge types in the high-order bits of an integer.
 */
public interface EdgeTypeMask {
  /**
   * Encodes the edge type in the high-order bits of the integer node id.
   *
   * @param node the original node id
   * @param edgeType edge type
   * @return the node id with encoded edge type
   */
  int encode(int node, byte edgeType);

  /**
   * Extracts the edge type from an integer node id that has been encoded with {@link #encode(int, byte)}.
   *
   * @param node the node id with encoded edge type
   * @return edge type
   */
  byte edgeType(int node);

  /**
   * Restores the original node id by removing the edge types encoded in the high-order bits.
   *
   * @param node the node id with encoded edge type
   * @return the original node id edge type removed
   */
  int restore(int node);
}
