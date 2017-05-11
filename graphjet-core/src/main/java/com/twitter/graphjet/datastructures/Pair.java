package com.twitter.graphjet.datastructures;

import com.google.common.base.Objects;

public class Pair<L, R> {
  private L l;
  private R r;

  public Pair(L l, R r) {
    this.l = l;
    this.r = r;
  }

  public L getL() {
    return l;
  }

  public R getR() {
    return r;
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

    Pair<L, R> other = (Pair<L, R>) obj;

    return Objects.equal(getL(), other.getL()) && Objects.equal(getR(), other.getR());
  }

  @Override
  public int hashCode() {
    return l.hashCode() & r.hashCode();
  }
}
