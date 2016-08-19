package com.twitter.graphjet.demo;

import com.twitter.graphjet.algorithms.SimilarityInfo;
import com.twitter.graphjet.algorithms.SimilarityResponse;
import com.twitter.graphjet.algorithms.intersection.CosineUpdateNormalization;
import com.twitter.graphjet.algorithms.intersection.IntersectionSimilarity;
import com.twitter.graphjet.algorithms.intersection.IntersectionSimilarityRequest;
import com.twitter.graphjet.algorithms.intersection.RelatedTweetUpdateNormalization;
import com.twitter.graphjet.bipartite.MultiSegmentPowerLawBipartiteGraph;
import com.twitter.graphjet.stats.NullStatsReceiver;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Random;

public class GetSimilarTweetsServlet extends HttpServlet {
  private final MultiSegmentPowerLawBipartiteGraph bigraph;

  public GetSimilarTweetsServlet(MultiSegmentPowerLawBipartiteGraph bigraph) {
    this.bigraph = bigraph;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    long id = 0;
    String p = request.getParameter("id");
    try {
      id = Long.parseLong(p);
    } catch (NumberFormatException e) {
      response.setStatus(HttpStatus.BAD_REQUEST_400); // Signal client error.
      response.getWriter().println("[]");             // Return empty results.
      return;
    }

    int k = 10;
    int maxNumNeighbors = 100;
    int minNeighborDegree = 1;
    int maxNumSamplesPerNeighbor = 100;
    int minCooccurrence = 1;
    int minDegree = 2;
    double maxLowerMultiplicativeDeviation = 5.0;
    double maxUpperMultiplicativeDeviation = 5.0;

    System.out.println("Running similarity for node " + id);
    IntersectionSimilarityRequest intersectionSimilarityRequest = new IntersectionSimilarityRequest(
        id,
        k,
        new LongOpenHashSet(),
        maxNumNeighbors,
        minNeighborDegree,
        maxNumSamplesPerNeighbor,
        minCooccurrence,
        minDegree,
        maxLowerMultiplicativeDeviation,
        maxUpperMultiplicativeDeviation,
        false);

    RelatedTweetUpdateNormalization cosineUpdateNormalization = new CosineUpdateNormalization();
    IntersectionSimilarity cosineSimilarity = new IntersectionSimilarity(bigraph,
        cosineUpdateNormalization, new NullStatsReceiver());
    SimilarityResponse similarityResponse =
        cosineSimilarity.getSimilarNodes(intersectionSimilarityRequest, new Random());

    response.setStatus(HttpStatus.OK_200);

    for (SimilarityInfo sim : similarityResponse.getRankedSimilarNodes()) {
      response.getWriter().println(sim.toString());
    }
  }
}
