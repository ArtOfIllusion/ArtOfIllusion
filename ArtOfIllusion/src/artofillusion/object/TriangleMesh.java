/* Copyright (C) 1999-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;
import java.util.*;

/** The TriangleMesh class represents an aritrary surface defined by a mesh of triangular 
    faces.  Depending on the selected smoothing method, the surface may simply consist of
    the triangular faces, or it may be a smooth subdivision surface which either interpolates
    or approximates the vertices of the control mesh. */

public class TriangleMesh extends Object3D implements FacetedMesh
{

  /** A vertex specifies a position vector, the number of edges which share the vertex, and
      the "first" edge.  If the vertex is in the interior of the mesh, any edge can be the
      first one.  If it is on the boundary, then the first edge must be one of the two boundary
      edges.  A vertex also has a "smoothness" parameter associated with it. */

  public class Vertex extends MeshVertex
  {
    public int edges, firstEdge;
    public float smoothness;
    
    public Vertex(Vec3 p)
    {
      super(p);
      edges = 0;
      firstEdge = -1;
      smoothness = 1.0f;
    }
    
    public Vertex(Vertex v)
    {
      super(v);
      edges = v.edges;
      firstEdge = v.firstEdge;
      smoothness = v.smoothness;
    }
    
    /** Make this vertex identical to another one. */
    
    public void copy(Vertex v)
    {
      r.set(v.r);
      edges = v.edges;
      firstEdge = v.firstEdge;
      smoothness = v.smoothness;
      ikJoint = v.ikJoint;
      ikWeight = v.ikWeight;
    }
    
    /** Multiple the fields of this vertex by a constant. */
    
    public void scale(double d)
    {
      r.scale(d);
      smoothness *= d;
      ikWeight *= d;
    }
    
    /** Set the various fields to zero. */
    
    public void clear()
    {
      r.set(0.0, 0.0, 0.0);
      smoothness = 0.0f;
      ikWeight = 0.0;
    }

    /** Construct a list of all edges which share the vertex. */

    public int[] getEdges()
    {
      int e[] = new int [edges], i;
      if (edges == 0)
        return e;
      Face f = face[edge[firstEdge].f1];
      e[0] = firstEdge;
      for (i = 1; i < edges; i++)
        {
          if (vertex[f.v1] == this)
            {
              if (f.e1 == e[i-1])
                e[i] = f.e3;
              else
                e[i] = f.e1;
            }
          else if (vertex[f.v2] == this)
            {
              if (f.e1 == e[i-1])
                e[i] = f.e2;
              else
                e[i] = f.e1;
            }
          else
            {
              if (f.e2 == e[i-1])
                e[i] = f.e3;
              else
                e[i] = f.e2;
            }
          if (face[edge[e[i]].f1] == f && edge[e[i]].f2 > -1)
            f = face[edge[e[i]].f2];
          else
            f = face[edge[e[i]].f1];
        }
      return e;
    }

    /** This method tells whether the list of edges returned by getEdges() are ordered
        clockwise or counter-clockwise. */

    public boolean clockwise()
    {
      Face f = face[edge[firstEdge].f1];
      
      if (f.e1 == firstEdge)
        return (vertex[f.v2] == this);
      else if (f.e2 == firstEdge)
        return (vertex[f.v3] == this);
      else
        return (vertex[f.v1] == this);
    }
  }

  /** An edge is defined by the two vertices which it connects, and the two faces it is
      adjacent to.  For a boundary edge, f2 must equal -1.  An edge also has a "smoothness"
      parameter. */

  public class Edge
  {
    public int v1, v2, f1, f2;
    public float smoothness;

    public Edge(int vertex1, int vertex2, int face1)
    {
      v1 = vertex1;
      v2 = vertex2;
      f1 = face1;
      f2 = -1;
      smoothness = 1.0f;
    }
  }

  /** A face is defined by its three vertices and three edges.  The vertices must be arranged
      in counter-clockwise order, when viewed from the outside.  Edges 1, 2, and 3 connect
      vertices 1 and 2, 2 and 3, and 3 and 1 respectively. */

  public class Face
  {
    public int v1, v2, v3, e1, e2, e3;
    
    public Face(int vertex1, int vertex2, int vertex3, int edge1, int edge2, int edge3)
    {
      v1 = vertex1;
      v2 = vertex2;
      v3 = vertex3;
      e1 = edge1;
      e2 = edge2;
      e3 = edge3;
    }

    /** Given another face, return the index of the edge it shares with this one, or -1 if
        they do not share an edge. */

    public int getSharedFace(Face f)
    {
      if (f.e1 == e1 || f.e2 == e1 || f.e3 == e1)
        return e1;
      if (f.e1 == e2 || f.e2 == e2 || f.e3 == e2)
        return e2;
      if (f.e1 == e3 || f.e2 == e3 || f.e3 == e3)
        return e3;
      return -1;
    }
  }
  
  /* Beginning of TriangleMesh's variables and methods. */
  
  private Vertex vertex[];
  private Edge edge[];
  private Face face[];
  private Skeleton skeleton;
  private boolean closed;
  private BoundingBox bounds;
  private int smoothingMethod = SMOOTH_SHADING;
  private RenderingMesh cachedMesh;
  private WireframeMesh cachedWire;
  
  private static double LOOP_BETA[], BUTTERFLY_COEFF[][];
  private static final int MAX_SUBDIVISIONS = 20;
  private static final Property PROPERTIES[] = new Property [] {
    new Property(Translate.text("menu.smoothingMethod"), new Object[] {
      Translate.text("menu.none"), Translate.text("menu.shading"), Translate.text("menu.interpolating"), Translate.text("menu.approximating")
    }, Translate.text("menu.shading"))
  };


  /* The following constants are used during subdivision for recording parameter types. */
  
  private static final int PER_OBJECT = 0;
  private static final int PER_VERTEX = 1;
  private static final int PER_FACE = 2;
  private static final int PER_FACE_VERTEX = 3;
  
  /* Precalculate coefficients for Loop and Butterfly subdivision. */

  static {
    double beta;
    int i, j;
    
    LOOP_BETA = new double [32];
    for (i = 3; i < LOOP_BETA.length; i++)
      {
        beta = 0.375+0.25*Math.cos(2.0*Math.PI/i);
        LOOP_BETA[i] = (0.625-beta*beta)/i;
      }
    BUTTERFLY_COEFF = new double [32][];
    for (i = 5; i < BUTTERFLY_COEFF.length; i++)
      {
        BUTTERFLY_COEFF[i] = new double [i+1];
        BUTTERFLY_COEFF[i][i] = 1.0;
        beta = 2.0*Math.PI/i;
        for (j = 0; j < i; j++)
          {
            BUTTERFLY_COEFF[i][j] = (0.25+Math.cos(beta*j)+0.5*Math.cos(2.0*beta*j))/i;
            BUTTERFLY_COEFF[i][i] -= BUTTERFLY_COEFF[i][j];
          }
      }
    BUTTERFLY_COEFF[3] = new double [] {5.0/12.0, -1.0/12.0, -1.0/12.0, 0.75};
    BUTTERFLY_COEFF[4] = new double [] {.375, 0.0, -0.125, 0.0, 0.75};
    BUTTERFLY_COEFF[6] = new double [] {1.0, 0.125, -0.125, 0.0, -0.125, 0.125, 0.0};
  }
  
  /** The constructor takes three arguments.  v[] is an array containing the vertices.
      faces[][] is an N by 3 array containing the indices of the vertices which define each
      face.  The vertices for each face must be listed in order, such that they go
      counter-clockwise when viewed from the outside of the mesh.  All faces must have a
      consistent vertex order, such that the object has a well defined outer surface.
      This is true even if the mesh does not form a closed surface.  It is an error to
      call the constructor with a faces[][] array which does not meet this condition, and
      the results are undefined.  norm[] contains the normal vector at each vertex.  If
      any element of norm[] is null, flat shading will be used around that vertex. */

  public TriangleMesh(Vec3 v[], int faces[][])
  {
    setSkeleton(new Skeleton());
    Vertex vt[] = new Vertex [v.length];
    for (int i = 0; i < v.length; i++)
      vt[i] = new Vertex(v[i]);
    setShape(vt, faces);
  }
  
  public TriangleMesh(Vertex v[], int faces[][])
  {
    setSkeleton(new Skeleton());
    setShape(v, faces);
  }
  
  protected TriangleMesh()
  {
  }

  /** Create a duplicate of this object. */

  public Object3D duplicate()
  {
    TriangleMesh mesh = new TriangleMesh();
    mesh.copyObject(this);
    return mesh;
  }

  /** Make this object exactly like another one. */

  public void copyObject(Object3D obj)
  {
    TriangleMesh mesh = (TriangleMesh) obj;
    
    texParam = null;
    vertex = new Vertex [mesh.vertex.length];
    edge = new Edge [mesh.edge.length];
    face = new Face [mesh.face.length];
    for (int i = 0; i < mesh.vertex.length; i++)
      vertex[i] = new Vertex(mesh.vertex[i]);
    for (int i = 0; i < mesh.edge.length; i++)
      {
        edge[i] = new Edge(mesh.edge[i].v1, mesh.edge[i].v2, mesh.edge[i].f1);
        edge[i].f2 = mesh.edge[i].f2;
        edge[i].smoothness = mesh.edge[i].smoothness;
      }
    for (int i = 0; i < mesh.face.length; i++)
      face[i] = new Face(mesh.face[i].v1, mesh.face[i].v2, mesh.face[i].v3, mesh.face[i].e1, mesh.face[i].e2, mesh.face[i].e3);
    if (skeleton == null)
      skeleton = mesh.skeleton.duplicate();
    else
      skeleton.copy(mesh.skeleton);
    setSmoothingMethod(mesh.getSmoothingMethod());
    closed = mesh.closed;
    copyTextureAndMaterial(obj);
  }

  /** Construct the list of edges. */

  void findEdges(int faces[][])
  {
    int i, numEdges1 = 0, numEdges2 = 0, numCopied = 0;
    int faceEdges[][] = new int [faces.length][3], copiedEdges[] = new int [faces.length*3];
    Edge edges1[] = new Edge [faces.length*3], edges2[] = new Edge [faces.length*3];

    // If the mesh is closed, then each edge should be traversed twice, once in each 
    // direction.  If the mesh is open, some edges will be traversed only once, which 
    // could be in either direction.
    
    closed = true;
    for (i = 0; i < faces.length; i++)
      {
        if (faces[i][0] > faces[i][1])
          edges1[faceEdges[i][0] = numEdges1++] = new Edge(faces[i][0], faces[i][1], i);
        else
          {
            edges2[faceEdges[i][0] = numEdges2++] = new Edge(faces[i][0], faces[i][1], i);
            faceEdges[i][0] += edges1.length;
          }
        if (faces[i][1] > faces[i][2])
          edges1[faceEdges[i][1] = numEdges1++] = new Edge(faces[i][1], faces[i][2], i);
        else
          {
            edges2[faceEdges[i][1] = numEdges2++] = new Edge(faces[i][1], faces[i][2], i);
            faceEdges[i][1] += edges1.length;
          }
        if (faces[i][2] > faces[i][0])
          edges1[faceEdges[i][2] = numEdges1++] = new Edge(faces[i][2], faces[i][0], i);
        else
          {
            edges2[faceEdges[i][2] = numEdges2++] = new Edge(faces[i][2], faces[i][0], i);
            faceEdges[i][2] += edges1.length;
          }
      }
    if (numEdges1 != numEdges2)
      closed = false;
    
    // We now have two lists of edges: one for each direction of traversal.  Determine which
    // which ones are duplicates, and add any unique edges from edges2 into edges1.
    
    Hashtable<Point, Integer> edgeTable = new Hashtable<Point, Integer>();
    for (i = 0; i < numEdges1; i++)
      edgeTable.put(new Point(edges1[i].v1, edges1[i].v2), i);
    for (i = 0; i < numEdges2; i++)
      {
        Integer index = edgeTable.get(new Point(edges2[i].v2, edges2[i].v1));
        if (index == null)
          {
            copiedEdges[i] = numEdges1+numCopied++;
            edges1[copiedEdges[i]] = edges2[i];
          }
        else
          {
            copiedEdges[i] = index;
            edges1[index].f2 = edges2[i].f1;
          }
      }
    if (numCopied > 0)
      closed = false;
    
    // Record the edges for each face.
    
    for (i = 0; i < faces.length; i++)
      {
        if (faceEdges[i][0] >= edges1.length)
          faceEdges[i][0] = copiedEdges[faceEdges[i][0]-edges1.length];
        if (faceEdges[i][1] >= edges1.length)
          faceEdges[i][1] = copiedEdges[faceEdges[i][1]-edges1.length];
        if (faceEdges[i][2] >= edges1.length)
          faceEdges[i][2] = copiedEdges[faceEdges[i][2]-edges1.length];
      }

    // Construct the edges and faces.

    edge = new Edge [numEdges1+numCopied];
    for (i = 0; i < numEdges1+numCopied; i++)
      edge[i] = edges1[i];
    face = new Face [faces.length];
    for (i = 0; i < faces.length; i++)
      face[i] = new Face(faces[i][0], faces[i][1], faces[i][2], faceEdges[i][0], faceEdges[i][1], faceEdges[i][2]);
  }

  /** Calculate the (approximate) bounding box for the mesh. */

  private void findBounds()
  {
    double minx, miny, minz, maxx, maxy, maxz;
    Vec3 vert[];
    int i;
    
    if (cachedMesh != null)
      vert = cachedMesh.vert;
    else if (cachedWire != null)
      vert = cachedWire.vert;
    else
    {
      getWireframeMesh();
      vert = cachedWire.vert;
    }
    if (vert.length == 0)
      minx = maxx = miny = maxy = minz = maxz = 0.0;
    else
    {
      minx = maxx = vert[0].x;
      miny = maxy = vert[0].y;
      minz = maxz = vert[0].z;
      for (i = 1; i < vert.length; i++)
      {
        if (vert[i].x < minx) minx = vert[i].x;
        if (vert[i].x > maxx) maxx = vert[i].x;
        if (vert[i].y < miny) miny = vert[i].y;
        if (vert[i].y > maxy) maxy = vert[i].y;
        if (vert[i].z < minz) minz = vert[i].z;
        if (vert[i].z > maxz) maxz = vert[i].z;
      }
    }
    bounds = new BoundingBox(minx, maxx, miny, maxy, minz, maxz);
  }

  /** Get the bounding box for the mesh.  This is always the bounding box for the unsmoothed
      control mesh.  If the smoothing method is set to approximating, the final surface may not
      actually touch the sides of this box.  If the smoothing method is set to interpolating,
      the final surface may actually extend outside this box. */

  public BoundingBox getBounds()
  {
    if (bounds == null)
      findBounds();
    return bounds;
  }

  /** These methods return the lists of vertices, edges, and faces for the mesh. */

  public MeshVertex[] getVertices()
  {
    return vertex;
  }
  
  public Edge[] getEdges()
  {
    return edge;
  }
  
  public Face[] getFaces()
  {
    return face;
  }

  /** Get the smoothing method being used for this mesh. */

  public int getSmoothingMethod()
  {
    return smoothingMethod;
  }
  
  /** Get a list of the positions of all vertices which define the mesh. */
  
  public Vec3 [] getVertexPositions()
  {
    Vec3 v[] = new Vec3 [vertex.length];
    for (int i = 0; i < v.length; i++)
      v[i] = new Vec3(vertex[i].r);
    return v;
  }

  /** Set the positions for all the vertices of the mesh. */

  public void setVertexPositions(Vec3 v[])
  {
    for (int i = 0; i < v.length; i++)
      vertex[i].r = v[i];
    cachedMesh = null;
    cachedWire = null;
    bounds = null;
  }
  
  /** Set the smoothing method. */

  public void setSmoothingMethod(int method)
  {
    smoothingMethod = method;
    cachedMesh = null;
    cachedWire = null;
    bounds = null;
  }
  
  /** This method rebuilds the mesh based on new lists of vertices and faces.  The smoothness
      values for all edges are lost in the process. */
  
  public void setShape(Vertex v[], int faces[][])
  {
    Vertex v1, v2;
    int i;
    
    // Create the vertices and edges.
    
    vertex = new Vertex [v.length];
    for (i = 0; i < v.length; i++)
      {
        vertex[i] = new Vertex(v[i]);
        vertex[i].firstEdge = -1;
        vertex[i].edges = 0;
      }
    if (faces.length == 0)
      {
        edge = new Edge [0];
        face = new Face [0];
      }
    else
      findEdges(faces);
    cachedMesh = null;
    cachedWire = null;
    bounds = null;
    
    // Find the edge information for vertices.
    
    for (i = 0; i < edge.length; i++)
      {
        v1 = vertex[edge[i].v1];
        v2 = vertex[edge[i].v2];
        v1.edges++;
        v2.edges++;
        if (edge[i].f2 == -1)
          v1.firstEdge = v2.firstEdge = i;
        else
          {
            if (v1.firstEdge == -1)
              v1.firstEdge = i;
            if (v2.firstEdge == -1)
              v2.firstEdge = i;
          }
      }
  }
  
  public boolean isClosed()
  {
    return closed;
  }

  public void setSize(double xsize, double ysize, double zsize)
  {
    Vec3 size = getBounds().getSize();
    double xscale, yscale, zscale;
    
    if (size.x == 0.0)
      xscale = 1.0;
    else
      xscale = xsize / size.x;
    if (size.y == 0.0)
      yscale = 1.0;
    else
      yscale = ysize / size.y;
    if (size.z == 0.0)
      zscale = 1.0;
    else
      zscale = zsize / size.z;
    for (int i = 0; i < vertex.length; i++)
      {
        vertex[i].r.x *= xscale;
        vertex[i].r.y *= yscale;
        vertex[i].r.z *= zscale;
      }
    if (xscale*yscale*zscale < 0.0)
      reverseNormals();
    skeleton.scale(xscale, yscale, zscale);
    cachedMesh = null;
    cachedWire = null;
    bounds = null;
  }
  
  /** Calculate a set of array representing the boundaries of this mesh.  There is one array
      for each distinct boundary, containing the indices of the edges which form that
      boundary. */
  
  public int [][] findBoundaryEdges()
  {
    // First, find every edge which is on a boundary.
    
    Vector<Integer> allEdges = new Vector<Integer>();
    for (int i = 0; i < edge.length; i++)
      if (edge[i].f2 == -1)
        allEdges.addElement(i);
    
    // Form boundaries one at a time.
    
    Vector<Vector<Integer>> boundary = new Vector<Vector<Integer>>();
    while (allEdges.size() > 0)
      {
        // Take one edge as a starting point, and follow around.

        Vector<Integer> current = new Vector<Integer>();
        Integer start = allEdges.elementAt(0);
        allEdges.removeElementAt(0);
        current.addElement(start);
        int i = start, j = 0;
        while (j < (allEdges.size()))
          {
            for (j = 0; j < allEdges.size(); j++)
              {
                int k = allEdges.elementAt(j);
                if (edge[i].v1 == edge[k].v1 || edge[i].v1 == edge[k].v2 ||
                    edge[i].v2 == edge[k].v1 || edge[i].v2 == edge[k].v2)
                  {
                    current.addElement(allEdges.elementAt(j));
                    allEdges.removeElementAt(j);
                    i = k;
                    j--;
                    break;
                  }
              }
          }
        boundary.addElement(current);
      }
    
    // Build the final arrays.
    
    int index[][] = new int [boundary.size()][];
    for (int i = 0; i < index.length; i++)
      {
        Vector<Integer> current = boundary.elementAt(i);
        index[i] = new int [current.size()];
        for (int j = 0; j < index[i].length; j++)
          index[i][j] = current.elementAt(j);
      }
    return index;
  }

  public boolean isEditable()
  {
    return true;
  }
  
  public void edit(final EditingWindow parent, ObjectInfo info, Runnable cb)
  {
    TriMeshEditorWindow ed = new TriMeshEditorWindow(parent, "Triangle Mesh '"+ info.getName() +"'", info, cb, true);
    ed.setVisible(true);
  }

  public void editGesture(final EditingWindow parent, ObjectInfo info, Runnable cb, ObjectInfo realObject)
  {
    TriMeshEditorWindow ed = new TriMeshEditorWindow(parent, "Gesture '"+ info.getName() +"'", info, cb, false);
    ViewerCanvas views[] = ed.getAllViews();
    for (int i = 0; i < views.length; i++)
      ((MeshViewer) views[i]).setScene(parent.getScene(), realObject);
    ed.setVisible(true);
  }
  
  /** Get a MeshViewer which can be used for viewing this mesh. */
  
  public MeshViewer createMeshViewer(MeshEditController controller, RowContainer options)
  {
    return new TriMeshViewer(controller, options);
  }

  public int canConvertToTriangleMesh()
  {
    if (smoothingMethod == NO_SMOOTHING || smoothingMethod == SMOOTH_SHADING)
      return EXACTLY;
    return APPROXIMATELY;
  }

  /** Get a more finely subdivided version of this mesh. */
  
  public TriangleMesh convertToTriangleMesh(double tol)
  {
    boolean split[];

    if (smoothingMethod == INTERPOLATING || smoothingMethod == APPROXIMATING)
      {
        split = new boolean [edge.length];
        for (int i = 0; i < split.length; i++)
          split[i] = true;
        if (smoothingMethod == INTERPOLATING)
          return subdivideButterfly(this, split, tol);
        return subdivideLoop(this, split, tol);
      }
    return (TriangleMesh) duplicate();
  }

  public WireframeMesh getWireframeMesh()
  {
    TriangleMesh mesh = this;
    Vertex v[];
    Edge e[];
    Vec3 point[];
    int i, from[], to[];
    boolean split[];

    if (cachedWire != null)
      return cachedWire;
    
    // If appropriate, subdivide the mesh.
    
    if (smoothingMethod == INTERPOLATING || smoothingMethod == APPROXIMATING)
      {
        split = new boolean [edge.length];
        for (i = 0; i < split.length; i++)
          split[i] = true;
        if (smoothingMethod == INTERPOLATING)
          mesh = subdivideButterfly(mesh, split, ArtOfIllusion.getPreferences().getInteractiveSurfaceError());
        else
          mesh = subdivideLoop(mesh, split, ArtOfIllusion.getPreferences().getInteractiveSurfaceError());
        v = mesh.vertex;
        e = mesh.edge;
      }
    else
      {
        v = vertex;
        e = edge;
      }
    point = new Vec3 [v.length];
    from = new int [e.length];
    to = new int [e.length];
    for (i = 0; i < v.length; i++)
      point[i] = v[i].r;
    for (i = 0; i < e.length; i++)
      {
        from[i] = e[i].v1;
        to[i] = e[i].v2;
      }
    return (cachedWire = new WireframeMesh(point, from, to));
  }

  public RenderingMesh getRenderingMesh(double tol, boolean interactive, ObjectInfo info)
  {
    TriangleMesh mesh = this;
    Vec3 vert[], normalArray[];
    Vector<Vec3> norm;
    Vertex v[];
    Edge e[];
    Face f[], tempFace;
    RenderingTriangle tri[];
    int i, j, k, m, first, last, normals, ed[], facenorm[];
    boolean split[];

    if (interactive && cachedMesh != null)
      return cachedMesh;
    if (face.length == 0)
    {
      RenderingMesh rend = new RenderingMesh(new Vec3 [] {new Vec3()}, new Vec3 [] {Vec3.vx()}, new RenderingTriangle [0], texMapping, matMapping);
      rend.setParameters(mesh.paramValue);
      return rend;      
    }
    
    // If appropriate, subdivide the mesh.
    
    if (smoothingMethod == INTERPOLATING || smoothingMethod == APPROXIMATING)
      {
        double tol2 = tol*tol;
        Vec3 diff = new Vec3();
        split = new boolean [edge.length];
        for (i = 0; i < split.length; i++)
          {
            Vec3 r1 = vertex[edge[i].v1].r, r2 = vertex[edge[i].v2].r;
            diff.set(r1.x-r2.x, r1.y-r2.y, r1.z-r2.z);
            split[i] = (diff.length2() > tol2);
          }
        if (smoothingMethod == INTERPOLATING)
          mesh = subdivideButterfly(mesh, split, tol);
        else
          mesh = subdivideLoop(mesh, split, tol);
        v = mesh.vertex;
        e = mesh.edge;
        f = mesh.face;
      }
    else
      {
        v = vertex;
        e = edge;
        f = face;
      }

    // Create the RenderingMesh.

    vert = new Vec3 [v.length];
    norm = new Vector<Vec3>();
    tri = new RenderingTriangle [f.length];
    facenorm = new int [f.length*3];
    normals = 0;
    k = last = 0;
    if (smoothingMethod != NO_SMOOTHING)
      {
        // The mesh needs to be smooth shaded, so we need to calculate the normal vectors.
        // There may be more than one normal associated with a vertex, if that vertex is
        // on a crease.  Begin by finding a "true" normal for each face.
        
        Vec3 trueNorm[] = new Vec3 [f.length];
        for (i = 0; i < f.length; i++)
          {
            trueNorm[i] = v[f[i].v2].r.minus(v[f[i].v1].r).cross(v[f[i].v3].r.minus(v[f[i].v1].r));
            double length = trueNorm[i].length();
            if (length > 0.0)
              trueNorm[i].scale(1.0/length);
          }
        
        // Now loop over each vertex.
        
        for (i = 0; i < v.length; i++)
          {
            vert[i] = v[i].r;
            ed = v[i].getEdges();
            
            // If this vertex is a corner, we can just set its normal to null.
            
            if (v[i].smoothness < 1.0f)
              {
                norm.addElement(null);
                for (j = 0; j < ed.length; j++)
                  {
                    k = e[ed[j]].f1;
                    tempFace = f[k];
                    if (tempFace.v1 == i)
                      facenorm[k*3] = normals;
                    else if (tempFace.v2 == i)
                      facenorm[k*3+1] = normals;
                    else
                      facenorm[k*3+2] = normals;
                    k = e[ed[j]].f2;
                    if (k != -1)
                      {
                        tempFace = f[k];
                        if (tempFace.v1 == i)
                          facenorm[k*3] = normals;
                        else if (tempFace.v2 == i)
                          facenorm[k*3+1] = normals;
                        else
                          facenorm[k*3+2] = normals;
                      }
                  }
                normals++;
                continue;
              }
            
            // If any of the edges intersecting this vertex are creases, we need to start at
            // one of them.
            
            for (j = 0, k = -1; j < ed.length; j++)
              {
                Edge tempEdge = e[ed[j]];
                if (tempEdge.f2 == -1 || tempEdge.smoothness < 1.0f)
                  {
                    if (k != -1)
                      break;
                    k = j;
                  }
              }

            if (j == ed.length)
              {
                // There are 0 or 1 crease edges intersecting this vertex, so we will use
                // the same normal for every face.  Find it by averaging the normals of all
                // the faces sharing this point.
                
                Vec3 temp = new Vec3();
                int faceIndex = -1;
                for (j = 0; j < ed.length; j++)
                  {
                    Edge tempEdge = e[ed[j]];
                    faceIndex = (tempEdge.f1 == faceIndex ? tempEdge.f2 : tempEdge.f1);
                    int otherFace = (tempEdge.f1 == faceIndex ? tempEdge.f2 : tempEdge.f1);
                    tempFace = f[faceIndex];
                    Vec3 edge1 = v[tempFace.v2].r.minus(v[tempFace.v1].r);
                    Vec3 edge2 = v[tempFace.v3].r.minus(v[tempFace.v1].r);
                    Vec3 edge3 = v[tempFace.v3].r.minus(v[tempFace.v2].r);
                    if (edge1.length2() < 1e-20 || edge2.length2() < 1e-20 || edge3.length2() < 1e-20)
                      continue;
                    edge1.normalize();
                    edge2.normalize();
                    edge3.normalize();
                    double dot;
                    if (tempFace.v1 == i)
                    {
                      facenorm[faceIndex*3] = normals;
                      dot = edge1.dot(edge2);
                    }
                    else if (tempFace.v2 == i)
                    {
                      facenorm[faceIndex*3+1] = normals;
                      dot = -edge1.dot(edge3);
                    }
                    else
                    {
                      facenorm[faceIndex*3+2] = normals;
                      dot = edge2.dot(edge3);
                    }
                    if (dot < -1.0)
                      dot = -1.0; // This can occassionally happen due to roundoff error
                    if (dot > 1.0)
                      dot = 1.0;
                    temp.add(trueNorm[faceIndex].times(Math.acos(dot)));
                    if (otherFace != -1)
                      {
                        tempFace = f[otherFace];
                        if (tempFace.v1 == i)
                          facenorm[otherFace*3] = normals;
                        else if (tempFace.v2 == i)
                          facenorm[otherFace*3+1] = normals;
                        else
                          facenorm[otherFace*3+2] = normals;
                      }
                  }
                temp.normalize();
                norm.addElement(temp);
                normals++;
                continue;
              }

            // This vertex is intersected by at least two crease edges, so we need to
            // calculate a normal vector for each group of faces between two creases.
            
            first = j = k;
            Edge tempEdge = e[ed[j]];
groups:     do
              {
                Vec3 temp = new Vec3();
                do
                  {
                    // For each group of faces, find the first and last edges.  Average
                    // the normals of the faces in between, and record that these faces 
                    // will use this normal.

                    j = (j+1) % ed.length;
                    m = tempEdge.f1;
                    tempFace = f[m];
                    if (tempFace.e1 != ed[j] && tempFace.e2 != ed[j] && tempFace.e3 != ed[j])
                      {
                        m = tempEdge.f2;
                        if (m == -1)
                          break groups;
                        tempFace = f[m];
                      }
                    Vec3 edge1 = v[tempFace.v2].r.minus(v[tempFace.v1].r);
                    Vec3 edge2 = v[tempFace.v3].r.minus(v[tempFace.v1].r);
                    Vec3 edge3 = v[tempFace.v3].r.minus(v[tempFace.v2].r);
                    edge1.normalize();
                    edge2.normalize();
                    edge3.normalize();
                    double dot;
                    if (tempFace.v1 == i)
                    {
                      facenorm[m*3] = normals;
                      dot = edge1.dot(edge2);
                    }
                    else if (tempFace.v2 == i)
                    {
                      facenorm[m*3+1] = normals;
                      dot = -edge1.dot(edge3);
                    }
                    else
                    {
                      facenorm[m*3+2] = normals;
                      dot = edge2.dot(edge3);
                    }
                    if (dot < -1.0)
                      dot = -1.0; // This can occassionally happen due to roundoff error
                    if (dot > 1.0)
                      dot = 1.0;
                    temp.add(trueNorm[m].times(Math.acos(dot)));
                    tempEdge = e[ed[j]];
                  } while (tempEdge.f2 != -1 && tempEdge.smoothness == 1.0f);
                last = j;
                temp.normalize();
                norm.addElement(temp);
                normals++;
                j = first = last;
                tempEdge = e[ed[first]];
              } while (last != k);
          }
            
        // Finally, assemble all the normals into an array and create the triangles.
            
        normalArray = new Vec3 [norm.size()];
        for (i = 0; i < normalArray.length; i++)
          normalArray[i] = (Vec3) norm.elementAt(i);
        for (i = 0; i < f.length; i++)
          {
            tempFace = mesh.face[i];
            tri[i] = texMapping.mapTriangle(tempFace.v1, tempFace.v2, tempFace.v3, 
                    facenorm[i*3], facenorm[i*3+1], facenorm[i*3+2], vert);
          }
      }
    else
      {
        // The mesh is not being smooth shaded, so all the normals can be set to null.
        
        normalArray = new Vec3 [] {null};
        for (i = 0; i < v.length; i++)
          vert[i] = v[i].r;
        for (i = 0; i < f.length; i++)
          tri[i] = texMapping.mapTriangle(f[i].v1, f[i].v2, f[i].v3, 0, 0, 0, vert);
      }
    RenderingMesh rend = new RenderingMesh(vert, normalArray, tri, texMapping, matMapping);
    rend.setParameters(mesh.paramValue);
    if (interactive)
      cachedMesh = rend;
    return rend;
  }

  /** When setting the texture, we need to clear the caches. */
  
  public void setTexture(Texture tex, TextureMapping mapping)
  {
    super.setTexture(tex, mapping);
    cachedMesh = null;
    cachedWire = null;
  }

  /** When setting texture parameters, we need to clear the caches. */

  public void setParameterValues(ParameterValue val[])
  {
    super.setParameterValues(val);
    cachedMesh = null;
  }
  
  /** When setting texture parameters, we need to clear the caches. */
  
  public void setParameterValue(TextureParameter param, ParameterValue val)
  {
    super.setParameterValue(param, val);
    cachedMesh = null;
  }

  /** Get the skeleton for this object. */

  public Skeleton getSkeleton()
  {
    return skeleton;
  }
  
  /** Set the skeleton for this object. */

  public void setSkeleton(Skeleton s)
  {
    skeleton = s;
  }
    
  /** Create a vertex which is a blend of two existing ones. */

  private Vertex blend(Vertex v1, Vertex v2, double w1, double w2)
  {
    return new Vertex (new Vec3(w1*v1.r.x + w2*v2.r.x, w1*v1.r.y + w2*v2.r.y, w1*v1.r.z + w2*v2.r.z));
  }
    
  /** Create a vertex which is a blend of three existing ones. */

  private Vertex blend(Vertex v1, Vertex v2, Vertex v3, double w1, double w2, double w3)
  {
    return new Vertex (new Vec3(w1*v1.r.x + w2*v2.r.x + w3*v3.r.x, w1*v1.r.y + w2*v2.r.y + w3*v3.r.y, w1*v1.r.z + w2*v2.r.z + w3*v3.r.z));
  }
    
  /** Set a vertex to be a blend of two other ones. */

  private static void setBlend(Vertex v, Vertex v1, Vertex v2, double w1, double w2)
  {
    v.r.set(w1*v1.r.x + w2*v2.r.x, w1*v1.r.y + w2*v2.r.y, w1*v1.r.z + w2*v2.r.z);
  }
  
  /** Given a pair of vertices and a new vertex that is to be created between them, find the
      IK binding parameters for the new vertex. */
  
  private static void blendIKParams(Vertex newvert, Vertex v1, Vertex v2)
  {
    if (v1.ikJoint == v2.ikJoint)
      {
        newvert.ikJoint = v1.ikJoint;
        newvert.ikWeight = 0.5*(v1.ikWeight+v2.ikWeight);
      }
    else if (v1.ikWeight > v2.ikWeight)
      {
        newvert.ikJoint = v1.ikJoint;
        newvert.ikWeight = v1.ikWeight;
      }
    else
      {
        newvert.ikJoint = v2.ikJoint;
        newvert.ikWeight = v2.ikWeight;
      }
  }
  
  /** When creating a new vertex during subdivision, calculate per-vertex parameter values for the
      new vertex. */
  
  private static void blendParamValues(double oldValues[][][], double newValues[][][], int paramType[], int v1, int v2, int newv)
  {
    for (int i = 0; i < paramType.length; i++)
      if (paramType[i] == PER_VERTEX)
        newValues[i][0][newv] = 0.5*(oldValues[i][0][v1]+oldValues[i][0][v2]);
  }
  
  /** Set the per-vertex texture parameters for a newly created vertex. */
  
  private static void setBlendParams(double newValues[], double vert1Val[], int v2, double w1, double w2, double oldVertValues[][][], int paramType[])
  {
    for (int i = 0; i < paramType.length; i++)
      if (paramType[i] == PER_VERTEX)
        newValues[i] = w1*vert1Val[i]+w2*oldVertValues[i][0][v2];
  }
  
  /** Set the per-vertex texture parameters for a newly created vertex. */
  
  private static void setBlendParams(double newValues[], int v1, int v2, double w1, double w2, double oldVertValues[][][], int paramType[])
  {
    for (int i = 0; i < paramType.length; i++)
      if (paramType[i] == PER_VERTEX)
        newValues[i] = w1*oldVertValues[i][0][v1]+w2*oldVertValues[i][0][v2];
  }
  
  /** Copy the per-vertex texture parameter values for a vertex into an array. */
  
  private static void recordParamValues(double values[], int v, double vertValues[][][], int paramType[])
  {
    for (int i = 0; i < paramType.length; i++)
      if (paramType[i] == PER_VERTEX)
        values[i] = vertValues[i][0][v];
  }

  /** Subdivide all or part of the mesh using the mesh's defined smoothing method (linear, approximating, or
      interpolating).
      
      @param mesh         the mesh to subdivide
      @param splitEdge    a flag for each edge, specifying which ones should be split.  If this is null,
                          every edge will be split.
      @param tol          the error tolerance.  Edges will be repeatedly subdivided until every point on the
                          subdivided mesh is within this distance of the limit surface.  If this is equal to
                          Double.MAX_VALUE, each edge will be subdivided exactly once.
      @return the subdivided mesh
  */
  
  public static TriangleMesh subdivideEdges(TriangleMesh mesh, boolean splitEdge[], double tol)
  {
    if (mesh.smoothingMethod == INTERPOLATING)
      return subdivideButterfly(mesh, splitEdge, tol);
    if (mesh.smoothingMethod == APPROXIMATING)
      return subdivideLoop(mesh, splitEdge, tol);
    return subdivideLinear(mesh, splitEdge);
  }

  /** This method subdivides each selected edge once, placing a new vertex in the midpoint
      of the edge, and returns the subdivided mesh.
      
      @param mesh    the mesh to subdivide
      @param split   a flag for each edge, specifying which ones should be split.  If this is null,
                     every edge will be split.
      @return the subdivided mesh
  */

  public static TriangleMesh subdivideLinear(TriangleMesh mesh, boolean split[])
  {
    Vertex vertex[] = mesh.vertex, newvert[];
    Edge edge[] = mesh.edge, newedge[];
    Face face[] = mesh.face, newface[];
    TriangleMesh newmesh = new TriangleMesh();
    int i, j, numVert, numEdge, numFace;

    if (split == null)
      {
        split = new boolean [edge.length];
        for (i = 0; i < split.length; i++)
          split[i] = true;
      }
    
    // Determine how many vertices, faces, and edges will be in the new mesh, and
    // create arrays.

    numVert = vertex.length;
    numEdge = edge.length;
    numFace = face.length;
    for (i = 0; i < edge.length; i++)
      if (split[i])
        {
          numVert++;
          numEdge += 2;
          numFace++;
          if (edge[i].f2 != -1)
            {
              numEdge++;
              numFace++;
            }
        }
    newvert = new Vertex [numVert];
    newedge = new Edge [numEdge];
    newface = new Face [numFace];
    
    // Create arrays for parameter values.
    
    int paramType[] = new int [mesh.paramValue.length];
    double newParamValue[][][] = new double [mesh.paramValue.length][][];
    double oldParamValue[][][] = new double [mesh.paramValue.length][][];
    for (i = 0; i < mesh.paramValue.length; i++)
    {
      if (mesh.paramValue[i] instanceof ConstantParameterValue)
        paramType[i] = PER_OBJECT;
      else if (mesh.paramValue[i] instanceof VertexParameterValue)
      {
        oldParamValue[i] = new double [][] {((VertexParameterValue) mesh.paramValue[i]).getValue()};
        newParamValue[i] = new double [1][numVert];
        paramType[i] = PER_VERTEX;
      }
      else if (mesh.paramValue[i] instanceof FaceParameterValue)
      {
        oldParamValue[i] = new double [][] {((FaceParameterValue) mesh.paramValue[i]).getValue()};
        newParamValue[i] = new double [1][numFace];
        paramType[i] = PER_FACE;
      }
      else if (mesh.paramValue[i] instanceof FaceVertexParameterValue)
      {
        FaceVertexParameterValue fvpv = (FaceVertexParameterValue) mesh.paramValue[i];
        oldParamValue[i] = new double [3][fvpv.getFaceCount()];
        for (int index = 0; index < fvpv.getFaceCount(); index++)
        {
          oldParamValue[i][0][index] = fvpv.getValue(index, 0);
          oldParamValue[i][1][index] = fvpv.getValue(index, 1);
          oldParamValue[i][2][index] = fvpv.getValue(index, 2);
        }
        newParamValue[i] = new double [3][numFace];
        paramType[i] = PER_FACE_VERTEX;
      }
    }

    // Copy over the old vertices, and then create the new ones.

    for (i = 0; i < vertex.length; i++)
    {
      newvert[i] = newmesh.new Vertex(vertex[i]);
      for (j = 0; j < paramType.length; j++)
        if (paramType[j] == PER_VERTEX)
          newParamValue[j][0][i] = oldParamValue[j][0][i];
    }
    for (j = 0; j < edge.length; j++)
      if (split[j])
        {
          newvert[i] = newmesh.blend(vertex[edge[j].v1], vertex[edge[j].v2], 0.5, 0.5);
          blendParamValues(oldParamValue, newParamValue, paramType, edge[j].v1, edge[j].v2, i);
          blendIKParams(newvert[i++], vertex[edge[j].v1], vertex[edge[j].v2]);
        }

    // Subdivide the mesh.

    doSubdivide(newmesh, vertex, edge, face, split, newvert, newedge, newface, oldParamValue, newParamValue, paramType);
    newmesh.copyTextureAndMaterial(mesh);
    for (i = 0; i < paramType.length; i++)
    {
      if (paramType[i] == PER_VERTEX)
        newmesh.paramValue[i] = new VertexParameterValue(newParamValue[i][0]);
      else if (paramType[i] == PER_FACE)
        newmesh.paramValue[i] = new FaceParameterValue(newParamValue[i][0]);
      else if (paramType[i] == PER_FACE_VERTEX)
      {
        double val[][] = new double [newParamValue[i][0].length][];
        for (int index = 0; index < val.length; index++)
          val[index] = new double [] {newParamValue[i][0][index], newParamValue[i][1][index], newParamValue[i][2][index]};
        newmesh.paramValue[i] = new FaceVertexParameterValue(val);
      }
    }
    newmesh.vertex = newvert;
    newmesh.edge = newedge;
    newmesh.face = newface;
    newmesh.closed = mesh.closed;
    newmesh.smoothingMethod = mesh.smoothingMethod;
    newmesh.skeleton = mesh.skeleton.duplicate();
    return newmesh;
  }

  /** This method subdivides the mesh using approximating (Loop) subdivision, and returns a new 
      TriangleMesh which approximates the limit surface to within the specified tolerance. 
      The subdivision coefficients are taken (with a few minor changes) from
      Hoppe et al. "Piecewise Smooth Surface Reconstruction." SIGGRAPH Proceedings, 
      1994, p. 295.  The algorithm for creating semi-smooth points and creases is original.
      
      @param mesh         the mesh to subdivide
      @param refineEdge   a flag for each edge, specifying which ones should be split.  If this is null,
                          every edge will be split.
      @param tol          the error tolerance.  Edges will be repeatedly subdivided until every point on the
                          subdivided mesh is within this distance of the limit surface.  If this is equal to
                          Double.MAX_VALUE, each edge will be subdivided exactly once.
      @return the subdivided mesh
  */
     
  public static TriangleMesh subdivideLoop(TriangleMesh mesh, boolean refineEdge[], double tol)
  {
    Vertex vertex[] = mesh.vertex, newvert[];
    Edge edge[] = mesh.edge, newedge[], tempEdge;
    Face face[] = mesh.face, newface[], tempFace;
    TriangleMesh newmesh = new TriangleMesh();
    int i, j, e[];
    int numVert, numEdge, numFace;
    Vertex creasePos = newmesh.new Vertex(new Vec3()), smoothPos = newmesh.new Vertex(new Vec3()), temp = newmesh.new Vertex(new Vec3());
    Vec3 finalPos = new Vec3(), tempVec = new Vec3();
    double cornerWeight, creaseWeight, smoothWeight, s1, s2, s3, beta;
    boolean refineVert[], notconverged[], done;
    double tol2 = tol*tol, error;

    if (refineEdge == null)
      {
        refineEdge = new boolean [edge.length];
        for (i = 0; i < refineEdge.length; i++)
          refineEdge[i] = true;
      }

    // Determine which vertices need to be refined.
    
    refineVert = new boolean [vertex.length];
    for (i = 0; i < refineEdge.length; i++)
      if (refineEdge[i])
            refineVert[edge[i].v1] = refineVert[edge[i].v2] = true;
    
    // Record parameter values.
    
    int paramType[] = new int [mesh.paramValue.length];
    double oldParamValue[][][] = new double [mesh.paramValue.length][][];
    for (i = 0; i < mesh.paramValue.length; i++)
    {
      if (mesh.paramValue[i] instanceof ConstantParameterValue)
        paramType[i] = PER_OBJECT;
      else if (mesh.paramValue[i] instanceof VertexParameterValue)
      {
        oldParamValue[i] = new double [][] {((VertexParameterValue) mesh.paramValue[i]).getValue()};
        paramType[i] = PER_VERTEX;
      }
      else if (mesh.paramValue[i] instanceof FaceParameterValue)
      {
        oldParamValue[i] = new double [][] {((FaceParameterValue) mesh.paramValue[i]).getValue()};
        paramType[i] = PER_FACE;
      }
      else if (mesh.paramValue[i] instanceof FaceVertexParameterValue)
      {
        FaceVertexParameterValue fvpv = (FaceVertexParameterValue) mesh.paramValue[i];
        oldParamValue[i] = new double [3][fvpv.getFaceCount()];
        for (int index = 0; index < fvpv.getFaceCount(); index++)
        {
          oldParamValue[i][0][index] = fvpv.getValue(index, 0);
          oldParamValue[i][1][index] = fvpv.getValue(index, 1);
          oldParamValue[i][2][index] = fvpv.getValue(index, 2);
        }
        paramType[i] = PER_FACE_VERTEX;
      }
    }
    double creaseParam[] = new double [paramType.length];
    double smoothParam[] = new double [paramType.length];
    double tempParam[] = new double [paramType.length];
    double finalParam[] = new double [paramType.length];

    // Repeatedly subdivide until all portions of the mesh have converged.
    
    int iterations = 0;
    do
      {
        done = true;
        notconverged = new boolean [vertex.length];
        
        // Determine how many vertices, faces, and edges will be in the new mesh, and
        // create arrays.
        
        numVert = vertex.length;
        numEdge = edge.length;
        numFace = face.length;
        for (i = 0; i < edge.length; i++)
          if (refineEdge[i])
            {
              numVert++;
              numEdge += 2;
              numFace++;
              if (edge[i].f2 != -1)
                {
                  numEdge++;
                  numFace++;
                }
            }
        newvert = new Vertex [numVert];
        newedge = new Edge [numEdge];
        newface = new Face [numFace];
        double newParamValue[][][] = new double [paramType.length][][];
        for (i = 0; i < paramType.length; i++)
        {
          if (paramType[i] == PER_VERTEX)
            newParamValue[i] = new double [1][numVert];
          else if (paramType[i] == PER_FACE)
            newParamValue[i] = new double [1][numFace];
          else if (paramType[i] == PER_FACE_VERTEX)
            newParamValue[i] = new double [3][numFace];
        }

        // Step 1: Find the new positions for existing vertices.  Positions can be calculated by 
        // three different rules: corner, crease, and smooth.  The final position will be a 
        // weighted average of these three positions, depending on the smoothness values of the
        // vertex and all incident edges.

        s2 = s3 = 0.0;
        for (i = 0; i < vertex.length; i++)
          {
            if (!refineVert[i])
              {
                // This vertex is already converged, so just copy it over.
                
                newvert[i] = newmesh.new Vertex(vertex[i]);
                for (j = 0; j < paramType.length; j++)
                  if (paramType[j] == PER_VERTEX)
                    newParamValue[j][0][i] = oldParamValue[j][0][i];
                continue;
              }

            // First determine the weights.
        
            e = vertex[i].getEdges();
            if (edge[e[0]].f2 == -1) // On the mesh boundary, so use crease rule
              {
                cornerWeight = 1.0 - vertex[i].smoothness;
                creaseWeight = 1.0 - cornerWeight;
                smoothWeight = 0.0;
              }
            else
              {
                s1 = s2 = s3 = vertex[i].smoothness;
                for (j = 0; j < e.length; j++)
                  {
                    if (edge[e[j]].smoothness < s1)
                      {
                        s3 = s2;
                        s2 = s1;
                        s1 = edge[e[j]].smoothness;
                      }
                    else if (edge[e[j]].smoothness < s2)
                      {
                        s3 = s2;
                        s2 = edge[e[j]].smoothness;
                      }
                    else if (edge[e[j]].smoothness < s3)
                      s3 = edge[e[j]].smoothness;
                  }
                cornerWeight = 1.0 - s3;
                creaseWeight = 1.0 - s2 - cornerWeight;
                smoothWeight = 1.0 - cornerWeight - creaseWeight;
              }

            // Now determine any of the three positions (corner, crease, and smooth) that are
            // necessary.  Also determine the "final" position (limit position using either 
            // the smooth or crease rule), which is used for judging convergence to the 
            // limit surface.

            temp.clear();
            for (j = 0; j < tempParam.length; j++)
              tempParam[j] = 0.0;
            if (e.length < LOOP_BETA.length)
              beta = LOOP_BETA[e.length];
            else
              {
                beta = 0.375+0.25*Math.cos(2.0*Math.PI/e.length);
                beta = (0.625-beta*beta)/e.length;
              }
            for (j = 0; j < e.length; j++)
              {
                tempEdge = edge[e[j]];
                if (tempEdge.v1 == i)
                {
                  setBlend(temp, temp, vertex[tempEdge.v2], 1.0, 1.0);
                  setBlendParams(tempParam, tempParam, tempEdge.v2, 1.0, 1.0, oldParamValue, paramType);
                }
                else
                {
                  setBlend(temp, temp, vertex[tempEdge.v1], 1.0, 1.0);
                  setBlendParams(tempParam, tempParam, tempEdge.v1, 1.0, 1.0, oldParamValue, paramType);
                }
              }
            if (smoothWeight > 0.0)
              {
                // Determine the smooth position.
                
                setBlend(smoothPos, vertex[i], temp, beta*(1.0/beta-e.length), beta);
                setBlendParams(smoothParam, tempParam, i, beta*(1.0/beta-e.length), beta, oldParamValue, paramType);
              }
            if (edge[e[0]].f2 == -1)
              {
                // This is a boundary edge, so use the crease rule.
                
                tempEdge = edge[e[0]];
                if (tempEdge.v1 == i)
                {
                  setBlend(creasePos, vertex[i], vertex[tempEdge.v2], 0.75, 0.125);
                  setBlendParams(creaseParam, i, tempEdge.v2, 0.75, 0.125, oldParamValue, paramType);
                }
                else
                {
                  setBlend(creasePos, vertex[i], vertex[tempEdge.v1], 0.75, 0.125);
                  setBlendParams(creaseParam, i, tempEdge.v1, 0.75, 0.125, oldParamValue, paramType);
                }
                tempEdge = edge[e[e.length-1]];
                if (tempEdge.v1 == i)
                {
                  setBlend(creasePos, creasePos, vertex[tempEdge.v2], 1.0, 0.125);
                  setBlendParams(creaseParam, creaseParam, tempEdge.v2, 1.0, 0.125, oldParamValue, paramType);
                }
                else
                {
                  setBlend(creasePos, creasePos, vertex[tempEdge.v1], 1.0, 0.125);
                  setBlendParams(creaseParam, creaseParam, tempEdge.v1, 1.0, 0.125, oldParamValue, paramType);
                }
              }
            else if (creaseWeight > 0.0)
              {
                // Determine the crease position.
                
                creasePos.copy(vertex[i]);
                creasePos.scale(0.75);
                for (j = 0; j < paramType.length; j++)
                  if (paramType[j] == PER_VERTEX)
                    creaseParam[j] = 0.75*oldParamValue[j][0][i];
                for (j = 0; j < e.length; j++)
                  {
                    tempEdge = edge[e[j]];
                    if (tempEdge.smoothness < s3)
                      {
                        if (tempEdge.v1 == i)
                        {
                          setBlend(creasePos, creasePos, vertex[tempEdge.v2], 1.0, 0.125);
                          setBlendParams(creaseParam, creaseParam, tempEdge.v2, 1.0, 0.125, oldParamValue, paramType);
                        }
                        else
                        {
                          setBlend(creasePos, creasePos, vertex[tempEdge.v1], 1.0, 0.125);
                          setBlendParams(creaseParam, creaseParam, tempEdge.v1, 1.0, 0.125, oldParamValue, paramType);
                        }
                      }
                  }
              }
            if (smoothWeight+cornerWeight > 0.0)
              {
                // Calculate final position with smooth rule.

                beta = 1.0/(.375/beta + e.length);
                double w1 = (smoothWeight+cornerWeight)*beta;
                double w2 = 1.0/beta-e.length;
                finalPos.set(vertex[i].r);
                finalPos.scale(w2);
                finalPos.add(temp.r);
                finalPos.scale(w1);
                for (j = 0; j < paramType.length; j++)
                  if (paramType[j] == PER_VERTEX)
                    finalParam[j] = w1*(tempParam[j]+w2*oldParamValue[j][0][i]);
              }
            else
            {
              finalPos.set(0.0, 0.0, 0.0);
              for (j = 0; j < paramType.length; j++)
                finalParam[j] = 0.0;
            }
            if (creaseWeight > 0.0)
              {
                // Calculate final position with crease rule.
                
                double w1 = creaseWeight/3.0;
                tempVec.set(creasePos.r);
                tempVec.scale(4.0);
                tempVec.subtract(vertex[i].r);
                tempVec.scale(w1);
                finalPos.add(tempVec);
                for (j = 0; j < paramType.length; j++)
                  if (paramType[j] == PER_VERTEX)
                    finalParam[j] += w1*(4.0*creaseParam[j]-oldParamValue[j][0][i]);
              }

            // Construct the new vertex.

            newvert[i] = newmesh.blend(vertex[i], creasePos, smoothPos, cornerWeight, creaseWeight, smoothWeight);
            newvert[i].smoothness = Math.min(2.0f*vertex[i].smoothness, 1.0f);
            newvert[i].ikJoint = vertex[i].ikJoint;
            newvert[i].ikWeight = vertex[i].ikWeight;
            for (j = 0; j < paramType.length; j++)
              if (paramType[j] == PER_VERTEX)
                newParamValue[j][0][i] = finalParam[j];
            finalPos.subtract(newvert[i].r);
            error = finalPos.length2();
            if (error > tol2)
              {
                notconverged[i] = true;
                done = false;
              }
          }

        // Step 2: Determine the positions for the new vertices (one for each edge that gets
        // split).  Depending on the smoothness value of the edge, this position can be determined
        // by the smooth rule, the crease rule, or a weighted average of the two.

        j = vertex.length;
        for (i = 0; i < edge.length; i++)
          {
            if (!refineEdge[i])
              continue;
            tempEdge = edge[i];
            setBlend(creasePos, vertex[tempEdge.v1], vertex[tempEdge.v2], 1.0, 1.0);
            for (int pm = 0; pm < paramType.length; pm++)
              if (paramType[pm] == PER_VERTEX)
                creaseParam[pm] = oldParamValue[pm][0][tempEdge.v1]+oldParamValue[pm][0][tempEdge.v2];
            if (tempEdge.f2 == -1 || tempEdge.smoothness == 0.0f)
              {
                // Use the crease rule.

                newvert[j] = newmesh.new Vertex(creasePos);
                newvert[j].scale(0.5);
                for (int pm = 0; pm < paramType.length; pm++)
                  if (paramType[pm] == PER_VERTEX)
                    newParamValue[pm][0][j] = 0.5*creaseParam[pm];
              }
            else
              {
                // Use the smooth rule, or a blend of the two.

                tempFace = face[tempEdge.f1];
                if (tempFace.e1 == i)
                {
                  smoothPos.copy(vertex[tempFace.v3]);
                  recordParamValues(smoothParam, tempFace.v3, oldParamValue, paramType);
                }
                else if (tempFace.e2 == i)
                {
                  smoothPos.copy(vertex[tempFace.v1]);
                  recordParamValues(smoothParam, tempFace.v1, oldParamValue, paramType);
                }
                else
                {
                  smoothPos.copy(vertex[tempFace.v2]);
                  recordParamValues(smoothParam, tempFace.v2, oldParamValue, paramType);
                }
                tempFace = face[tempEdge.f2];
                if (tempFace.e1 == i)
                {
                  setBlend(smoothPos, smoothPos, vertex[tempFace.v3], 1.0, 1.0);
                  setBlendParams(smoothParam, smoothParam, tempFace.v3, 1.0, 1.0, oldParamValue, paramType);
                }
                else if (tempFace.e2 == i)
                {
                  setBlend(smoothPos, smoothPos, vertex[tempFace.v1], 1.0, 1.0);
                  setBlendParams(smoothParam, smoothParam, tempFace.v1, 1.0, 1.0, oldParamValue, paramType);
                }
                else
                {
                  setBlend(smoothPos, smoothPos, vertex[tempFace.v2], 1.0, 1.0);
                  setBlendParams(smoothParam, smoothParam, tempFace.v2, 1.0, 1.0, oldParamValue, paramType);
                }
                s1 = 1.0-tempEdge.smoothness;
                creaseWeight = 0.125*s1+0.375;
                smoothWeight = 0.125*(1.0-s1);
                    newvert[j] = newmesh.blend(creasePos, smoothPos, creaseWeight, smoothWeight);
                for (int pm = 0; pm < paramType.length; pm++)
                  if (paramType[pm] == PER_VERTEX)
                    newParamValue[pm][0][j] = creaseWeight*creaseParam[pm] + smoothWeight*smoothParam[pm];
              }
            newvert[j].smoothness = 1.0f;
            blendIKParams(newvert[j], vertex[tempEdge.v1], vertex[tempEdge.v2]);
            j++;
          }

        // Step 3: Subdivide the mesh.

        doSubdivide(newmesh, vertex, edge, face, refineEdge, newvert, newedge, newface, oldParamValue, newParamValue, paramType);

        // Update data structures for the next iteration.
        
        if (!done)
          {
            refineVert = new boolean [newvert.length];
            refineEdge = new boolean [newedge.length];
            for (i = 0; i < newedge.length; i++)
              if (newedge[i].v1 < notconverged.length && notconverged[newedge[i].v1])
                refineVert[newedge[i].v1] = refineVert[newedge[i].v2] = true;
            for (i = 0; i < newedge.length; i++)
              if (refineVert[newedge[i].v1] || refineVert[newedge[i].v2])
                if (newvert[newedge[i].v1].r.distance2(newvert[newedge[i].v2].r) > tol2)
                  refineEdge[i] = true;
          }
        newmesh.vertex = vertex = newvert;
        newmesh.edge = edge = newedge;
        newmesh.face = face = newface;
        oldParamValue = newParamValue;
        newmesh.copyTextureAndMaterial(mesh);
        for (i = 0; i < newedge.length; i++)
          newedge[i].smoothness = Math.min(2.0f*newedge[i].smoothness, 1.0f);
        for (i = 0; i < paramType.length; i++)
        {
          if (paramType[i] == PER_VERTEX)
            newmesh.paramValue[i] = new VertexParameterValue(newParamValue[i][0]);
          else if (paramType[i] == PER_FACE)
            newmesh.paramValue[i] = new FaceParameterValue(newParamValue[i][0]);
          else if (paramType[i] == PER_FACE_VERTEX)
          {
            double val[][] = new double [newParamValue[i][0].length][];
            for (int index = 0; index < val.length; index++)
              val[index] = new double [] {newParamValue[i][0][index], newParamValue[i][1][index], newParamValue[i][2][index]};
            newmesh.paramValue[i] = new FaceVertexParameterValue(val);
          }
        }
      } while (!done && ++iterations < MAX_SUBDIVISIONS);
    
    // Return the new mesh.
    
    newmesh.closed = mesh.closed;
    newmesh.smoothingMethod = mesh.smoothingMethod;
    newmesh.skeleton = mesh.skeleton.duplicate();
    return newmesh;
  }

  /** This method subdivides the mesh using interpolating (modified Butterfly) subdivision, and returns 
      a new TriangleMesh which approximates the limit surface to within the specified tolerance. 
      The subdivision coefficients are taken from Zorin et al. "Interpolating Subdivision for 
      Meshes with Arbitrary Topology." 1996.  The algorithm for creating semi-smooth points
      and creases is original.
      
      @param mesh         the mesh to subdivide
      @param refineEdge   a flag for each edge, specifying which ones should be split.  If this is null,
                          every edge will be split.
      @param tol          the error tolerance.  Edges will be repeatedly subdivided until every point on the
                          subdivided mesh is within this distance of the limit surface.  If this is equal to
                          Double.MAX_VALUE, each edge will be subdivided exactly once.
      @return the subdivided mesh
  */
     
  public static TriangleMesh subdivideButterfly(TriangleMesh mesh, boolean refineEdge[], double tol)
  {
    Vertex vertex[] = mesh.vertex, newvert[];
    Edge edge[] = mesh.edge, newedge[], tempEdge;
    Face face[] = mesh.face, newface[], tempFace;
    TriangleMesh newmesh = new TriangleMesh();
    int i, j, k, n, e[], vertEdge[][];
    int numVert, numEdge, numFace;
    int v1, v2, v3, e2, e3;
    Vertex creasePos = newmesh.new Vertex(new Vec3()), smoothPos = newmesh.new Vertex(new Vec3()), cornerPos = newmesh.new Vertex(new Vec3()), temp = newmesh.new Vertex(new Vec3());
    Vec3 axis = new Vec3(), tempVec = new Vec3();
    double cornerWeight, creaseWeight, smoothWeight, s1, s2[], s3[], edgeSmoothness[], vertSmoothness[], coeff[];
    boolean refineVert[], notconverged[], regular[], done;
    double tol2 = tol*tol*9.0, error;

    if (refineEdge == null)
      {
        refineEdge = new boolean [edge.length];
        for (i = 0; i < refineEdge.length; i++)
          refineEdge[i] = true;
      }
    
    // Record parameter values.
    
    int paramType[] = new int [mesh.paramValue.length];
    double oldParamValue[][][] = new double [mesh.paramValue.length][][];
    for (i = 0; i < mesh.paramValue.length; i++)
    {
      if (mesh.paramValue[i] instanceof ConstantParameterValue)
        paramType[i] = PER_OBJECT;
      else if (mesh.paramValue[i] instanceof VertexParameterValue)
      {
        oldParamValue[i] = new double [][] {((VertexParameterValue) mesh.paramValue[i]).getValue()};
        paramType[i] = PER_VERTEX;
      }
      else if (mesh.paramValue[i] instanceof FaceParameterValue)
      {
        oldParamValue[i] = new double [][] {((FaceParameterValue) mesh.paramValue[i]).getValue()};
        paramType[i] = PER_FACE;
      }
      else if (mesh.paramValue[i] instanceof FaceVertexParameterValue)
      {
        FaceVertexParameterValue fvpv = (FaceVertexParameterValue) mesh.paramValue[i];
        oldParamValue[i] = new double [3][fvpv.getFaceCount()];
        for (int index = 0; index < fvpv.getFaceCount(); index++)
        {
          oldParamValue[i][0][index] = fvpv.getValue(index, 0);
          oldParamValue[i][1][index] = fvpv.getValue(index, 1);
          oldParamValue[i][2][index] = fvpv.getValue(index, 2);
        }
        paramType[i] = PER_FACE_VERTEX;
      }
    }
    double creaseParam[] = new double [paramType.length];
    double smoothParam[] = new double [paramType.length];
    double cornerParam[] = new double [paramType.length];
    double tempParam[] = new double [paramType.length];

    // Determine which vertices need to be refined.
    
    refineVert = new boolean [vertex.length];
    for (i = 0; i < refineEdge.length; i++)
      if (refineEdge[i])
            refineVert[edge[i].v1] = refineVert[edge[i].v2] = true;

    // Repeatedly subdivide until all portions of the mesh have converged.
    
    int iterations = 0;
    do
      {
        done = true;
        notconverged = new boolean [edge.length];
        
        // Determine how many vertices, faces, and edges will be in the new mesh, and
        // create arrays.
        
        numVert = vertex.length;
        numEdge = edge.length;
        numFace = face.length;
        for (i = 0; i < edge.length; i++)
          if (refineEdge[i])
            {
              numVert++;
              numEdge += 2;
              numFace++;
              if (edge[i].f2 != -1)
                {
                  numEdge++;
                  numFace++;
                }
            }
        newvert = new Vertex [numVert];
        newedge = new Edge [numEdge];
        newface = new Face [numFace];
        double newParamValue[][][] = new double [paramType.length][][];
        for (i = 0; i < paramType.length; i++)
        {
          if (paramType[i] == PER_VERTEX)
            newParamValue[i] = new double [1][numVert];
          else if (paramType[i] == PER_FACE)
            newParamValue[i] = new double [1][numFace];
          else if (paramType[i] == PER_FACE_VERTEX)
            newParamValue[i] = new double [3][numFace];
        }

        // Record the list of edges intersecting each vertex.
        
        vertEdge = new int [vertex.length][];
        for (i = 0; i < vertex.length; i++)
          vertEdge[i] = vertex[i].getEdges();
        
        // Determine the three sharpest edges intersecting each vertex.

        s2 = new double [vertex.length];
        s3 = new double [vertex.length];
        for (i = 0; i < vertex.length; i++)
          {
            s1 = s2[i] = s3[i] = 1.0;
            e = vertEdge[i];
            for (j = 0; j < e.length; j++)
              {
                if (edge[e[j]].f2 == -1)
                  {
                    s3[i] = s2[i];
                    s2[i] = s1;
                    s1 = 0.0;
                  }
                else if (edge[e[j]].smoothness < s1)
                  {
                    s3[i] = s2[i];
                    s2[i] = s1;
                    s1 = edge[e[j]].smoothness;
                  }
                else if (edge[e[j]].smoothness < s2[i])
                  {
                    s3[i] = s2[i];
                    s2[i] = edge[e[j]].smoothness;
                  }
                else if (edge[e[j]].smoothness < s3[i])
                  s3[i] = edge[e[j]].smoothness;
              }
          }

        // Determine the smoothness value for each edge and vertex, and mark which edges
        // are regular.
        
        edgeSmoothness = new double [edge.length];
        for (i = 0; i < edge.length; i++)
          {
            if (edge[i].f2 == -1)
              edgeSmoothness[i] = 0.0;
            else
              edgeSmoothness[i] = edge[i].smoothness;
          }
        vertSmoothness = new double [vertex.length];
        regular = new boolean [vertex.length];
        for (i = 0; i < vertex.length; i++)
          {
            vertSmoothness[i] = Math.min(vertex[i].smoothness, s3[i]);
            regular[i] = (vertEdge[i].length == 6) || (s2[i] < s3[i]);
            newvert[i] = newmesh.new Vertex(vertex[i]);
            newvert[i].smoothness = Math.min(2.0f*vertex[i].smoothness, 1.0f);
            for (j = 0; j < paramType.length; j++)
              if (paramType[j] == PER_VERTEX)
                newParamValue[j][0][i] = oldParamValue[j][0][i];
          }

        // First, determine the positions for the new vertices (one for each edge that gets
        // split).  Depending on the smoothness values of the edge and the vertices it connects,
        // this position can be determined by the smooth rule, the crease rule, the corner
        // rule, or a weighted average of the three.

        j = vertex.length;
        for (i = 0; i < edge.length; i++)
          {
            if (!refineEdge[i])
              continue;
            tempEdge = edge[i];
            v1 = tempEdge.v1;
            v2 = tempEdge.v2;
            cornerWeight = 1.0-Math.min(vertSmoothness[v1], vertSmoothness[v2]);
            if (tempEdge.f2 == -1)
              creaseWeight = 1.0-cornerWeight;
            else
              creaseWeight = Math.max(1.0-edgeSmoothness[i]-cornerWeight, 0.0);
            smoothWeight = 1.0-cornerWeight-creaseWeight;
            
            // The corner rule simply places the new point midway between the endpoint.
            
            setBlend(cornerPos, vertex[v1], vertex[v2], 0.5, 0.5);
            setBlendParams(cornerParam, v1, v2, 0.5, 0.5, oldParamValue, paramType);            

            // The crease rule uses the four-point rule (-1, 9, 9, -1).  Depending on the
            // smoothness values for the second and third points, these weights may be modified.
            
            if (creaseWeight > 0.0)
              {
                creasePos.copy(vertex[v1]);
                recordParamValues(creaseParam, v1, oldParamValue, paramType);
                if (s2[v1] < 1.0)
                  {
                    e = vertEdge[v1];
                    if (tempEdge.f2 == -1)
                      {
                        if (e[0] == i)
                          k = e.length-1;
                        else
                          k = 0;
                      }
                    else
                      for (k = 0; e[k] == i || edgeSmoothness[e[k]] > s2[v1]; k++);
                    int whichVert = (edge[e[k]].v1 == v1 ? edge[e[k]].v2 : edge[e[k]].v1);
                    double w2 = -0.125*vertex[v1].smoothness;
                    double w1 = 1.0-w2;
                    setBlend(creasePos, creasePos, vertex[whichVert], w1, w2);
                    setBlendParams(creaseParam, creaseParam, whichVert, w1, w2, oldParamValue, paramType);            
                  }
                temp.copy(vertex[v2]);
                recordParamValues(tempParam, v2, oldParamValue, paramType);
                if (s2[v2] < 1.0)
                  {
                    e = vertEdge[v2];
                    if (tempEdge.f2 == -1)
                      {
                        if (e[0] == i)
                          k = e.length-1;
                        else
                          k = 0;
                      }
                    else
                      for (k = 0; e[k] == i || edgeSmoothness[e[k]] > s2[v2]; k++);
                    int whichVert = (edge[e[k]].v1 == v2 ? edge[e[k]].v2 : edge[e[k]].v1);
                    double w2 = -0.125*vertex[v2].smoothness;
                    double w1 = 1.0-w2;
                    setBlend(temp, temp, vertex[whichVert], w1, w2);
                    setBlendParams(tempParam, tempParam, whichVert, w1, w2, oldParamValue, paramType);            
                  }
                setBlend(creasePos, creasePos, temp, 0.5, 0.5);
                for (k = 0; k < paramType.length; k++)
                  if (paramType[k] == PER_VERTEX)
                    creaseParam[k] = 0.5*(creaseParam[k]+tempParam[k]);
              }

            // The smooth rule uses the modified Butterfly coefficients.
            
            if (smoothWeight > 0.0)
              {
                if (regular[v1] && regular[v2])
                  {
                    // Both vertices are regular, so use the standard Butterfly coefficients.
                    
                    smoothPos.copy(cornerPos);
                    for (k = 0; k < smoothParam.length; k++)
                      smoothParam[k] = cornerParam[k];
                    tempFace = face[tempEdge.f1];
                    if (tempFace.e1 == i)
                      {
                        v3 = tempFace.v3;
                        e2 = tempFace.e2;
                        e3 = tempFace.e3;
                      }
                    else if (tempFace.e2 == i)
                      {
                        v3 = tempFace.v1;
                        e2 = tempFace.e3;
                        e3 = tempFace.e1;
                      }
                    else
                      {
                        v3 = tempFace.v2;
                        e2 = tempFace.e1;
                        e3 = tempFace.e2;
                      }
                    setBlend(smoothPos, smoothPos, vertex[v3], 1.0, 0.125);
                    setBlendParams(smoothParam, smoothParam, v3, 1.0, 0.125, oldParamValue, paramType);
                    findOppositeVertex(temp, tempEdge.f1, e2, edgeSmoothness[e2], vertex, edge, face, tempParam, oldParamValue, paramType);
                    setBlend(smoothPos, smoothPos, temp, 1.0, -0.0625);
                    for (k = 0; k < paramType.length; k++)
                      if (paramType[k] == PER_VERTEX)
                        smoothParam[k] -= 0.0625*tempParam[k];
                    findOppositeVertex(temp, tempEdge.f1, e3, edgeSmoothness[e3], vertex, edge, face, tempParam, oldParamValue, paramType);
                    setBlend(smoothPos, smoothPos, temp, 1.0, -0.0625);
                    for (k = 0; k < paramType.length; k++)
                      if (paramType[k] == PER_VERTEX)
                        smoothParam[k] -= 0.0625*tempParam[k];
                    tempFace = face[tempEdge.f2];
                    if (tempFace.e1 == i)
                      {
                        v3 = tempFace.v3;
                        e2 = tempFace.e2;
                        e3 = tempFace.e3;
                      }
                    else if (tempFace.e2 == i)
                      {
                        v3 = tempFace.v1;
                        e2 = tempFace.e3;
                        e3 = tempFace.e1;
                      }
                    else
                      {
                        v3 = tempFace.v2;
                        e2 = tempFace.e1;
                        e3 = tempFace.e2;
                      }
                    setBlend(smoothPos, smoothPos, vertex[v3], 1.0, 0.125);
                    setBlendParams(smoothParam, smoothParam, v3, 1.0, 0.125, oldParamValue, paramType);
                    findOppositeVertex(temp, tempEdge.f2, e2, edgeSmoothness[e2], vertex, edge, face, tempParam, oldParamValue, paramType);
                    setBlend(smoothPos, smoothPos, temp, 1.0, -0.0625);
                    for (k = 0; k < paramType.length; k++)
                      if (paramType[k] == PER_VERTEX)
                        smoothParam[k] -= 0.0625*tempParam[k];
                    findOppositeVertex(temp, tempEdge.f2, e3, edgeSmoothness[e3], vertex, edge, face, tempParam, oldParamValue, paramType);
                    setBlend(smoothPos, smoothPos, temp, 1.0, -0.0625);
                    for (k = 0; k < paramType.length; k++)
                      if (paramType[k] == PER_VERTEX)
                        smoothParam[k] -= 0.0625*tempParam[k];
                  }
                else
                  {
                    // At least one of the vertices is extraordinary.  We calculate the smooth
                    // position based on the extraordinary vertex, or if both vertices are
                    // extraordinary, an average of the two.
                    
                    smoothPos.clear();
                    for (k = 0; k < smoothParam.length; k++)
                      smoothParam[k] = 0.0;
                    if (!regular[v1])
                      {
                        e = vertEdge[v1];
                        coeff = getButterflyCoeff(e.length);
                        for (n = 0; e[n] != i; n++);
                        for (k = 0; k < e.length; k++)
                          {
                            tempEdge = edge[e[(n+k)%e.length]];
                            int whichVert = (tempEdge.v1 == v1 ? tempEdge.v2 : tempEdge.v1);
                            setBlend(smoothPos, smoothPos, vertex[whichVert], 1.0, coeff[k]);
                            setBlendParams(smoothParam, smoothParam, whichVert, 1.0, coeff[k], oldParamValue, paramType);
                          }
                        setBlend(smoothPos, smoothPos, vertex[v1], 1.0, coeff[k]);
                        setBlendParams(smoothParam, smoothParam, v1, 1.0, coeff[k], oldParamValue, paramType);
                      }
                    if (!regular[v2])
                      {
                        e = vertEdge[v2];
                        coeff = getButterflyCoeff(e.length);
                        for (n = 0; e[n] != i; n++);
                        for (k = 0; k < e.length; k++)
                          {
                            tempEdge = edge[e[(n+k)%e.length]];
                            int whichVert = (tempEdge.v1 == v2 ? tempEdge.v2 : tempEdge.v1);
                            setBlend(smoothPos, smoothPos, vertex[whichVert], 1.0, coeff[k]);
                            setBlendParams(smoothParam, smoothParam, whichVert, 1.0, coeff[k], oldParamValue, paramType);
                          }
                        setBlend(smoothPos, smoothPos, vertex[v2], 1.0, coeff[k]);
                        setBlendParams(smoothParam, smoothParam, v2, 1.0, coeff[k], oldParamValue, paramType);
                      }
                    if (!regular[v1] && !regular[v2])
                    {
                      smoothPos.scale(0.5);
                      for (k = 0; k < smoothParam.length; k++)
                        smoothParam[k] *= 0.5;
                    }
                  }
              }
            newvert[j] = newmesh.blend(cornerPos, creasePos, smoothPos, cornerWeight, creaseWeight, smoothWeight);
            for (k = 0; k < paramType.length; k++)
              if (paramType[k] == PER_VERTEX)
                newParamValue[k][0][j] = cornerWeight*cornerParam[k] + creaseWeight*creaseParam[k] + smoothWeight*smoothParam[k];
            blendIKParams(newvert[j], vertex[tempEdge.v1], vertex[tempEdge.v2]);
            j++;
            
            // Determine how far the newly created point is from the edge, and use this to 
            // estimate convergence.
            
            axis.set(vertex[v2].r);
            axis.subtract(vertex[v1].r);
            axis.normalize();
            tempVec.set(newvert[j-1].r);
            tempVec.subtract(vertex[v1].r);
            s1 = tempVec.dot(axis);
            axis.scale(s1);
            tempVec.subtract(axis);
            error = tempVec.length2();
            if (error > tol2)
              {
                notconverged[i] = true;
                done = false;
              }
          }

        // Subdivide the mesh.

        doSubdivide(newmesh, vertex, edge, face, refineEdge, newvert, newedge, newface, oldParamValue, newParamValue, paramType);
        
        // Update data structures for the next iteration.
        
        if (!done)
          {
            refineVert = new boolean [newvert.length];
            refineEdge = new boolean [newedge.length];
            for (i = 0; i < edge.length; i++)
              if (notconverged[i])
                refineVert[edge[i].v1] = refineVert[edge[i].v2] = refineVert[newedge[i].v2] = true;
            for (i = 0; i < newedge.length; i++)
              if (refineVert[newedge[i].v1] || refineVert[newedge[i].v2])
                if (newvert[newedge[i].v1].r.distance2(newvert[newedge[i].v2].r) > tol2)
                  refineEdge[i] = true;
          }
        newmesh.vertex = vertex = newvert;
        newmesh.edge = edge = newedge;
        newmesh.face = face = newface;
        oldParamValue = newParamValue;
        newmesh.copyTextureAndMaterial(mesh);
        for (i = 0; i < newedge.length; i++)
          newedge[i].smoothness = Math.min(2.0f*newedge[i].smoothness, 1.0f);
        for (i = 0; i < paramType.length; i++)
        {
          if (paramType[i] == PER_VERTEX)
            newmesh.paramValue[i] = new VertexParameterValue(newParamValue[i][0]);
          else if (paramType[i] == PER_FACE)
            newmesh.paramValue[i] = new FaceParameterValue(newParamValue[i][0]);
          else if (paramType[i] == PER_FACE_VERTEX)
          {
            double val[][] = new double [newParamValue[i][0].length][];
            for (int index = 0; index < val.length; index++)
              val[index] = new double [] {newParamValue[i][0][index], newParamValue[i][1][index], newParamValue[i][2][index]};
            newmesh.paramValue[i] = new FaceVertexParameterValue(val);
          }
        }
      } while (!done && ++iterations < MAX_SUBDIVISIONS);
    
    // Return the new mesh.
    
    newmesh.closed = mesh.closed;
    newmesh.smoothingMethod = mesh.smoothingMethod;
    newmesh.skeleton = mesh.skeleton.duplicate();
    return newmesh;
  }
  
  /** This method is used for Butterfly subdivision.  Given a face and an edge, it finds the
      other face which is across the edge from the specified one, finds the vertex of that face
      which is opposite the specified edge, and returns its position in pos.  The position of 
      this "opposite vertex" can be calculated two different ways.  For smooth edges, it is 
      the actual position of the vertex.  For boundary or crease edges, it is a "virtual 
      vertex" created by mirroring the specified face across the specified edge.  The 
      relative weights of these two are determined by smoothWeight. */

  private static void findOppositeVertex(Vertex pos, int whichFace, int whichEdge, double smoothWeight, Vertex v[], Edge e[], Face f[], double paramVal[], double oldParamValue[][][], int paramType[])
  {
    Face fc;
    Vec3 axis, delta, r;
    double dot;
    
    // First find the position of the actual vertex.

    if (smoothWeight > 0.0)
      {
        if (e[whichEdge].f1 == whichFace)
          fc = f[e[whichEdge].f2];
        else
          fc = f[e[whichEdge].f1];
        if (fc.e1 == whichEdge)
        {
          pos.copy(v[fc.v3]);
          recordParamValues(paramVal, fc.v3, oldParamValue, paramType);
        }
        else if (fc.e2 == whichEdge)
        {
          pos.copy(v[fc.v1]);
          recordParamValues(paramVal, fc.v1, oldParamValue, paramType);
        }
        else
        {
          pos.copy(v[fc.v2]);
          recordParamValues(paramVal, fc.v2, oldParamValue, paramType);
        }
        pos.scale(smoothWeight);
        for (int i = 0; i < paramVal.length; i++)
          paramVal[i] *= smoothWeight;
      }
    else
    {
      pos.clear();
      for (int i = 0; i < paramVal.length; i++)
        paramVal[i] = 0.0;
    }
    
    // Next find the position of the virtual vertex.
    
    if (smoothWeight < 1.0)
    {
      axis = v[e[whichEdge].v1].r.minus(v[e[whichEdge].v2].r);
      axis.normalize();
      fc = f[whichFace];
      if (fc.e1 == whichEdge)
        r = v[fc.v3].r;
      else if (fc.e2 == whichEdge)
        r = v[fc.v1].r;
      else
        r = v[fc.v2].r;
      delta = r.minus(v[e[whichEdge].v2].r);
      dot = delta.dot(axis);
      axis.scale(dot);
      delta.subtract(axis);
      pos.r.x += (1.0-smoothWeight)*(r.x+axis.x-2.0*delta.x);
      pos.r.y += (1.0-smoothWeight)*(r.y+axis.y-2.0*delta.y);
      pos.r.z += (1.0-smoothWeight)*(r.z+axis.z-2.0*delta.z);
    }
  }
  
  /** This method is used for Butterfly subdivision.  Given the number of edges intersecting
      an extraordinary vertex, it returns an array containing the subdivision coefficients
      to use for that vertex. */
  
  private static double [] getButterflyCoeff(int numEdges)
  {
    if (numEdges < BUTTERFLY_COEFF.length)
      return BUTTERFLY_COEFF[numEdges];
    double coeff[] = new double [numEdges+1], beta = 2.0*Math.PI/numEdges;
    coeff[numEdges] = 1.0;
    for (int i = 0; i < numEdges; i++)
      {
        coeff[i] = (0.25+Math.cos(beta*i)+0.5*Math.cos(2.0*beta*i))/numEdges;
        coeff[numEdges] -= coeff[i];
      }
    return coeff;
  }

  /** This method is called by the various subdivideXXX() methods to do the actual subdivision.
      The vertex, edge, and face arguments describe the mesh to be subdivided.  newvert 
      contains the vertices of the new mesh.  newedge and newface are empty arrays of the 
      correct length, into which the new faces and edges will be placed.  mesh is the 
      TriangleMesh which the new edges and faces should belong to.  split is an array 
      specifying which edges of the old mesh should be split. */

  private static void doSubdivide(TriangleMesh mesh, Vertex vertex[], Edge edge[], Face face[], boolean split[], Vertex newvert[], Edge newedge[], Face newface[], double oldParamValue[][][], double newParamValue[][][], int paramType[])
  {
    Edge tempEdge;
    Face tempFace;
    int i, j, k, n, v1, v2, v3, e1, e2, e3, newEdgeIndex[] = new int [edge.length];

    // First, subdivide edges.

    j = edge.length;
    k = vertex.length;
    for (i = 0; i < edge.length; i++)
      {
        tempEdge = edge[i];
        v1 = tempEdge.v1;
        v2 = tempEdge.v2;
        if (!split[i])
          {
            // This edge does not need to be split, so just copy it over.

            newedge[i] = mesh.new Edge(v1, v2, -1);
            newedge[i].smoothness = tempEdge.smoothness;
            if (vertex[v1].firstEdge == i)
              newvert[v1].firstEdge = i;
            if (vertex[v2].firstEdge == i)
              newvert[v2].firstEdge = i;
            newEdgeIndex[i] = i;
            continue;
          }
        newedge[i] = mesh.new Edge(v1, k, -1);
        newedge[j] = mesh.new Edge(v2, k, -1);
        newedge[i].smoothness = newedge[j].smoothness = tempEdge.smoothness;
        if (vertex[v1].firstEdge == i)
          newvert[v1].firstEdge = i;
        if (vertex[v2].firstEdge == i)
          newvert[v2].firstEdge = j;
        newvert[k].firstEdge = i;
        newEdgeIndex[i] = j++;
        k++;
      }

    // Next, subdivide faces.  For each face in the old mesh, the can be anywhere from
    // one to four faces in the new mesh, depending on how many of its edges were
    // subdivided.

    int addedFace[] = new int [4];
    k = face.length;
    for (i = 0; i < face.length; i++)
      {
        tempFace = face[i];

        // Figure out how to subdivide the face, based on which edges are subdivided.

        if (split[tempFace.e1])
          {
            if (split[tempFace.e2])
              {
                if (split[tempFace.e3])
                  {
                    n = 3;
                    v1 = tempFace.v1;  v2 = tempFace.v2;  v3 = tempFace.v3;
                    e1 = tempFace.e1;  e2 = tempFace.e2;  e3 = tempFace.e3;
                  }
                else
                  {
                    n = 2;
                    v1 = tempFace.v1;  v2 = tempFace.v2;  v3 = tempFace.v3;
                    e1 = tempFace.e1;  e2 = tempFace.e2;  e3 = tempFace.e3;
                  }
              }
            else
              {
                if (split[tempFace.e3])
                  {
                    n = 2;
                    v1 = tempFace.v3;  v2 = tempFace.v1;  v3 = tempFace.v2;
                    e1 = tempFace.e3;  e2 = tempFace.e1;  e3 = tempFace.e2;
                  }
                else
                  {
                    n = 1;
                    v1 = tempFace.v1;  v2 = tempFace.v2;  v3 = tempFace.v3;
                    e1 = tempFace.e1;  e2 = tempFace.e2;  e3 = tempFace.e3;
                  }
              }
          }
        else
          {
            if (split[tempFace.e2])
              {
                if (split[tempFace.e3])
                  {
                    n = 2;
                    v1 = tempFace.v2;  v2 = tempFace.v3;  v3 = tempFace.v1;
                    e1 = tempFace.e2;  e2 = tempFace.e3;  e3 = tempFace.e1;
                  }
                else
                  {
                    n = 1;
                    v1 = tempFace.v2;  v2 = tempFace.v3;  v3 = tempFace.v1;
                    e1 = tempFace.e2;  e2 = tempFace.e3;  e3 = tempFace.e1;
                  }
              }
            else
              {
                if (split[tempFace.e3])
                  {
                    n = 1;
                    v1 = tempFace.v3;  v2 = tempFace.v1;  v3 = tempFace.v2;
                    e1 = tempFace.e3;  e2 = tempFace.e1;  e3 = tempFace.e2;
                  }
                else
                  {
                    n = 0;
                    v1 = tempFace.v1;  v2 = tempFace.v2;  v3 = tempFace.v3;
                    e1 = tempFace.e1;  e2 = tempFace.e2;  e3 = tempFace.e3;
                  }
              }
          }
 
        // Now subdivide it, and create the new faces and edges.

        switch (n)
        {
          case 0:

            // No edges being split, so simply copy the face over.

            newface[i] = mesh.new Face(v1, v2, v3, e1, e2, e3);
            break;

          case 1:

            // e1 was split.

            newedge[j] = mesh.new Edge(v3, newedge[e1].v2, -1);
            if (edge[e1].v1 == v1)
              {
                newface[i] = mesh.new Face(v1, newedge[e1].v2, v3, e1, j, e3);
                newface[k] = mesh.new Face(v3, newedge[e1].v2, v2, j, newEdgeIndex[e1], e2);
              }
            else
              {
                newface[i] = mesh.new Face(v1, newedge[e1].v2, v3, newEdgeIndex[e1], j, e3);
                newface[k] = mesh.new Face(v3, newedge[e1].v2, v2, j, e1, e2);
              }
            break;

          case 2:

            // e1 and e2 were split.

            newedge[j] = mesh.new Edge(newedge[e1].v2, newedge[e2].v2, -1);
            newedge[j+1] = mesh.new Edge(v3, newedge[e1].v2, -1);
            if (edge[e1].v1 == v1)
              {
                if (edge[e2].v1 == v2)
                  {
                    newface[i] = mesh.new Face(v1, newedge[e1].v2, v3, e1, j+1, e3);
                    newface[k] = mesh.new Face(v3, newedge[e1].v2, newedge[e2].v2, j+1, j, newEdgeIndex[e2]);
                    newface[k+1] = mesh.new Face(newedge[e2].v2, newedge[e1].v2, v2, j, newEdgeIndex[e1], e2);
                  }
                else
                  {
                    newface[i] = mesh.new Face(v1, newedge[e1].v2, v3, e1, j+1, e3);
                    newface[k] = mesh.new Face(v3, newedge[e1].v2, newedge[e2].v2, j+1, j, e2);
                    newface[k+1] = mesh.new Face(newedge[e2].v2, newedge[e1].v2, v2, j, newEdgeIndex[e1], newEdgeIndex[e2]);
                  }
              }
            else
              {
                if (edge[e2].v1 == v2)
                  {
                    newface[i] = mesh.new Face(v1, newedge[e1].v2, v3, newEdgeIndex[e1], j+1, e3);
                    newface[k] = mesh.new Face(v3, newedge[e1].v2, newedge[e2].v2, j+1, j, newEdgeIndex[e2]);
                    newface[k+1] = mesh.new Face(newedge[e2].v2, newedge[e1].v2, v2, j, e1, e2);
                  }
                else
                  {
                    newface[i] = mesh.new Face(v1, newedge[e1].v2, v3, newEdgeIndex[e1], j+1, e3);
                    newface[k] = mesh.new Face(v3, newedge[e1].v2, newedge[e2].v2, j+1, j, e2);
                    newface[k+1] = mesh.new Face(newedge[e2].v2, newedge[e1].v2, v2, j, e1, newEdgeIndex[e2]);
                  }
              }
            break;

          case 3:

            // All edges being split.

            newedge[j] = mesh.new Edge(newedge[e1].v2, newedge[e2].v2, -1);
            newedge[j+1] = mesh.new Edge(newedge[e2].v2, newedge[e3].v2, -1);
            newedge[j+2] = mesh.new Edge(newedge[e3].v2, newedge[e1].v2, -1);
            if (edge[e1].v1 == v1)
              {
                if (edge[e2].v1 == v2)
                  {
                    if (edge[e3].v1 == v3)
                      {
                         newface[i] = mesh.new Face(v1, newedge[e1].v2, newedge[e3].v2, e1, j+2, newEdgeIndex[e3]);
                         newface[k] = mesh.new Face(v2, newedge[e2].v2, newedge[e1].v2, e2, j, newEdgeIndex[e1]);
                         newface[k+1] = mesh.new Face(v3, newedge[e3].v2, newedge[e2].v2, e3, j+1, newEdgeIndex[e2]);
                      }
                    else
                      {
                         newface[i] = mesh.new Face(v1, newedge[e1].v2, newedge[e3].v2, e1, j+2, e3);
                         newface[k] = mesh.new Face(v2, newedge[e2].v2, newedge[e1].v2, e2, j, newEdgeIndex[e1]);
                         newface[k+1] = mesh.new Face(v3, newedge[e3].v2, newedge[e2].v2, newEdgeIndex[e3], j+1, newEdgeIndex[e2]);
                      }
                  }
                else
                  {
                    if (edge[e3].v1 == v3)
                      {
                         newface[i] = mesh.new Face(v1, newedge[e1].v2, newedge[e3].v2, e1, j+2, newEdgeIndex[e3]);
                         newface[k] = mesh.new Face(v2, newedge[e2].v2, newedge[e1].v2, newEdgeIndex[e2], j, newEdgeIndex[e1]);
                         newface[k+1] = mesh.new Face(v3, newedge[e3].v2, newedge[e2].v2, e3, j+1, e2);
                      }
                    else
                      {
                         newface[i] = mesh.new Face(v1, newedge[e1].v2, newedge[e3].v2, e1, j+2, e3);
                         newface[k] = mesh.new Face(v2, newedge[e2].v2, newedge[e1].v2, newEdgeIndex[e2], j, newEdgeIndex[e1]);
                         newface[k+1] = mesh.new Face(v3, newedge[e3].v2, newedge[e2].v2, newEdgeIndex[e3], j+1, e2);
                      }
                  }
              }
            else
              {
                if (edge[e2].v1 == v2)
                  {
                    if (edge[e3].v1 == v3)
                      {
                         newface[i] = mesh.new Face(v1, newedge[e1].v2, newedge[e3].v2, newEdgeIndex[e1], j+2, newEdgeIndex[e3]);
                         newface[k] = mesh.new Face(v2, newedge[e2].v2, newedge[e1].v2, e2, j, e1);
                         newface[k+1] = mesh.new Face(v3, newedge[e3].v2, newedge[e2].v2, e3, j+1, newEdgeIndex[e2]);
                      }
                    else
                      {
                         newface[i] = mesh.new Face(v1, newedge[e1].v2, newedge[e3].v2, newEdgeIndex[e1], j+2, e3);
                         newface[k] = mesh.new Face(v2, newedge[e2].v2, newedge[e1].v2, e2, j, e1);
                         newface[k+1] = mesh.new Face(v3, newedge[e3].v2, newedge[e2].v2, newEdgeIndex[e3], j+1, newEdgeIndex[e2]);
                      }
                  }
                else
                  {
                    if (edge[e3].v1 == v3)
                      {
                         newface[i] = mesh.new Face(v1, newedge[e1].v2, newedge[e3].v2, newEdgeIndex[e1], j+2, newEdgeIndex[e3]);
                         newface[k] = mesh.new Face(v2, newedge[e2].v2, newedge[e1].v2, newEdgeIndex[e2], j, e1);
                         newface[k+1] = mesh.new Face(v3, newedge[e3].v2, newedge[e2].v2, e3, j+1, e2);
                      }
                    else
                      {
                         newface[i] = mesh.new Face(v1, newedge[e1].v2, newedge[e3].v2, newEdgeIndex[e1], j+2, e3);
                         newface[k] = mesh.new Face(v2, newedge[e2].v2, newedge[e1].v2, newEdgeIndex[e2], j, e1);
                         newface[k+1] = mesh.new Face(v3, newedge[e3].v2, newedge[e2].v2, newEdgeIndex[e3], j+1, e2);
                      }
                  }
              }
            newface[k+2] = mesh.new Face(newedge[e1].v2, newedge[e2].v2, newedge[e3].v2, j, j+1, j+2);
        }
        
        // Copy over per-face and per-face/per-vertex parameter values.
        
        int numAddedFaces = n+1;
        addedFace[0] = i;
        for (int m = 0; m < n; m++)
          addedFace[m+1] = k+m;
        for (int p = 0; p < paramType.length; p++)
        {
          if (paramType[p] == PER_FACE)
            for (int m = 0; m < numAddedFaces; m++)
              newParamValue[p][0][addedFace[m]] = oldParamValue[p][0][i];
          else if (paramType[p] == PER_FACE_VERTEX)
          {
            int vertInd[] = new int [] {v1, v2, v3, -1, -1, -1};
            if (n > 0)
              vertInd[3] = newedge[e1].v2;
            if (n > 1)
              vertInd[4] = newedge[e2].v2;
            if (n > 2)
              vertInd[5] = newedge[e3].v2;
            double vertVal[];
            if (v1 == tempFace.v1)
              vertVal = new double [] {oldParamValue[p][0][i], oldParamValue[p][1][i], oldParamValue[p][2][i],
                  0.5*(oldParamValue[p][0][i]+oldParamValue[p][1][i]),
                  0.5*(oldParamValue[p][1][i]+oldParamValue[p][2][i]),
                  0.5*(oldParamValue[p][2][i]+oldParamValue[p][0][i])};
            else if (v1 == tempFace.v2)
              vertVal = new double [] {oldParamValue[p][1][i], oldParamValue[p][2][i], oldParamValue[p][0][i],
                  0.5*(oldParamValue[p][1][i]+oldParamValue[p][2][i]),
                  0.5*(oldParamValue[p][2][i]+oldParamValue[p][0][i]),
                  0.5*(oldParamValue[p][0][i]+oldParamValue[p][1][i])};
            else
              vertVal = new double [] {oldParamValue[p][2][i], oldParamValue[p][0][i], oldParamValue[p][1][i],
                  0.5*(oldParamValue[p][2][i]+oldParamValue[p][0][i]),
                  0.5*(oldParamValue[p][0][i]+oldParamValue[p][1][i]),
                  0.5*(oldParamValue[p][1][i]+oldParamValue[p][2][i])};
            for (int m = 0; m < numAddedFaces; m++)
            {
              Face fc = newface[addedFace[m]];
              for (int q = 0; q < 6; q++)
              {
                if (fc.v1 == vertInd[q])
                  newParamValue[p][0][addedFace[m]] = vertVal[q];
                else if (fc.v2 == vertInd[q])
                  newParamValue[p][1][addedFace[m]] = vertVal[q];
                else if (fc.v3 == vertInd[q])
                  newParamValue[p][2][addedFace[m]] = vertVal[q];
              }
            }
          }
        }
        j += n;
        k += n;
      }

    // Record which faces are adjacent to each edge.

    for (i = 0; i < newface.length; i++)
      {
        tempFace = newface[i];
        if (newedge[tempFace.e1].f1 == -1)
          newedge[tempFace.e1].f1 = i;
        else
          newedge[tempFace.e1].f2 = i;
        if (newedge[tempFace.e2].f1 == -1)
          newedge[tempFace.e2].f1 = i;
        else
          newedge[tempFace.e2].f2 = i;
        if (newedge[tempFace.e3].f1 == -1)
          newedge[tempFace.e3].f1 = i;
        else
          newedge[tempFace.e3].f2 = i;
      }

    // Count the number of edges intersecting each vertex.

    for (i = 0; i < newvert.length; i++)
      newvert[i].edges = 0;
    for (i = 0; i < newedge.length; i++)
      {
        newvert[newedge[i].v1].edges++;
        newvert[newedge[i].v2].edges++;
      }
  }
  
  /** This method splits each selected face into three faces, and returns the subdivided mesh.
      split is a boolean array specifying which faces to split. */
  
  public static TriangleMesh subdivideFaces(TriangleMesh mesh, boolean split[])
  {
    Vertex vertex[] = mesh.vertex, newvert[];
    Edge edge[] = mesh.edge, newedge[];
    Face face[] = mesh.face, newface[], tempFace;
    TriangleMesh newmesh = new TriangleMesh();
    int i, j, numVert, numEdge, numFace;

    // Determine how many vertices, faces, and edges will be in the new mesh, and
    // create arrays.

    numVert = vertex.length;
    numEdge = edge.length;
    numFace = face.length;
    for (i = 0; i < split.length; i++)
      if (split[i])
        {
          numVert++;
          numEdge += 3;
          numFace += 2;
        }
    newvert = new Vertex [numVert];
    newedge = new Edge [numEdge];
    newface = new Face [numFace];
    
    // Create arrays for parameter values.
    
    int paramType[] = new int [mesh.paramValue.length];
    double newParamValue[][][] = new double [mesh.paramValue.length][][];
    double oldParamValue[][][] = new double [mesh.paramValue.length][][];
    for (i = 0; i < mesh.paramValue.length; i++)
    {
      if (mesh.paramValue[i] instanceof ConstantParameterValue)
        paramType[i] = PER_OBJECT;
      else if (mesh.paramValue[i] instanceof VertexParameterValue)
      {
        oldParamValue[i] = new double [][] {((VertexParameterValue) mesh.paramValue[i]).getValue()};
        newParamValue[i] = new double [1][numVert];
        paramType[i] = PER_VERTEX;
      }
      else if (mesh.paramValue[i] instanceof FaceParameterValue)
      {
        oldParamValue[i] = new double [][] {((FaceParameterValue) mesh.paramValue[i]).getValue()};
        newParamValue[i] = new double [1][numFace];
        paramType[i] = PER_FACE;
      }
      else if (mesh.paramValue[i] instanceof FaceVertexParameterValue)
      {
        FaceVertexParameterValue fvpv = (FaceVertexParameterValue) mesh.paramValue[i];
        oldParamValue[i] = new double [3][fvpv.getFaceCount()];
        for (int index = 0; index < fvpv.getFaceCount(); index++)
        {
          oldParamValue[i][0][index] = fvpv.getValue(index, 0);
          oldParamValue[i][1][index] = fvpv.getValue(index, 1);
          oldParamValue[i][2][index] = fvpv.getValue(index, 2);
        }
        newParamValue[i] = new double [3][numFace];
        paramType[i] = PER_FACE_VERTEX;
      }
    }
    
    // Copy over the vertices, edges, and faces which will not be changed.
    
    for (i = 0; i < vertex.length; i++)
    {
      newvert[i] = newmesh.new Vertex(vertex[i]);
      for (j = 0; j < paramType.length; j++)
        if (paramType[j] == PER_VERTEX)
          newParamValue[j][0][i] = oldParamValue[j][0][i];
    }
    for (i = 0; i < edge.length; i++)
      {
        newedge[i] = newmesh.new Edge(edge[i].v1, edge[i].v2, -1);
        newedge[i].smoothness = edge[i].smoothness;
      }
    for (i = 0; i < face.length; i++)
      if (!split[i])
      {
        newface[i] = newmesh.new Face(face[i].v1, face[i].v2, face[i].v3, face[i].e1, face[i].e2, face[i].e3);
        for (j = 0; j < paramType.length; j++)
        {
          if (paramType[j] == PER_FACE)
            newParamValue[j][0][i] = oldParamValue[j][0][i];
          else if (paramType[j] == PER_FACE_VERTEX)
          {
            newParamValue[j][0][i] = oldParamValue[j][0][i];
            newParamValue[j][1][i] = oldParamValue[j][1][i];
            newParamValue[j][2][i] = oldParamValue[j][2][i];
          }
        }
      }

    // Now create the new vertices, edges, and faces.

    numVert = vertex.length;
    numEdge = edge.length;
    numFace = face.length;
    for (i = 0; i < face.length; i++)
      {
        if (!split[i])
          continue;
        newvert[numVert] = newmesh.blend(vertex[face[i].v1], vertex[face[i].v2], vertex[face[i].v3], 1.0/3.0, 1.0/3.0, 1.0/3.0);
        blendIKParams(newvert[numVert], vertex[face[i].v1], vertex[face[i].v2]);
        newvert[numVert].firstEdge = numEdge;
        newedge[numEdge] = newmesh.new Edge(face[i].v1, numVert, -1);
        newedge[numEdge+1] = newmesh.new Edge(face[i].v2, numVert, -1);
        newedge[numEdge+2] = newmesh.new Edge(face[i].v3, numVert, -1);
        newface[i] = newmesh.new Face(face[i].v1, face[i].v2, numVert, face[i].e1, numEdge+1, numEdge);
         newface[numFace] = newmesh.new Face(face[i].v2, face[i].v3, numVert, face[i].e2, numEdge+2, numEdge+1);
        newface[numFace+1] = newmesh.new Face(face[i].v3, face[i].v1, numVert, face[i].e3, numEdge, numEdge+2);
        for (j = 0; j < paramType.length; j++)
        {
          if (paramType[j] == PER_FACE)
            newParamValue[j][0][i] = newParamValue[j][0][numFace] = newParamValue[j][0][numFace+1] = oldParamValue[j][0][i];
          else if (paramType[j] == PER_FACE_VERTEX)
          {
            double centerVal = (oldParamValue[j][0][i]+oldParamValue[j][1][i]+oldParamValue[j][2][i])/3.0;
            newParamValue[j][0][i] = oldParamValue[j][0][i];
            newParamValue[j][1][i] = oldParamValue[j][1][i];
            newParamValue[j][2][i] = centerVal;
            newParamValue[j][0][numFace] = oldParamValue[j][1][i];
            newParamValue[j][1][numFace] = oldParamValue[j][2][i];
            newParamValue[j][2][numFace] = centerVal;
            newParamValue[j][0][numFace+1] = oldParamValue[j][2][i];
            newParamValue[j][1][numFace+1] = oldParamValue[j][0][i];
            newParamValue[j][2][numFace+1] = centerVal;
          }
        }
        numVert++;
        numEdge += 3;
        numFace += 2;
     }

    // Record which faces are adjacent to each edge.

    for (i = 0; i < newface.length; i++)
      {
        tempFace = newface[i];
        if (newedge[tempFace.e1].f1 == -1)
          newedge[tempFace.e1].f1 = i;
        else
          newedge[tempFace.e1].f2 = i;
        if (newedge[tempFace.e2].f1 == -1)
          newedge[tempFace.e2].f1 = i;
        else
          newedge[tempFace.e2].f2 = i;
        if (newedge[tempFace.e3].f1 == -1)
          newedge[tempFace.e3].f1 = i;
        else
          newedge[tempFace.e3].f2 = i;
      }

    // Count the number of edges intersecting each vertex.

    for (i = 0; i < newvert.length; i++)
      newvert[i].edges = 0;
    for (i = 0; i < newedge.length; i++)
      {
        newvert[newedge[i].v1].edges++;
        newvert[newedge[i].v2].edges++;
      }

    // Return the new mesh.
    
    newmesh.copyTextureAndMaterial(mesh);
    for (i = 0; i < paramType.length; i++)
    {
      if (paramType[i] == PER_VERTEX)
        newmesh.paramValue[i] = new VertexParameterValue(newParamValue[i][0]);
      else if (paramType[i] == PER_FACE)
        newmesh.paramValue[i] = new FaceParameterValue(newParamValue[i][0]);
      else if (paramType[i] == PER_FACE_VERTEX)
      {
        double val[][] = new double [newParamValue[i][0].length][];
        for (int index = 0; index < val.length; index++)
          val[index] = new double [] {newParamValue[i][0][index], newParamValue[i][1][index], newParamValue[i][2][index]};
        newmesh.paramValue[i] = new FaceVertexParameterValue(val);
      }
    }
    newmesh.vertex = newvert;
    newmesh.edge = newedge;
    newmesh.face = newface;
    newmesh.closed = mesh.closed;
    newmesh.smoothingMethod = mesh.smoothingMethod;
    newmesh.skeleton = mesh.skeleton.duplicate();
    return newmesh;
  }
  
  /** Create a new triangle mesh by subdividing this one until no edge is longer
      than the specified tolerance. */
  
  public TriangleMesh subdivideToLimit(double tol)
  {
    TriangleMesh newmesh = this;
    boolean split[], converged = false;
    double tol2 = 2.0*tol*tol;

    // Subdivide the mesh until every edge is smaller than the specified tolerance.
    
    while (!converged)
      {
        converged = true;
        split = new boolean [newmesh.edge.length];
        for (int i = 0; i < split.length; i++)
          {
            Vec3 v1 = newmesh.vertex[newmesh.edge[i].v1].r, v2 = newmesh.vertex[newmesh.edge[i].v2].r;
            double dx = v1.x-v2.x, dy = v1.y-v2.y, dz = v1.z-v2.z;
            if (dx*dx+dy*dy+dz*dz > tol2)
              {
                split[i] = true;
                converged = false;
              }
            else
              split[i] = false;
          }
        if (getSmoothingMethod() == APPROXIMATING)
          newmesh = subdivideLoop(newmesh, split, Double.MAX_VALUE);
        else if (getSmoothingMethod() == INTERPOLATING)
          newmesh = subdivideButterfly(newmesh, split, Double.MAX_VALUE);
        else
          newmesh = subdivideLinear(newmesh, split);
      }
    return newmesh;
  }
  
  /** Create a new triangle mesh by applying the displacement map of the texture assigned
      to this object. */
  
  public TriangleMesh getDisplacedMesh(double tol, double time)
  {
    TriangleMesh newmesh = this;
    boolean split[], converged = false;
    double tol2 = 2.0*tol*tol, b;
    Vec3 t1 = new Vec3(), t2 = new Vec3(), temp, norm[];

    // Subdivide the mesh until every edge is smaller than the specified tolerance.
    
    while (!converged)
      {
        converged = true;
        split = new boolean [newmesh.edge.length];
        for (int i = 0; i < split.length; i++)
          {
            Vec3 v1 = newmesh.vertex[newmesh.edge[i].v1].r, v2 = newmesh.vertex[newmesh.edge[i].v2].r;
            double dx = v1.x-v2.x, dy = v1.y-v2.y, dz = v1.z-v2.z;
            if (dx*dx+dy*dy+dz*dz > tol2)
              {
                split[i] = true;
                converged = false;
              }
            else
              split[i] = false;
          }
        if (getSmoothingMethod() == APPROXIMATING)
          newmesh = subdivideLoop(newmesh, split, Double.MAX_VALUE);
        else if (getSmoothingMethod() == INTERPOLATING)
          newmesh = subdivideButterfly(newmesh, split, Double.MAX_VALUE);
        else
          newmesh = subdivideLinear(newmesh, split);
      }
    
    // Determine the normal (without regard to smoothness values) for each vertex.

    Vertex v[] = newmesh.vertex;
    Edge e[] = newmesh.edge, tempEdge;
    norm = new Vec3 [v.length];
    for (int i = 0; i < v.length; i++)
      {
        int ed[] = v[i].getEdges();
        b = 2.0*Math.PI/ed.length;
        t1.set(0.0, 0.0, 0.0);
        t2.set(0.0, 0.0, 0.0);
        for (int j = 0; j < ed.length; j++)
          {
            tempEdge = e[ed[j]];
            if (tempEdge.v1 == i)
              {
                t1.add(v[tempEdge.v2].r.times(Math.cos(b*j)));
                t2.add(v[tempEdge.v2].r.times(Math.sin(b*j)));
              }
            else
              {
                t1.add(v[tempEdge.v1].r.times(Math.cos(b*j)));
                t2.add(v[tempEdge.v1].r.times(Math.sin(b*j)));
              }
          }

        // We now have the tangent vectors, so calculate the normal.

        temp = t1.cross(t2);
        b = temp.length();
        if (b == 0.0)
          temp = null;
        else if (v[i].clockwise())
          temp.scale(-1.0/b);
        else
          temp.scale(1.0/b);
        norm[i] = temp;
      }
    
    // Find the default parameter values.
    
    double param[] = new double [paramValue.length];
    for (int i = 0; i < paramValue.length; i++)
      param[i] = paramValue[i].getAverageValue();
    
    // Displace each vertex outward along its normal.

    TextureMapping map = getTextureMapping();
    for (int i = 0; i < v.length; i++)
      {
        for (int j = 0; j < paramValue.length; j++)
          if (paramValue[j] instanceof VertexParameterValue)
            param[j] = ((VertexParameterValue) paramValue[j]).getValue()[i];
        double height = map.getDisplacement(v[i].r, tol, time, param);
        v[i].r.x += height*norm[i].x;
        v[i].r.y += height*norm[i].y;
        v[i].r.z += height*norm[i].z;
      }
    System.gc();
    return newmesh;
  }
  
  /** If necessary, reorder the points in each face so that the normals will be properly oriented. */
  
  public void makeRightSideOut()
  {
    Vec3 norm[] = getNormals(), result = new Vec3();

    for (int i = 0; i < norm.length; i++)
      {
        result.x += vertex[i].r.x*norm[i].x;
        result.y += vertex[i].r.y*norm[i].y;
        result.z += vertex[i].r.z*norm[i].z;
      }
    if (result.x + result.y + result.z < 0.0)
      reverseNormals();
  }
  
  /** Reorder the vertices in each face, so as to reverse all of the normal vectors. */
  
  public void reverseNormals()
  {
    int i, temp;
    
    for (i = 0; i < face.length; i++)
      {
        temp = face[i].v2;
        face[i].v2 = face[i].v3;
        face[i].v3 = temp;
        temp = face[i].e1;
        face[i].e1 = face[i].e3;
        face[i].e3 = temp;
      }
    cachedMesh = null;
  }

  /** Get an array of normal vectors.  This calculates a single normal for each vertex,
      ignoring smoothness values. */
     
  public Vec3 [] getNormals()
  {
    Vec3 faceNorm, norm[] = new Vec3 [vertex.length];
    
    // Calculate a normal for each face, and average the face normals for each vertex.
    
    for (int i = 0; i < norm.length; i++)
      norm[i] = new Vec3();
    for (int i = 0; i < face.length; i++)
      {
        Vec3 edge1 = vertex[face[i].v2].r.minus(vertex[face[i].v1].r);
        Vec3 edge2 = vertex[face[i].v3].r.minus(vertex[face[i].v1].r);
        Vec3 edge3 = vertex[face[i].v3].r.minus(vertex[face[i].v2].r);
        edge1.normalize();
        edge2.normalize();
        edge3.normalize();
        faceNorm = edge1.cross(edge2);
        double length = faceNorm.length();
        if (length == 0.0)
          continue;
        faceNorm.scale(1.0/length);
        double dot1 = edge1.dot(edge2);
        double dot2 = -edge1.dot(edge3);
        double dot3 = edge2.dot(edge3);
        if (dot1 < -1.0)
          dot1 = -1.0; // This can occassionally happen due to roundoff error
        if (dot1 > 1.0)
          dot1 = 1.0;
        if (dot2 < -1.0)
          dot2 = -1.0;
        if (dot2 > 1.0)
          dot2 = 1.0;
        if (dot3 < -1.0)
          dot3 = -1.0;
        if (dot3 > 1.0)
          dot3 = 1.0;
        norm[face[i].v1].add(faceNorm.times(Math.acos(dot1)));
        norm[face[i].v2].add(faceNorm.times(Math.acos(dot2)));
        norm[face[i].v3].add(faceNorm.times(Math.acos(dot3)));
      }
    for (int i = 0; i < norm.length; i++)
      norm[i].normalize();
    return norm;
  }

  public int getFaceCount()
  {
    return face.length;
  }

  public int getFaceVertexCount(int faceIndex)
  {
    return 3;
  }

  public int getFaceVertexIndex(int faceIndex, int vertexIndex)
  {
    Face f = face[faceIndex];
    if (vertexIndex == 0)
      return f.v1;
    if (vertexIndex == 1)
      return f.v2;
    return f.v3;
  }

  /** Return a new mesh which is an "optimized" version of the input mesh.  This is done by rearranging edges
      to eliminate very small angles, or vertices where many edges come together.  The resulting mesh will
      generally produce a better looking surface after smoothing is applied to it. */
  
  public static TriangleMesh optimizeMesh(TriangleMesh mesh)
  {
    Face face[] = mesh.face;
    Edge edge[] = mesh.edge;
    Vertex vertex[] = mesh.vertex;
    boolean candidate[] = new boolean [edge.length];
    TriangleMesh newmesh = null;

    for (int i = 0; i < edge.length; i++)
      candidate[i] = true;
    while (true)
      {
        Vec3 faceNorm[] = new Vec3 [face.length];
        boolean onBoundary[] = new boolean [vertex.length];
        int numEdges[] = new int [vertex.length];
        
        // Initialize the various arrays, and determine which edges are really candidates for optimization.
        
        for (int i = 0; i < face.length; i++)
          {
            Face f = face[i];
            if (candidate[f.e1] || candidate[f.e2] || candidate[f.e3])
              {
                Vec3 v1 = vertex[f.v1].r;
                Vec3 v2 = vertex[f.v2].r;
                Vec3 v3 = vertex[f.v3].r;
                Vec3 d1 = v2.minus(v1);
                Vec3 d2 = v3.minus(v1);
                faceNorm[i] = d1.cross(d2);
                double length = faceNorm[i].length();
                if (length > 0.0)
                  faceNorm[i].scale(1.0/length);
                else if (!v1.equals(v2) && !v1.equals(v3) && !v2.equals(v3))
                  faceNorm[i] = null;
              }
          }
        for (int i = 0; i < edge.length; i++)
          {
            Edge e = edge[i];
            numEdges[e.v1]++;
            numEdges[e.v2]++;
            if (e.f2 == -1)
              {
                onBoundary[e.v1] = onBoundary[e.v2] = true;
                candidate[i] = false;
              }
            else if (candidate[i] && faceNorm[e.f1] != null && faceNorm[e.f2] != null && faceNorm[e.f1].dot(faceNorm[e.f2]) < 0.99)
              candidate[i] = false;
          }
        
        // For each candidate edge, find the list of vertices and angles involved in swapping it.  The vertices
        // are ordered as follows:
        
        //              <-
        //              /\ 2
        //             /f1\
        //            /    \
        //          0 ------ 1
        //            \    /
        //             \f2/
        //              \/ 3
        //              ->
    
        int swapVert[][] = new int [edge.length][];
        double minAngle[][] = new double [edge.length][];
        for (int i = 0; i < edge.length; i++)
          if (candidate[i])
            {
              // First find the vertices.
              
              swapVert[i] = new int [4];
              Edge e = edge[i];
              Face f1 = face[e.f1], f2 = face[e.f2];
              if ((e.v1 == f1.v1 && e.v2 == f1.v2) || (e.v1 == f1.v2 && e.v2 == f1.v3) || (e.v1 == f1.v3 && e.v2 == f1.v1))
                {
                  swapVert[i][0] = e.v1;
                  swapVert[i][1] = e.v2;
                }
              else
                {
                  swapVert[i][0] = e.v2;
                  swapVert[i][1] = e.v1;
                }
              if (e.v1 != f1.v1 && e.v2 != f1.v1)
                swapVert[i][2] = f1.v1;
              else if (e.v1 != f1.v2 && e.v2 != f1.v2)
                swapVert[i][2] = f1.v2;
              else
                swapVert[i][2] = f1.v3;
              if (e.v1 != f2.v1 && e.v2 != f2.v1)
                swapVert[i][3] = f2.v1;
              else if (e.v1 != f2.v2 && e.v2 != f2.v2)
                swapVert[i][3] = f2.v2;
              else
                swapVert[i][3] = f2.v3;
              
              // Now calculate the angles.
              
              minAngle[i] = new double [4];
              Vec3 d1 = vertex[swapVert[i][1]].r.minus(vertex[swapVert[i][0]].r);
              Vec3 d2 = vertex[swapVert[i][2]].r.minus(vertex[swapVert[i][0]].r);
              Vec3 d3 = vertex[swapVert[i][3]].r.minus(vertex[swapVert[i][0]].r);
              Vec3 d4 = vertex[swapVert[i][2]].r.minus(vertex[swapVert[i][1]].r);
              Vec3 d5 = vertex[swapVert[i][3]].r.minus(vertex[swapVert[i][1]].r);
              Vec3 d6 = vertex[swapVert[i][3]].r.minus(vertex[swapVert[i][2]].r);
              d1.normalize();
              d2.normalize();
              d3.normalize();
              d4.normalize();
              d5.normalize();
              d6.normalize();
              double a1, a2;
              a1 = Math.acos(d1.dot(d2));
              a2 = Math.acos(d1.dot(d3));
              if (a1+a2 >= Math.PI)
                {
                  candidate[i] = false;
                  continue;
                }
              minAngle[i][0] = (a1 < a2 ? a1 : a2);
              a1 = Math.acos(-d1.dot(d4));
              a2 = Math.acos(-d1.dot(d5));
              if (a1+a2 >= Math.PI)
                {
                  candidate[i] = false;
                  continue;
                }
              minAngle[i][1] = (a1 < a2 ? a1 : a2);
              a1 = Math.acos(-d6.dot(d2));
              a2 = Math.acos(-d6.dot(d4));
              minAngle[i][2] = (a1 < a2 ? a1 : a2);
              a1 = Math.acos(d6.dot(d3));
              a2 = Math.acos(d6.dot(d5));
              minAngle[i][3] = (a1 < a2 ? a1 : a2);
            }
        
        // Calculate scores for each candidate edge, and decide which ones to swap.
        
        double score[] = new double [edge.length];
        boolean swap[] = new boolean [edge.length];
        for (int i = 0; i < score.length; i++)
          if (candidate[i])
            score[i] = calcSwapScore(minAngle[i], swapVert[i], numEdges, onBoundary);
        while (true)
          {
            int best = -1;
            double maxScore = 0.0;
            for (int i = 0; i < candidate.length; i++)
              if (candidate[i] && score[i] > maxScore)
                {
                  best = i;
                  maxScore = score[i];
                }
            if (best == -1)
              break;
            
            // Mark the edge to be swapped.  Remove it and every other edge that shares a face with it
            // from the candidate list.
            
            swap[best] = true;
            Edge e = edge[best];
            Face f = face[e.f1];
            candidate[f.e1] = candidate[f.e2] = candidate[f.e3] = false;
            f = face[e.f2];
            candidate[f.e1] = candidate[f.e2] = candidate[f.e3] = false;
            
            // Update the numEdges array, and recalculate scores.
            
            numEdges[swapVert[best][0]]--;
            numEdges[swapVert[best][1]]--;
            numEdges[swapVert[best][2]]++;
            numEdges[swapVert[best][3]]++;
            for (int i = 0; i < 4; i++)
              {
                int vertEdges[] = vertex[swapVert[best][i]].getEdges();
                for (int j = 0; j < vertEdges.length; j++)
                  if (candidate[vertEdges[j]])
                    score[vertEdges[j]] = calcSwapScore(minAngle[vertEdges[j]], swapVert[vertEdges[j]], numEdges, onBoundary);
              }
          }
        
        // We now know which edges we want to swap.  Create the new mesh.
        
        int newface[][] = new int [face.length][];
        int next = 0;
        for (int i = 0; i < face.length; i++)
          {
            Face f = face[i];
            if (!swap[f.e1] && !swap[f.e2] && !swap[f.e3])
              newface[next++] = new int [] {f.v1, f.v2, f.v3};
          }
        int firstSplit = next;
        for (int i = 0; i < edge.length; i++)
          if (swap[i])
            {
              newface[next++] = new int [] {swapVert[i][2], swapVert[i][0], swapVert[i][3]};
              newface[next++] = new int [] {swapVert[i][2], swapVert[i][3], swapVert[i][1]};
            }
        newmesh = new TriangleMesh(vertex, newface);
        
        // Copy over edge smoothness values.
        
        Vertex newvert[] = (Vertex []) newmesh.getVertices();
        Edge newedge[] = newmesh.getEdges();
        for (int i = 0; i < edge.length; i++)
          if (!swap[i] && edge[i].smoothness != 1.0)
            {
              int edges2[] = newvert[edge[i].v1].getEdges();
              Edge e1 = edge[i];
              for (int k = 0; k < edges2.length; k++)
                {
                  Edge e2 = newedge[edges2[k]];
                  if ((e1.v1 == e2.v1 && e1.v2 == e2.v2) || (e1.v1 == e2.v2 && e1.v2 == e2.v1))
                    {
                      e2.smoothness = e1.smoothness;
                      break;
                    }
                }
            }
        
        // Determine which edges are candidates for the next iteration.
        
        if (firstSplit == next)
          break;
        vertex = newvert;
        edge = newedge;
        face = newmesh.getFaces();
        candidate = new boolean [edge.length];
        for (int i = firstSplit; i < face.length; i++)
          {
            Face f = face[i];
            candidate[f.e1] = candidate[f.e2] = candidate[f.e3] = true;
          }
      }
    
    // Copy over other mesh properties.

    newmesh.copyTextureAndMaterial(mesh);
    newmesh.smoothingMethod = mesh.smoothingMethod;
    newmesh.skeleton = mesh.skeleton.duplicate();
    return newmesh;
  }
  
  /** This is a utility routine used by optimizeMesh().  It calculates the score for swapping a
      particular edge. */
  
  private static double calcSwapScore(double minAngle[], int vert[], int numEdges[], boolean onBoundary[])
  {
    double s[] = new double [4];
    for (int i = 0; i < 4; i++)
      {
        int v = vert[i];
        int ideal = (onBoundary[v] ? 4 : 6);
        if (i > 1)
          ideal--;
        s[i] = (numEdges[v] > ideal ? minAngle[i]/(numEdges[v]-ideal+1.5) : minAngle[i]);
      }
    double currentScore = (s[0] < s[1] ? s[0] : s[1]);
    double swapScore = (s[2] < s[3] ? s[2] : s[3]);
    return swapScore-currentScore;
  }

  /** Automatically select smoothness values for all edges in the mesh.  This is done based on the
      angle between the two faces sharing the edge.  If it is greater than the specified cutoff, the
      edge smoothness is set to 0.  Otherwise, it is set to 1.
      @param angle      the cutoff angle, in radians
   */

  public void autosmoothMeshEdges(double angle)
  {
    double cutoff = Math.cos(angle);
    for (int i = 0; i < edge.length; i++)
    {
      if (edge[i].f2 == -1)
      {
        edge[i].smoothness = 1.0f;
        continue;
      }
      Face f1 = face[edge[i].f1];
      Face f2 = face[edge[i].f2];
      Vec3 norm1 = vertex[f1.v1].r.minus(vertex[f1.v2].r).cross(vertex[f1.v1].r.minus(vertex[f1.v3].r));
      Vec3 norm2 = vertex[f2.v1].r.minus(vertex[f2.v2].r).cross(vertex[f2.v1].r.minus(vertex[f2.v3].r));
      norm1.normalize();
      norm2.normalize();
      if (norm1.dot(norm2) < cutoff)
        edge[i].smoothness = 0.0f;
      else
        edge[i].smoothness = 1.0f;
    }
  }
  
  /** The following two methods are used for reading and writing files.  The first is a
      constructor which reads the necessary data from an input stream.  The other writes
      the object's representation to an output stream. */

  public TriangleMesh(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);

    Vertex v1, v2;
    short version = in.readShort();

    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    vertex = new Vertex [in.readInt()];
    if (version == 0)
      for (int i = 0; i < paramValue.length; i++)
        paramValue[i] = new VertexParameterValue(new double [vertex.length]);
    for (int i = 0; i < vertex.length; i++)
      {
        vertex[i] = new Vertex(new Vec3(in));
        vertex[i].smoothness = in.readFloat();
        vertex[i].ikJoint = in.readInt();
        vertex[i].ikWeight = in.readDouble();
        if (version == 0)
          for (int j = 0; j < paramValue.length; j++)
            ((VertexParameterValue) paramValue[j]).getValue()[i] = in.readDouble();
      }
    edge = new Edge [in.readInt()];
    for (int i = 0; i < edge.length; i++)
      {
        edge[i] = new Edge (in.readInt(), in.readInt(), in.readInt());
        edge[i].f2 = in.readInt();
        edge[i].smoothness = in.readFloat();
      }
    face = new Face [in.readInt()];
    for (int i = 0; i < face.length; i++)
      face[i] = new Face (in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt());
    closed = in.readBoolean();
    smoothingMethod = in.readInt();
    
    // Find the edge information for vertices.
    
    for (int i = 0; i < edge.length; i++)
      {
        v1 = vertex[edge[i].v1];
        v2 = vertex[edge[i].v2];
        v1.edges++;
        v2.edges++;
        if (edge[i].f2 == -1)
          v1.firstEdge = v2.firstEdge = i;
        else
          {
            if (v1.firstEdge == -1)
              v1.firstEdge = i;
            if (v2.firstEdge == -1)
              v2.firstEdge = i;
          }
      }
    skeleton = new Skeleton(in);
  }

  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);

    out.writeShort(1);
    out.writeInt(vertex.length);
    for (int i = 0; i < vertex.length; i++)
      {
        vertex[i].r.writeToFile(out);
        out.writeFloat(vertex[i].smoothness);
        out.writeInt(vertex[i].ikJoint);
        out.writeDouble(vertex[i].ikWeight);
      }
    out.writeInt(edge.length);
    for (int i = 0; i < edge.length; i++)
      {
        out.writeInt(edge[i].v1);
        out.writeInt(edge[i].v2);
        out.writeInt(edge[i].f1);
        out.writeInt(edge[i].f2);
        out.writeFloat(edge[i].smoothness);
      }
    out.writeInt(face.length);
    for (int i = 0; i < face.length; i++)
      {
        out.writeInt(face[i].v1);
        out.writeInt(face[i].v2);
        out.writeInt(face[i].v3);
        out.writeInt(face[i].e1);
        out.writeInt(face[i].e2);
        out.writeInt(face[i].e3);
      }
    out.writeBoolean(closed);
    out.writeInt(smoothingMethod);
    skeleton.writeToStream(out);
  }

  public Property[] getProperties()
  {
    return PROPERTIES.clone();
  }

  public Object getPropertyValue(int index)
  {
    return PROPERTIES[0].getAllowedValues()[smoothingMethod];
  }

  public void setPropertyValue(int index, Object value)
  {
    Object values[] = PROPERTIES[0].getAllowedValues();
    for (int i = 0; i < values.length; i++)
      if (values[i].equals(value))
        setSmoothingMethod(i);
  }

  /** Return a Keyframe which describes the current pose of this object. */
  
  public Keyframe getPoseKeyframe()
  {
    return new TriangleMeshKeyframe(this);
  }
  
  /** Modify this object based on a pose keyframe. */
  
  public void applyPoseKeyframe(Keyframe k)
  {
    TriangleMeshKeyframe key = (TriangleMeshKeyframe) k;
    
    for (int i = 0; i < vertex.length; i++)
      {
        Vertex v = vertex[i];
        v.r.set(key.vertPos[i]);
        v.smoothness = (float) key.vertSmoothness[i];
      }
    if (texParam != null && texParam.length > 0)
      for (int i = 0; i < texParam.length; i++)
        paramValue[i] = key.paramValue[i].duplicate();
    for (int i = 0; i < edge.length; i++)
      edge[i].smoothness = key.edgeSmoothness[i];
    skeleton.copy(key.skeleton);
    cachedMesh = null;
    cachedWire = null;
    bounds = null;
  }

  /** Allow TriangleMeshes to be converted to Actors. */
  
  public boolean canConvertToActor()
  {
    return true;
  }
  
  /** TriangleMeshes cannot be keyframed directly, since any change to mesh topology would
      cause all keyframes to become invalid.  Return an actor for this mesh. */
  
  public Object3D getPosableObject()
  {
    TriangleMesh m = (TriangleMesh) duplicate();
    return new Actor(m);
  }

  /** This class represents a pose of a TriangleMesh. */
  
  public static class TriangleMeshKeyframe extends MeshGesture
  {
    Vec3 vertPos[];
    float vertSmoothness[], edgeSmoothness[];
    ParameterValue paramValue[];
    Skeleton skeleton;
    TriangleMesh mesh;

    public TriangleMeshKeyframe(TriangleMesh mesh)
    {
      this.mesh = mesh;
      skeleton = mesh.getSkeleton().duplicate();
      vertPos = new Vec3 [mesh.vertex.length];
      vertSmoothness = new float [mesh.vertex.length];
      edgeSmoothness = new float [mesh.edge.length];
      for (int i = 0; i < vertPos.length; i++)
        {
          Vertex v = mesh.vertex[i];
          vertPos[i] = new Vec3(v.r);
          vertSmoothness[i] = v.smoothness;
        }
      for (int i = 0; i < edgeSmoothness.length; i++)
        edgeSmoothness[i] = mesh.edge[i].smoothness;
      paramValue = new ParameterValue [mesh.texParam.length];
      for (int i = 0; i < paramValue.length; i++)
        paramValue[i] = mesh.paramValue[i].duplicate();
    }
    
    private TriangleMeshKeyframe()
    {
    }

    /** Get the Mesh this Gesture belongs to. */
    
    protected Mesh getMesh()
    {
      return mesh;
    }
    
    /** Get the positions of all vertices in this Gesture. */
    
    protected Vec3 [] getVertexPositions()
    {
      return vertPos;
    }
    
    /** Set the positions of all vertices in this Gesture. */
    
    protected void setVertexPositions(Vec3 pos[])
    {
      vertPos = pos;
    }

    /** Get the skeleton for this pose (or null if it doesn't have one). */
  
    public Skeleton getSkeleton()
    {
      return skeleton;
    }
  
    /** Set the skeleton for this pose. */
  
    public void setSkeleton(Skeleton s)
    {
      skeleton = s;
    }
    
    /** Create a duplicate of this keyframe. */
  
    public Keyframe duplicate()
    {
      return duplicate(mesh);
    }

    public Keyframe duplicate(Object owner)
    {
      TriangleMeshKeyframe k = new TriangleMeshKeyframe();
      if (owner instanceof TriangleMesh)
        k.mesh = (TriangleMesh) owner;
      else
        k.mesh = (TriangleMesh) ((ObjectInfo) owner).getObject();
      k.skeleton = skeleton.duplicate();
      k.vertPos = new Vec3 [vertPos.length];
      k.vertSmoothness = new float [vertSmoothness.length];
      k.edgeSmoothness = new float [edgeSmoothness.length];
      for (int i = 0; i < vertPos.length; i++)
        {
          k.vertPos[i] = new Vec3(vertPos[i]);
          k.vertSmoothness[i] = vertSmoothness[i];
        }
      for (int i = 0; i < edgeSmoothness.length; i++)
        k.edgeSmoothness[i] = edgeSmoothness[i];
      k.paramValue = new ParameterValue [paramValue.length];
      for (int i = 0; i < paramValue.length; i++)
        k.paramValue[i] = paramValue[i].duplicate();
      return k;
    }
  
    /** Get the list of graphable values for this keyframe. */
  
    public double [] getGraphValues()
    {
      return new double [0];
    }
  
    /** Set the list of graphable values for this keyframe. */
  
    public void setGraphValues(double values[])
    {
    }

    /** These methods return a new Keyframe which is a weighted average of this one and one,
       two, or three others.  These methods should never be called, since TriangleMeshes
       can only be keyframed by converting them to Actors. */
  
    public Keyframe blend(Keyframe o2, double weight1, double weight2)
    {
      return null;
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
    {
      return null;
    }

    public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
    {
      return null;
    }

    /** Modify the mesh surface of a Gesture to be a weighted average of an arbitrary list of Gestures,
        averaged about this pose.  This method only modifies the vertex positions and texture parameters,
        not the skeleton, and all vertex positions are based on the offsets from the joints they are
        bound to.
        @param average   the Gesture to modify to be an average of other Gestures
        @param p         the list of Gestures to average
        @param weight    the weights for the different Gestures
    */
    
    public void blendSurface(MeshGesture average, MeshGesture p[], double weight[])
    {
      super.blendSurface(average, p, weight);
      TriangleMeshKeyframe avg = (TriangleMeshKeyframe) average;
      for (int i = 0; i < weight.length; i++)
      {
        TriangleMeshKeyframe key = (TriangleMeshKeyframe) p[i];
        for (int j = 0; j < vertSmoothness.length; j++)
          avg.vertSmoothness[j] += (float) (weight[i]*(key.vertSmoothness[j]-vertSmoothness[j]));
        for (int j = 0; j < edgeSmoothness.length; j++)
          avg.edgeSmoothness[j] += weight[i]*(key.edgeSmoothness[j]-edgeSmoothness[j]);
      }

      // Make sure all smoothness values are within legal bounds.
      
      for (int i = 0; i < vertSmoothness.length; i++)
        {
          if (avg.vertSmoothness[i] < 0.0)
            avg.vertSmoothness[i] = 0.0f;
          if (avg.vertSmoothness[i] > 1.0)
            avg.vertSmoothness[i] = 1.0f;
        }
      for (int i = 0; i < edgeSmoothness.length; i++)
        {
          if (avg.edgeSmoothness[i] < 0.0)
            avg.edgeSmoothness[i] = 0.0f;
          if (avg.edgeSmoothness[i] > 1.0)
            avg.edgeSmoothness[i] = 1.0f;
        }
    }

    /** Determine whether this keyframe is identical to another one. */
  
    public boolean equals(Keyframe k)
    {
      if (!(k instanceof TriangleMeshKeyframe))
        return false;
      TriangleMeshKeyframe key = (TriangleMeshKeyframe) k;
      for (int i = 0; i < vertPos.length; i++)
        {
          if (!vertPos[i].equals(key.vertPos[i]))
            return false;
          if (vertSmoothness[i] != key.vertSmoothness[i])
            return false;
        }
      for (int i = 0; i < paramValue.length; i++)
        if (!paramValue[i].equals(key.paramValue[i]))
          return false;
      for (int i = 0; i < edgeSmoothness.length; i++)
        if (edgeSmoothness[i] != key.edgeSmoothness[i])
          return false;
      if (!skeleton.equals(key.skeleton))
        return false;
      return true;
    }
  
    /** Update the texture parameter values when the texture is changed. */
  
    public void textureChanged(TextureParameter oldParams[], TextureParameter newParams[])
    {
      ParameterValue newval[] = new ParameterValue [newParams.length];
      
      for (int i = 0; i < newParams.length; i++)
        {
          int j;
          for (j = 0; j < oldParams.length && !oldParams[j].equals(newParams[i]); j++);
          if (j == oldParams.length)
            {
              // This is a new parameter, so copy the value from the mesh.
              
              for (int k = 0; k < mesh.texParam.length; k++)
                if (mesh.texParam[k].equals(newParams[i]))
                {
                  newval[i] = mesh.paramValue[k].duplicate();
                  break;
                }
            }
          else
            {
              // This is an old parameter, so copy the values over.
              
              newval[i] = paramValue[j];
            }
        }
      paramValue = newval;
    }
  
    /** Get the value of a per-vertex texture parameter. */
    
    public ParameterValue getTextureParameter(TextureParameter p)
    {
      // Determine which parameter to get.
      
      for (int i = 0; i < mesh.texParam.length; i++)
        if (mesh.texParam[i].equals(p))
          return paramValue[i];
      return null;
    }
  
    /** Set the value of a per-vertex texture parameter. */
    
    public void setTextureParameter(TextureParameter p, ParameterValue value)
    {
      // Determine which parameter to set.
      
      int which;
      for (which = 0; which < mesh.texParam.length && !mesh.texParam[which].equals(p); which++);
      if (which == mesh.texParam.length)
        return;
      paramValue[which] = value;
    }
  
    /** Write out a representation of this keyframe to a stream. */
  
    public void writeToStream(DataOutputStream out) throws IOException
    {
      out.writeShort(1); // version
      out.writeInt(vertPos.length);
      for (int i = 0; i < vertPos.length; i++)
        {
          vertPos[i].writeToFile(out);
          out.writeFloat(vertSmoothness[i]);
        }
      for (int i = 0; i < paramValue.length; i++)
      {
        out.writeUTF(paramValue[i].getClass().getName());
        paramValue[i].writeToStream(out);
      }
      out.writeInt(edgeSmoothness.length);
      for (int i = 0; i < edgeSmoothness.length; i++)
        out.writeFloat(edgeSmoothness[i]);
      Joint joint[] = skeleton.getJoints();
      for (int i = 0; i < joint.length; i++)
        {
          joint[i].coords.writeToFile(out);
          out.writeDouble(joint[i].angle1.pos);
          out.writeDouble(joint[i].angle2.pos);
          out.writeDouble(joint[i].twist.pos);
          out.writeDouble(joint[i].length.pos);
        }
    }

    /** Reconstructs the keyframe from its serialized representation. */

    public TriangleMeshKeyframe(DataInputStream in, Object parent) throws IOException, InvalidObjectException
    {
      this();
      short version = in.readShort();
      if (version < 0 || version > 1)
        throw new InvalidObjectException("");
      mesh = (TriangleMesh) parent;
      int numVert = in.readInt();
      vertPos = new Vec3 [numVert];
      vertSmoothness = new float [numVert];
      paramValue = new ParameterValue [mesh.texParam.length];
      if (version == 0)
        for (int i = 0; i < paramValue.length; i++)
          paramValue[i] = new VertexParameterValue(new double [numVert]);
      for (int i = 0; i < numVert; i++)
        {
          vertPos[i] = new Vec3(in);
          vertSmoothness[i] = in.readFloat();
          if (version == 0)
            for (int j = 0; j < paramValue.length; j++)
              ((VertexParameterValue) paramValue[j]).getValue()[i] = in.readDouble();
        }
      if (version > 0)
        for (int i = 0; i < paramValue.length; i++)
          paramValue[i] = readParameterValue(in);
      edgeSmoothness = new float [in.readInt()];
      for (int i = 0; i < edgeSmoothness.length; i++)
        edgeSmoothness[i] = in.readFloat();
      skeleton = mesh.getSkeleton().duplicate();
      Joint joint[] = skeleton.getJoints();
      for (int i = 0; i < joint.length; i++)
        {
          joint[i].coords = new CoordinateSystem(in);
          joint[i].angle1.pos = in.readDouble();
          joint[i].angle2.pos = in.readDouble();
          joint[i].twist.pos = in.readDouble();
          joint[i].length.pos = in.readDouble();
        }
    }
  }
}
