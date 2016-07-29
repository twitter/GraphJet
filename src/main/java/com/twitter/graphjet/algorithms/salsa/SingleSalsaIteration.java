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
