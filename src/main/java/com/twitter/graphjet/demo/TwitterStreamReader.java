package com.twitter.graphjet.demo;

import com.twitter.graphjet.bipartite.MultiSegmentPowerLawBipartiteGraph;
import com.twitter.graphjet.bipartite.segment.IdentityEdgeTypeMask;
import com.twitter.graphjet.stats.NullStatsReceiver;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

/**
 * Demo of GraphJet. This program uses Twitter4j to read from the streaming API, where it observes status messages to
 * maintain a bipartite graph of users (on the left) and tweets (on the right). The program also starts up a Jetty
 * server to present a REST API to access statistics of the graph.
 */
public class TwitterStreamReader {
  private static class TwitterStreamReaderArgs {
    @Option(name = "-port", metaVar = "[port]", usage = "port")
    int port = 8888;

    @Option(name = "-maxSegments", metaVar = "[value]", usage = "maximum number of segments")
    int maxSegments = 10;

    @Option(name = "-maxEdgesPerSegment", metaVar = "[value]", usage = "maximum number of edges in each segment")
    int maxEdgesPerSegment = 10000;

    @Option(name = "-leftSize", metaVar = "[value]", usage = "expected number of nodes on left side")
    int leftSize = 1000;

    @Option(name = "-leftDegree", metaVar = "[value]", usage = "expected degree on left side")
    int leftDegree = 10;

    @Option(name = "-leftPowerLawExponent", metaVar = "[value]", usage = "left side Power Law exponent")
    float leftPowerLawExponent = 2.0f;

    @Option(name = "-rightSize", metaVar = "[value]", usage = "expected number of nodes on right side")
    int rightSize = 1000;

    @Option(name = "-rightDegree", metaVar = "[value]", usage = "expected degree on right side")
    int rightDegree = 10;

    @Option(name = "-rightPowerLawExponent", metaVar = "[value]", usage = "right side Power Law exponent")
    float rightPowerLawExponent = 2.0f;
  }

  public static void main(String[] argv) throws Exception {
    TwitterStreamReaderArgs args = new TwitterStreamReaderArgs();

    CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(90));

    try {
      parser.parseArgument(argv);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      return;
    }

    MultiSegmentPowerLawBipartiteGraph bigraph =
        new MultiSegmentPowerLawBipartiteGraph(args.maxSegments, args.maxEdgesPerSegment,
            args.leftSize, args.leftDegree, args.leftPowerLawExponent,
            args.rightSize, args.rightDegree, args.rightPowerLawExponent,
            new IdentityEdgeTypeMask(),
            new NullStatsReceiver());

    Long2ObjectOpenHashMap<String> users = new Long2ObjectOpenHashMap<>();
    LongOpenHashSet tweets = new LongOpenHashSet();

    StatusListener listener = new StatusListener() {
      long statusCnt = 0;

      public void onStatus(Status status) {
        statusCnt++;
        if (statusCnt % 1000 == 0) {
          System.out.println(String.format("%d statuses observed, %d unique users, %d unique tweets",
              statusCnt, users.size(), tweets.size()));
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

    Server jettyServer = new Server(args.port);
    jettyServer.setHandler(context);

    context.addServlet(new ServletHolder(new TopUsersServlet(bigraph, users)), "/top/users");
    context.addServlet(new ServletHolder(new TopTweetsServlet(bigraph, tweets)), "/top/tweets");

    System.out.println("Starting service on port " + args.port);
    try {
      jettyServer.start();
      jettyServer.join();
    } finally {
      jettyServer.destroy();
    }
 }
}
