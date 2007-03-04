/* Copyright (C) 2005 by Peter Eastman

This program is free software; you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY 
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.object.TriangleMesh.*;
import artofillusion.ui.*;

import java.util.*;

/** This class provides a variety of static methods for selecting parts of TriangleMeshes in different ways. */

public class TriMeshSelectionUtilities
{
  /** Convert one type of selection (point, edge, or face) to a different one.
      @param mesh        the mesh the selection applies to
      @param oldMode     the selection mode of the old selection
      @param selection   the old selection
      @param newMode     the new selection mode
      @return a selection in the new mode which corresponds to the old selection as closely as possible
   */

  public static boolean[] convertSelection(TriangleMesh mesh, int oldMode, boolean selection[], int newMode)
  {
    Vertex v[] = (Vertex []) mesh.getVertices();
    Edge e[] = mesh.getEdges();
    Face f[] = mesh.getFaces();
    boolean newSel[];
    
    if (newMode == oldMode)
      return (boolean []) selection.clone();
    if (newMode == MeshEditController.POINT_MODE)
    {
      newSel = new boolean [v.length];
      if (oldMode == MeshEditController.EDGE_MODE)
      {
        for (int i = 0; i < e.length; i++)
          if (selection[i])
            newSel[e[i].v1] = newSel[e[i].v2] = true;
      }
      else
      {
        for (int i = 0; i < f.length; i++)
          if (selection[i])
            newSel[f[i].v1] = newSel[f[i].v2] = newSel[f[i].v3] = true;
      }
    }
    else if (newMode == MeshEditController.EDGE_MODE)
    {
      newSel = new boolean [e.length];
      if (oldMode == MeshEditController.POINT_MODE)
      {
        for (int i = 0; i < e.length; i++)
          newSel[i] = (selection[e[i].v1] && selection[e[i].v2]);
      }
      else
      {
        for (int i = 0; i < f.length; i++)
          if (selection[i])
            newSel[f[i].e1] = newSel[f[i].e2] = newSel[f[i].e3] = true;
      }
    }
    else
    {
      newSel = new boolean [f.length];
      if (oldMode == MeshEditController.POINT_MODE)
      {
        for (int i = 0; i < f.length; i++)
          newSel[i] = (selection[f[i].v1] && selection[f[i].v2] && selection[f[i].v3]);
      }
      else
      {
        for (int i = 0; i < f.length; i++)
          newSel[i] = (selection[f[i].e1] && selection[f[i].e2] && selection[f[i].e3]);
      }
    }
    return newSel;
  }
  
  /** Find the boundary of the current selection.
      @param mesh        the mesh the selection applies to
      @param oldMode     the selection mode of the old selection
      @param selection   the old selection
      @return a selection in edge mode which corresponds to the boundary of the old selection
   */
  
  public static boolean[] findSelectionBoundary(TriangleMesh mesh, int oldMode, boolean selection[])
  {
    boolean edgeSelection[] = convertSelection(mesh, oldMode, selection, MeshEditController.EDGE_MODE);
    boolean faceSelection[] = convertSelection(mesh, oldMode, selection, MeshEditController.FACE_MODE);
    Edge e[] = mesh.getEdges();
    Face f[] = mesh.getFaces();
    boolean newSel[] = new boolean [e.length];
    
    for (int i = 0; i < e.length; i++)
      newSel[i] = (edgeSelection[i] && (!faceSelection[e[i].f1] || e[i].f2 == -1 || !faceSelection[e[i].f2]));
    return newSel;
  }
  
  /** Select an edge loop from a single edge.
      @param mesh        the mesh the selection applies to
      @param startEdge   the index of the edge from which to find an edge loop
      @return a selection containing all the edges in the loop, or null if no loop could be found
   */
  
  private static boolean[] findSingleEdgeLoop(TriangleMesh mesh, int startEdge)
  {
    Vertex v[] = (Vertex []) mesh.getVertices();
    Edge e[] = mesh.getEdges();
    Face f[] = mesh.getFaces();
    boolean newSel[] = new boolean [e.length];
    int currentEdge = startEdge;
    int currentVert = e[startEdge].v1;

    while (true)
    {
      if (newSel[currentEdge])
      {
        if (currentEdge == startEdge)
          return newSel; // This is a good edge loop.
        return null; // The path looped back on itself without hitting the original edge.
      }
      
      // Find the next edge which is most nearly parallel to this one.
      
      newSel[currentEdge] = true;
      Vec3 dir1 = v[e[currentEdge].v1].r.minus(v[e[currentEdge].v2].r);
      dir1.normalize();
      int vertEdges[] = v[currentVert].getEdges();
      int bestEdge = -1;
      double maxDot = -1.0;
      for (int i = 0; i < vertEdges.length; i++)
      {
        if (vertEdges[i] == currentEdge)
          continue;
        Vec3 dir2 = v[e[vertEdges[i]].v1].r.minus(v[e[vertEdges[i]].v2].r);
        dir2.normalize();
        double dot = dir1.dot(dir2);
        if (e[currentEdge].v1 == e[vertEdges[i]].v1 || e[currentEdge].v2 == e[vertEdges[i]].v2)
          dot = -dot;
        if (dot > maxDot)
        {
          maxDot = dot;
          bestEdge = vertEdges[i];
        }
      }
      currentEdge = bestEdge;
      currentVert = (e[currentEdge].v1 == currentVert ? e[currentEdge].v2 : e[currentEdge].v1);
    }
  }
  
  /** Select an edge loop from each edge that is currently selected.
      @param mesh        the mesh the selection applies to
      @param selection   the current selection (in edge mode)
      @return a selection containing all the edges in the loops, or null if no loop could be found for one or more edges
   */
  
  public static boolean[] findEdgeLoops(TriangleMesh mesh, boolean selection[])
  {
    boolean newSel[] = new boolean [selection.length];
    for (int i = 0; i < selection.length; i++)
      if (selection[i])
      {
        boolean loop[] = findSingleEdgeLoop(mesh, i);
        if (loop == null)
          return null;
        for (int j = 0; j < loop.length; j++)
          newSel[j] |= loop[j];
      }
    return newSel;
  }
  
  /** Select an edge strip from a single edge.
      @param mesh        the mesh the selection applies to
      @param startEdge   the index of the edge from which to find an edge strip
      @return a selection containing all the edges in the strip, or null if no strip could be found
   */
  
  private static boolean[] findSingleEdgeStrip(TriangleMesh mesh, int startEdge)
  {
    Vertex v[] = (Vertex []) mesh.getVertices();
    Edge e[] = mesh.getEdges();
    Face f[] = mesh.getFaces();
    boolean newSel[] = new boolean [e.length];
    int currentEdge = startEdge;
    int prevEdge = startEdge;

    while (true)
    {
      if (newSel[currentEdge])
      {
        if (currentEdge == startEdge)
          return newSel; // This is a good edge strip.
        return null; // The path looped back on itself without hitting the original edge.
      }
      newSel[currentEdge] = true;
      Edge ce = e[currentEdge];
      Vec3 dir1 = v[ce.v1].r.minus(v[ce.v2].r);
      dir1.normalize();
      int bestEdge = -1;
      double maxDot = -1.0;
      
      // Record every neighbor of each vertex of the current edge.
      
      HashSet v1neighbors = new HashSet();
      int v1edges[] = v[ce.v1].getEdges();
      for (int i = 0; i < v1edges.length; i++)
      {
        Edge ed = e[v1edges[i]];
        if (ed != ce)
          v1neighbors.add(new Integer(ed.v1 == ce.v1 ? ed.v2 : ed.v1));
      }
      HashSet v2neighbors = new HashSet();
      int v2edges[] = v[ce.v2].getEdges();
      for (int i = 0; i < v2edges.length; i++)
      {
        Edge ed = e[v2edges[i]];
        if (ed != ce)
          v2neighbors.add(new Integer(ed.v1 == ce.v2 ? ed.v2 : ed.v1));
      }
      
      // Look for candidates to be the next edge in the strip.  A candidate is an edge which connects
      // a neighbor of v1 to a neighbor of v2.
      
      Iterator n2iter = v2neighbors.iterator();
      while (n2iter.hasNext())
      {
        int neighbor = ((Integer) n2iter.next()).intValue();
        int neighborEdges[] = v[neighbor].getEdges();
        for (int i = 0; i < neighborEdges.length; i++)
        {
          if (neighborEdges[i] == prevEdge)
            continue;
          Edge ed = e[neighborEdges[i]];
          if (v1neighbors.contains(new Integer(ed.v1)) || v1neighbors.contains(new Integer(ed.v2)))
          {
            // This edge is a candidate.  See how close it is to being parallel to the current edge.
            
            Vec3 dir2 = v[ed.v1].r.minus(v[ed.v2].r);
            dir2.normalize();
            double dot = Math.abs(dir1.dot(dir2));
            if (dot > maxDot)
            {
              maxDot = dot;
              bestEdge = neighborEdges[i];
            }
          }
        }
      }
      if (bestEdge == -1)
        return null;
      
      // There may also be a diagonal edge between the current and next edges.  Check for one,
      // and select it.
      
      ArrayList faceList1 = new ArrayList();
      faceList1.add(f[ce.f1]);
      if (ce.f2 != -1)
        faceList1.add(f[ce.f2]);
      ArrayList faceList2 = new ArrayList();
      faceList2.add(f[e[bestEdge].f1]);
      if (e[bestEdge].f2 != -1)
        faceList2.add(f[e[bestEdge].f2]);
      for (int i = 0; i < faceList1.size(); i++)
      {
        Face fc1 = (Face) faceList1.get(i);
        for (int j = 0; j < faceList2.size(); j++)
        {
          Face fc2 = (Face) faceList2.get(j);
          if (fc1.e1 == fc2.e1 || fc1.e1 == fc2.e2 || fc1.e1 == fc2.e3)
          {
            newSel[fc1.e1] = true;
            break;
          }
          if (fc1.e2 == fc2.e1 || fc1.e2 == fc2.e2 || fc1.e2 == fc2.e3)
          {
            newSel[fc1.e2] = true;
            break;
          }
          if (fc1.e3 == fc2.e1 || fc1.e3 == fc2.e2 || fc1.e3 == fc2.e3)
          {
            newSel[fc1.e3] = true;
            break;
          }
        }
      }
      prevEdge = currentEdge;
      currentEdge = bestEdge;
    }
  }
  
  /** Select an edge strip from each edge that is currently selected.
      @param mesh        the mesh the selection applies to
      @param selection   the current selection (in edge mode)
      @return a selection containing all the edges in the strips, or null if no strip could be found for one or more edges
   */
  
  public static boolean[] findEdgeStrips(TriangleMesh mesh, boolean selection[])
  {
    boolean newSel[] = new boolean [selection.length];
    for (int i = 0; i < selection.length; i++)
      if (selection[i])
      {
        boolean loop[] = findSingleEdgeStrip(mesh, i);
        if (loop == null)
          return null;
        for (int j = 0; j < loop.length; j++)
          newSel[j] |= loop[j];
      }
    return newSel;
  }
}
