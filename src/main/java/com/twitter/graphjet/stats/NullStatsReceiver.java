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

/**
 * A NullStatsReceiver for use in GraphJet library unit tests.
 */
public class NullStatsReceiver implements StatsReceiver {

    private final Counter nullCounter = new Counter() {
        public void incr() { }
        public void incr(int delta) { }
    };

    public StatsReceiver scope(String namespace) {
        return this;
    }

    public Counter counter(String counterName) {
        return nullCounter;
    }
}
