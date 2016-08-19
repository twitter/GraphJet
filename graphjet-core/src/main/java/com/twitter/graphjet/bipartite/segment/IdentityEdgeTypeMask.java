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

/**
 * This edge type mask returns the original node id, which means the edge in graph is untyped.
 */
public class IdentityEdgeTypeMask implements EdgeTypeMask {
  public IdentityEdgeTypeMask() {
  }

  @Override
  public int encode(int node, byte edgeType) {
    return node;
  }

  @Override
  public byte edgeType(int node) {
    return 0;
  }

  @Override
  public int restore(int node) {
    return node;
  }
}
