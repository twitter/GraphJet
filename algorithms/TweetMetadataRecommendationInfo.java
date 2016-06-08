package com.twitter.graphjet.algorithms;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Objects;

import com.twitter.graphjet.hashing.SmallArrayBasedLongToDoubleMap;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;

public class TweetMetadataRecommendationInfo
  implements RecommendationInfo, Comparable<TweetMetadataRecommendationInfo> {
  private final int recommendation;
  private final RecommendationType recommendationType;
  private double weight;
  private final Map<Byte, Map<Long, LongList>> socialProof;
  private static final int INITIAL_TWEET_ARRAY_SIZE = 4;

  /**
   * This class specifies the metadata recommendation, such as hashtag and url.
   */
  public TweetMetadataRecommendationInfo(int recommendation, RecommendationType type, double weight,
                                         Map<Byte, Map<Long, LongList>> socialProof) {
    this.recommendation = recommendation;
    this.recommendationType = type;
    this.weight = weight;
    this.socialProof = socialProof;
  }

  public int getRecommendation() {
    return recommendation;
  }

  public RecommendationType getRecommendationType() {
    return recommendationType;
  }

  public double getWeight() {
    return weight;
  }

  public Map<Byte, Map<Long, LongList>> getSocialProof() {
    return socialProof;
  }

  /**
   * This method updates the user and tweet social proofs associated with each metadata
   * recommendation.
   *
   * @param socialProofType          the social proof type, such as like, retweet, reply
   * @param userSocialProofs         the list of user social proofs
   * @param rightNode                the tweet with the metadata
   * @param maxUserSocialProofSize   max user social proof size to collect
   * @param maxTweetSocialProofSize  max tweet social proof size to collect
   */
  public void addToTweetSocialProofs(
    byte socialProofType,
    SmallArrayBasedLongToDoubleMap userSocialProofs,
    long rightNode,
    int maxUserSocialProofSize,
    int maxTweetSocialProofSize
  ) {
    Map<Long, LongList> socialProofByType = socialProof.get(socialProofType);
    long[] leftNodes = userSocialProofs.keys();

    if (socialProofByType == null) {
      // if this is the first social proof of this type, for each user social proof, create an empty
      // tweetIds list, add tweet id in the list, and then add the list to socialProof along with
      // the user id
      socialProofByType = new HashMap<Long, LongList>();
      for (int i = 0; i < userSocialProofs.size(); i++) {
        LongList tweetIds = new LongArrayList(INITIAL_TWEET_ARRAY_SIZE);
        tweetIds.add(rightNode);
        socialProofByType.put(leftNodes[i], tweetIds);
      }

      socialProof.put(socialProofType, socialProofByType);
    } else {
      // if the social proof type is already in the map, for each user social proof, create or
      // update the corresponding tweet social proof.
      for (int i = 0; i < userSocialProofs.size(); i++) {
        LongList tweetIds = socialProofByType.get(leftNodes[i]);
        if (socialProofByType.size() < maxUserSocialProofSize) {
          if (tweetIds == null) {
            tweetIds = new LongArrayList(INITIAL_TWEET_ARRAY_SIZE);
            socialProofByType.put(leftNodes[i], tweetIds);
          }
          if (tweetIds.size() < maxTweetSocialProofSize) {
            tweetIds.add(rightNode);
          }
        } else {
          if (tweetIds != null && tweetIds.size() < maxTweetSocialProofSize) {
            tweetIds.add(rightNode);
          }
        }
      }
    }
  }

  public void addToWeight(double delta) {
    this.weight += delta;
  }

  //sort the list of TweetMetadataRecommendationInfo in ascending order according to their weights
  public int compareTo(TweetMetadataRecommendationInfo otherRecommendationInfo) {
    return Double.compare(otherRecommendationInfo.getWeight(), this.weight);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(recommendation, recommendationType, weight, socialProof);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }

    TweetMetadataRecommendationInfo other = (TweetMetadataRecommendationInfo) obj;

    return
      Objects.equal(getRecommendation(), other.getRecommendation())
        && Objects.equal(getRecommendationType(), other.getRecommendationType())
        && Objects.equal(getWeight(), other.getWeight())
        && Objects.equal(getSocialProof(), other.getSocialProof());
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("recommendation", recommendation)
      .add("recommendationType", recommendationType)
      .add("weight", weight)
      .add("socialProof", socialProof)
      .toString();
  }
}
