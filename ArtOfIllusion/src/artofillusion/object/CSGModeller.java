/* Copyright (C) 2001-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.texture.*;
import artofillusion.math.*;
import artofillusion.*;

import java.util.*;

/** This class is used by CSGObject.  It performs the actual work of applying boolean
    operators to create meshes based on the original objects.  This is based on the
    algorithm described in
    <p>
    D. H. Laidlaw, W. B. Trumbore, and J. F. Hughes.  "Constructive Solid Geometry
    for Polyhedral Objects." SIGGRAPH Proceedings, 1986, p.161.
    <p>
    with some simplifications due to the fact that we only permit triangles, rather
    than arbitrary convex polygons. */

public class CSGModeller
{
  private Vector<VertexInfo> vert1, vert2;
  private Vector<FaceInfo> face1, face2;
  private int mainAxis;

  static final int VERTEX = 0;
  static final int FACE = 1;
  static final int EDGE = 2;

  static final int UNKNOWN = 0;
  static final int BOUNDARY = 1;
  static final int INSIDE = 2;
  static final int OUTSIDE = 3;
  static final int SAME = 4;
  static final int OPPOSITE = 5;
  
  static final double TOL = 1e-10;
  
  public CSGModeller(TriangleMesh obj1, TriangleMesh obj2, CoordinateSystem coords1, CoordinateSystem coords2)
  {
    // Find the axis which best divides the two objects from each other.

    BoundingBox bounds1 = obj1.getBounds().transformAndOutset(coords1.fromLocal());
    BoundingBox bounds2 = obj2.getBounds().transformAndOutset(coords2.fromLocal());
    double xoverlap = Math.max(0, Math.min(bounds1.maxx-bounds2.minx, bounds2.maxx-bounds1.minx));
    double yoverlap = Math.max(0, Math.min(bounds1.maxy-bounds2.miny, bounds2.maxy-bounds1.miny));
    double zoverlap = Math.max(0, Math.min(bounds1.maxz-bounds2.minz, bounds2.maxz-bounds1.minz));
    double xwidth = Math.max(bounds1.maxx-bounds2.minx, bounds2.maxx-bounds1.minx);
    double ywidth = Math.max(bounds1.maxy-bounds2.miny, bounds2.maxy-bounds1.miny);
    double zwidth = Math.max(bounds1.maxz-bounds2.minz, bounds2.maxz-bounds1.minz);
    double xratio = (xwidth == 0.0 ? 1.0 : xoverlap/xwidth);
    double yratio = (ywidth == 0.0 ? 1.0 : yoverlap/ywidth);
    double zratio = (zwidth == 0.0 ? 1.0 : zoverlap/zwidth);
    if (xratio <= yratio && xratio <= zratio)
      mainAxis = 0;
    else if (yratio <= xratio && yratio <= zratio)
      mainAxis = 1;
    else
      mainAxis = 2;

    // Create the lists of vertices, edges, and faces for each mesh.
    
    vert1 = new Vector<VertexInfo>();
    vert2 = new Vector<VertexInfo>();
    face1 = new Vector<FaceInfo>();
    face2 = new Vector<FaceInfo>();
    TriangleMesh.Vertex vert[] = (TriangleMesh.Vertex []) obj1.getVertices();
    Mat4 trans = coords1.fromLocal();
    for (int i = 0; i < vert.length; i++)
      vert1.addElement(new VertexInfo(trans.times(vert[i].r), vert[i].smoothness, null));
    vert = (TriangleMesh.Vertex []) obj2.getVertices();
    trans = coords2.fromLocal();
    for (int i = 0; i < vert.length; i++)
      vert2.addElement(new VertexInfo(trans.times(vert[i].r), vert[i].smoothness, null));
    TriangleMesh.Edge edge[] = obj1.getEdges();
    TriangleMesh.Face face[] = obj1.getFaces();
    if (obj1.getSmoothingMethod() == Mesh.NO_SMOOTHING)
      for (int i = 0; i < face.length; i++)
        face1.addElement(new FaceInfo(face[i].v1, face[i].v2, face[i].v3, vert1, 0.0f, 0.0f, 0.0f));
    else
      for (int i = 0; i < face.length; i++)
        face1.addElement(new FaceInfo(face[i].v1, face[i].v2, face[i].v3, vert1,
            edge[face[i].e1].smoothness, edge[face[i].e2].smoothness, edge[face[i].e3].smoothness));
    edge = obj2.getEdges();
    face = obj2.getFaces();
    if (obj2.getSmoothingMethod() == Mesh.NO_SMOOTHING)
      for (int i = 0; i < face.length; i++)
        face2.addElement(new FaceInfo(face[i].v1, face[i].v2, face[i].v3, vert2, 0.0f, 0.0f, 0.0f));
    else
      for (int i = 0; i < face.length; i++)
        face2.addElement(new FaceInfo(face[i].v1, face[i].v2, face[i].v3, vert2,
            edge[face[i].e1].smoothness, edge[face[i].e2].smoothness, edge[face[i].e3].smoothness));
    
    // Step 1: Split the faces of each object so that they do not intersect.
    
    splitFaces(vert1, face1, bounds1, vert2, face2, bounds2);
    splitFaces(vert2, face2, bounds2, vert1, face1, bounds1);
    splitFaces(vert1, face1, bounds1, vert2, face2, bounds2);
    
    // Step 2: Determine which vertices on one object are inside or outside the other.

    findInsideVertices(vert1, face1, vert2, face2);
    findInsideVertices(vert2, face2, vert1, face1);
  }
  
  /** Get a triangle mesh representing the union, intersection, or difference of the two objects.
      @param op    the operation to perform (one of the constants defined by CSGObject)
  */
  
  public TriangleMesh getMesh(int op, Texture texture)
  {
    Vector<VertexInfo> allVert = new Vector<VertexInfo>();
    Vector<int[]> faceIndex = new Vector<int[]>();
    Vector<float[]> faceSmoothness = new Vector<float[]>();
    int index1[] = new int [vert1.size()], index2[] = new int [vert2.size()];
    int firstBoundary = -1, faces1;

    // Add vertices from object 1.

    for (int i = 0; i < vert1.size(); i++)
      {
        VertexInfo v = vert1.elementAt(i);
        if (v.type == INSIDE && (op == CSGObject.INTERSECTION || op == CSGObject.DIFFERENCE21))
          {
            index1[i] = allVert.size();
            allVert.addElement(v);
          }
        else if (v.type == OUTSIDE && (op == CSGObject.UNION || op == CSGObject.DIFFERENCE12))
          {
            index1[i] = allVert.size();
            allVert.addElement(v);
          }
        else if (v.type == SAME && (op == CSGObject.UNION || op == CSGObject.INTERSECTION))
          {
            index1[i] = allVert.size();
            allVert.addElement(v);
          }
        else if (v.type == OPPOSITE && (op == CSGObject.DIFFERENCE12 || op == CSGObject.DIFFERENCE21))
          {
            index1[i] = allVert.size();
            allVert.addElement(v);
          }
        else if (v.type == BOUNDARY)
          {
            // See if this is the same as a vertex we have already added.
            
            index1[i] = -1;
            if (firstBoundary == -1)
              firstBoundary = allVert.size();
            else
              for (int j = firstBoundary; index1[i] == -1 && j < allVert.size(); j++)
                {
                  VertexInfo v2 = allVert.elementAt(j);
                  if (v2.type == BOUNDARY && areEqual(v2.r, v.r))
                    index1[i] = j;
                }
            if (index1[i] == -1)
              {
                // This is a new vertex.
                
                index1[i] = allVert.size();
                allVert.addElement(v);
              }
          }
      }

    // Add vertices from object 2.

    for (int i = 0; i < vert2.size(); i++)
      {
        VertexInfo v = vert2.elementAt(i);
        if (v.type == INSIDE && (op == CSGObject.INTERSECTION || op == CSGObject.DIFFERENCE12))
          {
            index2[i] = allVert.size();
            allVert.addElement(v);
          }
        else if (v.type == OUTSIDE && (op == CSGObject.UNION || op == CSGObject.DIFFERENCE21))
          {
            index2[i] = allVert.size();
            allVert.addElement(v);
          }
        else if (v.type == BOUNDARY)
          {
            // Find the corresponding vertex from object 1.
            
            index2[i] = -1;
            for (int j = firstBoundary; index2[i] == -1 && j < allVert.size(); j++)
              {
                VertexInfo v2 = allVert.elementAt(j);
                if (v2.type == BOUNDARY && areEqual(v2.r, v.r))
                  index2[i] = j;
              }
            if (index2[i] == -1)
              System.out.println("Unknown!");
          }
      }

    // Add the faces from object 1.

    for (int i = 0; i < face1.size(); i++)
      {
        FaceInfo f = face1.elementAt(i);
        if (f.type == INSIDE && op == CSGObject.INTERSECTION)
          {
            faceIndex.addElement(new int [] {index1[f.v1], index1[f.v2], index1[f.v3]});
            faceSmoothness.addElement(new float [] {f.smoothness1, f.smoothness2, f.smoothness3});
          }
        else if (f.type == INSIDE && op == CSGObject.DIFFERENCE21)
          {
            faceIndex.addElement(new int [] {index1[f.v2], index1[f.v1], index1[f.v3]});
            faceSmoothness.addElement(new float [] {f.smoothness1, f.smoothness3, f.smoothness2});
          }
        else if (f.type == OUTSIDE && (op == CSGObject.UNION || op == CSGObject.DIFFERENCE12))
          {
            faceIndex.addElement(new int [] {index1[f.v1], index1[f.v2], index1[f.v3]});
            faceSmoothness.addElement(new float [] {f.smoothness1, f.smoothness2, f.smoothness3});
          }
        else if (f.type == SAME && (op == CSGObject.UNION || op == CSGObject.INTERSECTION))
          {
            faceIndex.addElement(new int [] {index1[f.v1], index1[f.v2], index1[f.v3]});
            faceSmoothness.addElement(new float [] {f.smoothness1, f.smoothness2, f.smoothness3});
          }
        else if (f.type == OPPOSITE && (op == CSGObject.DIFFERENCE12 || op == CSGObject.DIFFERENCE21))
          {
            faceIndex.addElement(new int [] {index1[f.v1], index1[f.v2], index1[f.v3]});
            faceSmoothness.addElement(new float [] {f.smoothness1, f.smoothness2, f.smoothness3});
          }
      }
    faces1 = faceIndex.size();
    
    // Add the faces from object 2.

    for (int i = 0; i < face2.size(); i++)
      {
        FaceInfo f = face2.elementAt(i);
        if (f.type == INSIDE && op == CSGObject.INTERSECTION)
          {
            faceIndex.addElement(new int [] {index2[f.v1], index2[f.v2], index2[f.v3]});
            faceSmoothness.addElement(new float [] {f.smoothness1, f.smoothness2, f.smoothness3});
          }
        else if (f.type == INSIDE && op == CSGObject.DIFFERENCE12)
          {
            faceIndex.addElement(new int [] {index2[f.v2], index2[f.v1], index2[f.v3]});
            faceSmoothness.addElement(new float [] {f.smoothness1, f.smoothness3, f.smoothness2});
          }
        else if (f.type == OUTSIDE && (op == CSGObject.UNION || op == CSGObject.DIFFERENCE21))
          {
            faceIndex.addElement(new int [] {index2[f.v1], index2[f.v2], index2[f.v3]});
            faceSmoothness.addElement(new float [] {f.smoothness1, f.smoothness2, f.smoothness3});
          }
      }
    
    // If the entire object is eliminated by a difference operation, just return a null mesh.
    
    if (allVert.size() == 0 || faceIndex.size() == 0)
      return new TriangleMesh(new Vec3 [] {new Vec3()}, new int [0][0]);
    
    // Create the triangle mesh.
    
    Vec3 v[] = new Vec3 [allVert.size()];
    for (int i = 0; i < v.length; i++)
      v[i] = new Vec3(allVert.elementAt(i).r);
    int f[][] = new int [faceIndex.size()][];
    for (int i = 0; i < f.length; i++)
      f[i] = faceIndex.elementAt(i);
    TriangleMesh mesh = new TriangleMesh(v, f);
    if (texture != null)
      mesh.setTexture(texture, texture.getDefaultMapping(mesh));
    
    // Copy over the smoothness values.
    
    TriangleMesh.Vertex mv[] = (TriangleMesh.Vertex []) mesh.getVertices();
    for (int i = 0; i < mv.length; i++)
      mv[i].smoothness = allVert.elementAt(i).smoothness;
    TriangleMesh.Edge edge[] = mesh.getEdges();
    TriangleMesh.Face face[] = mesh.getFaces();
    for (int i = 0; i < edge.length; i++)
      {
        for (int k = 0; k < 2; k++)
          {
            int j = (k == 0 ? edge[i].f1 : edge[i].f2);
            if (j == -1)
              continue;
            float smoothness[] = faceSmoothness.elementAt(j), s;
            if (face[j].v1 == edge[i].v1 && face[j].v2 == edge[i].v2)
              s = smoothness[0];
            else if (face[j].v1 == edge[i].v2 && face[j].v2 == edge[i].v1)
              s = smoothness[0];
            else if (face[j].v2 == edge[i].v1 && face[j].v3 == edge[i].v2)
              s = smoothness[1];
            else if (face[j].v2 == edge[i].v2 && face[j].v3 == edge[i].v1)
              s = smoothness[1];
            else if (face[j].v3 == edge[i].v1 && face[j].v1 == edge[i].v2)
              s = smoothness[2];
            else if (face[j].v3 == edge[i].v2 && face[j].v1 == edge[i].v1)
              s = smoothness[2];
            else
              continue;
            edge[i].smoothness = Math.min(edge[i].smoothness, s);
          }
        if ((edge[i].f1 < faces1 && edge[i].f2 >= faces1) || (edge[i].f2 < faces1 && edge[i].f1 >= faces1))
          edge[i].smoothness = 0.0f; // This edge is part of the boundary.
      }

    // Identify any edges with both ends at boundary vertices.  These are
    // candidates for simplifying.

    try
    {
      boolean candidate[] = new boolean [edge.length];
      boolean any = false;
      for (int i = 0; i < edge.length; i++)
      {
        VertexInfo vi1 = allVert.elementAt(edge[i].v1);
        VertexInfo vi2 = allVert.elementAt(edge[i].v2);
        candidate[i] = (vi1.type == BOUNDARY && vi2.type == BOUNDARY);
        any |= candidate[i];
      }
      if (any)
        new TriMeshSimplifier(mesh, candidate, 0.0001, null);
    }
    catch (Exception ex)
    {
      // This sometimes happens due to problems (especially degenerate triangles)
      // in the input objects.  Nothing we can do about it.
    }
    return mesh;
  }
  
  /** Split the faces in one mesh so that they do not intersect the faces of the other mesh. */
  
  private void splitFaces(Vector<VertexInfo> v1, Vector<FaceInfo> f1, BoundingBox bounds1, Vector<VertexInfo> v2, final Vector<FaceInfo> f2, BoundingBox bounds2)
  {
    if (!intersect(bounds1, bounds2))
      return;
    
    int intersectVertA[] = new int [2], intersectVertB[] = new int [2];
    double intersectDistA[] = new double [2], intersectDistB[] = new double [2];
    int intersectTypeA[] = new int [2];
    int spanTypeA;
    double m[][] = new double [3][3], b[] = new double [3];
    Vec3 root = new Vec3();

    // Sort the faces in the second mesh along the main axis.

    Integer faceIndex[] = new Integer[f2.size()];
    for (int i = 0; i < faceIndex.length; i++)
      faceIndex[i] = i;
    Arrays.sort(faceIndex, new Comparator<Integer>() {
      public int compare(Integer index1, Integer index2)
      {
        double max1 = f2.get(index1).max;
        double max2 = f2.get(index2).max;
        if (max1 < max2)
          return -1;
        if (max2 < max1)
          return 1;
        return 0;
      }
    });
    double minAfter[] = new double[f2.size()];
    minAfter[f2.size()-1] = f2.get(faceIndex[f2.size()-1]).min;
    for (int i = faceIndex.length-2; i >= 0; i--)
      minAfter[i] = Math.min(minAfter[i+1], f2.get(faceIndex[i]).min);

p1 :for (int i = 0; i < f1.size(); i++)
      {
        FaceInfo fa = f1.elementAt(i);
        if (!intersect(fa.bounds, bounds2))
          continue;
        VertexInfo va1 = v1.elementAt(fa.v1);
        VertexInfo va2 = v1.elementAt(fa.v2);
        VertexInfo va3 = v1.elementAt(fa.v3);

        // Do a binary search to find the first face we need to intersect against.

        int start = 0;
        int end = f2.size();
        while (end > start+1)
        {
          int mid = (start+end)/2;
          if (f2.get(faceIndex[mid]).max > fa.min+TOL)
            end = mid;
          else
            start = mid;
        }

        // Look for intersecting faces.

        for (int j = start; j < f2.size(); j++)
          {
            FaceInfo fb = f2.elementAt(faceIndex[j]);
            if (minAfter[j] > fa.max+TOL)
              break;
            if (!intersect(fa.bounds, fb.bounds))
              continue;
            
            // Determine whether two faces actually intersect.
            
            VertexInfo vb1 = v2.elementAt(fb.v1);
            VertexInfo vb2 = v2.elementAt(fb.v2);
            VertexInfo vb3 = v2.elementAt(fb.v3);
            double dista1, dista2, dista3, distb1, distb2, distb3;
            dista1 = va1.r.dot(fb.norm)-fb.distRoot;
            dista2 = va2.r.dot(fb.norm)-fb.distRoot;
            dista3 = va3.r.dot(fb.norm)-fb.distRoot;
            if (dista1 > TOL && dista2 > TOL && dista3 > TOL)
              continue;
            if (dista1 < -TOL && dista2 < -TOL && dista3 < -TOL)
              continue;
            int signa1 = (dista1 > TOL ? 1 : (dista1 < -TOL ? -1 : 0));
            int signa2 = (dista2 > TOL ? 1 : (dista2 < -TOL ? -1 : 0));
            int signa3 = (dista3 > TOL ? 1 : (dista3 < -TOL ? -1 : 0));
            if (signa1 == 0 && signa2 == 0 && signa3 == 0)
              continue;
            distb1 = vb1.r.dot(fa.norm)-fa.distRoot;
            distb2 = vb2.r.dot(fa.norm)-fa.distRoot;
            distb3 = vb3.r.dot(fa.norm)-fa.distRoot;
            if (distb1 > TOL && distb2 > TOL && distb3 > TOL)
              continue;
            if (distb1 < -TOL && distb2 < -TOL && distb3 < -TOL)
              continue;
            
            // Find the line of intersection between the planes of the two faces.  Find
            // the span along that line where face A intersects it.

            Vec3 line = fa.norm.cross(fb.norm);
            line.normalize();
            int index = 0;
            if (signa1 == 0)
              {
                intersectVertA[index] = fa.v1;
                intersectDistA[index] = line.dot(va1.r);
                intersectTypeA[index++] = VERTEX;
                if (signa2 == signa3)
                  {
                    intersectVertA[index] = fa.v1;
                    intersectDistA[index] = intersectDistA[index-1];
                    intersectTypeA[index++] = VERTEX;
                  }
              }
            if (signa2 == 0)
              {
                intersectVertA[index] = fa.v2;
                intersectDistA[index] = line.dot(va2.r);
                intersectTypeA[index++] = VERTEX;
                if (signa1 == signa3)
                  {
                    intersectVertA[index] = fa.v2;
                    intersectDistA[index] = intersectDistA[index-1];
                    intersectTypeA[index++] = VERTEX;
                  }
              }
            if (signa3 == 0)
              {
                intersectVertA[index] = fa.v3;
                intersectDistA[index] = line.dot(va3.r);
                intersectTypeA[index++] = VERTEX;
                if (signa1 == signa2)
                  {
                    intersectVertA[index] = fa.v3;
                    intersectDistA[index] = intersectDistA[index-1];
                    intersectTypeA[index++] = VERTEX;
                  }
              }
            if (index == 2)
              {
                if (intersectVertA[0] == intersectVertA[1])
                  spanTypeA = VERTEX;
                else
                  spanTypeA = EDGE;
              }
            else
              {
                if ((signa1 == 1 && signa2 == -1) || (signa1 == -1 && signa2 == 1))
                  {
                    intersectVertA[index] = fa.v1;
                    double fract = dista2/(dista2-dista1);
                    intersectDistA[index] = fract*line.dot(va1.r) + (1.0-fract)*line.dot(va2.r);
                    intersectTypeA[index++] = EDGE;
                  }
                if ((signa2 == 1 && signa3 == -1) || (signa2 == -1 && signa3 == 1))
                  {
                    intersectVertA[index] = fa.v2;
                    double fract = dista3/(dista3-dista2);
                    intersectDistA[index] = fract*line.dot(va2.r) + (1.0-fract)*line.dot(va3.r);
                    intersectTypeA[index++] = EDGE;
                  }
                if ((signa3 == 1 && signa1 == -1) || (signa3 == -1 && signa1 == 1))
                  {
                    intersectVertA[index] = fa.v3;
                    double fract = dista1/(dista1-dista3);
                    intersectDistA[index] = fract*line.dot(va3.r) + (1.0-fract)*line.dot(va1.r);
                    intersectTypeA[index++] = EDGE;
                  }
                spanTypeA = FACE;
              }
            
            // Now do the same for face B.
            
            int signb1 = (distb1 > TOL ? 1 : (distb1 < -TOL ? -1 : 0));
            int signb2 = (distb2 > TOL ? 1 : (distb2 < -TOL ? -1 : 0));
            int signb3 = (distb3 > TOL ? 1 : (distb3 < -TOL ? -1 : 0));
            if (signb1 == 0 && signb2 == 0 && signb3 == 0)
              continue;
            index = 0;
            if (signb1 == 0)
              {
                intersectVertB[index] = fb.v1;
                intersectDistB[index] = line.dot(vb1.r);
                index++;
                if (signb2 == signb3)
                  {
                    intersectVertB[index] = fb.v1;
                    intersectDistB[index] = intersectDistB[index-1];
                    index++;
                  }
              }
            if (signb2 == 0)
              {
                intersectVertB[index] = fb.v2;
                intersectDistB[index] = line.dot(vb2.r);
                index++;
                if (signb1 == signb3)
                  {
                    intersectVertB[index] = fb.v2;
                    intersectDistB[index] = intersectDistB[index-1];
                    index++;
                  }
              }
            if (signb3 == 0)
              {
                intersectVertB[index] = fb.v3;
                intersectDistB[index] = line.dot(vb3.r);
                index++;
                if (signb1 == signb2)
                  {
                    intersectVertB[index] = fb.v3;
                    intersectDistB[index] = intersectDistB[index-1];
                    index++;
                  }
              }
            if (index != 2)
              {
                if ((signb1 == 1 && signb2 == -1) || (signb1 == -1 && signb2 == 1))
                  {
                    intersectVertB[index] = fb.v1;
                    double fract = distb2/(distb2-distb1);
                    intersectDistB[index] = fract*line.dot(vb1.r) + (1.0-fract)*line.dot(vb2.r);
                    index++;
                  }
                if ((signb2 == 1 && signb3 == -1) || (signb2 == -1 && signb3 == 1))
                  {
                    intersectVertB[index] = fb.v2;
                    double fract = distb3/(distb3-distb2);
                    intersectDistB[index] = fract*line.dot(vb2.r) + (1.0-fract)*line.dot(vb3.r);
                    index++;
                  }
                if ((signb3 == 1 && signb1 == -1) || (signb3 == -1 && signb1 == 1))
                  {
                    intersectVertB[index] = fb.v3;
                    double fract = distb1/(distb1-distb3);
                    intersectDistB[index] = fract*line.dot(vb3.r) + (1.0-fract)*line.dot(vb1.r);
                  }
              }

            // Determine whether the spans overlap.
            
            if (Math.max(intersectDistA[0], intersectDistA[1]) < Math.min(intersectDistB[0], intersectDistB[1])+TOL)
              continue;
            if (Math.max(intersectDistB[0], intersectDistB[1]) < Math.min(intersectDistA[0], intersectDistA[1])+TOL)
              continue;
            
            // Ok!  The faces intersect, and we know the positions and types of their
            // spans along the line of intersection.  Now we need to actually subdivide
            // the faces.
            
            m[0][0] = fa.norm.x;
            m[0][1] = fa.norm.y;
            m[0][2] = fa.norm.z;
            m[1][0] = fb.norm.x;
            m[1][1] = fb.norm.y;
            m[1][2] = fb.norm.z;
            m[2][0] = line.x;
            m[2][1] = line.y;
            m[2][2] = line.z;
            b[0] = fa.distRoot;
            b[1] = fb.distRoot;
            b[2] = 0.0;
            SVD.solve(m, b);
            root.set(b[0], b[1], b[2]);
            int oldSize = f1.size();
            splitOneFace(v1, f1, i, intersectVertA, intersectDistA, intersectDistB, intersectTypeA, spanTypeA, line, root);
            if (f1.size() == oldSize)
              continue;
            i--;
            continue p1;
          }
      }
  }
  
  /** Split one face of one of the component objects. */
  
  private void splitOneFace(Vector<VertexInfo> vert, Vector<FaceInfo> face, int which, int intersectVert[], double distA[],
      double distB[], int typeA[], int spanTypeA, Vec3 line, Vec3 root)
  {
    FaceInfo f = face.elementAt(which);
    VertexInfo v1 = vert.elementAt(f.v1);
    VertexInfo v2 = vert.elementAt(f.v2);
    VertexInfo v3 = vert.elementAt(f.v3);
    VertexInfo startVert = vert.elementAt(intersectVert[0]);
    VertexInfo endVert = vert.elementAt(intersectVert[1]);
    int startType, endType;
    double startDist, endDist;
    double startParams[] = null, endParams[] = null;

    if (distA[0] > distA[1])
      {
        double swap1 = distA[0];
        distA[0] = distA[1];
        distA[1] = swap1;
        int swap2 = typeA[0];
        typeA[0] = typeA[1];
        typeA[1] = swap2;
        VertexInfo swap3 = startVert;
        startVert = endVert;
        endVert = swap3;
      }
    if (distB[0] > distB[1])
      {
        double swap1 = distB[0];
        distB[0] = distB[1];
        distB[1] = swap1;
      }
    if (distB[0] > distA[0]+TOL)
      {
        startDist = distB[0];
        startType = spanTypeA;
      }
    else
      {
        startDist = distA[0];
        startType = typeA[0];
      }
    if (distB[1] < distA[1]-TOL)
      {
        endDist = distB[1];
        endType = spanTypeA;
      }
    else
      {
        endDist = distA[1];
        endType = typeA[1];
      }
    if (startType == VERTEX && endType == VERTEX)
      {
        // Vertex-Vertex-Vertex or Vertex-Edge-Vertex.
        
        startVert.type = endVert.type = BOUNDARY;
        return;
      }
    Vec3 startPos = null, endPos = null;
    if (startType == VERTEX)
      startVert.type = BOUNDARY;
    else
      {
        double d = startDist;
        startPos = new Vec3(root.x+d*line.x, root.y+d*line.y, root.z+d*line.z);
        startParams = interpTextureParams(startPos, v1, v2, v3, f);
      }
    if (endType == VERTEX)
      endVert.type = BOUNDARY;
    else
      {
        double d = endDist;
        endPos = new Vec3(root.x+d*line.x, root.y+d*line.y, root.z+d*line.z);
        endParams = interpTextureParams(endPos, v1, v2, v3, f);
      }
    
    // Go through each of the possible combinations of intersection types, and split the
    // face accordingly.
    
    if (spanTypeA == EDGE)
      {
        int splitEdge;
        if ((startVert == v1 && endVert == v2) || (startVert == v2 && endVert == v1))
          splitEdge = 1;
        else if ((startVert == v2 && endVert == v3) || (startVert == v3 && endVert == v2))
          splitEdge = 2;
        else
          splitEdge = 3;
        if (startType == VERTEX)
          {
            // Vertex-Edge-Edge.
            
            int newindex = vert.size();
            vert.addElement(new VertexInfo(endPos, 1.0f, endParams, BOUNDARY));
            face.removeElementAt(which);
            if (splitEdge == 1)
              {
                face.addElement(new FaceInfo(f.v1, newindex, f.v3, vert, startVert == v1 ? 0.0f : f.smoothness1, 1.0f, f.smoothness3));
                face.addElement(new FaceInfo(newindex, f.v2, f.v3, vert, startVert == v2 ? 0.0f : f.smoothness1, f.smoothness2, 1.0f));
              }
            else if (splitEdge == 2)
              {
                face.addElement(new FaceInfo(f.v2, newindex, f.v1, vert, startVert == v2 ? 0.0f : f.smoothness2, 1.0f, f.smoothness1));
                face.addElement(new FaceInfo(newindex, f.v3, f.v1, vert, startVert == v3 ? 0.0f : f.smoothness2, f.smoothness3, 1.0f));
              }
            else
              {
                face.addElement(new FaceInfo(f.v3, newindex, f.v2, vert, startVert == v3 ? 0.0f : f.smoothness3, 1.0f, f.smoothness2));
                face.addElement(new FaceInfo(newindex, f.v1, f.v2, vert, startVert == v1 ? 0.0f : f.smoothness3, f.smoothness1, 1.0f));
              }
            return;
          }
        if (endType == VERTEX)
          {
            // Edge-Edge-Vertex.
            
            int newindex = vert.size();
            vert.addElement(new VertexInfo(startPos, 1.0f, startParams, BOUNDARY));
            face.removeElementAt(which);
            if (splitEdge == 1)
              {
                face.addElement(new FaceInfo(f.v1, newindex, f.v3, vert, endVert == v1 ? 0.0f : f.smoothness1, 1.0f, f.smoothness3));
                face.addElement(new FaceInfo(newindex, f.v2, f.v3, vert, endVert == v2 ? 0.0f : f.smoothness1, f.smoothness2, 1.0f));
              }
            else if (splitEdge == 2)
              {
                face.addElement(new FaceInfo(f.v2, newindex, f.v1, vert, endVert == v2 ? 0.0f : f.smoothness2, 1.0f, f.smoothness1));
                face.addElement(new FaceInfo(newindex, f.v3, f.v1, vert, endVert == v3 ? 0.0f : f.smoothness2, f.smoothness3, 1.0f));
              }
            else
              {
                face.addElement(new FaceInfo(f.v3, newindex, f.v2, vert, endVert == v3 ? 0.0f : f.smoothness3, 1.0f, f.smoothness2));
                face.addElement(new FaceInfo(newindex, f.v1, f.v2, vert, endVert == v1 ? 0.0f : f.smoothness3, f.smoothness1, 1.0f));
              }
            return;
          }
        
        // Edge-Edge-Edge.
        
        if (startDist == endDist)
          {
            // Only create one new triangle.
            
            int newindex = vert.size();
            vert.addElement(new VertexInfo(endPos, 1.0f, endParams, BOUNDARY));
            face.removeElementAt(which);
            if (splitEdge == 1)
              {
                face.addElement(new FaceInfo(f.v1, newindex, f.v3, vert, f.smoothness1, 1.0f, f.smoothness3));
                face.addElement(new FaceInfo(newindex, f.v2, f.v3, vert, f.smoothness1, f.smoothness2, 1.0f));
              }
            else if (splitEdge == 2)
              {
                face.addElement(new FaceInfo(f.v2, newindex, f.v1, vert, f.smoothness2, 1.0f, f.smoothness1));
                face.addElement(new FaceInfo(newindex, f.v3, f.v1, vert, f.smoothness2, f.smoothness3, 1.0f));
              }
            else
              {
                face.addElement(new FaceInfo(f.v3, newindex, f.v2, vert, f.smoothness3, 1.0f, f.smoothness2));
                face.addElement(new FaceInfo(newindex, f.v1, f.v2, vert, f.smoothness3, f.smoothness1, 1.0f));
              }
          }
        else
          {
            // Create two new triangles.
            
            int newindex = vert.size();
            if ((startVert == v1 && endVert == v2) || (startVert == v2 && endVert == v3) || (startVert == v3 && endVert == v1))
              {
                vert.addElement(new VertexInfo(startPos, 1.0f, startParams, BOUNDARY));
                vert.addElement(new VertexInfo(endPos, 1.0f, endParams, BOUNDARY));
              }
            else
              {
                vert.addElement(new VertexInfo(endPos, 1.0f, endParams, BOUNDARY));
                vert.addElement(new VertexInfo(startPos, 1.0f, startParams, BOUNDARY));
              }
            face.removeElementAt(which);
            if (splitEdge == 1)
              {
                face.addElement(new FaceInfo(f.v1, newindex, f.v3, vert, f.smoothness1, 1.0f, f.smoothness3));
                face.addElement(new FaceInfo(newindex, newindex+1, f.v3, vert, 0.0f, 1.0f, 1.0f));
                face.addElement(new FaceInfo(newindex+1, f.v2, f.v3, vert, f.smoothness1, f.smoothness2, 1.0f));
              }
            else if (splitEdge == 2)
              {
                face.addElement(new FaceInfo(f.v2, newindex, f.v1, vert, f.smoothness2, 1.0f, f.smoothness1));
                face.addElement(new FaceInfo(newindex, newindex+1, f.v1, vert, 0.0f, 1.0f, 1.0f));
                face.addElement(new FaceInfo(newindex+1, f.v3, f.v1, vert, f.smoothness2, f.smoothness3, 1.0f));
              }
            else
              {
                face.addElement(new FaceInfo(f.v3, newindex, f.v2, vert, f.smoothness3, 1.0f, f.smoothness2));
                face.addElement(new FaceInfo(newindex, newindex+1, f.v2, vert, 0.0f, 1.0f, 1.0f));
                face.addElement(new FaceInfo(newindex+1, f.v1, f.v2, vert, f.smoothness3, f.smoothness1, 1.0f));
              }
          }
        return;
      }
    if (startType == VERTEX && endType == EDGE)
      {
        // Vertex-Face-Edge.
        
        int newindex = vert.size();
        vert.addElement(new VertexInfo(endPos, 1.0f, endParams, BOUNDARY));
        face.removeElementAt(which);
        if (endVert == v1)
          {
            face.addElement(new FaceInfo(f.v1, newindex, f.v3, vert, f.smoothness1, 0.0f, f.smoothness3));
            face.addElement(new FaceInfo(newindex, f.v2, f.v3, vert, f.smoothness1, f.smoothness2, 0.0f));
          }
        else if (endVert == v2)
          {
            face.addElement(new FaceInfo(f.v2, newindex, f.v1, vert, f.smoothness2, 0.0f, f.smoothness1));
            face.addElement(new FaceInfo(newindex, f.v3, f.v1, vert, f.smoothness2, f.smoothness3, 0.0f));
          }
        else
          {
            face.addElement(new FaceInfo(f.v3, newindex, f.v2, vert, f.smoothness3, 0.0f, f.smoothness2));
            face.addElement(new FaceInfo(newindex, f.v1, f.v2, vert, f.smoothness3, f.smoothness1, 0.0f));
          }
      }
    else if (startType == EDGE && endType == VERTEX)
      {
        // Edge-Face-Vertex.
        
        int newindex = vert.size();
        vert.addElement(new VertexInfo(startPos, 1.0f, startParams, BOUNDARY));
        face.removeElementAt(which);
        if (startVert == v1)
          {
            face.addElement(new FaceInfo(f.v1, newindex, f.v3, vert, f.smoothness1, 0.0f, f.smoothness3));
            face.addElement(new FaceInfo(newindex, f.v2, f.v3, vert, f.smoothness1, f.smoothness2, 0.0f));
          }
        else if (startVert == v2)
          {
            face.addElement(new FaceInfo(f.v2, newindex, f.v1, vert, f.smoothness2, 0.0f, f.smoothness1));
            face.addElement(new FaceInfo(newindex, f.v3, f.v1, vert, f.smoothness2, f.smoothness3, 0.0f));
          }
        else
          {
            face.addElement(new FaceInfo(f.v3, newindex, f.v2, vert, f.smoothness3, 0.0f, f.smoothness2));
            face.addElement(new FaceInfo(newindex, f.v1, f.v2, vert, f.smoothness3, f.smoothness1, 0.0f));
          }
      }
    else if (startType == VERTEX && endType == FACE)
      {
        // Vertex-Face-Face.
        
        int newindex = vert.size();
        vert.addElement(new VertexInfo(endPos, 1.0f, endParams, BOUNDARY));
        face.removeElementAt(which);
        if (startVert == v1)
          {
            face.addElement(new FaceInfo(f.v1, f.v2, newindex, vert, f.smoothness1, 1.0f, 0.0f));
            face.addElement(new FaceInfo(f.v2, f.v3, newindex, vert, f.smoothness2, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v3, f.v1, newindex, vert, f.smoothness3, 0.0f, 1.0f));
          }
        else if (startVert == v2)
          {
            face.addElement(new FaceInfo(f.v2, f.v3, newindex, vert, f.smoothness2, 1.0f, 0.0f));
            face.addElement(new FaceInfo(f.v3, f.v1, newindex, vert, f.smoothness3, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v1, f.v2, newindex, vert, f.smoothness1, 0.0f, 1.0f));
          }
        else
          {
            face.addElement(new FaceInfo(f.v3, f.v1, newindex, vert, f.smoothness3, 1.0f, 0.0f));
            face.addElement(new FaceInfo(f.v1, f.v2, newindex, vert, f.smoothness1, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v2, f.v3, newindex, vert, f.smoothness2, 0.0f, 1.0f));
          }
      }
    else if (startType == FACE && endType == VERTEX)
      {
        // Face-Face-Vertex.
        
        int newindex = vert.size();
        vert.addElement(new VertexInfo(startPos, 1.0f, startParams, BOUNDARY));
        face.removeElementAt(which);
        if (endVert == v1)
          {
            face.addElement(new FaceInfo(f.v1, f.v2, newindex, vert, f.smoothness1, 1.0f, 0.0f));
            face.addElement(new FaceInfo(f.v2, f.v3, newindex, vert, f.smoothness2, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v3, f.v1, newindex, vert, f.smoothness3, 0.0f, 1.0f));
          }
        else if (endVert == v2)
          {
            face.addElement(new FaceInfo(f.v2, f.v3, newindex, vert, f.smoothness2, 1.0f, 0.0f));
            face.addElement(new FaceInfo(f.v3, f.v1, newindex, vert, f.smoothness3, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v1, f.v2, newindex, vert, f.smoothness1, 0.0f, 1.0f));
          }
        else
          {
            face.addElement(new FaceInfo(f.v3, f.v1, newindex, vert, f.smoothness3, 1.0f, 0.0f));
            face.addElement(new FaceInfo(f.v1, f.v2, newindex, vert, f.smoothness1, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v2, f.v3, newindex, vert, f.smoothness2, 0.0f, 1.0f));
          }
      }
    else if (startType == EDGE && endType == EDGE)
      {
        // Edge-Face-Edge.
        
        int newindex = vert.size();
        vert.addElement(new VertexInfo(startPos, 1.0f, startParams, BOUNDARY));
        vert.addElement(new VertexInfo(endPos, 1.0f, endParams, BOUNDARY));
        face.removeElementAt(which);
        if (startVert == v1 && endVert == v2)
          {
            face.addElement(new FaceInfo(f.v1, newindex, newindex+1, vert, f.smoothness1, 0.0f, 1.0f));
            face.addElement(new FaceInfo(f.v1, newindex+1, f.v3, vert, 1.0f, f.smoothness2, f.smoothness3));
            face.addElement(new FaceInfo(newindex, f.v2, newindex+1, vert, f.smoothness1, f.smoothness2, 0.0f));
          }
        else if (startVert == v2 && endVert == v1)
          {
            face.addElement(new FaceInfo(f.v1, newindex+1, newindex, vert, f.smoothness1, 0.0f, 1.0f));
            face.addElement(new FaceInfo(f.v1, newindex, f.v3, vert, 1.0f, f.smoothness2, f.smoothness3));
            face.addElement(new FaceInfo(newindex+1, f.v2, newindex, vert, f.smoothness1, f.smoothness2, 0.0f));
          }
        else if (startVert == v2 && endVert == v3)
          {
            face.addElement(new FaceInfo(f.v2, newindex, newindex+1, vert, f.smoothness2, 0.0f, 1.0f));
            face.addElement(new FaceInfo(f.v2, newindex+1, f.v1, vert, 1.0f, f.smoothness3, f.smoothness1));
            face.addElement(new FaceInfo(newindex, f.v3, newindex+1, vert, f.smoothness2, f.smoothness3, 0.0f));
          }
        else if (startVert == v3 && endVert == v2)
          {
            face.addElement(new FaceInfo(f.v2, newindex+1, newindex, vert, f.smoothness2, 0.0f, 1.0f));
            face.addElement(new FaceInfo(f.v2, newindex, f.v1, vert, 1.0f, f.smoothness3, f.smoothness1));
            face.addElement(new FaceInfo(newindex+1, f.v3, newindex, vert, f.smoothness2, f.smoothness3, 0.0f));
          }
        else if (startVert == v3 && endVert == v1)
          {
            face.addElement(new FaceInfo(f.v3, newindex, newindex+1, vert, f.smoothness3, 0.0f, 1.0f));
            face.addElement(new FaceInfo(f.v3, newindex+1, f.v2, vert, 1.0f, f.smoothness1, f.smoothness2));
            face.addElement(new FaceInfo(newindex, f.v1, newindex+1, vert, f.smoothness3, f.smoothness1, 0.0f));
          }
        else
          {
            face.addElement(new FaceInfo(f.v3, newindex+1, newindex, vert, f.smoothness3, 0.0f, 1.0f));
            face.addElement(new FaceInfo(f.v3, newindex, f.v2, vert, 1.0f, f.smoothness1, f.smoothness2));
            face.addElement(new FaceInfo(newindex+1, f.v1, newindex, vert, f.smoothness3, f.smoothness1, 0.0f));
          }
      }
    else if (startType == EDGE && endType == FACE)
      {
        // Edge-Face-Face.
        
        int newindex = vert.size();
        vert.addElement(new VertexInfo(startPos, 1.0f, startParams, BOUNDARY));
        vert.addElement(new VertexInfo(endPos, 1.0f, endParams, BOUNDARY));
        face.removeElementAt(which);
        if (startVert == v1)
          {
            face.addElement(new FaceInfo(f.v1, newindex, newindex+1, vert, f.smoothness1, 0.0f, 1.0f));
            face.addElement(new FaceInfo(newindex, f.v2, newindex+1, vert, f.smoothness1, 1.0f, 0.0f));
            face.addElement(new FaceInfo(f.v2, f.v3, newindex+1, vert, f.smoothness2, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v3, f.v1, newindex+1, vert, f.smoothness3, 1.0f, 1.0f));
          }
        else if (startVert == v2)
          {
            face.addElement(new FaceInfo(f.v2, newindex, newindex+1, vert, f.smoothness2, 0.0f, 1.0f));
            face.addElement(new FaceInfo(newindex, f.v3, newindex+1, vert, f.smoothness2, 1.0f, 0.0f));
            face.addElement(new FaceInfo(f.v3, f.v1, newindex+1, vert, f.smoothness3, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v1, f.v2, newindex+1, vert, f.smoothness1, 1.0f, 1.0f));
          }
        else
          {
            face.addElement(new FaceInfo(f.v3, newindex, newindex+1, vert, f.smoothness3, 0.0f, 1.0f));
            face.addElement(new FaceInfo(newindex, f.v1, newindex+1, vert, f.smoothness3, 1.0f, 0.0f));
            face.addElement(new FaceInfo(f.v1, f.v2, newindex+1, vert, f.smoothness1, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v2, f.v3, newindex+1, vert, f.smoothness2, 1.0f, 1.0f));
          }
      }
    else if (startType == FACE && endType == EDGE)
      {
        // Face-Face-Edge.
        
        int newindex = vert.size();
        vert.addElement(new VertexInfo(endPos, 1.0f, endParams, BOUNDARY));
        vert.addElement(new VertexInfo(startPos, 1.0f, startParams, BOUNDARY));
        face.removeElementAt(which);
        if (endVert == v1)
          {
            face.addElement(new FaceInfo(f.v1, newindex, newindex+1, vert, f.smoothness1, 0.0f, 1.0f));
            face.addElement(new FaceInfo(newindex, f.v2, newindex+1, vert, f.smoothness1, 1.0f, 0.0f));
            face.addElement(new FaceInfo(f.v2, f.v3, newindex+1, vert, f.smoothness2, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v3, f.v1, newindex+1, vert, f.smoothness3, 1.0f, 1.0f));
          }
        else if (endVert == v2)
          {
            face.addElement(new FaceInfo(f.v2, newindex, newindex+1, vert, f.smoothness2, 0.0f, 1.0f));
            face.addElement(new FaceInfo(newindex, f.v3, newindex+1, vert, f.smoothness2, 1.0f, 0.0f));
            face.addElement(new FaceInfo(f.v3, f.v1, newindex+1, vert, f.smoothness3, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v1, f.v2, newindex+1, vert, f.smoothness1, 1.0f, 1.0f));
          }
        else
          {
            face.addElement(new FaceInfo(f.v3, newindex, newindex+1, vert, f.smoothness3, 0.0f, 1.0f));
            face.addElement(new FaceInfo(newindex, f.v1, newindex+1, vert, f.smoothness3, 1.0f, 0.0f));
            face.addElement(new FaceInfo(f.v1, f.v2, newindex+1, vert, f.smoothness1, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v2, f.v3, newindex+1, vert, f.smoothness2, 1.0f, 1.0f));
          }
      }
    else if (startType == FACE && endType == FACE)
      {
        // Face-Face-Face.  At most one vertex can be on the line of intersection, so find
        // that one (if there is one).
        
        double dx = startPos.x-endPos.x, dy = startPos.y-endPos.y, dz = startPos.z-endPos.z;
        if (dx < TOL && dx > -TOL && dy < TOL && dy > -TOL && dz < TOL && dz > -TOL)
          {
            // The points are at the same location, so only add one new point.
            
            int newindex = vert.size();
            vert.addElement(new VertexInfo(startPos, 1.0f, startParams, BOUNDARY));
            face.removeElementAt(which);
            face.addElement(new FaceInfo(f.v1, f.v2, newindex, vert, f.smoothness1, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v2, f.v3, newindex, vert, f.smoothness2, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v3, f.v1, newindex, vert, f.smoothness3, 1.0f, 1.0f));
            return;
          }
        Vec3 d = new Vec3(endPos.x-v1.r.x, endPos.y-v1.r.y, endPos.z-v1.r.z);
        double dot1 = Math.abs((d.x*dx + d.y*dy + d.z*dz)/d.length());
        d.set(endPos.x-v2.r.x, endPos.y-v2.r.y, endPos.z-v2.r.z);
        double dot2 = Math.abs((d.x*dx + d.y*dy + d.z*dz)/d.length());
        d.set(endPos.x-v3.r.x, endPos.y-v3.r.y, endPos.z-v3.r.z);
        double dot3 = Math.abs((d.x*dx + d.y*dy + d.z*dz)/d.length());
        Vec3 onLinePos;
        int onLine;
        if (dot1 >= dot2 && dot1 >= dot3)
          {
            onLine = 1;
            onLinePos = v1.r;
          }
        else if (dot2 >= dot3 && dot2 >= dot1)
          {
            onLine = 2;
            onLinePos = v2.r;
          }
        else
          {
            onLine = 3;
            onLinePos = v3.r;
          }
        
        // Now find which of the intersection endpoints is nearest to that vertex.

        int newindex = vert.size();
        if (onLinePos.distance(startPos) > onLinePos.distance(endPos))
          {
            vert.addElement(new VertexInfo(startPos, 1.0f, startParams, BOUNDARY));
            vert.addElement(new VertexInfo(endPos, 1.0f, endParams, BOUNDARY));
          }
        else
          {
            vert.addElement(new VertexInfo(endPos, 1.0f, endParams, BOUNDARY));
            vert.addElement(new VertexInfo(startPos, 1.0f, startParams, BOUNDARY));
          }
        face.removeElementAt(which);
        if (onLine == 3)
          {
            face.addElement(new FaceInfo(f.v1, f.v2, newindex, vert, f.smoothness1, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v1, newindex, newindex+1, vert, 1.0f, 0.0f, 1.0f));
            face.addElement(new FaceInfo(f.v2, newindex+1, newindex, vert, 1.0f, 0.0f, 1.0f));
            face.addElement(new FaceInfo(f.v1, newindex+1, f.v3, vert, 1.0f, 1.0f, f.smoothness3));
            face.addElement(new FaceInfo(f.v2, f.v3, newindex+1, vert, f.smoothness2, 1.0f, 1.0f));
          }
        else if (onLine == 1)
          {
            face.addElement(new FaceInfo(f.v2, f.v3, newindex, vert, f.smoothness2, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v2, newindex, newindex+1, vert, 1.0f, 0.0f, 1.0f));
            face.addElement(new FaceInfo(f.v3, newindex+1, newindex, vert, 1.0f, 0.0f, 1.0f));
            face.addElement(new FaceInfo(f.v2, newindex+1, f.v1, vert, 1.0f, 1.0f, f.smoothness1));
            face.addElement(new FaceInfo(f.v3, f.v1, newindex+1, vert, f.smoothness3, 1.0f, 1.0f));
          }
        else
          {
            face.addElement(new FaceInfo(f.v3, f.v1, newindex, vert, f.smoothness3, 1.0f, 1.0f));
            face.addElement(new FaceInfo(f.v3, newindex, newindex+1, vert, 1.0f, 0.0f, 1.0f));
            face.addElement(new FaceInfo(f.v1, newindex+1, newindex, vert, 1.0f, 0.0f, 1.0f));
            face.addElement(new FaceInfo(f.v3, newindex+1, f.v2, vert, 1.0f, 1.0f, f.smoothness2));
            face.addElement(new FaceInfo(f.v1, f.v2, newindex+1, vert, f.smoothness1, 1.0f, 1.0f));
          }
      }
  }
  
  /** Determine which vertices of one object are inside or outside the other object. */
  
  private void findInsideVertices(Vector v1, Vector f1, Vector v2, Vector f2)
  {
    // Make a list of the faces sharing each vertex.
    
    int faceCount[] = new int [v1.size()];
    for (int i = 0; i < f1.size(); i++)
      {
        FaceInfo f = (FaceInfo) f1.elementAt(i);
        faceCount[f.v1]++;
        faceCount[f.v2]++;
        faceCount[f.v3]++;
      }
    int vertFace[][] = new int [v1.size()][];
    for (int i = 0; i < vertFace.length; i++)
      {
        vertFace[i] = new int [faceCount[i]];
        faceCount[i] = 0;
      }
    for (int i = 0; i < f1.size(); i++)
      {
        FaceInfo f = (FaceInfo) f1.elementAt(i);
        vertFace[f.v1][faceCount[f.v1]++] = i;
        vertFace[f.v2][faceCount[f.v2]++] = i;
        vertFace[f.v3][faceCount[f.v3]++] = i;
      }
    
    // Loop over the faces, and determine whether they are inside or outside.
    
    for (int i = 0; i < f1.size(); i++)
      {
        FaceInfo f = (FaceInfo) f1.elementAt(i);
        
        if (f.type != UNKNOWN)
          continue;
        f.type = classifyFace(f, v1, v2, f2);
        
        // Mark the vertices of this face, and any adjacent faces.
        
        VertexInfo vi1 = (VertexInfo) v1.elementAt(f.v1);
        VertexInfo vi2 = (VertexInfo) v1.elementAt(f.v2);
        VertexInfo vi3 = (VertexInfo) v1.elementAt(f.v3);
        int type = f.type;
//        if (type == SAME || type == OPPOSITE)
//          continue;
        if (vi1.type == UNKNOWN)
          markVertex(f.v1, type, v1, f1, vertFace, 0);
        if (vi2.type == UNKNOWN)
          markVertex(f.v2, type, v1, f1, vertFace, 0);
        if (vi3.type == UNKNOWN)
          markVertex(f.v3, type, v1, f1, vertFace, 0);
      }
  }
  
  /** Determine whether a particular face is inside or outside the other object. */
  
  private int classifyFace(FaceInfo f, Vector v1, Vector v2, Vector f2)
  {
    VertexInfo vi1 = (VertexInfo) v1.elementAt(f.v1);
    VertexInfo vi2 = (VertexInfo) v1.elementAt(f.v2);
    VertexInfo vi3 = (VertexInfo) v1.elementAt(f.v3);
    Vec3 orig = new Vec3(), dir = new Vec3(f.norm);

    // Send a ray out from the center of this face, and see what the first thing is
    // that it intersects.

    orig.set(vi1.r.x+vi2.r.x+vi3.r.x, vi1.r.y+vi2.r.y+vi3.r.y, vi1.r.z+vi2.r.z+vi3.r.z);
    orig.scale(1.0/3.0);
    int first;
    double firstDist;
    do
      {
        first = -1;
        firstDist = Double.MAX_VALUE;
        for (int j = 0; j < f2.size(); j++)
          {
            FaceInfo fb = (FaceInfo) f2.elementAt(j);
            double dist = rayBoxIntersectionDist(orig, dir, fb.bounds);
            if (dist >= firstDist)
              continue;
            VertexInfo vb1 = (VertexInfo) v2.elementAt(fb.v1);
            VertexInfo vb2 = (VertexInfo) v2.elementAt(fb.v2);
            VertexInfo vb3 = (VertexInfo) v2.elementAt(fb.v3);
            dist = rayFaceIntersectionDist(orig, dir, fb, vb1.r, vb2.r, vb3.r);
            if (dist < firstDist)
              {
                first = j;
                firstDist = dist;
              }
            if (dist == -Double.MAX_VALUE)
              break;
          }
        if (firstDist == -Double.MAX_VALUE)
          {
            // Randomly perturb the ray and try again.
            
            dir.x += 1e-5*Math.random();
            dir.y += 1e-5*Math.random();
            dir.z += 1e-5*Math.random();
          }
      } while (firstDist == -Double.MAX_VALUE);
    if (firstDist == Double.MAX_VALUE)
      return OUTSIDE;
    double dot = dir.dot(((FaceInfo) f2.elementAt(first)).norm);
    if (firstDist > -TOL && firstDist < TOL)
      {
        if (dot > 0.0)
          return SAME;
        return OPPOSITE;
      }
    if (dot > 0.0)
      return INSIDE;
    return OUTSIDE;
  }
  
  /** Determine whether two bounding boxes intersect, to within the minimum tolerance. */
  
  private boolean intersect(BoundingBox b1, BoundingBox b2)
  {
    if (b1.minx > b2.maxx+TOL || b1.maxx < b2.minx-TOL || b1.miny > b2.maxy+TOL || b1.maxy < b2.miny-TOL || b1.minz > b2.maxz+TOL || b1.maxz < b2.minz-TOL)
      return false;
    return true;
  }

  /** Determine whether a ray intersects the bounding box.  Return the distance along the
      ray at which it enters the box, or Double.MAX_VALUE if it does not intersect. */
  
  private double rayBoxIntersectionDist(Vec3 origin, Vec3 direction, BoundingBox bb)
  {
    double t1, t2, mint = -Double.MAX_VALUE, maxt = Double.MAX_VALUE;
    if (direction.x == 0.0)
      {
        if (origin.x < bb.minx || origin.x > bb.maxx)
          return Double.MAX_VALUE;
      }
    else
      {
        t1 = (bb.minx-origin.x)/direction.x;
        t2 = (bb.maxx-origin.x)/direction.x;
        if (t1 < t2)
          {
            if (t1 > mint)
              mint = t1;
            if (t2 < maxt)
              maxt = t2;
          }
        else
          {
            if (t2 > mint)
              mint = t2;
            if (t1 < maxt)
              maxt = t1;
          }
        if (mint > maxt || maxt < 0.0)
          return Double.MAX_VALUE;
      }
    if (direction.y == 0.0)
      {
        if (origin.y < bb.miny || origin.y > bb.maxy)
          return Double.MAX_VALUE;
      }
    else
      {
        t1 = (bb.miny-origin.y)/direction.y;
        t2 = (bb.maxy-origin.y)/direction.y;
        if (t1 < t2)
          {
            if (t1 > mint)
              mint = t1;
            if (t2 < maxt)
              maxt = t2;
          }
        else
          {
            if (t2 > mint)
              mint = t2;
            if (t1 < maxt)
              maxt = t1;
          }
        if (mint > maxt || maxt < 0.0)
          return Double.MAX_VALUE;
      }
    if (direction.z == 0.0)
      {
        if (origin.z < bb.minz || origin.z > bb.maxz)
          return Double.MAX_VALUE;
      }
    else
      {
        t1 = (bb.minz-origin.z)/direction.z;
        t2 = (bb.maxz-origin.z)/direction.z;
        if (t1 < t2)
          {
            if (t1 > mint)
              mint = t1;
            if (t2 < maxt)
              maxt = t2;
          }
        else
          {
            if (t2 > mint)
              mint = t2;
            if (t1 < maxt)
              maxt = t1;
          }
        if (mint > maxt || maxt < 0.0)
          return Double.MAX_VALUE;
      }
    return mint;
  }

  /** Determine whether a ray intersects a triangle.  Return the distance along the
      ray at which it enters the triangle, or Double.MAX_VALUE if it does not intersect.
      If the ray lies in the plane of the triangle, or passes through an edge of it,
      return -Double.MAX_VALUE. */
  
  private double rayFaceIntersectionDist(Vec3 orig, Vec3 dir, FaceInfo f, Vec3 v1, Vec3 v2, Vec3 v3)
  {
    double vd = f.norm.dot(dir);
    double v0 = f.norm.x*(v1.x-orig.x) + f.norm.y*(v1.y-orig.y) + f.norm.z*(v1.z-orig.z);
    if (vd > -TOL && vd < TOL)
      {
        // The ray is parallel to the plane.
        
        if (v0 > -TOL && v0 < TOL)
          return -Double.MAX_VALUE;
        return Double.MAX_VALUE;
      }
    double t = v0/vd;
    if (t < -TOL)
      return Double.MAX_VALUE;  // Ray points away from plane of triangle.

    // Determine whether the intersection point is inside the triangle.

    Vec3 ri = new Vec3(orig.x+dir.x*t, orig.y+dir.y*t, orig.z+dir.z*t);
    Vec2 edge2d1, edge2d2;
    double vx, vy;
    if (f.norm.x > 0.5 || f.norm.x < -0.5)
      {
        edge2d1 = new Vec2(v1.y-v2.y, v1.z-v2.z);
        edge2d2 = new Vec2(v1.y-v3.y, v1.z-v3.z);
        vx = ri.y - v1.y;
        vy = ri.z - v1.z;
      }
    else if (f.norm.y > 0.5 || f.norm.y < -0.5)
      {
        edge2d1 = new Vec2(v1.x-v2.x, v1.z-v2.z);
        edge2d2 = new Vec2(v1.x-v3.x, v1.z-v3.z);
        vx = ri.x - v1.x;
        vy = ri.z - v1.z;
      }
    else
      {
        edge2d1 = new Vec2(v1.x-v2.x, v1.y-v2.y);
        edge2d2 = new Vec2(v1.x-v3.x, v1.y-v3.y);
        vx = ri.x - v1.x;
        vy = ri.y - v1.y;
      }
    double denom = 1.0/edge2d1.cross(edge2d2);
    double u, v, w;
    v = (edge2d2.x*vy - edge2d2.y*vx)*denom;
    if (v < -TOL || v > 1.0+TOL)
      return Double.MAX_VALUE;
//    if (v < TOL || v > 1.0-TOL)
//      return -Double.MAX_VALUE;
    w = (vx*edge2d1.y - vy*edge2d1.x)*denom;
    if (w < -TOL || w > 1.0+TOL)
      return Double.MAX_VALUE;
//    if (w < TOL || w > 1.0-TOL)
//      return -Double.MAX_VALUE;
    u = 1.0-v-w;
    if (u < -TOL || u > 1.0+TOL)
      return Double.MAX_VALUE;
//    if (u < TOL || u > 1.0-TOL)
//      return -Double.MAX_VALUE;
    return t;
  }
  
  /* Mark a vertex as inside or outside, the recursively call this routine for vertices
     of adjacent faces. */

  private void markVertex(int which, int value, Vector v1, Vector f1, int vertFace[][], int stackDepth)
  {
    VertexInfo v = (VertexInfo) v1.elementAt(which);
    v.type = value;
    if (stackDepth == 500)
      return; // Limit recursion to prevent stack overflows.
    for (int i = 0; i < vertFace[which].length; i++)
      {
        FaceInfo f = (FaceInfo) f1.elementAt(vertFace[which][i]);
        if (f.type == UNKNOWN || f.type == value)
        {
          f.type = value;
          VertexInfo vi1 = (VertexInfo) v1.elementAt(f.v1);
          VertexInfo vi2 = (VertexInfo) v1.elementAt(f.v2);
          VertexInfo vi3 = (VertexInfo) v1.elementAt(f.v3);
          if (vi1.type == UNKNOWN)
            markVertex(f.v1, value, v1, f1, vertFace, stackDepth+1);
          if (vi2.type == UNKNOWN)
            markVertex(f.v2, value, v1, f1, vertFace, stackDepth+1);
          if (vi3.type == UNKNOWN)
            markVertex(f.v3, value, v1, f1, vertFace, stackDepth+1);
        }
      }
  }
  
  /* Given the three vertices of a face and a point incide that face, calculate
     the texture parameters for that point. */
  
  private double [] interpTextureParams(Vec3 pos, VertexInfo v1, VertexInfo v2, VertexInfo v3, FaceInfo f)
  {
    if (v1.param == null)
      return null;
    Vec2 edge2d1, edge2d2;
    double vx, vy;
    if (f.norm.x > 0.5 || f.norm.x < -0.5)
      {
        edge2d1 = new Vec2(v1.r.y-v2.r.y, v1.r.z-v2.r.z);
        edge2d2 = new Vec2(v1.r.y-v3.r.y, v1.r.z-v3.r.z);
        vx = pos.y - v1.r.y;
        vy = pos.z - v1.r.z;
      }
    else if (f.norm.y > 0.5 || f.norm.y < -0.5)
      {
        edge2d1 = new Vec2(v1.r.x-v2.r.x, v1.r.z-v2.r.z);
        edge2d2 = new Vec2(v1.r.x-v3.r.x, v1.r.z-v3.r.z);
        vx = pos.x - v1.r.x;
        vy = pos.z - v1.r.z;
      }
    else
      {
        edge2d1 = new Vec2(v1.r.x-v2.r.x, v1.r.y-v2.r.y);
        edge2d2 = new Vec2(v1.r.x-v3.r.x, v1.r.y-v3.r.y);
        vx = pos.x - v1.r.x;
        vy = pos.y - v1.r.y;
      }
    double denom = 1.0/edge2d1.cross(edge2d2);
    double u, v, w;
    v = (edge2d2.x*vy - edge2d2.y*vx)*denom;
    w = (vx*edge2d1.y - vy*edge2d1.x)*denom;
    u = 1.0-v-w;
    double param[] = new double [v1.param.length];
    for (int i = 0; i < param.length; i++)
      param[i] = u*v1.param[i] + v*v2.param[i] + w*v3.param[i];
    return param;
  }

  /**
   * Get whether two points are equal to within a tolerance.
   */

  private static boolean areEqual(Vec3 a, Vec3 b)
  {
    double dist = a.distance(b);
    double norm = a.length();
    if (norm < 1.0)
      return (dist < TOL);
    return (dist < TOL*norm);
  }
  
  /* Inner classes for keeping track of information about vertices and faces. */

  private static class VertexInfo
  {
    Vec3 r;
    float smoothness;
    double param[];
    int type;
    
    public VertexInfo(Vec3 r, float smoothness, double param[])
    {
      this.r = r;
      this.smoothness = smoothness;
      this.param = param;
      type = UNKNOWN;
    }

    public VertexInfo(Vec3 r, float smoothness, double param[], int type)
    {
      this.r = r;
      this.smoothness = smoothness;
      this.param = param;
      this.type = type;
    }
  }

  private class FaceInfo
  {
    int v1, v2, v3;
    int type;
    BoundingBox bounds;
    Vec3 norm;
    float smoothness1, smoothness2, smoothness3;
    double distRoot, min, max;
    
    public FaceInfo(int v1, int v2, int v3, Vector vertices, float s1, float s2, float s3)
    {
      this.v1 = v1;
      this.v2 = v2;
      this.v3 = v3;
      smoothness1 = s1;
      smoothness2 = s2;
      smoothness3 = s3;
      type = UNKNOWN;
      Vec3 vert1 = ((VertexInfo) vertices.elementAt(v1)).r;
      Vec3 vert2 = ((VertexInfo) vertices.elementAt(v2)).r;
      Vec3 vert3 = ((VertexInfo) vertices.elementAt(v3)).r;
      double minx, miny, minz, maxx, maxy, maxz;
      minx = Math.min(Math.min(vert1.x, vert2.x), vert3.x);
      miny = Math.min(Math.min(vert1.y, vert2.y), vert3.y);
      minz = Math.min(Math.min(vert1.z, vert2.z), vert3.z);
      maxx = Math.max(Math.max(vert1.x, vert2.x), vert3.x);
      maxy = Math.max(Math.max(vert1.y, vert2.y), vert3.y);
      maxz = Math.max(Math.max(vert1.z, vert2.z), vert3.z);
      bounds = new BoundingBox(minx, maxx, miny, maxy, minz, maxz);
      norm = vert2.minus(vert1).cross(vert3.minus(vert1));
      double length = norm.length();
      if (length > 0.0)
        norm.scale(1.0/length);
      distRoot = vert1.dot(norm);
      if (mainAxis == 0)
      {
        min = minx;
        max = maxx;
      }
      else if (mainAxis == 1)
      {
        min = miny;
        max = maxy;
      }
      else
      {
        min = minz;
        max = maxz;
      }
    }
  }
}