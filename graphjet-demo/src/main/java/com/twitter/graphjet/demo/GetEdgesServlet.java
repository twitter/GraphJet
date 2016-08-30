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

package com.twitter.graphjet.demo;

import com.twitter.graphjet.bipartite.api.EdgeIterator;
import com.twitter.graphjet.bipartite.MultiSegmentPowerLawBipartiteGraph;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet of {@link TwitterStreamReader} that fetches the edges incident to either the left (users) or the right
 * (tweets) side of the user-tweet bipartite graph.
 */
public class GetEdgesServlet extends HttpServlet {
  public enum Side {LEFT, RIGHT}

  private final MultiSegmentPowerLawBipartiteGraph bigraph;
  private final Side side;

  /**
   * Creates an instance of this servlet.
   *
   * @param bigraph the bipartite graph
   * @param side which side (right or left) being queried
   */
  public GetEdgesServlet(MultiSegmentPowerLawBipartiteGraph bigraph, Side side) {
    this.bigraph = bigraph;
    this.side = side;
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

    StringBuffer output = new StringBuffer();
    output.append("[");
    EdgeIterator iter = side.equals(Side.LEFT) ? bigraph.getLeftNodeEdges(id) :
        bigraph.getRightNodeEdges(id);

    while (iter.hasNext()) {
      output.append("\"");
      output.append(iter.nextLong());
      output.append(iter.hasNext() ? "\", " : "\"");
    }
    output.append("]");

    response.setStatus(HttpStatus.OK_200);
    response.getWriter().println(output.toString());
  }
}
