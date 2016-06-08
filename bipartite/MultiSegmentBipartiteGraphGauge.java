package com.twitter.graphjet.bipartite;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import scala.runtime.AbstractFunction0;

import com.twitter.finagle.stats.StatsReceiver;
import com.twitter.graphjet.bipartite.segment.LeftIndexedBipartiteGraphSegment;

/**
 * wrapper for the MultiSegmentBipartiteGraph gauge value providers
 * so that we don't have memory leak
 */
public class MultiSegmentBipartiteGraphGauge
    <T extends LeftIndexedBipartiteGraphSegment,
     S extends LeftIndexedMultiSegmentBipartiteGraph<T>> {
  private final WeakReference<S> multiSegmentBipartiteGraph;
  private final StatsReceiver statsReceiver;

  public MultiSegmentBipartiteGraphGauge(StatsReceiver statsReceiver, S graph) {
    multiSegmentBipartiteGraph = new WeakReference<S>(graph);
    this.statsReceiver = statsReceiver;
  }

  /**
   * add the BipartiteGraphSegment gauges
   */
  public void addGauges() {
    statsReceiver.addGauge(
        scala.collection.JavaConversions.asScalaBuffer(Arrays.asList("numEdgesInLiveSegment")),
        numEdgesInLiveSegmentGauge());
    statsReceiver.addGauge(
        scala.collection.JavaConversions.asScalaBuffer(Arrays.asList("numEdges")),
        numEdgesGauge());
  }

  private float numEdgesInLiveSegment() {
    S graph = multiSegmentBipartiteGraph.get();
    float numEdgesInLiveSegment = 0.0f;
    if (graph != null) {
      numEdgesInLiveSegment = (float) graph.numEdgesInLiveSegment;
    }
    return numEdgesInLiveSegment;
  }

  /**
   * get the gauge function for number of edges in live segment
   *
   * @return numEdgesInLiveSegmentGauge
   */
  public AbstractFunction0<Object> numEdgesInLiveSegmentGauge() {
    return new AbstractFunction0<Object>() {
      @Override
      public Object apply() {
        return Float.valueOf(numEdgesInLiveSegment());
      }
    };
  }

  private float numEdges() {
    S graph = multiSegmentBipartiteGraph.get();
    float numEdges = 0.0f;
    if (graph != null) {
      numEdges = (float) (graph.numEdgesInLiveSegment + graph.getNumEdgesInNonLiveSegments());
    }
    return numEdges;
  }

  /**
   * return the gauge function for number of edges EVER
   *
   * @return numEdgesGauge
   */
  public AbstractFunction0<Object> numEdgesGauge() {
    return new AbstractFunction0<Object>() {
      @Override
      public Object apply() {
        return Float.valueOf(numEdges());
      }
    };
  }
}
