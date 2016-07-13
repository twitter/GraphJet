package com.twitter.graphjet.algorithms.salsa;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class encapsulates the logic of a single iteration.
 */
public abstract class SingleSalsaIteration {
  protected static final Logger LOG = LoggerFactory.getLogger("graph");

  protected Random random;

  /**
   * Implementations need to write only this single function that captures all the logic of
   * running a single iteration. This function is also expected to update the internal state,
   * include {@link com.twitter.graphjet.algorithms.salsa.SalsaStats} appropriately.
   */
  public abstract void runSingleIteration();

  /**
   * The reset function allows reuse of the object for answering multiple requests. This should
   * reset ALL internal state maintained locally for iterations.
   *
   * @param salsaRequest  is the new incoming request
   * @param newRandom     is the new random number generator to be used for all random choices
   */
  public void resetWithRequest(SalsaRequest salsaRequest, Random newRandom) {
    this.random = newRandom;
  }
}
