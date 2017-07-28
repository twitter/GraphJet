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


package com.twitter.graphjet.algorithms.filters;

import java.util.List;

import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

/**
 * Provides a simple chaining filter that takes in a list of filters, applies them one after
 * the other, and filters the result if any one filter asks to filter the result.
 */
public class ResultFilterChain {
  private final List<ResultFilter> resultFilterSet;

  public ResultFilterChain(List<ResultFilter> resultFilterSet) {
    this.resultFilterSet = resultFilterSet;
  }

  /**
   * Resets all the underlying filters with the request.
   *
   * @param request  is the incoming request to be used for resetting
   */
  public void resetFilters(RecommendationRequest request) {
    for (ResultFilter resultFilter : resultFilterSet) {
      resultFilter.resetFilter(request);
    }
  }

  /**
   * Provides an OR of the underlying filters, returning true if any of the underlying filters would
   * return true.
   *
   * @param node  is the node to check for filtering
   * @param socialProofs is the socialProofs of different types associated with the node
   * @return true if the node should be discarded, false otherwise
   */
  public boolean filterResult(long node, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    for (ResultFilter resultFilter : resultFilterSet) {
      resultFilter.inputCounter.incr();
      if (resultFilter.filterResult(node, socialProofs)) {
        resultFilter.filteredCounter.incr();
        return true;
      }
    }
    return false;
  }
}
