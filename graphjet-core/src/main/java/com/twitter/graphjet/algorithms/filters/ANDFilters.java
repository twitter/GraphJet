/**
 * Copyright 2017 Twitter. All rights reserved.
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
import com.twitter.graphjet.stats.StatsReceiver;

/**
 * This filter applies logical AND operation to all the filters in this class.
 * It will return true if only if all filters return true.
 * For example, given 3 filters, List[A, B, C], ANDFilters will return the result of (A && B && C).
 */
public class ANDFilters extends ResultFilter {
  private final List<ResultFilter> resultFilterList;

  public ANDFilters(List<ResultFilter> resultFilterList, StatsReceiver statsReceiver) {
    super(statsReceiver);
    this.resultFilterList = resultFilterList;
  }

  @Override
  public void resetFilter(RecommendationRequest request) {
    for (ResultFilter filter: resultFilterList) {
      filter.resetFilter(request);
    }
  }

  @Override
  public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
    if (resultFilterList.size() == 0) {
      return false;
    }

    for (ResultFilter filter: resultFilterList) {
      if (!filter.filterResult(resultNode, socialProofs)) {
        // only filter if all filters agree to filter
        return false;
      }
    }
    return true;
  }
}
