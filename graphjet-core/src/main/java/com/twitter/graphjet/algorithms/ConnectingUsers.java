package com.twitter.graphjet.algorithms;

import com.google.common.base.Objects;

import it.unimi.dsi.fastutil.longs.LongList;

public class ConnectingUsers {
  private LongList users;
  private LongList metadata;

  public ConnectingUsers(LongList users, LongList metadata) {
    this.users = users;
    this.metadata = metadata;
  }

  public LongList getUsers() {
    return users;
  }

  public LongList getMetadata() {
    return metadata;
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

    ConnectingUsers other = (ConnectingUsers) obj;

    return Objects.equal(getUsers(), other.getUsers()) && Objects.equal(getMetadata(), other.getMetadata());
  }

  @Override
  public int hashCode() {
    return users.hashCode() & metadata.hashCode();
  }
}
