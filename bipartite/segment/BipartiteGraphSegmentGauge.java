package com.twitter.graphjet.bipartite.segment;

import java.util.Arrays;

import scala.runtime.AbstractFunction0;

import com.twitter.finagle.stats.StatsReceiver;

/**
 * wrapper for the BipartiteGraphSegment gauge value providers
 * so that we don't have memory leak
 */
public class BipartiteGraphSegmentGauge
    extends LeftIndexedBipartiteGraphSegmentGauge<BipartiteGraphSegment> {

  /**
   * construct and add the BipartiteGraphSegment gauges
   */
  public BipartiteGraphSegmentGauge(StatsReceiver statsReceiver, BipartiteGraphSegment seg) {
    super(statsReceiver, seg);
  }

  /**
   * add the BipartiteGraphSegment gauges
   */
  @Override
  public void addGauges() {
    super.addGauges();
    statsReceiver.addGauge(
        scala.collection.JavaConversions.asScalaBuffer(Arrays.asList("rightPoolFillPercentage")),
        rightPoolFillPercentageGauge());
  }

  private float rightPoolFillPercentage() {
    BipartiteGraphSegment seg = thisSeg.get();
    float rightPoolFillPercentage = 0.0f;
    if (seg != null) {
      rightPoolFillPercentage = (float) seg.getRightNodeEdgePoolFillPercentage();
    }
    return rightPoolFillPercentage;
  }

  /**
   * return the gauge for rightPoolFillPercentage
   *
   * @return the gauge for rightPoolFillPercentage
   */
  public AbstractFunction0<Object> rightPoolFillPercentageGauge() {
    return new AbstractFunction0<Object>() {
      @Override
      public Object apply() {
        return Float.valueOf(rightPoolFillPercentage());
      }
    };
  }
}
