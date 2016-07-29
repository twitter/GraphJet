package com.twitter.graphjet.demo;

import com.twitter.graphjet.bipartite.MultiSegmentPowerLawBipartiteGraph;
import com.twitter.graphjet.bipartite.segment.IdentityEdgeTypeMask;
import com.twitter.graphjet.stats.NullStatsReceiver;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

public class TwitterStreamReader {
  public static void main(String[] args) throws Exception {
    int maxNumSegments = 10;
    int maxNumEdgesPerSegment = 10000;
    int leftSize = 1000;
    int rightSize = 1000;

    MultiSegmentPowerLawBipartiteGraph bigraph =
        new MultiSegmentPowerLawBipartiteGraph(maxNumSegments, maxNumEdgesPerSegment,
            leftSize, 10, 2.0,
            rightSize, 10, 2.0,
            new IdentityEdgeTypeMask(),
            new NullStatsReceiver());

    Long2ObjectOpenHashMap<String> users = new Long2ObjectOpenHashMap<String>();
    LongOpenHashSet tweets = new LongOpenHashSet();

    StatusListener listener = new StatusListener() {
      long statusCnt = 0;

      public void onStatus(Status status) {
        statusCnt++;
        if (statusCnt % 1000 == 0) {
          System.out.println(String.format("%d statuses observed, %d unique users, %d unique tweets", statusCnt, users.size(), tweets.size()));
        }

        String screenname = status.getUser().getScreenName();
        long userId = status.getUser().getId();
        long tweetId = status.isRetweet() ? status.getRetweetedStatus().getId() : status.getId();

        bigraph.addEdge(userId, tweetId, (byte) 0);

        if (!users.containsKey(userId)) {
          users.put(userId, screenname);
        }

        if (!tweets.contains(tweetId)) {
          tweets.add(tweetId);
        }
      }

      public void onScrubGeo(long userId, long upToStatusId) {}
      public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}
      public void onTrackLimitationNotice(int numberOfLimitedStatuses) {}
      public void onStallWarning(StallWarning warning) {}

      public void onException(Exception e) {
        e.printStackTrace();
      }
    };

    TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
    twitterStream.addListener(listener);
    twitterStream.sample();

    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");

    Server jettyServer = new Server(4444);
    jettyServer.setHandler(context);

    context.addServlet(new ServletHolder(new TopUsersServlet(bigraph, users, tweets)), "/topUsers");
    context.addServlet(new ServletHolder(new TopTweetsServlet(bigraph, users, tweets)), "/topTweets");

    try {
      jettyServer.start();
      jettyServer.join();
    } finally {
      jettyServer.destroy();
    }
 }
}
