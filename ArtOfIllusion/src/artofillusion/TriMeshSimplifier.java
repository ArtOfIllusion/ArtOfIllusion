/* Copyright (C) 1999-2012 by Peter Eastman

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
import artofillusion.texture.*;
import buoy.widget.*;

/** TriMeshSimplifier defines methods for simplifying a TriangleMesh.  The algorithm
   is based on
   <p>
   R. Ronfard and J. Rossignac, "Full-range Approximation of Triangulated Polyhedra."
   Computer Graphics Forum, vol. 15, no. 3, 1996.
   <p>
   I have made the following modifications to their algorithm:
   <p>
   1. When calculating the cost for an edge collapse, they arbitrarily choose which direction
   to collapse it in.  This results in meshes not being simplified as far as they could be.
   Instead, I always test both collapse directions to see which one has the lower cost.
   <p>
   2. After collapsing vertex v1 to v2, they state that it is only necessary to recalculate
   costs for edges which have been modified, that is, those edges in the star of v1 or v2.
   This is incorrect.  It is also necessary to recalculate the cost for any edge which shares
   a vertex with an edge in v1's star.
   <p>
   3. They define the local tesselation error for a face as being proportional to its angle
   of rotation.  I replace the angle by 1-cos(angle).  This has the same effect, and saves
   the need to evalate an acos.
   <p>
   4. Rather than always simplifying the entire mesh, I allow the user to select only a
   subset of the mesh to be simplified.
   <p>
   5. Their algorithm only works for closed meshes (meshes without a boundary).  I have
   modified it to also work for open meshes.
   <p>
   6. I omit the relaxation step, since this would produce bad results for smoothed
   meshes. */

public class TriMeshSimplifier implements Runnable
{
  private class Constraint
  {
    double a, b, c, d;
  }

  private class Zone
  {
    int constraints;
    Constraint con[];
    Zone next;
  }
    
  private class MeshEdge
  {
    int v1, v2;
    MeshFace f1, f2;
    float smoothness;
    double cost;
    boolean selected;
  }

  private class MeshFace
  {
    int v1, v2, v3, index, origIndex;
    MeshEdge e1, e2, e3;
    Vec3 normal;
  }

  private class VertexInfo
  {
    Vertex theVert;
    Vec3 pos;
    int faces, edges;
    float smoothness;
    MeshEdge star[];
    MeshFace crown[];
    Zone zone;
  }

  private TriangleMesh mesh;
  private VertexInfo vertex[];
  private MeshEdge edge[];
  private MeshFace face[];
  private Vec3 temp1, temp2, temp3;
  private int faces;
  private BDialog dial;
  private BButton cancelButton;
  private BLabel status, numLabel;
  private boolean cancel;
  private double tol;

  public TriMeshSimplifier(TriangleMesh theMesh, boolean selection[], double tolerance, BFrame fr)
  {
    mesh = theMesh;
    tol = tolerance*tolerance;
    buildDataStructures(selection);
    if (fr == null)
      run();
    else
    {
      createDialog(fr);
      new Thread(this).start();
      Thread update = new Thread() {
        public void run()
        {
          try
          {
            while (!cancel)
            {
              sleep(200);
              status.setText(String.valueOf(faces));
            }
          }
          catch (InterruptedException ex)
          {
          }
        }
      };
      update.start();
      dial.setVisible(true);
    }
  }
  
  public void run()
  {
    doSimplification();
    if (!cancel)
      buildMesh();
    cancel = true;
    if (numLabel != null)
    {
      numLabel.setText(Translate.text("Final")+":");
      status.setText(String.valueOf(faces));
      cancelButton.setText(Translate.text("Done"));
      dial.setDefaultButton(cancelButton);
    }
  }

  /* Put up a status dialog. */
  
  private void createDialog(BFrame fr)
  {
    dial = new BDialog(fr, true);
    BorderContainer content = new BorderContainer();
    dial.setContent(BOutline.createEmptyBorder(content, UIUtilities.getStandardDialogInsets()));
    content.add(Translate.label("numTrianglesInMesh"), BorderContainer.NORTH);
    GridContainer grid = new GridContainer(2, 2);
    grid.add(new BLabel(Translate.text("Original")+":"), 0, 0);
    grid.add(numLabel = new BLabel(Translate.text("Current")+":"), 0, 1);
    grid.add(new BLabel(String.valueOf(faces)), 1, 0);
    grid.add(status = new BLabel(String.valueOf(faces)), 1, 1);
    content.add(grid, BorderContainer.CENTER);
    content.add(cancelButton = Translate.button("cancel", new Object() {
      void processEvent()
      {
        cancel = true;
        dial.dispose();
      }
    }, "processEvent"), BorderContainer.SOUTH, new LayoutInfo());
    dial.pack();
    dial.setResizable(false);
    UIUtilities.centerDialog(dial, fr);
  }

  /* Build the new TriangleMesh. */
  
  private void buildMesh()
  {
    int index[] = new int [vertex.length], f[][] = new int [faces][];
    Vertex vert[];
    Edge ed[];

    // Find the indices of all unique vertices.

    int k = 0;
    for (int i = 0; i < vertex.length; i++)
    {
      index[i] = k++;
      for (int j = 0; j < i; j++)
        if (vertex[i] == vertex[j])
        {
          index[i] = index[j];
          k--;
          break;
        }
    }

    // Update the mesh shape.
    
    vert = new Vertex [k];
    for (int i = 0; i < vertex.length; i++)
    {
      vert[index[i]] = mesh.new Vertex(vertex[i].theVert);
      vert[index[i]].r = new Vec3(vertex[i].pos);
      vert[index[i]].smoothness = vertex[i].smoothness;
      vert[index[i]].edges = 0;
      vert[index[i]].firstEdge = -1;
    }
    for (int i = 0; i < faces; i++)
      f[i] = new int [] {index[face[i].v1], index[face[i].v2], index[face[i].v3]};
    mesh.setShape(vert, f);
    
    // Set the smoothness values for edges.
    
    ed = mesh.getEdges();
    for (int i = 0; i < ed.length; i++)
      for (int j = 0; j < edge.length; j++)
        if (edge[j] != null && (index[edge[j].v1] == ed[i].v1 && index[edge[j].v2] == ed[i].v2 ||
            index[edge[j].v2] == ed[i].v1 && index[edge[j].v1] == ed[i].v2))
        {
          ed[i].smoothness = edge[j].smoothness;
          break;
        }

    // Update surface parameters.

    ParameterValue paramValue[] = mesh.getParameterValues();
    if (paramValue != null)
    {
      for (int i = 0; i < paramValue.length; i++)
      {
        if (paramValue[i] instanceof VertexParameterValue)
        {
          VertexParameterValue value = (VertexParameterValue) paramValue[i];
          double oldValue[] = value.getValue();
          double newValue[] = new double [vertex.length];
          for (int j = 0; j < newValue.length; j++)
            newValue[index[j]] = oldValue[j];
          value.setValue(newValue);
        }
        else if (paramValue[i] instanceof FaceParameterValue)
        {
          FaceParameterValue value = (FaceParameterValue) paramValue[i];
          double oldValue[] = value.getValue();
          double newValue[] = new double [faces];
          for (int j = 0; j < newValue.length; j++)
            newValue[j] = oldValue[face[j].origIndex];
          value.setValue(newValue);
        }
        else if (paramValue[i] instanceof FaceVertexParameterValue)
        {
          FaceVertexParameterValue value = (FaceVertexParameterValue) paramValue[i];
          double newValue[][] = new double [faces][3];
          for (int j = 0; j < newValue.length; j++)
          {
            newValue[j][0] = value.getValue(face[j].origIndex, 0);
            newValue[j][1] = value.getValue(face[j].origIndex, 1);
            newValue[j][2] = value.getValue(face[j].origIndex, 2);
          }
          value.setValue(newValue);
        }
      }
      mesh.setParameterValues(paramValue);
    }
  }

  /* Build the data structures necessary for simplifying the mesh. */

  private void buildDataStructures(boolean selection[])
  {
    TriangleMesh.Vertex v[] = (Vertex []) mesh.getVertices();
    TriangleMesh.Edge e[] = mesh.getEdges();
    TriangleMesh.Face f[] = mesh.getFaces();
    int i, j, edgeCount[], faceCount[];
    MeshEdge tempEdge;
    VertexInfo v1, v2;
    Constraint con;

    vertex = new VertexInfo [v.length];
    edge = new MeshEdge [e.length];
    face = new MeshFace [f.length];
    edgeCount = new int [v.length];
    faceCount = new int [v.length];
    faces = f.length;

    // Create temporary vectors for later use.

    temp1 = new Vec3();
    temp2 = new Vec3();
    temp3 = new Vec3();

    // Count the edges and faces touching each vertex, and build the list of vertices.

    for (i = 0; i < e.length; i++)
    {
      edgeCount[e[i].v1]++;
      edgeCount[e[i].v2]++;
    }
    for (i = 0; i < f.length; i++)
    {
      faceCount[f[i].v1]++;
      faceCount[f[i].v2]++;
      faceCount[f[i].v3]++;
    }
    for (i = 0; i < v.length; i++)
    {
      vertex[i] = new VertexInfo();
      vertex[i].theVert = v[i];
      vertex[i].pos = v[i].r;
      vertex[i].smoothness = v[i].smoothness;
      vertex[i].star = new MeshEdge [edgeCount[i]];
      vertex[i].crown = new MeshFace [faceCount[i]];
      vertex[i].zone = new Zone();
      if (faceCount[i] == edgeCount[i])
        vertex[i].zone.con = new Constraint [faceCount[i]];
      else
        vertex[i].zone.con = new Constraint [faceCount[i]+2];
    }
    
    // Build the list of edges.
    
    for (i = 0; i < e.length; i++)
    {
      edge[i] = new MeshEdge();
      edge[i].v1 = e[i].v1;
      edge[i].v2 = e[i].v2;
      edge[i].smoothness = e[i].smoothness;
      v1 = vertex[e[i].v1];
      v2 = vertex[e[i].v2];
      v1.star[v1.edges++] = edge[i];
      v2.star[v2.edges++] = edge[i];
      edge[i].selected = selection[i];
    }
    
    // Build the list of faces, and record the constraint associated with each one.
    
    for (i = 0; i < f.length; i++)
    {
      face[i] = new MeshFace();
      face[i].v1 = f[i].v1;
      face[i].v2 = f[i].v2;
      face[i].v3 = f[i].v3;
      face[i].e1 = edge[f[i].e1];
      face[i].e2 = edge[f[i].e2];
      face[i].e3 = edge[f[i].e3];
      face[i].index = i;
      face[i].origIndex = i;
      face[i].normal = new Vec3();
      findNormal(vertex[f[i].v1].pos, vertex[f[i].v2].pos, vertex[f[i].v3].pos, face[i].normal);
      vertex[f[i].v1].crown[vertex[f[i].v1].faces++] = face[i];
      vertex[f[i].v2].crown[vertex[f[i].v2].faces++] = face[i];
      vertex[f[i].v3].crown[vertex[f[i].v3].faces++] = face[i];
      con = new Constraint();
      if (face[i].normal.length2() == 0.0)
      {
        con.a = 1;
        con.b = 1;
        con.c = 1;
      }
      else
      {
        con.a = face[i].normal.x;
        con.b = face[i].normal.y;
        con.c = face[i].normal.z;
      }
      con.d = -(con.a*v[f[i].v1].r.x + con.b*v[f[i].v1].r.y + con.c*v[f[i].v1].r.z);
      vertex[f[i].v1].zone.con[vertex[f[i].v1].zone.constraints++] = con;
      vertex[f[i].v2].zone.con[vertex[f[i].v2].zone.constraints++] = con;
      vertex[f[i].v3].zone.con[vertex[f[i].v3].zone.constraints++] = con;
      if (edge[f[i].e1].f1 == null)
        edge[f[i].e1].f1 = face[i];
      else
        edge[f[i].e1].f2 = face[i];
      if (edge[f[i].e2].f1 == null)
        edge[f[i].e2].f1 = face[i];
      else
        edge[f[i].e2].f2 = face[i];
      if (edge[f[i].e3].f1 == null)
        edge[f[i].e3].f1 = face[i];
      else
        edge[f[i].e3].f2 = face[i];
    }

    // Record additional constraints for boundary edges.
    
    for (i = 0; i < e.length; i++)
      if (edge[i].f2 == null)
      {
        temp1.set(v[edge[i].v1].r);
        temp1.subtract(v[edge[i].v2].r);
        temp2.set(edge[i].f1.normal);
        con = new Constraint();
        con.a = temp1.y*temp2.z - temp1.z*temp2.y;
        con.b = temp1.z*temp2.x - temp1.x*temp2.z;
        con.c = temp1.x*temp2.y - temp1.y*temp2.x;
        con.d = -(con.a*v[edge[i].v1].r.x + con.b*v[edge[i].v1].r.y + con.c*v[edge[i].v1].r.z);
        vertex[edge[i].v1].zone.con[vertex[edge[i].v1].zone.constraints++] = con;
        vertex[edge[i].v2].zone.con[vertex[edge[i].v2].zone.constraints++] = con;
      }

    // Record the initial cost associated with each edge, and bring the lowest cost edge
    // to the front of the list.
    
    for (i = 0; i < e.length; i++)
      updateCost(edge[i]);
    j = 0;
    for (i = 0; i < e.length; i++)
      if (edge[i].cost < edge[j].cost)
        j = i;
    tempEdge = edge[j];
    edge[j] = edge[0];
    edge[0] = tempEdge;
  }

  /** Simplify the mesh! */

  private void doSimplification()
  {
    int i, j, k, skip;
    MeshEdge rem1, rem2, rep1, rep2;
    MeshEdge e, star[], tempEdge;
    MeshFace tempCrown[];
    VertexInfo v1, v2, v3, v4, tempVert;
    Zone zone1, zone2;
    boolean boundary;

    for (i = 0; i < edge.length; i += skip)
    {
      e = edge[i];
      if (e.cost >= tol || cancel)
        return;
      v1 = vertex[e.v1];
      v2 = vertex[e.v2];
      boundary = (e.f2 == null);

      // Move v1 to v2.
      
      v2.pos = v1.pos;

      // Flag all the edges whose costs must be updated.  This includes all edges in v1's
      // star or v2's star, and all edges which share a vertex with an edge in v1's star.
      
      star = v1.star;
      for (j = 0; j < v1.edges; j++)
      {
        if (vertex[star[j].v1] == v1)
          tempVert = vertex[star[j].v2];
        else
          tempVert = vertex[star[j].v1];
        for (k = 0; k < tempVert.edges; k++)
          tempVert.star[k].cost = -1.0;
      }
      star = v2.star;
      for (j = 0; j < v2.edges; j++)
        star[j].cost = -1.0;

      // Find the two extra edges to remove, and the ones to replace them with.
      
      if (e.f1.e1 == e)
      {
        if (vertex[e.f1.e2.v1] == v2 || vertex[e.f1.e2.v2] == v2)
        {
          rem1 = e.f1.e2;
          rep1 = e.f1.e3;
        }
        else
        {
          rem1 = e.f1.e3;
          rep1 = e.f1.e2;
        }
      }
      else if (e.f1.e2 == e)
      {
        if (vertex[e.f1.e3.v1] == v2 || vertex[e.f1.e3.v2] == v2)
        {
          rem1 = e.f1.e3;
          rep1 = e.f1.e1;
        }
        else
        {
          rem1 = e.f1.e1;
          rep1 = e.f1.e3;
        }
      }
      else
      {
        if (vertex[e.f1.e1.v1] == v2 || vertex[e.f1.e1.v2] == v2)
        {
          rem1 = e.f1.e1;
          rep1 = e.f1.e2;
        }
        else
        {
          rem1 = e.f1.e2;
          rep1 = e.f1.e1;
        }
      }
      if (boundary)
        rem2 = rep2 = null;
      else
      {
        if (e.f2.e1 == e)
        {
          if (vertex[e.f2.e2.v1] == v2 || vertex[e.f2.e2.v2] == v2)
          {
            rem2 = e.f2.e2;
            rep2 = e.f2.e3;
          }
          else
          {
            rem2 = e.f2.e3;
            rep2 = e.f2.e2;
          }
        }
        else if (e.f2.e2 == e)
        {
          if (vertex[e.f2.e3.v1] == v2 || vertex[e.f2.e3.v2] == v2)
          {
            rem2 = e.f2.e3;
            rep2 = e.f2.e1;
          }
          else
          {
            rem2 = e.f2.e1;
            rep2 = e.f2.e3;
          }
        }
        else
        {
          if (vertex[e.f2.e1.v1] == v2 || vertex[e.f2.e1.v2] == v2)
          {
            rem2 = e.f2.e1;
            rep2 = e.f2.e2;
          }
          else
          {
            rem2 = e.f2.e2;
            rep2 = e.f2.e1;
          }
        }
      }

      // Identify the other two points whose stars and crowns must be modified.
      
      if (vertex[rep1.v1] == v1)
        v3 = vertex[rep1.v2];
      else
        v3 = vertex[rep1.v1];
      if (boundary)
        v4 = null;
      else
      {
        if (vertex[rep2.v1] == v1)
          v4 = vertex[rep2.v2];
        else
          v4 = vertex[rep2.v1];
      }
      
      // Update v1's crown.
      
      if (boundary && v1.faces+v2.faces-2 > v1.crown.length)
        tempCrown = new MeshFace [v1.faces+v2.faces-2];
      else if (v1.faces+v2.faces-4 > v1.crown.length)
        tempCrown = new MeshFace [v1.faces+v2.faces-4];
      else
        tempCrown = v1.crown;
      for (j = 0, k = 0; k < v1.faces; k++)
        if (v1.crown[k] != e.f1 && v1.crown[k] != e.f2)
          tempCrown[j++] = v1.crown[k];
      for (k = 0; k < v2.faces; k++)
        if (v2.crown[k] != e.f1 && v2.crown[k] != e.f2)
          tempCrown[j++] = v2.crown[k];
      v1.crown = tempCrown;
      if (boundary)
        v1.faces = v1.faces+v2.faces-2;
      else
        v1.faces = v1.faces+v2.faces-4;

      // Update v1's star.

      if (v1.edges+v2.edges-2 > v1.star.length)
        star = new MeshEdge [v1.edges+v2.edges-2];
      else
        star = v1.star;
      for (j = 0, k = 0; k < v1.edges; k++)
        if (v1.star[k] != e)
          star[j++] = v1.star[k];
      for (k = 0; k < v2.edges; k++)
        if (v2.star[k] != e)
          star[j++] = v2.star[k];
      v1.star = star;
      v1.edges = v1.edges+v2.edges-2;

      // Update v3 and v4's crowns.
      
      for (j = 0, k = 0; k < v3.faces; k++)
        if (v3.crown[k] != e.f1)
          v3.crown[j++] = v3.crown[k];
      v3.faces--;
      if (!boundary)
      {
        for (j = 0, k = 0; k < v4.faces; k++)
          if (v4.crown[k] != e.f2)
            v4.crown[j++] = v4.crown[k];
        v4.faces--;
      }

      // Update v3 and v4's stars.

      for (j = 0, k = 0; k < v3.edges; k++)
        if (v3.star[k] != rem1)
          v3.star[j++] = v3.star[k];
      v3.edges--;
      if (!boundary)
      {
        for (j = 0, k = 0; k < v4.edges; k++)
          if (v4.star[k] != rem2)
            v4.star[j++] = v4.star[k];
        v4.edges--;
      }

      // Eliminate the faces which share this edge.
      
      if (boundary)
      {
        j = e.f1.index;
        face[j] = face[faces-1];
        face[j].index = j;
        faces--;
      }
      else
      {
        j = e.f1.index;
        k = e.f2.index;
        if (j < faces-2 && k < faces-2)
        {
          face[j] = face[faces-1];
          face[k] = face[faces-2];
        }
        else
        {
          if (j != faces-1 && k != faces-1)
            face[Math.min(j,k)] = face[faces-1];
          else
            face[Math.min(j,k)] = face[faces-2];
        }
        face[j].index = j;
        face[k].index = k;
        faces -= 2;
      }
      
      // Add the constraints from v2 to v1.  We do this by removing any duplicate
      // constraints, then appending v2's zone to v1's zone.
      
      zone2 = v2.zone;
      while (zone2 != null)
      {
        zone1 = v1.zone;
        while (zone1 != null)
        {
          for (j = 0; j < zone2.constraints; j++)
            for (k = 0; k < zone1.constraints; k++)
              if (zone2.con[j] == zone1.con[k])
                zone2.con[j] = zone2.con[--zone2.constraints];
          zone1 = zone1.next;
        }
        zone2 = zone2.next;
      }
      
      // Eliminate any zones which are completely free of constraints.
      
      zone2 = v2.zone;
      while (zone2.next != null)
      {
        if (zone2.next.constraints == 0)
          zone2.next = zone2.next.next;
        else
          zone2 = zone2.next;
      }
      
      // Add v2's zone to v1's zone.
      
      zone2 = v2.zone;
      zone1 = v1.zone;
      while (zone1.next != null)
        zone1 = zone1.next;
      if (zone2.constraints == 0)
        zone1.next = zone2.next;
      else
        zone1.next = zone2;
      
      // Go through the faces in v2's crown, and replace references to rem1 and rem2
      // by rep1 and rep2.
      
      for (j = 0; j < v2.faces; j++)
      {
        if (v2.crown[j].e1 == rem1)
          v2.crown[j].e1 = rep1;
        else if (v2.crown[j].e2 == rem1)
          v2.crown[j].e2 = rep1;
        else if (v2.crown[j].e3 == rem1)
          v2.crown[j].e3 = rep1;
      }
      if (!boundary)
        for (j = 0; j < v2.faces; j++)
        {
          if (v2.crown[j].e1 == rem2)
            v2.crown[j].e1 = rep2;
          else if (v2.crown[j].e2 == rem2)
            v2.crown[j].e2 = rep2;
          else if (v2.crown[j].e3 == rem2)
            v2.crown[j].e3 = rep2;
        }
      
      // Similarly, go through the edges adjacent to the faces being removed, and
      // replace references.
      
      if (rem1.f1 == e.f1)
      {
        if (rep1.f1 == e.f1)
          rep1.f1 = rem1.f2;
        else
          rep1.f2 = rem1.f2;
      }
      else
      {
        if (rep1.f1 == e.f1)
          rep1.f1 = rem1.f1;
        else
          rep1.f2 = rem1.f1;
      }
      if (rep1.f1 == null)
      {
        rep1.f1 = rep1.f2;
        rep1.f2 = null;
      }
      if (!boundary)
      {
        if (rem2.f1 == e.f2)
        {
          if (rep2.f1 == e.f2)
            rep2.f1 = rem2.f2;
          else
            rep2.f2 = rem2.f2;
        }
        else
        {
          if (rep2.f1 == e.f2)
            rep2.f1 = rem2.f1;
          else
            rep2.f2 = rem2.f1;
        }
        if (rep2.f1 == null)
        {
          rep2.f1 = rep2.f2;
          rep2.f2 = null;
        }
      }

      // Replace all references to v2 by v1.
      
      for (j = 0; j < vertex.length; j++)
        if (vertex[j] == v2)
          vertex[j] = v1;

      // Update smoothness values for vertices and edges.
      
      v1.smoothness = Math.min(v1.smoothness, v2.smoothness);
      rep1.smoothness = Math.min(rep1.smoothness, rem1.smoothness);
      if (!boundary)
        rep2.smoothness = Math.min(rep2.smoothness, rem2.smoothness);

      // Remove rem1 and rem2 from the list by shifting forward other edges.
      
      for (j = edge.length-1, k = edge.length-1; j > i; j--)
        if (edge[j] != rem1 && edge[j] != rem2)
          edge[k--] = edge[j];

      // Set to null edges left in the list before the shift.
      // Such edges cause a subtle bug because they can still be taken as valid edges
      // by buildMesh(), and this results into wrong smoothness values.

      for (j = k; j > i; j--)
        edge[j] = null;

      // Update the cost for edges which need it.

      if (boundary)
        skip = 2;
      else
        skip = 3;
      k = i+skip;
      for (j = i+skip; j < edge.length; j++)
      {
        if (edge[j].cost == -1.0)
          updateCost(edge[j]);
        if (edge[j].cost < edge[k].cost)
          k = j;
      }
      tempEdge = edge[i+skip];
      edge[i+skip] = edge[k];
      edge[k] = tempEdge;
    }
  }

  /* Update the cost for an edge.  Try both directions to see which gives a lower cost. */
  
  private void updateCost(MeshEdge ed)
  {
    double cost = findCost(ed);
    int v1 = ed.v1;

    ed.v1 = ed.v2;
    ed.v2 = v1;
    ed.cost = findCost(ed);
    if (ed.cost > cost)
    {
      ed.v2 = ed.v1;
      ed.v1 = v1;
      ed.cost = cost;
    }
    if (ed.cost < tol)
    {
      // If this edge is adjacent to a face whose other edges are both boundaries, then
      // collapsing this edge would leave a dangling edge.  This should not be allowed.

      MeshFace f = ed.f1;
      if ((f.e1 == ed || f.e1.f2 == null) && (f.e2 == ed || f.e2.f2 == null) && (f.e3 == ed || f.e3.f2 == null))
      {
        ed.cost = tol;
        return;
      }
      f = ed.f2;
      if (f != null)
        if ((f.e1 == ed || f.e1.f2 == null) && (f.e2 == ed || f.e2.f2 == null) && (f.e3 == ed || f.e3.f2 == null))
        {
          ed.cost = tol;
          return;
        }

      // If this is a non-boundary edge connecting two different boundaries, then contracting it
      // would produce a non-manifold mesh.  That should not be allowed.

      if (ed.f2 != null)
      {
        MeshEdge star[] = vertex[ed.v1].star;
        boolean v1IsBoundary = false;
        for (int i = 0; i < star.length; i++)
          if (star[i].f2 == null)
          {
            v1IsBoundary = true;
            break;
          }
        if (v1IsBoundary)
        {
          star = vertex[ed.v2].star;
          for (int i = 0; i < star.length; i++)
            if (star[i].f2 == null)
            {
              ed.cost = tol;
              return;
            }
        }
      }
    }
  }

  /* Find the cost associated with contracting a given edge.  If the cost is determined 
     to be greater than tol, the method simply returns tol.  This allows findCost() to
     return as soon as it determines the edge will never be contracted, and also speeds 
     up the sorting of edges (since many edges will have identical costs). */

  private double findCost(MeshEdge ed)
  {
    VertexInfo v1 = vertex[ed.v1], v2 = vertex[ed.v2];
    MeshFace f;
    Zone zone = v2.zone;
    double cost, max = 0.0;
    int i;
    
    if (!ed.selected)
      return tol;

    // First calculate the local tesselation error.

    for (i = 0; i < v2.faces; i++)
      if (v2.crown[i] != ed.f1 && v2.crown[i] != ed.f2)
      {
        f = v2.crown[i];
        if (vertex[f.v1] == v2)
          findNormal(v1.pos, vertex[f.v2].pos, vertex[f.v3].pos, temp3);
        else if (vertex[f.v2] == v2)
          findNormal(vertex[f.v1].pos, v1.pos, vertex[f.v3].pos, temp3);
        else
          findNormal(vertex[f.v1].pos, vertex[f.v2].pos, v1.pos, temp3);
        cost = tol*(1.0-temp3.dot(f.normal));
        if (cost >= tol || Double.isNaN(cost))
          return tol;
        if (cost > max)
          max = cost;
      }

    // Now calculate the local geometric error.
      
    while (zone != null)
    {
      for (i = 0; i < zone.constraints; i++)
      {
        cost = zone.con[i].a*v1.pos.x + zone.con[i].b*v1.pos.y + zone.con[i].c*v1.pos.z + zone.con[i].d;
        cost *= cost;
        if (cost >= tol)
          return tol;
        if (cost > max)
          max = cost;
      }
      zone = zone.next;
    }
    return max;
  }

  /* Given three points which define a face, find the normal vector. */

  private void findNormal(Vec3 v1, Vec3 v2, Vec3 v3, Vec3 normal)
  {
    temp1.set(v2.x, v2.y, v2.z);
    temp1.subtract(v1);
    temp2.set(v3.x, v3.y, v3.z);
    temp2.subtract(v1);
    normal.set(temp1.y*temp2.z-temp1.z*temp2.y, temp1.z*temp2.x-temp1.x*temp2.z, temp1.x*temp2.y-temp1.y*temp2.x);
    double length = normal.length();
    if (length < 1e-10)
      normal.set(0.0, 0.0, 0.0);
    else
      normal.scale(1.0/length);
  }
}