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

package com.twitter.graphjet.algorithms.filter;

import java.util.ArrayList;

import org.junit.Test;

import com.twitter.graphjet.algorithms.filters.ANDFilters;
import com.twitter.graphjet.algorithms.RecommendationRequest;
import com.twitter.graphjet.algorithms.filters.ResultFilter;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.NullStatsReceiver;

import static org.junit.Assert.assertEquals;

public class ANDFilterTest {
  // Dummy filter that always return true
  private class TrueFilter extends ResultFilter {
    private TrueFilter() {
      super(new NullStatsReceiver());
    }
    @Override
    public void resetFilter(RecommendationRequest request) {}

    @Override
    public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
      return true;
    }
  }

  // Dummy filter that always return false
  private class FalseFilter extends ResultFilter {
    private FalseFilter() {
      super(new NullStatsReceiver());
    }
    @Override
    public void resetFilter(RecommendationRequest request) {}

    @Override
    public boolean filterResult(long resultNode, SmallArrayBasedLongToDoubleMap[] socialProofs) {
      return false;
    }
  }

  @Test
  public void testNoFilter() throws Exception {
    ANDFilters andFilters = new ANDFilters(new ArrayList<>(), new NullStatsReceiver());
    SmallArrayBasedLongToDoubleMap[] socialProofs = {};
    assertEquals(false, andFilters.filterResult(0L, socialProofs));
  }

  @Test
  public void testWithFilters() throws Exception {
    TrueFilter trueFilter = new TrueFilter();
    FalseFilter falseFilter = new FalseFilter();

    ArrayList<ResultFilter> filters = new ArrayList<>();

    // true && false = false
    filters.add(trueFilter);
    filters.add(falseFilter);
    ANDFilters andFilter = new ANDFilters(filters, new NullStatsReceiver());
    assertEquals(false, andFilter.filterResult(0, new SmallArrayBasedLongToDoubleMap[]{}));

    // true && true = true
    filters.clear();
    filters.add(trueFilter);
    filters.add(trueFilter);
    andFilter = new ANDFilters(filters, new NullStatsReceiver());
    assertEquals(true, andFilter.filterResult(0, new SmallArrayBasedLongToDoubleMap[]{}));
  }
}
