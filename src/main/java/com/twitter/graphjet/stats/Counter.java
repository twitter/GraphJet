package com.twitter.graphjet.stats;

/**
 * This interface specifies the api for a counter for graphjet.
 */
public interface Counter {
    /**
     * Increment this counter by one.
     */
    void incr();

    /**
     * Increment the counter by a set amount.
     *
     * @param delta is the amount by which to increment this counter
     */
     void incr(int delta);
}
