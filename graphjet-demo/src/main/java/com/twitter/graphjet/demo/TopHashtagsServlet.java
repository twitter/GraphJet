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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.twitter.graphjet.bipartite.MultiSegmentPowerLawBipartiteGraph;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Servlet of {@link TwitterStreamReader} that returns the top <i>k</i> users in terms of degree in the user-tweet
 * bipartite graph.
 */
public class TopHashtagsServlet extends HttpServlet {
  private static final Joiner JOINER = Joiner.on(",\n");
  private final MultiSegmentPowerLawBipartiteGraph bigraph;
  private final Long2ObjectMap<String> hashtags;

  public TopHashtagsServlet(MultiSegmentPowerLawBipartiteGraph bigraph, Long2ObjectOpenHashMap<String> hashtags) {
    this.bigraph = bigraph;
    this.hashtags = hashtags;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    int k = 10;
    String p = request.getParameter("k");
    if (p != null) {
      try {
        k = Integer.parseInt(p);
      } catch (NumberFormatException e) {
        // Just eat it, don't need to worry.
      }
    }

    PriorityQueue<NodeValueEntry> queue = new PriorityQueue<>(k);
    LongIterator iter = hashtags.keySet().iterator();
    while (iter.hasNext()) {
      long hashtagHash = iter.nextLong();
      int cnt = bigraph.getRightNodeDegree(hashtagHash);
      if (cnt == 1) continue;

      if (queue.size() < k) {
        queue.add(new NodeValueEntry(hashtagHash, cnt));
      } else {
        NodeValueEntry peek = queue.peek();
        if (cnt > peek.getValue()) {
          queue.poll();
          queue.add(new NodeValueEntry(hashtagHash, cnt));
        }
      }
    }

    if (queue.size() == 0) {
      response.getWriter().println("[]\n");
      return;
    }

    NodeValueEntry e;
    List<String> entries = new ArrayList<>(queue.size());
    while ((e = queue.poll()) != null) {
      // Note that we explicitly use id_str and treat the tweet id as a String. See:
      // https://dev.twitter.com/overview/api/twitter-ids-json-and-snowflake
      entries.add(String.format("{\"hashtag_str\": \"%s\", \"id\": %d, \"cnt\": %d}",
              hashtags.get(e.getNode()), e.getNode(), (int) e.getValue()));
    }

    response.setStatus(HttpStatus.OK_200);
    response.getWriter().println("[\n" + JOINER.join(Lists.reverse(entries)) + "\n]");
  }
}
