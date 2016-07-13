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
