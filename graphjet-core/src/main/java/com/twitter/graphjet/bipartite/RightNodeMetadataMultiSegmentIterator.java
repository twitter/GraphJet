/**
 * Copyright 2018 Twitter. All rights reserved.
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

import com.twitter.graphjet.bipartite.api.NodeMetadataEdgeIterator;
import com.twitter.graphjet.bipartite.segment.RightNodeMetadataLeftIndexedBipartiteGraphSegment;
import com.twitter.graphjet.hashing.IntArrayIterator;

import it.unimi.dsi.fastutil.ints.IntIterator;

public class RightNodeMetadataMultiSegmentIterator
  extends ReverseChronologicalMultiSegmentIterator<RightNodeMetadataLeftIndexedBipartiteGraphSegment>
  implements NodeMetadataEdgeIterator, ReusableNodeLongIterator {

  // Space ratio between integer and short.
  private static final int SPACE_RATIO_BETWEEN_INTEGER_AND_SHORT = 2;

  /**
   * This constructor is for easy reuse in the random iterator derived from this one.
   *
   * @param multiSegmentBipartiteGraph  is the underlying
   *                                    {@link RightNodeMetadataLeftIndexedBipartiteGraphSegment}
   * @param segmentEdgeAccessor         abstracts the left/right access in a common interface
   */
  public RightNodeMetadataMultiSegmentIterator(
    LeftIndexedMultiSegmentBipartiteGraph<RightNodeMetadataLeftIndexedBipartiteGraphSegment>
      multiSegmentBipartiteGraph,
    SegmentEdgeAccessor<RightNodeMetadataLeftIndexedBipartiteGraphSegment>
      segmentEdgeAccessor) {
    super(multiSegmentBipartiteGraph, segmentEdgeAccessor);
  }

  @Override
  public IntIterator getLeftNodeMetadata(byte nodeMetadataType) {
    throw new UnsupportedOperationException(
      "The getLeftNodeMetadata operation is currently not supported"
    );
  }

  @Override
  public IntIterator getRightNodeMetadata(byte nodeMetadataType) {
    return ((NodeMetadataEdgeIterator) currentSegmentIterator)
      .getRightNodeMetadata(nodeMetadataType);
  }

  /**
   * The method populates both mutable and immutable features of the right node.
   * For mutable features, it iterates through all segments, queries the features in each segment,
   * and then sums them up.
   * For immutable features, because different segments hold the exact same copy, the method will
   * populate the immutable features once at its first appearance.
   *
   * @param rightNode the right node long id
   * @param metadataIndex the feature index
   * @param features the feature array that is allocated by the caller
   * @param numAdditionalIntegerToUnpackShort the number of the additional integers to unpack
   *                                          features stored in short i16.
   */
  public void fetchFeatureArrayForNode(
    long rightNode,
    int metadataIndex,
    int[] features,
    int numAdditionalIntegerToUnpackShort
  ) {
    boolean setImmutableFeatures = false;
    for (int i = oldestSegmentId; i <= liveSegmentId; i++) {
      RightNodeMetadataLeftIndexedBipartiteGraphSegment segment = readerAccessibleInfo.getSegments().get(i);
      if (segment == null) {
        continue;
      }

      int rightNodeIndex = segment.getRightNodesToIndexBiMap().get(rightNode);
      // Default value is -1, which means the rightNode is not in the index map.
      if (rightNodeIndex == -1) {
        continue;
      }

      IntArrayIterator metadataIterator = (IntArrayIterator)
        segment.getRightNodesToMetadataMap().get(metadataIndex).get(rightNodeIndex);
      int packedMetadataSize = metadataIterator.size();

      // Sum up mutable features, and each of them takes the size of two bytes.
      for (int j = 0; j < numAdditionalIntegerToUnpackShort; j++) {
        int featureInShortFormat = metadataIterator.nextInt();
        // Extract the value from the higher two bytes of the integer.
        features[SPACE_RATIO_BETWEEN_INTEGER_AND_SHORT * j] += featureInShortFormat >> 16;
        // Extract the value from the lower two bytes of the integer.
        features[SPACE_RATIO_BETWEEN_INTEGER_AND_SHORT * j + 1] += featureInShortFormat & 0xffff;
      }

      // For the immutable features, only set them once.
      if (!setImmutableFeatures) {
        setImmutableFeatures = true;
        int startIndex = SPACE_RATIO_BETWEEN_INTEGER_AND_SHORT * numAdditionalIntegerToUnpackShort;
        int endIndex = packedMetadataSize + numAdditionalIntegerToUnpackShort;
        for (int j = startIndex; j < endIndex; j++) {
          features[j] = metadataIterator.nextInt();
        }
      }
    }
  }

  @Override
  // Return 0 because RightNodeMetadataMultiSegmentIterator does not contain edge metadata.
  public long currentMetadata() {
    return 0L;
  }
}

