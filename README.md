# GraphJet

[![Build Status](https://travis-ci.com/twitter/GraphJet.svg?token=CCxmTv4ejMPiyx81sXqe&branch=master)](https://travis-ci.com/twitter/GraphJet)

GraphJet is a real-time graph processing library written in Java. It powers a variety of real-time recommendation services within Twitter.

# Quick Start and Example

After cloning the repo, build as follows (for the impatient, use option `-DskipTests` to skip tests):

```
$ mvn package install
```

GraphJet includes a demo that reads from the Twitter public sample stream using the [Twitter4j library](http://twitter4j.org/en/) and maintains an in-memory bipartite graph of user-tweet interactions. To run the demo, create a file called `twitter4j.properties` in the GraphJet base directory with your Twitter credentials (replace `xxxx` with actual credentials):

```
oauth.consumerKey=xxxx
oauth.consumerSecret=xxxx
oauth.accessToken=xxxx
oauth.accessTokenSecret=xxxx
```

The public sample stream is available to registered users, see [documentation about Twitter streaming APIs](https://dev.twitter.com/streaming/overview) for more details.

Once you've built GraphJet, start the demo as follows:

```
$ mvn exec:java -pl graphjet-demo -Dexec.mainClass=com.twitter.graphjet.demo.TwitterStreamReader
```

Once the demo starts up, it begins ingesting the Twitter public sample stream. The program will print out a sequence of status messages indicating the internal state of the graph.

You can interact with the graph via a REST API, running on port 8888 by default; use ` -Dexec.args="-port xxxx"` to specify a different port. The following calls are available to query the state of the in-memory bipartite graph:

+ `top/tweets`: queries for the top tweets in terms of interactions (retweets). Use parameter `k` to specify number of results to return (default ten). Sample invocation:

```
curl http://localhost:8888/top/tweets?k=5
```

+ `top/users`: queries for the top users in terms of interactions (retweets).  Use parameter `k` to specify number of results to return (default ten). Sample invocation:

```
curl http://localhost:8888/top/users?k=5
```

+ `edges/tweets`: queries for the edges incident to a particular tweet, i.e., users who have interacted with the tweet. Use parameter `id` to specify tweetid (e.g., from `top/tweets` above). Sample invocation:

```
curl http://localhost:8888/edges/tweets?id=xxx
```

+ `top/users`: queries for the edges incident to a particular user, i.e., tweets the user interacted with. Use parameter `id` to specify userid (e.g., from `top/users` above). Sample invocation:

```
curl http://localhost:8888/edges/users?id=xxx
```

Note that the current demo program does not illustrate actual recommendation algorithms because the public sample stream API is too sparse in terms of interactions to give good results. We are working on alternative demos that will highlight GraphJet's recommendation algorithms.


# License

Copyright 2016 Twitter, Inc.

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
