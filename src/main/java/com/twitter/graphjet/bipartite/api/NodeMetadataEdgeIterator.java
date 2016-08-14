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

import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * An optimized iterator for traversing graph edges that also provides access to node metadata. Iterator returns
 * primitive values to avoid (un)boxing.
 */
public interface NodeMetadataEdgeIterator extends EdgeIterator {
  /**
   * Returns the metadata of current left node.
   *
   * @return the metadata of current left node.
   */
  IntIterator getLeftNodeMetadata(byte nodeMetadataType);

  /**
   * Returns the metadata of current right node.
   *
   * @return the metadata of current right node.
   */
  IntIterator getRightNodeMetadata(byte nodeMetadataType);
}
