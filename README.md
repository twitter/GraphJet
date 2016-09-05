# GraphJet

[![Build Status](https://travis-ci.com/twitter/GraphJet.svg?token=CCxmTv4ejMPiyx81sXqe&branch=master)](https://travis-ci.com/twitter/GraphJet)

GraphJet is a real-time graph processing library written in Java. It powers a variety of real-time recommendation services within Twitter.

# Quick Start and Example

After cloning the repo, build as follows (for the impatient, use option `-DskipTests` to skip tests):

```
$ mvn package install
```

GraphJet includes a demo that reads from the Twitter public sample stream using the [Twitter4j library](http://twitter4j.org/en/) and maintains two separate in-memory bipartite graphs:

+ A bipartite graph of user-tweet interactions. The left-hand side vertices represent users, the right-hand side vertices represent tweets, and the edges represent tweet posts and retweets.
+ A bipartite graph of tweet-hashtag contents. The left-hand side vertices represent tweets, the right-hand side vertices represent hashtags, and the edges represent content association (e.g., a tweet contains a hashtag).

To run the demo, create a file called `twitter4j.properties` in the GraphJet base directory with your Twitter credentials (replace `xxxx` with actual credentials):

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

Once the demo starts up, it begins ingesting the Twitter public sample stream. The program will print out a sequence of status messages indicating the internal state of the user-tweet graph and the tweet-hashtag graph.

You can interact with the graph via a REST API, running on port 8888 by default; use ` -Dexec.args="-port xxxx"` to specify a different port. 

The following calls are available to query the state of the in-memory bipartite graph of user-tweet interactions:

+ `userTweetGraph/topTweets`: queries for the top tweets in terms of interactions (retweets). Use parameter `k` to specify number of results to return (default ten). Sample invocation:

```
curl http://localhost:8888/userTweetGraph/topTweets?k=5
```

+ `userTweetGraph/topUsers`: queries for the top users in terms of interactions (retweets).  Use parameter `k` to specify number of results to return (default ten). Sample invocation:

```
curl http://localhost:8888/userTweetGraph/topUsers?k=5
```

+ `userTweetGraphEdges/tweets`: queries for the edges incident to a particular tweet in the user-tweet graph, i.e., users who have interacted with the tweet. Use parameter `id` to specify tweetid (e.g., from `userTweetGraph/topTweets` above). Sample invocation:

```
curl http://localhost:8888/userTweetGraphEdges/tweets?id=xxx
```

+ `userTweetGraphEdges/users`: queries for the edges incident to a particular user in the user-tweet graph, i.e., tweets the user interacted with. Use parameter `id` to specify userid (e.g., from `userTweetGraph/topUsers` above). Sample invocation:

```
curl http://localhost:8888/userTweetGraphEdges/users?id=xxx
```

The following calls are available to query the state of the in-memory bipartite graph of tweet-hashtag contents:

+ `tweetHashtagGraphEdges/tweets`: queries for the edges incident to a particular tweet in the tweet-hashtag graph, i.e., hashtags contained in the tweet. Use parameter `id` to specify tweetid (e.g., from `tweetHashtagGraph/topTweets` above). Sample invocation:

```
curl http://localhost:8888/tweetHashtagGraphEdges/tweets?id=xxx
```

+ `tweetHashtagGraphEdges/hashtags`: queries for the edges incident to a particular hashtag hashtag in the tweet-hashtag graph, i.e., tweets the given hashtag is contained in. Use parameter `id` to specify hashtagid (e.g., from `tweetHashtagGraph/topHashtags` above). Sample invocation:

```
curl http://localhost:8888/tweetHashtagGraphEdges/hashtags?hashtag=xxx
```

+ `tweetHashtagGraph/topTweets`: queries for the top tweets in terms of hashtags. Use parameter `k` to specify number of results to return (default ten). Sample invocation:

```
curl http://localhost:8888/tweetHashtagGraph/topTweets?k=5
```

+ `tweetHashtagGraph/topHashtags`: queries for the top hashtags in terms of tweets.  Use parameter `k` to specify number of results to return (default ten). Sample invocation:

```
curl http://localhost:8888/tweetHashtagGraph/topHashtags?k=5
```

The demo program does not illustrate actual recommendation algorithms on the user-tweet graph (as is deployed inside Twitter) because the public sample stream API is too sparse in terms of interactions to give good results. Instead, the demo illustrates similarity queries on the tweet-hashtag graph. The following endpoint offers related hashtags given an input hashtag:

+ `similarHashtags`: computes similar hashtag to the input hashtag based on real time data. Use parameter `hashtag` to specify hashtag (e.g., from `tweetHashtagGraph/topHashtags` above). Sample invocation:

```
curl http://localhost:8888/similarHashtags?hashtag=trump&k=10
```

# License

Copyright 2016 Twitter, Inc.

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
