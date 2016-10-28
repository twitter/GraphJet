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

package com.twitter.graphjet.demo;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TopNodesTest {
  @Test
  public void testSimple1() throws Exception {
    TopNodes top = new TopNodes(3);

    top.offer(1, 4.5);
    top.offer(2, 1.2);
    top.offer(3, 0.2);
    top.offer(4, 6.3);
    top.offer(5, 4.4);
    top.offer(6, 2.9);

    List<NodeValueEntry> nodes = top.getNodes();
    assertEquals(3, nodes.size());
    assertEquals(4, nodes.get(0).getNode());
    assertEquals(1, nodes.get(1).getNode());
    assertEquals(5, nodes.get(2).getNode());
    assertEquals(6.3, nodes.get(0).getValue(), 10e-6);
    assertEquals(4.5, nodes.get(1).getValue(), 10e-6);
    assertEquals(4.4, nodes.get(2).getValue(), 10e-6);
  }

  @Test
  public void testSimple2() throws Exception {
    // If we want to keep track of top 5 but only see 3 entries, we should get all of them.
    TopNodes top = new TopNodes(5);

    top.offer(1, 4.5);
    top.offer(4, 6.3);
    top.offer(5, 4.4);

    List<NodeValueEntry> nodes = top.getNodes();
    assertEquals(3, nodes.size());
    assertEquals(4, nodes.get(0).getNode());
    assertEquals(1, nodes.get(1).getNode());
    assertEquals(5, nodes.get(2).getNode());
    assertEquals(6.3, nodes.get(0).getValue(), 10e-6);
    assertEquals(4.5, nodes.get(1).getValue(), 10e-6);
    assertEquals(4.4, nodes.get(2).getValue(), 10e-6);
  }
}
