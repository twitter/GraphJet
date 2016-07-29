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
 * This interface should specify all the read-only operations that are needed from a Bipartite
 * graph. In particular, any recommendation algorithms should only need to use this interface.
 *
 * NOTE: the graph is assumed to have nodes that are longs -- this is a very deliberate choice to
 * avoid the need for (un)boxing at any point.
 *
 * We also note the expected runtime cost for the operations here that the clients will assume,
 * and assume that implementations respect that.
 */
public interface BipartiteGraph extends LeftIndexedBipartiteGraph, RightIndexedBipartiteGraph {
  // all operations are defined in the left/right interfaces and this interface is just a
  // convenience wrapper that is used to refer to a bi-indexed graph.
}
