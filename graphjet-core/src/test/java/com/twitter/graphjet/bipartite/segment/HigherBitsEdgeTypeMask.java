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

import com.google.common.base.Strings;
import com.twitter.graphjet.bipartite.api.EdgeTypeMask;

/**
 * This edge type mask adds the edge type in the highest order bits of the encoded int.
 */
public class HigherBitsEdgeTypeMask implements EdgeTypeMask {
    private static final int BITS_IN_INT = 32;
    private static final int BITS_IN_BYTE = 8;
    private static final int ALLOWED_NODE_BITS = BITS_IN_INT - BITS_IN_BYTE;

    public HigherBitsEdgeTypeMask(
    ) {
    }

    @Override
    public int encode(int node, byte edgeType) {
      if (node >>> ALLOWED_NODE_BITS > 0) {
        throw new IllegalArgumentException(
          String.format("The node needs to be less than %d bits long.", ALLOWED_NODE_BITS)
        );
      }
      return (edgeType << ALLOWED_NODE_BITS) | node;
    }

    @Override
    public byte edgeType(int node) {
      return (byte) (node >>> ALLOWED_NODE_BITS);
    }

    @Override
    public int restore(int node) {
      String bitMaskString = Strings.repeat("0", BITS_IN_BYTE) +
        Strings.repeat("1", ALLOWED_NODE_BITS);
      int bitMask = Integer.parseInt(bitMaskString, 2);
      return node & bitMask;
    }
}
