package com.twitter.graphjet.stats;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A default Counter for use in the GraphJet library.
 */
public class DefaultCounter implements Counter {
    private AtomicLong count;

    public DefaultCounter() {
        count = new AtomicLong();
    }

    public void incr() {
        count.incrementAndGet();
    }

    public void incr(int delta) {
        count.addAndGet(delta);
    }

    public long getCount() {
        return count.get();
    }
}
