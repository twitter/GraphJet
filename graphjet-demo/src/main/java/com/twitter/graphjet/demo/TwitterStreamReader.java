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

import java.util.Date;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import com.twitter.graphjet.bipartite.MultiSegmentPowerLawBipartiteGraph;
import com.twitter.graphjet.bipartite.segment.IdentityEdgeTypeMask;
import com.twitter.graphjet.stats.NullStatsReceiver;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import twitter4j.HashtagEntity;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

/**
 * Demo of GraphJet. This program uses Twitter4j to read from the streaming API, where it observes status messages to
 * maintain a bipartite graph of users (on the left) and tweets (on the right). An edge indicates that a user posted a
 * tweet (with retweets resolved to their sources). The program also starts up a Jetty server to present a REST API to
 * access statistics of the graph.
 */
public class TwitterStreamReader {
  private static class TwitterStreamReaderArgs {
    @Option(name = "-port", metaVar = "[port]", usage = "port")
    int port = 8888;

    // For the month of July 2016, analysis of the sample stream shows approximately 150k tweets per hour.
    // The demo parameters are guesstimates based on this value. Heuristically, we tune the settings so that each
    // segment spans roughly an hour. Since the observations from the sample stream are sparse, we just assume the
    // expected number of left and right nodes to be the same as the number of edges. This is obviously not true,
    // but close enough for demo purposes.

    // Keep track of around eight hours worth of the sample stream
    @Option(name = "-maxSegments", metaVar = "[value]", usage = "maximum number of segments")
    int maxSegments = 8;

    @Option(name = "-maxEdgesPerSegment", metaVar = "[value]", usage = "maximum number of edges in each segment")
    int maxEdgesPerSegment = 150000;

    @Option(name = "-leftSize", metaVar = "[value]", usage = "expected number of nodes on left side")
    int leftSize = 150000;

    @Option(name = "-leftDegree", metaVar = "[value]", usage = "expected degree on left side")
    int leftDegree = 2;

    @Option(name = "-leftPowerLawExponent", metaVar = "[value]", usage = "left side Power Law exponent")
    float leftPowerLawExponent = 2.0f;

    @Option(name = "-rightSize", metaVar = "[value]", usage = "expected number of nodes on right side")
    int rightSize = 150000;

    @Option(name = "-rightDegree", metaVar = "[value]", usage = "expected degree on right side")
    int rightDegree = 2;

    @Option(name = "-rightPowerLawExponent", metaVar = "[value]", usage = "right side Power Law exponent")
    float rightPowerLawExponent = 2.0f;

    @Option(name = "-minorUpdateInterval", metaVar = "[value]", usage = "number of statuses before minor status update")
    int minorUpdateInterval = 1000;

    @Option(name = "-majorUpdateInterval", metaVar = "[value]", usage = "number of statuses before major status update")
    int majorUpdateInterval = 10000;
  }

  public static void main(String[] argv) throws Exception {
    final TwitterStreamReaderArgs args = new TwitterStreamReaderArgs();

    CmdLineParser parser = new CmdLineParser(args, ParserProperties.defaults().withUsageWidth(90));

    try {
      parser.parseArgument(argv);
    } catch (CmdLineException e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
      return;
    }

    final Date demoStart = new Date();
    final MultiSegmentPowerLawBipartiteGraph userTweetBigraph =
        new MultiSegmentPowerLawBipartiteGraph(args.maxSegments, args.maxEdgesPerSegment,
            args.leftSize, args.leftDegree, args.leftPowerLawExponent,
            args.rightSize, args.rightDegree, args.rightPowerLawExponent,
            new IdentityEdgeTypeMask(),
            new NullStatsReceiver());

    final MultiSegmentPowerLawBipartiteGraph tweetHashtagBigraph =
        new MultiSegmentPowerLawBipartiteGraph(args.maxSegments, args.maxEdgesPerSegment,
            args.leftSize, args.leftDegree, args.leftPowerLawExponent,
            args.rightSize, args.rightDegree, args.rightPowerLawExponent,
            new IdentityEdgeTypeMask(),
            new NullStatsReceiver());


    // Note that we're keeping track of the nodes on the left and right sides externally, apart from the bigraphs,
    // because the bigraph currently does not provide an API for enumerating over nodes. Currently, this is liable to
    // running out of memory, but this is fine for the demo.
    Long2ObjectOpenHashMap<String> users = new Long2ObjectOpenHashMap<>();
    LongOpenHashSet tweets = new LongOpenHashSet();
    Long2ObjectOpenHashMap<String> hashtags = new Long2ObjectOpenHashMap<>();
    // It is accurate of think of these two data structures as holding all users and tweets observed on the stream since
    // the demo program was started.

    StatusListener listener = new StatusListener() {
      long statusCnt = 0;

      public void onStatus(Status status) {

        String screenname = status.getUser().getScreenName();
        long userId = status.getUser().getId();
        long tweetId = status.getId();
        long resolvedTweetId = status.isRetweet() ? status.getRetweetedStatus().getId() : status.getId();
        HashtagEntity[] hashtagEntities = status.getHashtagEntities();

        userTweetBigraph.addEdge(userId, resolvedTweetId, (byte) 0);

        if (!users.containsKey(userId)) {
          users.put(userId, screenname);
        }

        if (!tweets.contains(tweetId)) {
          tweets.add(tweetId);
        }
        if (!tweets.contains(resolvedTweetId)) {
          tweets.add(resolvedTweetId);
        }

        for (HashtagEntity entity: hashtagEntities) {
          long hashtagHash = (long)entity.getText().toLowerCase().hashCode();
          tweetHashtagBigraph.addEdge(tweetId, hashtagHash, (byte) 0);
          if (!hashtags.containsKey(hashtagHash)) {
            hashtags.put(hashtagHash, entity.getText().toLowerCase());
          }
	    }
       
        statusCnt++;

        // Note that status updates are currently performed synchronously (i.e., blocking). Best practices dictate that
        // they should happen on another thread so as to not interfere with ingest, but this is okay for the pruposes
        // of the demo and the volume of the sample stream.

        // Minor status update: just print counters.
        if (statusCnt % args.minorUpdateInterval == 0) {
          long duration = (new Date().getTime() - demoStart.getTime()) / 1000;

          System.out.println(String.format("%tc: %,d statuses, %,d unique tweets, %,d unique hashtags (observed); " +
              "%.2f edges/s; totalMemory(): %,d bytes, freeMemory(): %,d bytes",
              new Date(), statusCnt, tweets.size(), hashtags.size(), (float) statusCnt / duration,
              Runtime.getRuntime().totalMemory(), Runtime.getRuntime().freeMemory()));
        }

        // Major status update: iterate over right and left nodes.
        if (statusCnt % args.majorUpdateInterval == 0 ) {
          int leftCnt = 0;
          LongIterator leftIter = tweets.iterator();
          while (leftIter.hasNext()) {
            if (userTweetBigraph.getLeftNodeDegree(leftIter.nextLong()) != 0)
              leftCnt++;
          }

          int rightCnt = 0;
          LongIterator rightIter = hashtags.keySet().iterator();
          while (rightIter.hasNext()) {
            if (userTweetBigraph.getRightNodeDegree(rightIter.nextLong()) != 0)
              rightCnt++;
          }
          System.out.println(String.format("%tc: Current user-tweet graph state: %,d left nodes (users), " +
              "%,d right nodes (tweets)",
              new Date(), leftCnt, rightCnt));
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

    context.addServlet(new ServletHolder(new TopUsersServlet(userTweetBigraph, users)),
            "/userTweetGraph/topUsers");
    context.addServlet(new ServletHolder(new TopTweetsServlet(userTweetBigraph, tweets,
            TopTweetsServlet.GraphType.USER_TWEET)),  "/userTweetGraph/topTweets");
    context.addServlet(new ServletHolder(new TopTweetsServlet(tweetHashtagBigraph, tweets,
            TopTweetsServlet.GraphType.TWEET_HASHTAG)), "/tweetHashtagGraph/topTweets");
    context.addServlet(new ServletHolder(new TopHashtagsServlet(tweetHashtagBigraph, hashtags)),
            "/tweetHashtagGraph/topHashtags");
    context.addServlet(new ServletHolder(new GetEdgesServlet(userTweetBigraph, GetEdgesServlet.Side.LEFT)),
            "/userTweetGraphEdges/users");
    context.addServlet(new ServletHolder(new GetEdgesServlet(userTweetBigraph, GetEdgesServlet.Side.RIGHT)),
            "/userTweetGraphEdges/tweets");
    context.addServlet(new ServletHolder(new GetEdgesServlet(tweetHashtagBigraph, GetEdgesServlet.Side.LEFT)),
            "/tweetHashtagGraphEdges/tweets");
    context.addServlet(new ServletHolder(new GetEdgesServlet(tweetHashtagBigraph, GetEdgesServlet.Side.RIGHT)),
            "/tweetHashtagGraphEdges/hashtags");
    context.addServlet(new ServletHolder(new GetSimilarHashtagsServlet(tweetHashtagBigraph, hashtags)),
            "/similarHashtags");

    System.out.println(String.format("%tc: Starting service on port %d", new Date(), args.port));
    try {
      jettyServer.start();
      jettyServer.join();
    } finally {
      jettyServer.destroy();
    }
 }
}
