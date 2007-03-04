/* Copyright (C) 1999-2004 by Peter Eastman

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
import artofillusion.texture.*;
import java.util.Vector;

/** TriMeshBeveler defines methods for beveling/extruding a TriangleMesh. */

public class TriMeshBeveler
{
  private TriangleMesh origMesh, mesh;
  private boolean selected[], newSelection[];
  private int mode;
  private Vec3 faceInsets[][], faceNormal[];
  private Vector newIndex;
  
  public static final int BEVEL_FACES = 0;
  public static final int BEVEL_FACE_GROUPS = 1;
  public static final int BEVEL_EDGES = 2;
  public static final int BEVEL_VERTICES = 3;

  public TriMeshBeveler(TriangleMesh theMesh, boolean selection[], int bevelMode)
  {
    origMesh = theMesh;
    selected = selection;
    mode = bevelMode;
    if (mode < 0 || mode > 3)
      throw new IllegalArgumentException();
  }
  
  /**
   * Construct a new mesh by beveling and extruding the original one.
   *
   * @param height      the extrude height
   * @param width       the bevel width
   */
  
  public TriangleMesh bevelMesh(double height, double width)
  {
    switch (mode)
    {
      case BEVEL_FACES:
        return bevelIndividualFaces(height, width);
      case BEVEL_FACE_GROUPS:
        return bevelFacesAsGroup(height, width);
      case BEVEL_EDGES:
        return bevelEdges(height, width);
      case BEVEL_VERTICES:
        return bevelVertices(height, width);
    }
    return null;
  }
  
  /**
   * Construct a new mesh, beveling each face individually.
   *
   * @param height      the extrude height
   * @param width       the bevel width
   */

  private TriangleMesh bevelIndividualFaces(double height, double width)
  {
    mesh = (TriangleMesh) origMesh.duplicate();
    if (width == 0.0 && height == 0.0)
    {
      newSelection = selected;
      return mesh;
    }
    Vertex v[] = (Vertex []) mesh.getVertices();
    Edge e[] = mesh.getEdges();
    Face f[] = mesh.getFaces();
    int i, j;
    Vector face = new Vector(), vert = new Vector();

    newIndex = new Vector();
    findVertexInsets(height, width);

    // All old vertices will be in the new mesh, so first copy them over.

    for (i = 0; i < v.length; i++)
      vert.addElement(v[i]);

    // Create the new list of faces.

    for (i = 0; i < f.length; i++)
      {
	if (selected[i])
	  {
	    j = vert.size();
	    vert.addElement(offsetVertex(mesh, v[f[i].v1], faceInsets[i][0]));
	    vert.addElement(offsetVertex(mesh, v[f[i].v2], faceInsets[i][1]));
	    vert.addElement(offsetVertex(mesh, v[f[i].v3], faceInsets[i][2]));
	    newIndex.addElement(new Integer(face.size()));
	    face.addElement(new int [] {j, j+1, j+2, i});
	    face.addElement(new int [] {f[i].v1, f[i].v2, j, i});
	    face.addElement(new int [] {j, f[i].v2, j+1, i});
	    face.addElement(new int [] {f[i].v2, f[i].v3, j+1, i});
	    face.addElement(new int [] {j+1, f[i].v3, j+2, i});
	    face.addElement(new int [] {f[i].v3, f[i].v1, j+2, i});
	    face.addElement(new int [] {j+2, f[i].v1, j, i});
	  }
	else
	  face.addElement(new int [] {f[i].v1, f[i].v2, f[i].v3, i});
      }
    prepareMesh(mesh, face, vert, null);

    // Copy over smoothness values for edges.

    Edge newe[] = mesh.getEdges();
    for (i = 0; i < e.length; i++)
      for (j = 0; j < newe.length; j++)
	if ((e[i].v1 == newe[j].v1 && e[i].v2 == newe[j].v2) ||
		(e[i].v1 == newe[j].v2 && e[i].v2 == newe[j].v1))
	  {
	    newe[j].smoothness = e[i].smoothness;
	    break;
	  }
    
    // Record which faces should be selected.
    
    newSelection = new boolean [mesh.getFaces().length];
    for (i = 0; i < newIndex.size(); i++)
      newSelection[((Integer) newIndex.elementAt(i)).intValue()] = true;
    return mesh;
  }

  /** Find the list of selected faces, and for each one,  calculate a displacement for each vertex. */

  private void findVertexInsets(double height, double width)
  {
    Vertex v[] = (Vertex []) mesh.getVertices();
    Face f[] = mesh.getFaces();
    double length, dot;
    Vec3 e1, e2, e3, normal;
    int i;
    
    faceInsets = new Vec3 [f.length][];
    for (i = 0; i < f.length; i++)
      if (selected[i])
	{
	  faceInsets[i] = new Vec3 [3];
	  e1 = v[f[i].v2].r.minus(v[f[i].v1].r);
	  e2 = v[f[i].v3].r.minus(v[f[i].v2].r);
	  e3 = v[f[i].v1].r.minus(v[f[i].v3].r);
	  e1.normalize();
	  e2.normalize();
	  e3.normalize();
	  normal = e1.cross(e2);
	  length = normal.length();
	  if (length == 0.0) // A degenerate triangle.
	    faceInsets[i][0] = faceInsets[i][1] = faceInsets[i][2] = new Vec3();
	  else
	    {
	      normal.scale(height/length);
	      dot = -e1.dot(e3);
	      faceInsets[i][0] = e1.minus(e3).times(width/Math.sqrt(1-dot*dot)).plus(normal);
	      dot = -e2.dot(e1);
	      faceInsets[i][1] = e2.minus(e1).times(width/Math.sqrt(1-dot*dot)).plus(normal);
	      dot = -e3.dot(e2);
	      faceInsets[i][2] = e3.minus(e2).times(width/Math.sqrt(1-dot*dot)).plus(normal);
	    }
	}
  }

  /**
   * Construct a new mesh, beveling all selected faces as a group.
   *
   * @param height      the extrude height
   * @param width       the bevel width
   */

  private TriangleMesh bevelFacesAsGroup(double height, double width)
  {
    mesh = (TriangleMesh) origMesh.duplicate();
    if (width == 0.0 && height == 0.0)
    {
      newSelection = selected;
      return mesh;
    }
    Vertex v[] = (Vertex []) mesh.getVertices();
    Edge e[] = mesh.getEdges();
    Face f1, f2, f[] = mesh.getFaces();
    int i, j, k, m, n, newface[][], vertFace[][], numVertFaces[], tempFace[], bev[];
    int group[], touchCount[], groupCount;
    Vec3 temp = new Vec3();
    Vector face = new Vector(), vert = new Vector(), bevel = new Vector();
    boolean someSelected[] = new boolean [v.length], allSelected[] = new boolean [v.length];
    boolean touching[][], inGroup[], beveled[] = new boolean [e.length];
    double coeff[][] = new double [3][3], rhs[] = new double [3];

    newIndex = new Vector();
    findEdgeInsets(height, width);

    // First copy over the old faces and vertices.  (Some of these will be modified later.)

    for (i = 0; i < v.length; i++)
      vert.addElement(v[i]);
    for (i = 0; i < f.length; i++)
      face.addElement(new int [] {f[i].v1, f[i].v2, f[i].v3, i});

    // Each vertex goes into one of three categories: NONE of the faces touching it are
    // selected, ALL of the faces touching it are selected, or (the complicated case) SOME
    // of the faces touching it are selected.

    for (i = 0; i < v.length; i++)
      allSelected[i] = true;
    for (i = 0; i < f.length; i++)
      {
	if (selected[i])
	  someSelected[f[i].v1] = someSelected[f[i].v2] = someSelected[f[i].v3] = true;
	else
	  allSelected[f[i].v1] = allSelected[f[i].v2] = allSelected[f[i].v3] = false;
      }

    // For each vertex, build a list of all the selected faces that touch it.
    
    vertFace = new int [v.length][];
    numVertFaces = new int [v.length];
    for (i = 0; i < f.length; i++)
      if (selected[i])
	{
	  numVertFaces[f[i].v1]++;
	  numVertFaces[f[i].v2]++;
	  numVertFaces[f[i].v3]++;
	}
    for (i = 0; i < v.length; i++)
      {
	vertFace[i] = new int [numVertFaces[i]];
	numVertFaces[i] = 0;
      }
    for (i = 0; i < f.length; i++)
      if (selected[i])
	{
	  vertFace[f[i].v1][numVertFaces[f[i].v1]++] = i;
	  vertFace[f[i].v2][numVertFaces[f[i].v2]++] = i;
	  vertFace[f[i].v3][numVertFaces[f[i].v3]++] = i;
	}

    // If ALL or NONE of the faces touching a vertex are selected, everything is simple.
    // If SOME but not all are selected, things get complicated.  We will need to keep the
    // original vertex, but also create a displaced one.  Furthermore, the selected faces
    // may not all touch each other (that is, share edges).  In this case, we need to sort
    // the selected faces into groups which touch each other, and there will be a different
    // displaced vertex for each group.

    for (i = 0; i < v.length; i++)
      {
	if (allSelected[i])
	  {
	    // Find the new position for this vertex.  It is not on the boundary of the
	    // selection, so we don't need to worry about insets.  Ideally, we would like 
	    // it to be the correct height from every one of the faces, but in general 
	    // this is not possible.  We choose the position to minimize the sum of the
	    // squared height deviations.
	    
	    for (j = 0; j < 3; j++)
	      rhs[j] = coeff[j][0] = coeff[j][1] = coeff[j][2] = 0.0;
	    for (j = 0; j < numVertFaces[i]; j++)
	      {
		coeff[0][0] += faceNormal[vertFace[i][j]].x * faceNormal[vertFace[i][j]].x;
		coeff[0][1] += faceNormal[vertFace[i][j]].x * faceNormal[vertFace[i][j]].y;
		coeff[0][2] += faceNormal[vertFace[i][j]].x * faceNormal[vertFace[i][j]].z;
		coeff[1][1] += faceNormal[vertFace[i][j]].y * faceNormal[vertFace[i][j]].y;
		coeff[1][2] += faceNormal[vertFace[i][j]].y * faceNormal[vertFace[i][j]].z;
		coeff[2][2] += faceNormal[vertFace[i][j]].z * faceNormal[vertFace[i][j]].z;
		rhs[0] += height*faceNormal[vertFace[i][j]].x;
		rhs[1] += height*faceNormal[vertFace[i][j]].y;
		rhs[2] += height*faceNormal[vertFace[i][j]].z;
		newIndex.addElement(new Integer(vertFace[i][j]));
	      }
	    coeff[1][0] = coeff[0][1];
	    coeff[2][0] = coeff[0][2];
	    coeff[2][1] = coeff[1][2];
	    SVD.solve(coeff, rhs, 1e-3);
	    temp.set(rhs[0], rhs[1], rhs[2]);
	    vert.setElementAt(offsetVertex(mesh, (Vertex) vert.elementAt(i), temp), i);
	  }
        else if (someSelected[i])
	  {
	    // Find which faces directly touch each other.
	  
	    touching = new boolean [numVertFaces[i]][numVertFaces[i]];
	    for (j = 1; j < numVertFaces[i]; j++)
	      for (k = 0; k < j; k++)
		{
		  f1 = f[vertFace[i][j]];
		  f2 = f[vertFace[i][k]];
		  if (f1.e1 == f2.e1 || f1.e1 == f2.e2 || f1.e1 == f2.e3 || 
		      f1.e2 == f2.e1 || f1.e2 == f2.e2 || f1.e2 == f2.e3 || 
		      f1.e3 == f2.e1 || f1.e3 == f2.e2 || f1.e3 == f2.e3)
		    touching[j][k] = touching[k][j] = true;
		}
	  
	    // Count the number of other faces each face is touching.
	  
	    touchCount = new int [numVertFaces[i]];
	    for (j = 0; j < numVertFaces[i]; j++)
	      for (k = 0; k < numVertFaces[i]; k++)
		touchCount[j] += touching[j][k] ? 1 : 0;

	    // Find the groups.
	  
	    inGroup = new boolean [numVertFaces[i]];
	    group = new int [numVertFaces[i]];
	    while (true)
	      {
		groupCount = 0;
	      
		// Find the first face in the new group.
	      
		for (j = 0; j < numVertFaces[i] && (inGroup[j] == true || touchCount[j] > 1); j++);
		if (j == numVertFaces[i])
		  break;
		inGroup[j] = true;
		group[0] = j;
		groupCount = 1;
	      
		// Find the rest of the faces in the group.
	      
		while (touchCount[j] > (groupCount == 1 ? 0 : 1))
 		  for (j = 0; j < numVertFaces[i]; j++)
		    if (!inGroup[j] && touching[group[groupCount-1]][j])
		      {
			group[groupCount++] = j;
			inGroup[j] = true;
			break;
		      }

		// We also will need to add two faces to define the beveled edge at the
		// start of the group, and another two at the end of the group.  We can't
		// actually add them yet, since not all vertices have been determined, so
		// instead we record the edges which need this.
	      
		if (groupCount == 1)
		  {
		    // If the group consists of only one face, then simply find the two edges
		    // which share this vertex.
		  
		    f1 = f[vertFace[i][group[0]]];
		    m = n = -1;
		    if (e[f1.e1].v1 == i || e[f1.e1].v2 == i)
		      {
			bevel.addElement(new int [] {vertFace[i][group[0]], f1.e1});
			m = 0;
		      }
		    if (e[f1.e2].v1 == i || e[f1.e2].v2 == i)
		      {
			bevel.addElement(new int [] {vertFace[i][group[0]], f1.e2});
			if (m == -1)
			  m = 1;
			else
			  n = 1;
		      }
		    if (e[f1.e3].v1 == i || e[f1.e3].v2 == i)
		      {
			bevel.addElement(new int [] {vertFace[i][group[0]], f1.e3});
			n = 2;
		      }
		  }
		else
		  {
		    // We must find the edge of the first face which 1) shares this vertex,
		    // and 2) is not shared by the second face.
		  
		    f1 = f[vertFace[i][group[0]]];
		    f2 = f[vertFace[i][group[1]]];
		    if ((e[f1.e1].v1 == i || e[f1.e1].v2 == i) && f2.e1 != f1.e1 && f2.e2 != f1.e1 && f2.e3 != f1.e1)
		      {
			bevel.addElement(new int [] {vertFace[i][group[0]], f1.e1});
			m = 0;
		      }
		    else if ((e[f1.e2].v1 == i || e[f1.e2].v2 == i) && f2.e1 != f1.e2 && f2.e2 != f1.e2 && f2.e3 != f1.e2)
		      {
			bevel.addElement(new int [] {vertFace[i][group[0]], f1.e2});
			m = 1;
		      }
		    else
		      {
			bevel.addElement(new int [] {vertFace[i][group[0]], f1.e3});
			m = 2;
		      }
		  
		    // Similarly for the last face.
		  
		    f1 = f[vertFace[i][group[groupCount-1]]];
		    f2 = f[vertFace[i][group[groupCount-2]]];
		    if ((e[f1.e1].v1 == i || e[f1.e1].v2 == i) && f2.e1 != f1.e1 && f2.e2 != f1.e1 && f2.e3 != f1.e1)
		      {
			bevel.addElement(new int [] {vertFace[i][group[groupCount-1]], f1.e1});
			n = 0;
		      }
		    else if ((e[f1.e2].v1 == i || e[f1.e2].v2 == i) && f2.e1 != f1.e2 && f2.e2 != f1.e2 && f2.e3 != f1.e2)
		      {
			bevel.addElement(new int [] {vertFace[i][group[groupCount-1]], f1.e2});
			n = 1;
		      }
		    else
		      {
			bevel.addElement(new int [] {vertFace[i][group[groupCount-1]], f1.e3});
			n = 2;
		      }
		  }

		// Find the displaced vertex for this group.  Ideally, we would like to
		// satisfy both the inset (width) and extrude (height) constraints for both
		// the first and last triangles in the group, but in general this is not
		// possible.  We settle for getting the insets, and the average of the two
		// heights.
	      
		coeff[0][0] = faceInsets[vertFace[i][group[0]]][m].x;
		coeff[0][1] = faceInsets[vertFace[i][group[0]]][m].y;
		coeff[0][2] = faceInsets[vertFace[i][group[0]]][m].z;
		coeff[1][0] = faceInsets[vertFace[i][group[groupCount-1]]][n].x;
		coeff[1][1] = faceInsets[vertFace[i][group[groupCount-1]]][n].y;
		coeff[1][2] = faceInsets[vertFace[i][group[groupCount-1]]][n].z;
		coeff[2][0] = faceNormal[vertFace[i][group[0]]].x + faceNormal[vertFace[i][group[groupCount-1]]].x;
		coeff[2][1] = faceNormal[vertFace[i][group[0]]].y + faceNormal[vertFace[i][group[groupCount-1]]].y;
		coeff[2][2] = faceNormal[vertFace[i][group[0]]].z + faceNormal[vertFace[i][group[groupCount-1]]].z;
		rhs[0] = rhs[1] = width;
		rhs[2] = 2.0*height;
		SVD.solve(coeff, rhs, 1e-3);
		temp.set(rhs[0], rhs[1], rhs[2]);
		vert.addElement(offsetVertex(mesh, (Vertex) vert.elementAt(i), temp));
	      
		// Modify the faces to use the new vertex.
	      
		k = vert.size()-1;
		for (j = 0; j < groupCount; j++)
		  {
		    tempFace = (int []) face.elementAt(vertFace[i][group[j]]);
		    f1 = f[vertFace[i][group[j]]];
		    if (f1.v1 == i)
		      tempFace[0] = k;
		    else if (f1.v2 == i)
		      tempFace[1] = k;
		    else
		      tempFace[2] = k;
		    newIndex.addElement(new Integer(vertFace[i][group[j]]));
		  }
	      }
	  }
	}

    // Whew!  Done with that.  Now we need to add two faces for each edge which is at the
    // end of a group.
    
    for (i = 0; i < bevel.size(); i++)
      {
	bev = (int []) bevel.elementAt(i);
	if (beveled[bev[1]])
	  continue;
	beveled[bev[1]] = true;
	j = e[bev[1]].v1;
	k = e[bev[1]].v2;
	if ((f[bev[0]].v1 == k && f[bev[0]].v2 == j) || 
	    (f[bev[0]].v2 == k && f[bev[0]].v3 == j) || 
	    (f[bev[0]].v3 == k && f[bev[0]].v1 == j))
	  {
	    m = j;
	    j = k;
	    k = m;
	  }
	tempFace = (int []) face.elementAt(bev[0]);
	if (f[bev[0]].v1 == j)
	  m = tempFace[0];
	else if (f[bev[0]].v2 == j)
	  m = tempFace[1];
	else
	  m = tempFace[2];
	if (f[bev[0]].v1 == k)
	  n = tempFace[0];
	else if (f[bev[0]].v2 == k)
	  n = tempFace[1];
	else
	  n = tempFace[2];
	face.addElement(new int [] {j, k, m, bev[0]});
	face.addElement(new int [] {m, k, n, bev[0]});
      }

    // There!  Nothing left to do but construct the new mesh.

    prepareMesh(mesh, face, vert, null);

    // Copy over smoothness values for edges.

    Edge newe[] = mesh.getEdges();
    for (i = 0; i < e.length; i++)
      for (j = 0; j < newe.length; j++)
	if ((e[i].v1 == newe[j].v1 && e[i].v2 == newe[j].v2) ||
		(e[i].v1 == newe[j].v2 && e[i].v2 == newe[j].v1))
	  {
	    newe[j].smoothness = e[i].smoothness;
	    break;
	  }
    
    // Record which faces should be selected.
    
    newSelection = new boolean [mesh.getFaces().length];
    for (i = 0; i < newIndex.size(); i++)
      newSelection[((Integer) newIndex.elementAt(i)).intValue()] = true;
    return mesh;
  }

  /** Find the list of selected faces, and for each one,  calculate a displacement for each edge. */

  private void findEdgeInsets(double height, double width)
  {
    Vertex v[] = (Vertex []) mesh.getVertices();
    Face f[] = mesh.getFaces();
    double length, dot;
    Vec3 e1, e2, e3;
    int i;
    
    faceInsets = new Vec3 [f.length][];
    faceNormal = new Vec3 [f.length];
    for (i = 0; i < f.length; i++)
      if (selected[i])
	{
	  faceInsets[i] = new Vec3 [3];
	  e1 = v[f[i].v2].r.minus(v[f[i].v1].r);
	  e2 = v[f[i].v3].r.minus(v[f[i].v2].r);
	  e3 = v[f[i].v1].r.minus(v[f[i].v3].r);
	  e1.normalize();
	  e2.normalize();
	  e3.normalize();
	  faceNormal[i] = e1.cross(e2);
	  length = faceNormal[i].length();
	  if (length == 0.0) // A degenerate triangle.
	    faceInsets[i][0] = faceInsets[i][1] = faceInsets[i][2] = faceNormal[i] = new Vec3();
	  else
	    {
	      faceNormal[i].scale(1.0/length);
	      dot = -e1.dot(e2);
	      faceInsets[i][0] = e2.plus(e1.times(dot));
	      faceInsets[i][0].normalize();
	      dot = -e2.dot(e3);
	      faceInsets[i][1] = e3.plus(e2.times(dot));
	      faceInsets[i][1].normalize();
	      dot = -e3.dot(e1);
	      faceInsets[i][2] = e1.plus(e3.times(dot));
	      faceInsets[i][2].normalize();
	    }
	}
  }
  
  /** Given one vertex, create another one which is offset from it. */
  
  private Vertex offsetVertex(TriangleMesh mesh, Vertex v, Vec3 offset)
  {
    Vertex vert = mesh.new Vertex(v);
    
    vert.r.add(offset);
    vert.edges = 0;
    vert.firstEdge = -1;
    return vert;
  }
  
  /**
   * Construct a new mesh, beveling each selected vertex.
   *
   * @param height      the extrude height
   * @param width       the bevel width
   */

  private TriangleMesh bevelVertices(double height, double width)
  {
    mesh = (TriangleMesh) origMesh.duplicate();
    if (width == 0.0)
    {
      newSelection = selected;
      return mesh;
    }
    Vertex v[] = (Vertex []) mesh.getVertices();
    Edge e[] = mesh.getEdges();
    Face f[] = mesh.getFaces();
    Vec3 norm[] = mesh.getNormals();
    int vertIndex[] = new int [v.length];
    int extraVertIndex[][] = new int [v.length][], vertEdgeIndex[][] = new int [v.length][];
    boolean forward[] = new boolean [v.length];
    Vector face = new Vector(), vert = new Vector();

    // Create the vertices of the new mesh.
    
    for (int i = 0; i < v.length; i++)
    {
      if (!selected[i])
      {
        // Just copy over this vertex.
        
        vertIndex[i] = vert.size();
        vert.addElement(v[i]);
        continue;
      }
      
      // Look at the edges intersecting this vertex, and determine how far along them to bevel.
      
      int edges[] = v[i].getEdges();
      vertEdgeIndex[i] = edges;
      Vec3 edgeDir[] = new Vec3 [edges.length];
      double dot[] = new double [edges.length];
      double minDot = 1.0;
      for (int j = 0; j < edges.length; j++)
      {
        edgeDir[j] = v[e[edges[j]].v2].r.minus(v[e[edges[j]].v1].r);
        edgeDir[j].normalize();
        if (e[edges[j]].v2 == i)
          edgeDir[j].scale(-1.0);
        dot[j] = Math.abs(norm[i].dot(edgeDir[j]));
        if (dot[j] < minDot)
          minDot = dot[j];
      }
      double bevelDist = width/Math.tan(Math.acos(minDot));
      
      // Position the beveled vertices.
      
      extraVertIndex[i] = new int [edges.length];
      for (int j = 0; j < edges.length; j++)
      {
        extraVertIndex[i][j] = vert.size();
        double dist = (dot[j] == 0.0 ? width : bevelDist/dot[j]);
        vert.addElement(offsetVertex(mesh, v[i], edgeDir[j].times(dist)));
      }
      
      // Position the central vertex.
      
      boolean convex = (norm[i].dot(edgeDir[0]) < 0.0);
      vertIndex[i] = vert.size();
      vert.addElement(offsetVertex(mesh, v[i], norm[i].times(convex ? height-bevelDist : height+bevelDist)));
      
      // Determine which way the vertices are ordered.
      
      Edge e0 = e[edges[0]];
      Edge e1 = e[edges[1]];
      Face fc = f[e0.f1];
      int v0 = (e0.v1 == i ? e0.v2 : e0.v1);
      int v1 = (e1.v1 == i ? e1.v2 : e1.v1);
      forward[i] = ((fc.v1 == v0 && fc.v3 == v1) || (fc.v2 == v0 && fc.v1 == v1) || (fc.v3 == v0 && fc.v2 == v1));
    }
    
    // We now have all the vertices.  Next, create the faces.  We begin with the ones
    // corresponding to the faces of the original mesh.
    
    for (int i = 0; i < f.length; i++)
    {
      Face fc = f[i];
      
      // After beveling, the three vertices of the original face may have turned into as many
      // as six vertices.  Find them, in order.
      
      int origVert[] = new int [] {fc.v1, fc.v2, fc.v2, fc.v3, fc.v3, fc.v1};
      int origEdge[] = new int [] {fc.e1, fc.e1, fc.e2, fc.e2, fc.e3, fc.e3};
      int newVert[] = new int [6];
      int numVert = 0;
      for (int j = 0; j < origVert.length; j++)
      {
        int index = vertIndex[origVert[j]];
        if (selected[origVert[j]])
        {
          int orig = origVert[j];
          for (int k = 0; k < vertEdgeIndex[orig].length; k++)
            if (vertEdgeIndex[orig][k] == origEdge[j])
            {
              index = extraVertIndex[orig][k];
              break;
            }
        }
        if (numVert == 0 || (index != newVert[numVert-1] && index != newVert[0]))
          newVert[numVert++] = index;
      }
      
      // We now need to triangulate the vertices and create new faces.
      
      if (numVert == 3)
        face.addElement(new int [] {newVert[0], newVert[1], newVert[2], i});
      else if (numVert == 4)
      {
        face.addElement(new int [] {newVert[0], newVert[1], newVert[2], i});
        face.addElement(new int [] {newVert[0], newVert[2], newVert[3], i});
      }
      else
        for (int step = 1; 2*step < numVert; step *= 2)
          {
            int start;
            for (start = 0; start+2*step < numVert; start += 2*step)
              face.addElement(new int [] {newVert[start], newVert[start+step], newVert[start+2*step], i});
            if (start+step < numVert)
              face.addElement(new int [] {newVert[start], newVert[start+step], newVert[0], i});
          }
    }
    
    // Next create the faces capping the vertices that have just been beveled.
    
    for (int i = 0; i < v.length; i++)
    {
      if (!selected[i])
        continue;
      for (int j = 0; j < extraVertIndex[i].length; j++)
      {
        int prev = (j == 0 ? vertEdgeIndex[i].length-1 : j-1);
        if (forward[i])
          face.addElement(new int [] {extraVertIndex[i][j], extraVertIndex[i][prev], vertIndex[i], -1});
        else
          face.addElement(new int [] {extraVertIndex[i][prev], extraVertIndex[i][j], vertIndex[i], -1});
      }
    }
    
    // Create the mesh.
    
    prepareMesh(mesh, face, vert, vertIndex);

    // Copy over smoothness values for edges.

    Edge newe[] = mesh.getEdges();
    for (int i = 0; i < e.length; i++)
    {
      int v1 = vertIndex[e[i].v1];
      int v2 = vertIndex[e[i].v2];
      for (int j = 0; j < newe.length; j++)
	if ((v1 == newe[j].v1 && v2 == newe[j].v2) ||
		(v1 == newe[j].v2 && v2 == newe[j].v1))
	  {
	    newe[j].smoothness = e[i].smoothness;
	    break;
	  }
    }
    
    // Record which vertices should be selected.
    
    newSelection = new boolean [mesh.getVertices().length];
    for (int i = 0; i < vertIndex.length; i++)
      if (selected[i])
        newSelection[vertIndex[i]] = true;
    return mesh;
  }
  
  /**
   * Construct a new mesh, beveling each selected edges.
   *
   * @param height      the extrude height
   * @param width       the bevel width
   */

  private TriangleMesh bevelEdges(double height, double width)
  {
    mesh = (TriangleMesh) origMesh.duplicate();
    if (width == 0.0)
    {
      newSelection = selected;
      return mesh;
    }
    Vertex v[] = (Vertex []) mesh.getVertices();
    Edge e[] = mesh.getEdges();
    Face f[] = mesh.getFaces();
    Vec3 norm[] = mesh.getNormals();
    int vertIndex[] = new int [v.length];
    int extraVertIndex[][] = new int [v.length][];
    int faceVertIndex[][] = new int [v.length][];
    boolean forward[] = new boolean [v.length];
    Vector face = new Vector(), vert = new Vector();

    // Find the bevel and extrude directions for every edge.
    
    Vec3 edgeDir[] = new Vec3 [e.length];
    Vec3 bevelDir[] = new Vec3 [e.length];
    Vec3 extrudeDir[] = new Vec3 [e.length];
    for (int i = 0; i < e.length; i++)
    {
      edgeDir[i] = v[e[i].v2].r.minus(v[e[i].v1].r);
      if (selected[i])
      {
        Vec3 avgNorm = norm[e[i].v1].plus(norm[e[i].v2]);
        bevelDir[i] = edgeDir[i].cross(avgNorm);
        bevelDir[i].normalize();
        extrudeDir[i] = bevelDir[i].cross(edgeDir[i]);
        extrudeDir[i].normalize();
      }
    }
    
    // Count the selected edges touching each vertex.

    int vertEdgeIndex[][] = new int [v.length][];
    int vertEdgeCount[] = new int [v.length];
    for (int i = 0; i < v.length; i++)
    {
      vertEdgeIndex[i] = v[i].getEdges();
      for (int j = 0; j < vertEdgeIndex[i].length; j++)
        if (selected[vertEdgeIndex[i][j]])
          vertEdgeCount[i]++;
    }
    
    // Record the faces touching each vertex.
    
    int vertFaceIndex[][] = new int [v.length][];
    for (int i = 0; i < v.length; i++)
    {
      vertFaceIndex[i] = new int [vertEdgeIndex[i].length];
      int e0 = vertEdgeIndex[i][0];
      int e1 = vertEdgeIndex[i][1];
      int prev = e[e0].f1;
      if (f[prev].e1 == e1 || f[prev].e2 == e1 || f[prev].e3 == e1)
        prev = e[e0].f2;
      for (int j = 0; j < vertFaceIndex[i].length; j++)
      {
        Edge ed = e[vertEdgeIndex[i][j]];
        vertFaceIndex[i][j] = (ed.f1 == prev ? ed.f2 : ed.f1);
        prev = vertFaceIndex[i][j];
      }
    }
    
    // Create the vertices of the new mesh.
    
    for (int i = 0; i < v.length; i++)
    {
      if (vertEdgeCount[i] == 0)
      {
        // Just copy over this vertex.
        
        vertIndex[i] = vert.size();
        vert.addElement(v[i]);
        continue;
      }
      
      // Look at the edges intersecting this vertex, and determine how far along them to bevel.
      
      int edges[] = vertEdgeIndex[i];
      double offsetDist[] = new double [edges.length];
      for (int j = 0; j < edges.length; j++)
        if (selected[edges[j]])
        {
          // Calculate the offsets based on beveling this particular edge.
          
          Vec3 offsetDir = extrudeDir[edges[j]];
          double dot[] = new double [edges.length];
          double minDot = 1.0;
          for (int k = 0; k < edges.length; k++)
            if (!selected[edges[k]])
            {
              Vec3 dir = edgeDir[vertEdgeIndex[i][k]];
              dot[k] = Math.abs(offsetDir.dot(dir));
              if (dot[k] < minDot)
                minDot = dot[k];
            }
          double bevelDist = width/Math.tan(Math.acos(minDot));
          for (int k = 0; k < edges.length; k++)
            if (!selected[edges[k]])
            {
              double dist = (dot[k] == 0.0 ? width : bevelDist/dot[k]);
              if (dist > offsetDist[k])
                offsetDist[k] = dist;
            }
        }
      
      // Position the beveled vertices.
      
      extraVertIndex[i] = new int [edges.length];
      for (int j = 0; j < edges.length; j++)
      {
        if (selected[edges[j]])
        {
          extraVertIndex[i][j] = -1;
          continue;
        }
        extraVertIndex[i][j] = vert.size();
        double dist = offsetDist[j];
        if (e[edges[j]].v2 == i)
          dist = -dist;
        vert.addElement(offsetVertex(mesh, v[i], edgeDir[edges[j]].times(dist)));
      }
      
      // If two adjacent edges were both beveled, we need to create a new vertex between them.
      
      faceVertIndex[i] = new int [edges.length];
      for (int j = 0; j < edges.length; j++)
      {
        int next = j+1;
        if (next == edges.length)
          next = (e[edges[0]].f2 == -1 ? -1 : 0);
        if (next == -1 || (!selected[edges[j]] && !selected[edges[next]]))
          continue;
        if (!selected[edges[j]])
        {
          faceVertIndex[i][j] = extraVertIndex[i][j];
          continue;
        }
        if (!selected[edges[next]])
        {
          faceVertIndex[i][j] = extraVertIndex[i][next];
          continue;
        }
        faceVertIndex[i][j] = vert.size();
        
        // Find the vertex position.
        
        double dist1 = offsetDist[j];
        if (e[edges[j]].v2 == i)
          dist1 = -dist1;
        double dist2 = offsetDist[next];
        if (e[edges[next]].v2 == i)
          dist2 = -dist2;
        double edgeDot = edgeDir[edges[j]].dot(edgeDir[edges[next]]);
        double m[][] = new double [][] {{1.0, edgeDot}, {edgeDot, 1.0}};
        double b[] = new double [] {dist1, dist2};
        SVD.solve(m, b);
        Vec3 offset = edgeDir[edges[j]].times(b[0]).plus(edgeDir[edges[next]].times(b[1]));
        vert.addElement(offsetVertex(mesh, v[i], offset));
      }
      
      // Determine which way the vertices are ordered.
      
      Edge e0 = e[edges[0]];
      Edge e1 = e[edges[1]];
      Face fc = f[e0.f1];
      int v0 = (e0.v1 == i ? e0.v2 : e0.v1);
      int v1 = (e1.v1 == i ? e1.v2 : e1.v1);
      forward[i] = ((fc.v1 == v0 && fc.v3 == v1) || (fc.v2 == v0 && fc.v1 == v1) || (fc.v3 == v0 && fc.v2 == v1));
    }
    
    // For each end of each beveled edge, calculate an "ideal position" which is midway between the
    // vertices on either side of it.
    
    Vec3 idealEndPos[][] = new Vec3 [e.length][];
    for (int i = 0; i < e.length; i++)
    {
      if (!selected[i])
        continue;
      
      // Find the vertices surrounding this edge.
      
      int faceList[] = new int [e[i].f2 == -1 ? 1 : 2];
      faceList[0] = e[i].f1;
      if (faceList.length > 1)
        faceList[1] = e[i].f2;
      int vi[] = new int [4];
      for (int j = 0; j < faceList.length; j++)
      {
        int v1 = -1, v2 = -1;
        for (int k = 0; k < vertFaceIndex[e[i].v1].length && v1 == -1; k++)
          if (vertFaceIndex[e[i].v1][k] == faceList[j])
            v1 = faceVertIndex[e[i].v1][k];
        for (int k = 0; k < vertFaceIndex[e[i].v2].length && v2 == -1; k++)
          if (vertFaceIndex[e[i].v2][k] == faceList[j])
            v2 = faceVertIndex[e[i].v2][k];
        vi[j*2] = v1;
        vi[j*2+1] = v2;
      }
      
      // Calculate the ideal end positions.
      
      idealEndPos[i] = new Vec3 [2];
      if (faceList.length == 1)
      {
        Vec3 delta;
        double d;
        delta = ((Vertex) vert.elementAt(vi[0])).r.minus(v[e[i].v1].r);
        d = delta.dot(extrudeDir[i]);
        idealEndPos[i][0] = v[e[i].v1].r.plus(extrudeDir[i].times(d));
        delta = ((Vertex) vert.elementAt(vi[1])).r.minus(v[e[i].v2].r);
        d = delta.dot(extrudeDir[i]);
        idealEndPos[i][1] = v[e[i].v2].r.plus(extrudeDir[i].times(d));
      }
      else
      {
        idealEndPos[i][0] = ((Vertex) vert.elementAt(vi[0])).r.plus(((Vertex) vert.elementAt(vi[2])).r).times(0.5);
        idealEndPos[i][1] = ((Vertex) vert.elementAt(vi[1])).r.plus(((Vertex) vert.elementAt(vi[3])).r).times(0.5);
      }
    }
    
    // If a vertex is touched by multiple beveled vertices, there may be several "ideal positions" for
    // it.  Find a single position which is a best compromise between them.
    
    for (int i = 0; i < v.length; i++)
    {
      if (vertEdgeCount[i] == 0)
        continue;
      Vec3 ideal[] = new Vec3 [vertEdgeCount[i]];
      int index[] = new int [vertEdgeCount[i]];
      int num = 0;
      int edges[] = vertEdgeIndex[i];
      
      // Find all the ideal positions (as offsets from the current vertex position) and edge indices.
      
      for (int j = 0; j < edges.length; j++)
      {
        int ej = edges[j];
        if (selected[ej])
        {
          ideal[num] = (e[ej].v1 == i ? idealEndPos[ej][0] : idealEndPos[ej][1]);
          ideal[num].subtract(v[i].r);
          ideal[num].add(extrudeDir[ej].times(height));
          index[num] = ej;
          num++;
        }
      }
      
      // Combine them to find a single position.
      
      vertIndex[i] = vert.size();
      if (ideal.length == 1)
      {
        vert.addElement(offsetVertex(mesh, v[i], ideal[0]));
        continue;
      }
      double m[][] = new double [2*ideal.length][];
      double b[] = new double [2*ideal.length];
      for (int j = 0; j < ideal.length; j++)
      {
        Vec3 dir;
        dir = extrudeDir[index[j]];
        m[2*j] = new double [] {dir.x, dir.y, dir.z};
        b[2*j] = dir.dot(ideal[j]);
        dir = bevelDir[index[j]];
        m[2*j+1] = new double [] {dir.x, dir.y, dir.z};
        b[2*j+1] = dir.dot(ideal[j]);
      }
      SVD.solve(m, b);
      vert.addElement(offsetVertex(mesh, v[i], new Vec3(b[0], b[1], b[2])));
    }
    
    // We now have all the vertices.  Next, create the faces.  We begin with the ones
    // corresponding to the faces of the original mesh.
    
    for (int i = 0; i < f.length; i++)
    {
      Face fc = f[i];
      
      // After beveling, the three vertices of the original face may have turned into as many
      // as six vertices.  Find them, in order.
      
      int origVert[] = new int [] {fc.v1, fc.v2, fc.v2, fc.v3, fc.v3, fc.v1};
      int origEdge[] = new int [] {fc.e1, fc.e1, fc.e2, fc.e2, fc.e3, fc.e3};
      int newVert[] = new int [6];
      int numVert = 0;
      for (int j = 0; j < origEdge.length; j++)
      {
        int index = -1;
        int orig = origVert[j];
        if (extraVertIndex[orig] != null)
        {
          // A beveled edge touches the vertex.
          
          for (int k = 0; k < vertEdgeIndex[orig].length; k++)
            if (vertEdgeIndex[orig][k] == origEdge[j])
            {
              if (extraVertIndex[orig][k] == -1)
              {
                // Two adjacent edges were beveled.
                
                for (int m = 0; m < vertFaceIndex[orig].length; m++)
                  if (vertFaceIndex[orig][m] == i)
                  {
                    index = faceVertIndex[orig][m];
                    break;
                  }
              }
              else
                index = extraVertIndex[orig][k];
              break;
            }
        }
        else
          index = vertIndex[origVert[j]];
        if (index > -1 && (numVert == 0 || (index != newVert[numVert-1] && index != newVert[0])))
          newVert[numVert++] = index;
      }
      
      // We now need to triangulate the vertices and create new faces.
      
      if (numVert == 3)
        face.addElement(new int [] {newVert[0], newVert[1], newVert[2], i});
      else if (numVert == 4)
      {
        face.addElement(new int [] {newVert[0], newVert[1], newVert[2], i});
        face.addElement(new int [] {newVert[0], newVert[2], newVert[3], i});
      }
      else
        for (int step = 1; 2*step < numVert; step *= 2)
          {
            int start;
            for (start = 0; start+2*step < numVert; start += 2*step)
              face.addElement(new int [] {newVert[start], newVert[start+step], newVert[start+2*step], i});
            if (start+step < numVert)
              face.addElement(new int [] {newVert[start], newVert[start+step], newVert[0], i});
          }
    }
    
    // Next create the faces capping the vertices at the ends of the beveled edges.
    
    for (int i = 0; i < v.length; i++)
    {
      if (extraVertIndex[i] == null)
        continue;
      for (int j = 0; j < extraVertIndex[i].length; j++)
      {
        int prev = (j == 0 ? vertEdgeIndex[i].length-1 : j-1);
        int v1 = extraVertIndex[i][j];
        int v2 = extraVertIndex[i][prev];
        if (v1 == -1 || v2 == -1)
          continue;
        if (forward[i])
          face.addElement(new int [] {v1, v2, vertIndex[i], -1});
        else
          face.addElement(new int [] {v2, v1, vertIndex[i], -1});
      }
    }
    
    // Finally, create the faces capping the beveled edges themselves.
    
    for (int i = 0; i < e.length; i++)
    {
      if (!selected[i])
        continue;
      int faceList[] = new int [e[i].f2 == -1 ? 1 : 2];
      faceList[0] = e[i].f1;
      if (faceList.length > 1)
        faceList[1] = e[i].f2;
      for (int j = 0; j < faceList.length; j++)
      {
        int v0, v1, v2, v3;
        v0 = vertIndex[e[i].v1];
        v3 = vertIndex[e[i].v2];
        v1 = v2 = -1;
        for (int k = 0; k < vertFaceIndex[e[i].v1].length && v1 == -1; k++)
          if (vertFaceIndex[e[i].v1][k] == faceList[j])
            v1 = faceVertIndex[e[i].v1][k];
        for (int k = 0; k < vertFaceIndex[e[i].v2].length && v2 == -1; k++)
          if (vertFaceIndex[e[i].v2][k] == faceList[j])
            v2 = faceVertIndex[e[i].v2][k];
        Face fc = f[faceList[j]];
        if ((fc.v1 == e[i].v1 && fc.v3 == e[i].v2) || (fc.v2 == e[i].v1 && fc.v1 == e[i].v2) || (fc.v3 == e[i].v1 && fc.v2 == e[i].v2))
        {
          face.addElement(new int [] {v0, v1, v2, -1});
          face.addElement(new int [] {v2, v3, v0, -1});
        }
        else
        {
          face.addElement(new int [] {v1, v0, v2, -1});
          face.addElement(new int [] {v3, v2, v0, -1});
        }
      }
    }
    
    // Create the mesh.
    
    prepareMesh(mesh, face, vert, vertIndex);

    // Copy over smoothness values for edges.

    Edge newe[] = mesh.getEdges();
    for (int i = 0; i < e.length; i++)
    {
      int v1 = vertIndex[e[i].v1];
      int v2 = vertIndex[e[i].v2];
      for (int j = 0; j < newe.length; j++)
	if ((v1 == newe[j].v1 && v2 == newe[j].v2) ||
		(v1 == newe[j].v2 && v2 == newe[j].v1))
	  {
	    newe[j].smoothness = e[i].smoothness;
	    break;
	  }
    }
    
    // Record which edges should be selected.
    
    newSelection = new boolean [newe.length];
    for (int i = 0; i < e.length; i++)
      if (selected[i])
      {
        int v1 = vertIndex[e[i].v1];
        int v2 = vertIndex[e[i].v2];
        for (int j = 0; j < newe.length; j++)
          if ((v1 == newe[j].v1 && v2 == newe[j].v2) || (v1 == newe[j].v2 && v2 == newe[j].v1))
          {
            newSelection[j] = true;
            break;
          }
      }
    return mesh;
  }

  /** Get a list of which faces, edges, or vertices (depending on the bevel mode) should
      be selected after beveling. */
  
  public boolean [] getNewSelection()
  {
    return newSelection;
  }
  
  /** Setup the final mesh. */
  
  private void prepareMesh(TriangleMesh mesh, Vector face, Vector vert, int vertIndex[])
  {
    int newface[][] = new int [face.size()][];
    Vertex newvert[] = new Vertex [vert.size()];
    for (int i = 0; i < face.size(); i++)
    {
      int f[] = (int []) face.elementAt(i);
      newface[i] = new int [] {f[0], f[1], f[2]};
    }
    for (int i = 0; i < vert.size(); i++)
      newvert[i] = (Vertex) vert.elementAt(i);
    mesh.setShape(newvert, newface);
    ParameterValue oldParam[] = mesh.getParameterValues();
    ParameterValue newParam[] = new ParameterValue [oldParam.length];
    for (int i = 0; i < oldParam.length; i++)
    {
      if (oldParam[i] instanceof VertexParameterValue)
      {
        double oldval[] = ((VertexParameterValue) oldParam[i]).getValue();
        double newval[] = new double [vert.size()];
        double defaultVal = mesh.getParameters()[i].defaultVal;
        for (int j = 0; j < newval.length; j++)
          newval[j] = defaultVal;
        if (vertIndex != null)
          for (int j = 0; j < vertIndex.length; j++)
            newval[vertIndex[j]] = oldval[j];
        else
          for (int j = 0; j < oldval.length; j++)
            newval[j] = oldval[j];
        newParam[i] = new VertexParameterValue(newval);
      }
      else if (oldParam[i] instanceof FaceParameterValue)
      {
        double oldval[] = ((FaceParameterValue) oldParam[i]).getValue();
        double newval[] = new double [face.size()];
        double defaultVal = mesh.getParameters()[i].defaultVal;
        for (int j = 0; j < newval.length; j++)
        {
          int f[] = (int []) face.elementAt(j);
          if (f[3] == -1)
            newval[j] = defaultVal;
          else
            newval[j] = oldval[f[3]];
        }
        newParam[i] = new FaceParameterValue(newval);
      }
      else if (oldParam[i] instanceof FaceVertexParameterValue)
      {
        FaceVertexParameterValue fvpv = (FaceVertexParameterValue) oldParam[i];
        double newval[][] = new double [face.size()][3];
        double defaultVal = mesh.getParameters()[i].defaultVal;
        for (int j = 0; j < face.size(); j++)
        {
          int f[] = (int []) face.elementAt(j);
          if (f[3] == -1)
          {
            newval[j][0] = defaultVal;
            newval[j][1] = defaultVal;
            newval[j][2] = defaultVal;
          }
          else
          {
            newval[j][0] = fvpv.getValue(f[3], 0);
            newval[j][1] = fvpv.getValue(f[3], 1);
            newval[j][2] = fvpv.getValue(f[3], 2);
          }
        }
        newParam[i] = new FaceVertexParameterValue(newval);
      }
      else
        newParam[i] = oldParam[i];
    }
    mesh.setParameterValues(newParam);
  }
}