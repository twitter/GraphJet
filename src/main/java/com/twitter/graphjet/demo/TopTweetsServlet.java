package com.twitter.graphjet.demo;

import com.twitter.graphjet.bipartite.MultiSegmentPowerLawBipartiteGraph;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TopTweetsServlet extends HttpServlet {
  private final MultiSegmentPowerLawBipartiteGraph bigraph;
  private final Long2ObjectOpenHashMap<String> users;
  private final LongOpenHashSet tweets;

  public TopTweetsServlet(MultiSegmentPowerLawBipartiteGraph bigraph,
                          Long2ObjectOpenHashMap<String> users, LongOpenHashSet tweets) {
    this.bigraph = bigraph;
    this.users = users;
    this.tweets = tweets;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setStatus(HttpStatus.OK_200);
    LongIterator iter = tweets.iterator();
    while (iter.hasNext()) {
      long user = iter.nextLong();
      int cnt = bigraph.getRightNodeDegree(user);
      if (cnt > 1)
        resp.getWriter().println(user + " " + cnt);
    }
  }
}
