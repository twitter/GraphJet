package com.twitter.graphjet.bipartite.segment;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import scala.runtime.AbstractFunction0;

import com.twitter.finagle.stats.StatsReceiver;

/**
 * wrapper for the BipartiteGraphSegment gauge value providers
 * so that we don't have memory leak
 */
public class LeftIndexedBipartiteGraphSegmentGauge<T extends LeftIndexedBipartiteGraphSegment> {
  protected final WeakReference<T> thisSeg;
  protected final StatsReceiver statsReceiver;

  /**
   * construct and add the BipartiteGraphSegment gauges
   */
  public LeftIndexedBipartiteGraphSegmentGauge(
      StatsReceiver statsReceiver,
      T seg) {
    thisSeg = new WeakReference<T>(seg);
    this.statsReceiver = statsReceiver;
  }

  /**
   * add the BipartiteGraphSegment gauges
   */
  public void addGauges() {
    statsReceiver.addGauge(
        scala.collection.JavaConversions.asScalaBuffer(Arrays.asList("leftPoolFillPercentage")),
        leftPoolFillPercentageGauge());
  }

  private float leftPoolFillPercentage() {
    T seg = thisSeg.get();
    float leftPoolFillPercentage = 0.0f;
    if (seg != null) {
      leftPoolFillPercentage = (float) seg.getLeftNodeEdgePoolFillPercentage();
    }
    return leftPoolFillPercentage;
  }

  /**
   * return the gauge for leftPoolFillPercentage
   *
   * @return the gauge for leftPoolFillPercentage
   */
  public AbstractFunction0<Object> leftPoolFillPercentageGauge() {
    return new AbstractFunction0<Object>() {
      @Override
      public Object apply() {
        return Float.valueOf(leftPoolFillPercentage());
      }
    };
  }
}
