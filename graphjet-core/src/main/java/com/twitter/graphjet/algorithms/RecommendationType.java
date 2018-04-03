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


package com.twitter.graphjet.algorithms;

public enum RecommendationType {
  HASHTAG(0),       // hashtag metadata type
  URL(1),           // url metadata type
  METADATASIZE(2),  // the size of supported metadata types
  TWEET(3),         // tweet, not a metadata type
  USER(4),          // users, in follows, mentions & mediatags
  MOMENT(5);        // moment
  private final int value;

  private RecommendationType(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  private static final RecommendationType[] VALUES = {HASHTAG, URL, METADATASIZE, TWEET, USER, MOMENT};

  public static RecommendationType at(int index) {
    return VALUES[index];
  }
}
