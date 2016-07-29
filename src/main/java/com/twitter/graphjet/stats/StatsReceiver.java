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
 * This interface specifies the api for a stats collector for graphjet.
 */
public interface StatsReceiver {
    /**
     * This is used to produce a new StatsReceiver with a specified scope.
     *
     * @param namespace
     * @return a new StatsReciever with the additional scope specified
     */
    StatsReceiver scope(String namespace);

    /**
     * This is used to produce a Counter.
     *
     * @param counterName is the name of the Counter to be returned
     * @return Counter
     */
    Counter counter(String counterName);
}
