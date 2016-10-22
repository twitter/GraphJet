package com.twitter.graphjet.algorithms.counting;

import com.twitter.graphjet.algorithms.NodeInfo;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Shared utility functions among RecsGenerators
 */
public interface GeneratorUtils {
  /**
   * Pick the top social proofs for each RHS node
   */
   static Map<Byte, LongList> pickTopSocialProofs(
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

  static void addResultToPriorityQueue(
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
