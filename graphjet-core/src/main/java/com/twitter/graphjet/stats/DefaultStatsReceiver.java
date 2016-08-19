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


package com.twitter.graphjet.stats;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A default StatsReceiver for use in the GraphJet library.
 */
public class DefaultStatsReceiver implements StatsReceiver {

    private static ConcurrentHashMap<String, DefaultCounter> counters = new ConcurrentHashMap<>();
    private static final long ZERO_COUNT = 0L;
    private String name;

    public DefaultStatsReceiver(String name) {
        this.name = name;
    }

    public StatsReceiver scope(String namespace) {
        return new DefaultStatsReceiver(name + "/" + namespace);
    }

    public DefaultCounter counter(String counterName) {
        DefaultCounter defaultCounter = new DefaultCounter();
        counters.putIfAbsent(name + "/" + counterName, defaultCounter);
        return defaultCounter;
    }

    /**
     * A method for getting the count held by a particular counter.
     */
    public static long getCount(String counterName) {
        DefaultCounter defaultCounter = counters.get(counterName);
        if (defaultCounter != null) {
            return defaultCounter.getCount();
        } else {
            return ZERO_COUNT;
        }
    }
}
