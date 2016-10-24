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

package com.twitter.graphjet.algorithms.counting;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Shared utility functions among RecsGenerators
 */
public class GeneratorHelper {
  /**
   * Pick the top social proofs for each RHS node
   */
  public static Map<Byte, LongList> pickTopSocialProofs(
    SmallArrayBasedLongToDoubleMap[] socialProofs,
    byte[] validSocialProofs,
    int maxSocialProofSize) {

    Map<Byte, LongList> results = new HashMap<>();
    int length = validSocialProofs.length;

    for (int i = 0; i < length; i++) {
      SmallArrayBasedLongToDoubleMap socialProof = socialProofs[validSocialProofs[i]];
      if (socialProof != null) {
        if (socialProof.size() > 1) {
          socialProof.sort();
        }
        socialProof.trim(maxSocialProofSize);
        results.put(validSocialProofs[i], new LongArrayList(socialProof.keys()));
      }
    }

    return results;
  }

  public static void addResultToPriorityQueue(
    PriorityQueue<NodeInfo> topResults,
    NodeInfo nodeInfo,
    int maxNumResults) {
    if (topResults.size() < maxNumResults) {
      topResults.add(nodeInfo);
    } else if (nodeInfo.getWeight() > topResults.peek().getWeight()) {
      topResults.poll();
      topResults.add(nodeInfo);
    }
  }
}
