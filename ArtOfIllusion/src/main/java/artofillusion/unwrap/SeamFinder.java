/* Copyright (C) 2015 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.unwrap;

import artofillusion.math.*;
import artofillusion.object.*;

import java.util.*;

public class SeamFinder
{
  private TriangleMesh mesh;
  private double edgeVisibility[];
  private double vertexDistortion[];
  private int terminalVertices[];
  private ArrayList<Integer> seamEdges;

  public SeamFinder(TriangleMesh mesh)
  {
    this.mesh = mesh;
    seamEdges = new ArrayList<Integer>();
    ArrayList<HashSet<Integer>> surface = findSurfaces();
    computeEdgeVisibility();
    computeVertexDistortion();
    findTerminalVertices();
    findSeams();
  }

  public List<Integer> getSeamEdges()
  {
    return Collections.unmodifiableList(seamEdges);
  }

  private ArrayList<HashSet<Integer>> findSurfaces()
  {
    ArrayList<HashSet<Integer>> surfaces = new ArrayList<HashSet<Integer>>();
    TriangleMesh.Edge meshEdge[] = mesh.getEdges();
    TriangleMesh.Face meshFace[] = mesh.getFaces();
    boolean assigned[] = new boolean[meshFace.length];
    int boundary[] = new int[meshFace.length];
    for (int i = 0; i < assigned.length; i++)
    {
      if (assigned[i])
        continue;

      // This face is not part of any surface, so begin a new one.

      HashSet<Integer> surface = new HashSet<Integer>();
      surfaces.add(surface);
      int boundarySize = 0;
      boundary[boundarySize++] = i;
      while (boundarySize > 0)
      {
        // Add a boundary face to the surface.

        int faceIndex = boundary[--boundarySize];
        surface.add(faceIndex);
        assigned[faceIndex] = true;

        // Add all its neighbors to the boundary if they haven't already been assigned.

        TriangleMesh.Face face = meshFace[faceIndex];
        int f1 = (meshEdge[face.e1].f1 == faceIndex ? meshEdge[face.e1].f2 : meshEdge[face.e1].f1);
        int f2 = (meshEdge[face.e2].f1 == faceIndex ? meshEdge[face.e2].f2 : meshEdge[face.e2].f1);
        int f3 = (meshEdge[face.e3].f1 == faceIndex ? meshEdge[face.e3].f2 : meshEdge[face.e3].f1);
        if (f1 != -1 && !assigned[f1])
          boundary[boundarySize++] = f1;
        if (f2 != -1 && !assigned[f2])
          boundary[boundarySize++] = f2;
        if (f3 != -1 && !assigned[f3])
          boundary[boundarySize++] = f3;
      }
    }
    return surfaces;
  }

  private void computeEdgeVisibility()
  {
    MeshVertex meshVertex[] = mesh.getVertices();
    TriangleMesh.Face meshFace[] = mesh.getFaces();
    TriangleMesh.Edge meshEdge[] = mesh.getEdges();

    // Compute a normal vector for each face.

    Vec3 faceNorm[] = new Vec3[meshFace.length];
    for (int i = 0; i < meshFace.length; i++)
    {
      Vec3 edge1 = meshVertex[meshFace[i].v2].r.minus(meshVertex[meshFace[i].v1].r);
      Vec3 edge2 = meshVertex[meshFace[i].v3].r.minus(meshVertex[meshFace[i].v1].r);
      Vec3 edge3 = meshVertex[meshFace[i].v3].r.minus(meshVertex[meshFace[i].v2].r);
      edge1.normalize();
      edge2.normalize();
      edge3.normalize();
      faceNorm[i] = edge1.cross(edge2);
      double length = faceNorm[i].length();
      if (length > 0.0)
        faceNorm[i].scale(1.0/length);
    }

    // Compute a visibility for each vertex based on the normal direction.

    double vertexVisibility[] = new double[meshVertex.length];
    Vec3 center = mesh.getBounds().getCenter();
    Vec3 vertexNormal[] = mesh.getNormals();
    for (int i = 0; i < vertexVisibility.length; i++)
    {
      Vec3 outward = meshVertex[i].r.minus(center);
      outward.normalize();
      vertexVisibility[i] = 0.25*(2.0 + Math.max(vertexNormal[i].y, 0.0) + vertexNormal[i].dot(outward));
    }

    // Compute a visibility for each edge based on the visibility of the two vertices, plus the angle formed by
    // the two faces.

    edgeVisibility = new double[meshEdge.length];
    for (int i = 0; i < edgeVisibility.length; i++)
    {
      TriangleMesh.Edge edge = meshEdge[i];
      edgeVisibility[i] = 0.5 + 0.5*(vertexVisibility[edge.v1]+vertexVisibility[edge.v2]);
      if (edge.f2 != -1)
        edgeVisibility[i] += 0.5*faceNorm[edge.f1].dot(faceNorm[edge.f2]);
    }
  }

  private void computeVertexDistortion()
  {
    MeshVertex meshVertex[] = mesh.getVertices();
    TriangleMesh.Face meshFace[] = mesh.getFaces();
    TriangleMesh.Edge meshEdge[] = mesh.getEdges();
    vertexDistortion = new double[meshVertex.length];
    double localDistortion[] = new double[meshVertex.length];
    for (int vertIndex = 0; vertIndex < vertexDistortion.length; vertIndex++)
    {
      // Start by finding a patch immediately surrounding this vertex.

      HashSet<Integer> faces = new HashSet<Integer>();
      HashSet<Integer> edges = new HashSet<Integer>();
      int vertEdges[] = ((TriangleMesh.Vertex) meshVertex[vertIndex]).getEdges();
      for (int edgeIndex : vertEdges)
      {
        TriangleMesh.Edge edge = meshEdge[edgeIndex];
        faces.add(edge.f1);
        faces.add(edge.f2);
      }
      for (int faceIndex : faces)
      {
        TriangleMesh.Face face = meshFace[faceIndex];
        if (face.v1 != vertIndex && face.v2 != vertIndex)
          edges.add(face.e1);
        if (face.v2 != vertIndex && face.v3 != vertIndex)
          edges.add(face.e2);
        if (face.v3 != vertIndex && face.v1 != vertIndex)
          edges.add(face.e3);
      }

      // Compute the distortion of this local patch.

      localDistortion[vertIndex] = computePatchDistortion(meshVertex[vertIndex].r, edges);
      vertexDistortion[vertIndex] = localDistortion[vertIndex];

      // Now keep expanding the patch until the distortion stops increasing.

      int iteration = 0;
      while (true)
      {
        // Find all faces adjacent to the group.

        HashSet<Integer> neighbors = new HashSet<Integer>();
        for (int edgeIndex : edges)
        {
          TriangleMesh.Edge edge = meshEdge[edgeIndex];
          if (!faces.contains(edge.f1))
          {
            if (!neighbors.contains(edge.f1))
              neighbors.add(edge.f1);
          }
          if (edge.f2 != -1 && !faces.contains(edge.f2))
          {
            if (!neighbors.contains(edge.f2))
              neighbors.add(edge.f2);
          }
        }

        // Add faces to the group.

        faces.addAll(neighbors);
        HashSet<Integer> addedEdges = new HashSet<Integer>();
        for (int faceIndex : neighbors)
        {
          TriangleMesh.Face face = meshFace[faceIndex];
          addedEdges.add(face.e1);
          addedEdges.add(face.e2);
          addedEdges.add(face.e3);
        }
        for (int edgeIndex : addedEdges)
        {
          TriangleMesh.Edge edge = meshEdge[edgeIndex];
          if (faces.contains(edge.f1) && faces.contains(edge.f2))
            edges.remove(edgeIndex);
          else
            edges.add(edgeIndex);
        }

        // Evaluate the new group.

        if (edges.isEmpty() || faces.size() > meshFace.length/4)
          break;
        double distortion = computePatchDistortion(meshVertex[vertIndex].r, edges);
        if (distortion > vertexDistortion[vertIndex])
          vertexDistortion[vertIndex] = distortion;
        else if (++iteration > 3)
          break;
      }
    }
  }

  private double computePatchDistortion(Vec3 vertexPos, HashSet<Integer> edges)
  {
    // Divide the edges up into continuous loops.

    MeshVertex meshVertex[] = mesh.getVertices();
    TriangleMesh.Edge meshEdge[] = mesh.getEdges();
    HashSet<Integer> remainingEdges = new HashSet<Integer>(edges);
    ArrayList<HashSet<Integer>> loopEdges = new ArrayList<HashSet<Integer>>();
    ArrayList<HashSet<Integer>> loopVerts = new ArrayList<HashSet<Integer>>();
    while (remainingEdges.size() > 0)
    {
      // Search for an edge that can be added to an existing loop.

      boolean foundEdge = false;
      Iterator<Integer> iter = remainingEdges.iterator();
      while (iter.hasNext() && loopEdges.size() > 0)
      {
        int edgeIndex = iter.next();
        TriangleMesh.Edge edge = meshEdge[edgeIndex];
        for (int i = 0; i < loopVerts.size(); i++)
        {
          if (loopVerts.get(i).contains(edge.v1))
          {
            loopVerts.get(i).add(edge.v2);
            loopEdges.get(i).add(edgeIndex);
            iter.remove();
            foundEdge = true;
            break;
          }
          if (loopVerts.get(i).contains(edge.v2))
          {
            loopVerts.get(i).add(edge.v1);
            loopEdges.get(i).add(edgeIndex);
            iter.remove();
            foundEdge = true;
            break;
          }
        }
      }
      if (!foundEdge)
      {
        // We didn't find one, so create a new loop starting from the first edge.

        HashSet<Integer> newLoopEdges = new HashSet<Integer>();
        HashSet<Integer> newLoopVerts = new HashSet<Integer>();
        int edgeIndex = remainingEdges.iterator().next();
        TriangleMesh.Edge edge = meshEdge[edgeIndex];
        newLoopEdges.add(edgeIndex);
        newLoopVerts.add(edge.v1);
        newLoopVerts.add(edge.v2);
        loopEdges.add(newLoopEdges);
        loopVerts.add(newLoopVerts);
        remainingEdges.remove(edgeIndex);
      }
    }

    // Find the total distortion by adding up the contributions from all the loops.

    double totalDistortion = 0;
    for (int loop = 0; loop < loopEdges.size(); loop++)
    {
      // Add up the angles formed by all the edges.

      double totalAngle = 0;
      boolean isBoundary = false;
      for (int edgeIndex : loopEdges.get(loop))
      {
        TriangleMesh.Edge edge = meshEdge[edgeIndex];
        if (edge.f2 == -1)
          isBoundary = true;
        Vec3 v1 = meshVertex[edge.v1].r.minus(vertexPos);
        Vec3 v2 = meshVertex[edge.v2].r.minus(vertexPos);
        v1.normalize();
        v2.normalize();
        totalAngle += Math.acos(v1.dot(v2));
      }

      // Add in the distortion.

      double distortion = 1.0-totalAngle/(2*Math.PI);
      if (isBoundary && distortion > 0)
        distortion = 0;
      totalDistortion += distortion;
    }
    return totalDistortion;
  }

  private void findTerminalVertices()
  {
    MeshVertex meshVertex[] = mesh.getVertices();
    TriangleMesh.Edge meshEdge[] = mesh.getEdges();
    ArrayList<Integer> terminals = new ArrayList<Integer>();
    for (int vertIndex = 0; vertIndex < vertexDistortion.length; vertIndex++)
    {
      if (vertexDistortion[vertIndex] < 0.2)
        continue;
      int vertEdges[] = ((TriangleMesh.Vertex) meshVertex[vertIndex]).getEdges();
      boolean isLocalMaximum = true;
      for (int i = 0; i < vertEdges.length && isLocalMaximum; i++)
      {
        TriangleMesh.Edge edge = meshEdge[vertEdges[i]];
        int otherVert = (edge.v1 == vertIndex ? edge.v2 : edge.v1);
        if (vertexDistortion[otherVert] > vertexDistortion[vertIndex])
          isLocalMaximum = false;
      }
      if (isLocalMaximum)
        terminals.add(vertIndex);
    }
    terminalVertices = new int[terminals.size()];
    for (int i = 0; i < terminals.size(); i++)
      terminalVertices[i] = terminals.get(i);
  }

  private void findSeams()
  {
    // Compute a cost for each edge.

    MeshVertex meshVertex[] = mesh.getVertices();
    TriangleMesh.Edge meshEdge[] = mesh.getEdges();
    double edgeCost[] = new double[meshEdge.length];
    double minCost = 1e-10;
    for (int edgeIndex = 0; edgeIndex < meshEdge.length; edgeIndex++)
    {
      TriangleMesh.Edge edge = meshEdge[edgeIndex];
      if (edge.f2 == -1)
        edgeCost[edgeIndex] = minCost;
      else
      {
        Vec3 delta = meshVertex[edge.v1].r.minus(meshVertex[edge.v2].r);
        edgeCost[edgeIndex] = Math.max(minCost, delta.length()*edgeVisibility[edgeIndex]);
      }
    }

    // Cache the list of edges surrounding each vertex, since we will use it often.

    int vertEdges[][] = new int[meshVertex.length][];
    for (int vertIndex = 0; vertIndex < meshVertex.length; vertIndex++)
      vertEdges[vertIndex] = ((TriangleMesh.Vertex) meshVertex[vertIndex]).getEdges();

    // Create an initial patch for each terminal vertex.

    ArrayList<Patch> patches = new ArrayList<Patch>();
    for (int vertIndex : terminalVertices)
    {
      Patch patch = new Patch();
      computeCostToVertex(patch.costToVertex, vertIndex, edgeCost, vertEdges);
      patch.addVertex(vertIndex, vertEdges[vertIndex]);
      patches.add(patch);
    }

    // Now grow the patches.

    int nextPatch = -1;
    while (patches.size() > 1)
    {
      nextPatch++;
      if (nextPatch >= patches.size())
        nextPatch = 0;
      Patch patch = patches.get(nextPatch);
      if (patch.candidates.isEmpty())
      {
        // Nothing else to do with this patch, so remove it from further consideration.

        patches.remove(nextPatch);
        continue;
      }

      // Get the candidate vertex to add to the patch.

      int newVert = patch.getNextCandidate();

      // See if this vertex is already part of another patch.

      int alreadyInPatch = -1;
      for (int i = 0; i < patches.size(); i++)
        if (i != nextPatch && patches.get(i).vertices.contains(newVert))
        {
          alreadyInPatch = i;
          break;
        }
      if (alreadyInPatch == -1)
      {
        // Add this vertex to the patch.

        patch.addVertex(newVert, vertEdges[newVert]);
      }
      else
      {
        // Two patches meet at this vertex, so it is a Steiner point.  First, add edges connecting the vertex to
        // each of the two patches.

        Patch otherPatch = patches.get(alreadyInPatch);
        connectVertexToPatch(newVert, patch, vertEdges);
        connectVertexToPatch(newVert, otherPatch, vertEdges);

        // Now merge the two patches together.  The cost to connect a vertex to the new patch is the minimum of the
        // cost to connect it to either of the original patches, or to connect it to the vertex where they meet.

        patch.vertices.addAll(otherPatch.vertices);
        patch.candidates.removeAll(otherPatch.vertices);
        otherPatch.candidates.removeAll(patch.vertices);
        patch.candidates.addAll(otherPatch.candidates);
        double costToNewVert[] = new double[meshVertex.length];
        computeCostToVertex(costToNewVert, newVert, edgeCost, vertEdges);
        for (int i = 0; i < costToNewVert.length; i++)
          patch.costToVertex[i] = Math.min(Math.min(costToNewVert[i], patch.costToVertex[i]), otherPatch.costToVertex[i]);
        patches.remove(alreadyInPatch);
      }
   }
  }

  private void computeCostToVertex(final double costToVertex[], int vertex, double edgeCost[], int vertEdges[][])
  {
    TriangleMesh.Edge meshEdge[] = mesh.getEdges();
    Arrays.fill(costToVertex, Double.MAX_VALUE);
    costToVertex[vertex] = 0;
    boolean processed[] = new boolean[costToVertex.length];

    // The front initially consists of just the central vertex.

    PriorityQueue<Integer> front = new PriorityQueue<Integer>(10, new Comparator<Integer>()
    {
      @Override
      public int compare(Integer v1, Integer v2)
      {
        return Double.compare(costToVertex[v1], costToVertex[v2]);
      }
    });
    front.add(vertex);

    // Propagate the front outward, recording the cost to each vertex as we pass it.

    while (!front.isEmpty())
    {
      // Find the vertex in the front with lowest cost.

      int vertIndex = front.poll();

      // Process it.

      processed[vertIndex] = true;
      for (int edgeIndex : vertEdges[vertIndex])
      {
        TriangleMesh.Edge edge = meshEdge[edgeIndex];
        int newVert = (edge.v1 == vertIndex ? edge.v2 : edge.v1);
        if (!processed[newVert])
        {
          double newDist = costToVertex[vertIndex]+edgeCost[edgeIndex];
          if (newDist < costToVertex[newVert])
          {
            costToVertex[newVert] = newDist;
            front.add(newVert);
          }
        }
      }
    }
  }

  private void connectVertexToPatch(int vertIndex, Patch patch, int vertEdges[][])
  {
    TriangleMesh.Edge meshEdge[] = mesh.getEdges();
    while (patch.costToVertex[vertIndex] > 0)
    {
      // Find the best edge to follow from this vertex.

      int bestEdge = -1;
      double bestCost = Double.MAX_VALUE;
      int nextVert = -1;
      for (int edgeIndex : vertEdges[vertIndex])
      {
        TriangleMesh.Edge edge = meshEdge[edgeIndex];
        int otherVert = (edge.v1 == vertIndex ? edge.v2 : edge.v1);
        if (patch.costToVertex[otherVert] < bestCost)
        {
          bestEdge = edgeIndex;
          bestCost = patch.costToVertex[otherVert];
          nextVert = otherVert;
        }
      }
      seamEdges.add(bestEdge);
      vertIndex = nextVert;
    }
  }

  private class Patch
  {
    HashSet<Integer> vertices, candidates;
    double costToVertex[];

    Patch()
    {
      vertices = new HashSet<Integer>();
      candidates = new HashSet<Integer>();
      costToVertex = new double[mesh.getVertices().length];
    }

    int getNextCandidate()
    {
      int bestIndex = -1;
      double bestCost = Double.MAX_VALUE;
      for (int vertIndex : candidates)
        if (costToVertex[vertIndex] < bestCost)
        {
          bestIndex = vertIndex;
          bestCost = costToVertex[vertIndex];
        }
      return bestIndex;
    }

    void addVertex(int vertIndex, int vertEdges[])
    {
      vertices.add(vertIndex);
      candidates.remove(vertIndex);
      for (int edgeIndex : vertEdges)
      {
        TriangleMesh.Edge edge = mesh.getEdges()[edgeIndex];
        int otherVert = (edge.v1 == vertIndex ? edge.v2 : edge.v1);
        if (!vertices.contains(otherVert))
          candidates.add(otherVert);
      }
    }
  }
}
