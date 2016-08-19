package com.twitter.graphjet.demo;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.twitter.graphjet.bipartite.MultiSegmentPowerLawBipartiteGraph;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
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
 * Servlet of {@link TwitterStreamReader} that returns the top <i>k</i> tweets in terms of degree in the user-tweet
 * bipartite graph.
 */
public class TopTweetsServlet extends HttpServlet {
  private static final Joiner JOINER = Joiner.on(",\n");
  private final MultiSegmentPowerLawBipartiteGraph bigraph;
  private final LongSet tweets;

  public TopTweetsServlet(MultiSegmentPowerLawBipartiteGraph bigraph, LongOpenHashSet tweets) {
    this.bigraph = bigraph;
    this.tweets = tweets;
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
    LongIterator iter = tweets.iterator();
    while (iter.hasNext()) {
      long tweet = iter.nextLong();
      int cnt = bigraph.getRightNodeDegree(tweet);
      if (cnt == 1) continue;

      if (queue.size() < k) {
        queue.add(new NodeValueEntry(tweet, cnt));
      } else {
        NodeValueEntry peek = queue.peek();
        // Break ties by preferring higher tweetid (i.e., more recent tweet)
        if (cnt > peek.getValue() || (cnt == peek.getValue() && tweet > peek.getNode())) {
          queue.poll();
          queue.add(new NodeValueEntry(tweet, cnt));
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
      entries.add(String.format("{\"id_str\": \"%d\", \"cnt\": %d}", e.getNode(), e.getValue()));
    }

    response.setStatus(HttpStatus.OK_200);
    response.getWriter().println("[\n" + JOINER.join(Lists.reverse(entries)) + "\n]");
  }
}
