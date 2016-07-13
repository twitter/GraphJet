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
