package com.twitter.graphjet.algorithms;

import com.twitter.graphjet.algorithms.filters.ANDFilters;
import com.twitter.graphjet.algorithms.filters.RecentTweetFilter;
import com.twitter.graphjet.algorithms.filters.ResultFilter;
import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;
import com.twitter.graphjet.stats.NullStatsReceiver;
import com.twitter.graphjet.stats.StatsReceiver;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertFalse;

/**
 * Unit tests for class {@link ANDFilters}.
 *
 * @date 2017-09-18
 * @see ANDFilters
 **/
public class ANDFiltersTest {
    @Test
    public void testFilterResultReturningFalseOne() {
        List<ResultFilter> linkedList = new LinkedList<ResultFilter>();
        StatsReceiver statsReceiver = new NullStatsReceiver();
        ANDFilters aNDFilters = new ANDFilters(linkedList, statsReceiver);
        SmallArrayBasedLongToDoubleMap[] smallArrayBasedLongToDoubleMapArray = new SmallArrayBasedLongToDoubleMap[9];

        assertFalse(aNDFilters.filterResult(1633L, smallArrayBasedLongToDoubleMapArray));
    }

    @Test
    public void testFilterResultReturningFalseTwo() {
        List<ResultFilter> linkedList = new LinkedList<ResultFilter>();
        StatsReceiver statsReceiver = new NullStatsReceiver();
        RecentTweetFilter recentTweetFilter = new RecentTweetFilter(2147483639L, statsReceiver);
        linkedList.add(recentTweetFilter);
        ANDFilters aNDFilters = new ANDFilters(linkedList, statsReceiver);
        SmallArrayBasedLongToDoubleMap[] smallArrayBasedLongToDoubleMapArray = new SmallArrayBasedLongToDoubleMap[5];

        assertFalse(aNDFilters.filterResult(1L, smallArrayBasedLongToDoubleMapArray));
    }
}