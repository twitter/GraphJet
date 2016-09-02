# GraphJet

[![Build Status](https://travis-ci.com/twitter/GraphJet.svg?token=CCxmTv4ejMPiyx81sXqe&branch=master)](https://travis-ci.com/twitter/GraphJet)

GraphJet is a real-time graph processing library written in Java. It powers a variety of real-time recommendation services within Twitter.

# Quick Start and Example

After cloning the repo, build as follows (for the impatient, use option `-DskipTests` to skip tests):

```
$ mvn package install
```

GraphJet includes a demo that reads from the Twitter public sample stream using the [Twitter4j library](http://twitter4j.org/en/) and maintains an in-memory bipartite graph of user-tweet interactions. It also maintains an in-memory bipartite graph of tweet-token associations according to recent tweet creation. To run the demo, create a file called `twitter4j.properties` in the GraphJet base directory with your Twitter credentials (replace `xxxx` with actual credentials):

```
oauth.consumerKey=xxxx
oauth.consumerSecret=xxxx
oauth.accessToken=xxxx
oauth.accessTokenSecret=xxxx
```

For obtaining the credentials, see [documentation on obtaining Twitter OAuth tokens](https://dev.twitter.com/oauth/overview/application-owner-access-tokens). The public sample stream is available to registered users, see [documentation about Twitter streaming APIs](https://dev.twitter.com/streaming/overview) for more details.

Once you've built GraphJet, start the demo as follows:

```
$ mvn exec:java -pl graphjet-demo -Dexec.mainClass=com.twitter.graphjet.demo.TwitterStreamReader
```

Once the demo starts up, it begins ingesting the Twitter public sample stream. The program will print out a sequence of status messages indicating the internal state of the user-tweet graph and the tweet-token graph.

You can interact with the graph via a REST API, running on port 8888 by default; use ` -Dexec.args="-port xxxx"` to specify a different port. The following calls are available to query the state of the in-memory bipartite graphs:

    context.addServlet(new ServletHolder(new GetEdgesServlet(userTweetBigraph, GetEdgesServlet.Side.RIGHT)),
            "/userTweetGraphEdges/users");
    context.addServlet(new ServletHolder(new GetEdgesServlet(userTweetBigraph, GetEdgesServlet.Side.LEFT)),
            "/userTweetGraphEdges/tweets");
    context.addServlet(new ServletHolder(new GetEdgesServlet(tweetHashtagBigraph, GetEdgesServlet.Side.RIGHT)),
            "/tweetHashtagGraphEdges/tweets");
    context.addServlet(new ServletHolder(new GetEdgesServlet(tweetHashtagBigraph, GetEdgesServlet.Side.LEFT)),
            "/tweetHashtagGraphEdges/tokens");
    context.addServlet(new ServletHolder(new GetSimilarTokensServlet(tweetHashtagBigraph, tokens)), "/similarTokens");


+ `userTweetGraph/topTweets`: queries for the top tweets in terms of interactions (retweets). Use parameter `k` to specify number of results to return (default ten). Sample invocation:

```
curl http://localhost:8888/userTweetGraph/topTweets?k=5
```

+ `userTweetGraph/topUsers`: queries for the top users in terms of interactions (retweets).  Use parameter `k` to specify number of results to return (default ten). Sample invocation:

```
curl http://localhost:8888/userTweetGraph/topUsers?k=5
```

+ `tweetHashtagGraph/topTweets`: queries for the top tweets in terms of tweet creation. Use parameter `k` to specify number of results to return (default ten). Sample invocation:

```
curl http://localhost:8888/tweetHashtagGraph/topTweets/topTweets?k=5
```

+ `tweetHashtagGraph/topTokens`: queries for the top tokens in terms of tweet creation.  Use parameter `k` to specify number of results to return (default ten). Sample invocation:

```
curl http://localhost:8888/tweetHashtagGraph/topTokens?k=5
```

+ `userTweetGraphEdges/tweets`: queries for the edges incident to a particular tweet in the user-tweet graph, i.e., users who have interacted with the tweet. Use parameter `id` to specify tweetid (e.g., from `userTweetGraph/topTweets` above). Sample invocation:

```
curl http://localhost:8888/userTweetGraphEdges/tweets?id=xxx
```

+ `userTweetGraphEdges/users`: queries for the edges incident to a particular user in the user-tweet graph, i.e., tweets the user interacted with. Use parameter `id` to specify userid (e.g., from `userTweetGraph/topUsers` above). Sample invocation:

```
curl http://localhost:8888/userTweetGraphEdges/users?id=xxx
```

+ `tweetHashtagGraphEdges/tweets`: queries for the edges incident to a particular tweet in the tweet-hashtag graph, i.e., hashtags which are contained within the tweet. Use parameter `id` to specify tweetid (e.g., from `tweetHashtagGraph/topTweets` above). Sample invocation:

```
curl http://localhost:8888/tweetHashtagGraphEdges/tweets?id=xxx
```

+ `tweetHashtagGraphEdges/tokens`: queries for the edges incident to a particular hashtag token in the tweet-hashtag graph, i.e., tweets the given hashtag token is contained in. Use parameter `id` to specify tokenid (e.g., from `tweetHashtagGraph/topTokens` above). Sample invocation:

```
curl http://localhost:8888/userTweetGraphEdges/users?id=xxx
```
Note that the current demo program does not illustrate actual recommendation algorithms because the public sample stream API is too sparse in terms of interactions to give good results. The following endpoint offers related hashtag tokens given an input hashtag token:

+ `similarTokens`: computes similar hashtag tokens to the input hashtag token based on real time data. Use parameter `token` to specify token (e.g., from `tweetHashtagGraph/topTokens` above). Sample invocation:

```
curl http://localhost:8888/similarTokens?token=trump&k=10
```

# License

Copyright 2016 Twitter, Inc.

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
