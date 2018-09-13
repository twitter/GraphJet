package com.twitter.graphjet.algorithms;

import com.google.common.base.Objects;

import it.unimi.dsi.fastutil.longs.LongList;

/**
 * Holder class of connecting users and edge metadata. It is usually used in the social proof map.
 * The key is the social proof type, and the value is {@link ConnectingUsersWithMetadata}. For
 * example, the social proof type is like, and connectingUsers is {A, B, C}, and metadata could be a
 * list of timestamps of the corresponding like engagements.
 */
public class ConnectingUsersWithMetadata {
  private LongList connectingUsers;
  private LongList metadata;

  public ConnectingUsersWithMetadata(LongList users, LongList metadata) {
    this.connectingUsers = users;
    this.metadata = metadata;
  }

  public LongList getConnectingUsers() {
    return connectingUsers;
  }

  public LongList getMetadata() {
    return metadata;
  }

  @Override
  public String toString() {
    return "connectingUsers = " + connectingUsers.toString() + ", metadata = " + metadata.toString();
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

    ConnectingUsersWithMetadata other = (ConnectingUsersWithMetadata) obj;

    return Objects.equal(getConnectingUsers(), other.getConnectingUsers())
      && Objects.equal(getMetadata(), other.getMetadata());
  }

  @Override
  public int hashCode() {
    return connectingUsers.hashCode() & metadata.hashCode();
  }
}
