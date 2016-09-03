package com.twitter.graphjet.demo;

import com.twitter.graphjet.algorithms.SimilarityInfo;
import com.twitter.graphjet.algorithms.SimilarityResponse;
import com.twitter.graphjet.algorithms.intersection.CosineUpdateNormalization;
import com.twitter.graphjet.algorithms.intersection.IntersectionSimilarity;
import com.twitter.graphjet.algorithms.intersection.IntersectionSimilarityRequest;
import com.twitter.graphjet.algorithms.intersection.RelatedTweetUpdateNormalization;
import com.twitter.graphjet.bipartite.MultiSegmentPowerLawBipartiteGraph;
import com.twitter.graphjet.stats.NullStatsReceiver;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Random;

/**
 * Servlet of {@link TwitterStreamReader} that computes similar hashtags in a tweet-hashtag bipartite graph.
 */

public class GetSimilarHashtagsServlet extends HttpServlet {
    private final MultiSegmentPowerLawBipartiteGraph bigraph;
    private final Long2ObjectOpenHashMap<String> hashtags;

    public GetSimilarHashtagsServlet(MultiSegmentPowerLawBipartiteGraph bigraph, Long2ObjectOpenHashMap<String> hashtags) {
        this.bigraph = bigraph;
        this.hashtags = hashtags;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String hashtag= request.getParameter("hashtag").toLowerCase();
        String numResults = request.getParameter("k");

        long id = (long)hashtag.hashCode();
        int k = 10;
        int maxNumNeighbors = 100;
        int minNeighborDegree = 1;
        int maxNumSamplesPerNeighbor = 100;
        int minCooccurrence = 2;
        int minDegree = 2;
        double maxLowerMultiplicativeDeviation = 5.0;
        double maxUpperMultiplicativeDeviation = 5.0;

        try {
            k = Integer.parseInt(numResults);
        } catch (NumberFormatException e) {
            // Just eat it, don't need to worry.
        }

        if (bigraph.getRightNodeDegree(id) == 0) {
            response.getWriter().println(String.format("Hashtag #%s not found", hashtag));
            return;
        }

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
        response.getWriter().println("Related hashtags for: #" + hashtag);
        for (SimilarityInfo sim : similarityResponse.getRankedSimilarNodes()) {
            response.getWriter().println(hashtags.get(sim.getSimilarNode()) + ": " + sim.toString());
        }
    }
}
