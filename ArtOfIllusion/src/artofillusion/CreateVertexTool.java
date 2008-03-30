/* Copyright (C) 2003-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import java.awt.*;

/** CreateVertexTool is an EditingTool used for adding vertices to triangle meshes. */

public class CreateVertexTool extends MeshEditingTool
{
  private Point clickPoint;
  private Vec3 clickPos, constrainDir;
  private int target;
  private int vertexToMove;

  public CreateVertexTool(EditingWindow fr, MeshEditController controller)
  {
    super(fr, controller);
    initButton("createVertex");
  }

  public void activate()
  {
    super.activate();
    setHelpText();
  }
  
  /**
   * Set the help text based on the current selection mode.
   */
  
  private void setHelpText()
  {
    int mode = controller.getSelectionMode();
    if (mode == MeshEditController.POINT_MODE)
      theWindow.setHelpText(Translate.text("createVertexTool.helpText.point"));
    else if (mode == MeshEditController.EDGE_MODE)
      theWindow.setHelpText(Translate.text("createVertexTool.helpText.edge"));
    else
      theWindow.setHelpText(Translate.text("createVertexTool.helpText.face"));
  }

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public String getToolTipText()
  {
    return Translate.text("createVertexTool.tipText");
  }

  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    TriMeshViewer tmv = (TriMeshViewer) view;
    TriangleMesh mesh = (TriangleMesh) controller.getObject().getObject();
    MeshVertex vert[] = mesh.getVertices();
    int mode = controller.getSelectionMode();
    Vec3 uvw = new Vec3();

    clickPoint = e.getPoint();
    target = tmv.findClickTarget(clickPoint, uvw);
    if (target == -1)
      return;
    theWindow.setUndoRecord(new UndoRecord(theWindow, false, UndoRecord.COPY_OBJECT, new Object [] {mesh, mesh.duplicate()}));
    if (mode == MeshEditController.POINT_MODE)
    {
      // Create a new vertex if this is a boundary vertex.  Otherwise, just move it.
      
      constrainDir = mesh.getNormals()[target];
      if (createBoundaryVertex(mesh))
      {
        vertexToMove = mesh.getVertices().length-1;
        controller.setMesh(mesh);
        controller.objectChanged();
        theWindow.updateImage();
        theWindow.updateMenus();
      }
      else
        vertexToMove = target;
      clickPos = mesh.getVertices()[vertexToMove].r;
    }
    else
    {
      boolean[] split;
      TriangleMesh newmesh = null;
      if (mode == MeshEditController.EDGE_MODE)
      {
        // Subdivide an edge.
        
        TriangleMesh.Edge edge[] = mesh.getEdges();
        Vec3 v1 = vert[edge[target].v1].r;
        Vec3 v2 = vert[edge[target].v2].r;
        clickPos = v1.times(uvw.x).plus(v2.times(uvw.y));
        split = new boolean[edge.length];
        split[target] = true;
        newmesh = TriangleMesh.subdivideLinear(mesh, split);
        constrainDir = v1.minus(v2);
        constrainDir.normalize();
      }
      else if (mode == MeshEditController.FACE_MODE)
      {
        // Subdivide a face.
        
        TriangleMesh.Face face[] = mesh.getFaces();
        Vec3 v1 = vert[face[target].v1].r;
        Vec3 v2 = vert[face[target].v2].r;
        Vec3 v3 = vert[face[target].v3].r;
        clickPos = v1.times(uvw.x).plus(v2.times(uvw.y)).plus(v3.times(uvw.z));
        split = new boolean[face.length];
        split[target] = true;
        newmesh = TriangleMesh.subdivideFaces(mesh, split);
        constrainDir = v1.minus(v2).cross(v3.minus(v2));
        constrainDir.normalize();
      }
      vertexToMove = newmesh.getVertices().length-1;
      newmesh.getVertices()[vertexToMove].r.set(clickPos);
      mesh.copyObject(newmesh);
      controller.setMesh(mesh);
      controller.objectChanged();
      theWindow.updateImage();
      theWindow.updateMenus();
    }
  }
  
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    if (target == -1)
      return;
    TriMeshViewer tmv = (TriMeshViewer) view;
    TriangleMesh mesh = (TriangleMesh) controller.getObject().getObject();
    Point dragPoint = e.getPoint();
    Vec3 v[], drag;
    int dx, dy;

    dx = dragPoint.x - clickPoint.x;
    dy = dragPoint.y - clickPoint.y;
    drag = findDragVector(dx, dy, tmv, e.isShiftDown());
    v = findDraggedPositions(mesh, drag);
    mesh.setVertexPositions(v);
    controller.objectChanged();
    theWindow.updateImage();
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    if (target == -1)
      return;
    TriMeshViewer tmv = (TriMeshViewer) view;
    Object3D meshobj = controller.getObject().getObject();
    TriangleMesh mesh = (TriangleMesh) meshobj;
    Point dragPoint = e.getPoint();
    int dx, dy;
    Vec3 v[], drag;

    dx = dragPoint.x - clickPoint.x;
    dy = dragPoint.y - clickPoint.y;
    drag = findDragVector(dx, dy, tmv, e.isShiftDown());
    v = findDraggedPositions(mesh, drag);

    mesh.setVertexPositions(v);
    controller.objectChanged();
    theWindow.updateImage();
    setHelpText();
  }
  
  /**
   * Return the new positions of every vertex.
   */
  
  private Vec3 [] findDraggedPositions(TriangleMesh mesh, Vec3 drag)
  {
    MeshVertex vert[] = mesh.getVertices();
    Vec3 pos[] = new Vec3 [vert.length];

    for (int i = 0; i < pos.length; i++)
      pos[i] = vert[i].r;
    pos[vertexToMove] = clickPos.plus(drag);
    return pos;
  }
  
  /**
   * Find the displacement vector for the vertex being dragged.
   */
  
  private Vec3 findDragVector(int dx, int dy, TriMeshViewer view, boolean constrain)
  {
    Vec3 drag = view.getCamera().findDragVector(clickPos, dx, dy);
    if (constrain)
    {
      if (controller.getSelectionMode() == MeshEditController.EDGE_MODE)
        drag = constrainDir.times(drag.dot(constrainDir));
      else
        drag.subtract(constrainDir.times(drag.dot(constrainDir)));
    }
    return drag;
  }

  /**
   * Try to create a new vertex when a boundary vertex is dragged.  Returns true if a new vertex was
   * created, false otherwise.
   */
  
  private boolean createBoundaryVertex(TriangleMesh mesh)
  {
    TriangleMesh.Vertex vert[] = (TriangleMesh.Vertex []) mesh.getVertices();
    TriangleMesh.Vertex v = (TriangleMesh.Vertex) vert[target];
    TriangleMesh.Edge edge[] = mesh.getEdges();
    TriangleMesh.Face face[] = mesh.getFaces();
    int vertEdge[] = v.getEdges();

    if (edge[vertEdge[0]].f2 != -1)
      return false;
    theWindow.setUndoRecord(new UndoRecord(theWindow, false, UndoRecord.COPY_OBJECT, new Object [] {mesh, mesh.duplicate()}));

    // Create a new vertex and two new faces.
    
    TriangleMesh.Vertex newvert[] = new TriangleMesh.Vertex [vert.length+1];
    int newface[][] = new int [face.length+2][];
    for (int i = 0; i < vert.length; i++)
      newvert[i] = vert[i];
    newvert[vert.length] = mesh.new Vertex(v);
    for (int i = 0; i < face.length; i++)
      newface[i] = new int [] {face[i].v1, face[i].v2, face[i].v3};
    TriangleMesh.Edge e = edge[vertEdge[0]];
    TriangleMesh.Face f = face[e.f1];
    int oldFace1 = e.f1;
    if ((f.v1 == e.v1 && f.v2 == e.v2) || (f.v2 == e.v1 && f.v3 == e.v2) || (f.v3 == e.v1 && f.v1 == e.v2))
      newface[face.length] = new int [] {e.v2, e.v1, vert.length};
    else
      newface[face.length] = new int [] {e.v1, e.v2, vert.length};
    e = edge[vertEdge[vertEdge.length-1]];
    f = face[e.f1];
    int oldFace2 = e.f1;
    if ((f.v1 == e.v1 && f.v2 == e.v2) || (f.v2 == e.v1 && f.v3 == e.v2) || (f.v3 == e.v1 && f.v1 == e.v2))
      newface[face.length+1] = new int [] {e.v2, e.v1, vert.length};
    else
      newface[face.length+1] = new int [] {e.v1, e.v2, vert.length};
    mesh.setShape(newvert, newface);
    
    // Copy over the edge smoothness values.
    
    TriangleMesh.Edge newedge[] = mesh.getEdges();
    for (int i = 0; i < newedge.length; i++)
      for (int j = 0; j < edge.length; j++)
        if ((newedge[i].v1 == edge[j].v1 && newedge[i].v2 == edge[j].v2) || (newedge[i].v1 == edge[j].v2 && newedge[i].v2 == edge[j].v1))
          newedge[i].smoothness = edge[j].smoothness;
    
    // Update the parameter values.
    
    TextureParameter param[] = mesh.getParameters();
    ParameterValue paramValue[] = mesh.getParameterValues();
    for (int i = 0; i < paramValue.length; i++)
    {
      if (paramValue[i] instanceof VertexParameterValue)
      {
        double val[] = ((VertexParameterValue) paramValue[i]).getValue();
        double newval[] = new double [newvert.length];
        System.arraycopy(val, 0, newval, 0, val.length);
        newval[val.length] = val[target];
        ((VertexParameterValue) paramValue[i]).setValue(newval);
      }
      if (paramValue[i] instanceof FaceParameterValue)
      {
        double val[] = ((FaceParameterValue) paramValue[i]).getValue();
        double newval[] = new double [newface.length];
        System.arraycopy(val, 0, newval, 0, val.length);
        newval[val.length] = val[oldFace1];
        newval[val.length+1] = val[oldFace2];
        ((FaceParameterValue) paramValue[i]).setValue(newval);
      }
      if (paramValue[i] instanceof FaceVertexParameterValue)
      {
        FaceVertexParameterValue fvpv = (FaceVertexParameterValue) paramValue[i];
        double newval[][] = new double [newface.length][3];
        for (int index = 0; index < newval.length; index++)
        {
          newval[index][0] = fvpv.getValue(index, 0);
          newval[index][1] = fvpv.getValue(index, 1);
          newval[index][2] = fvpv.getValue(index, 2);
        }
        newval[face.length][0] = newval[face.length][1] = newval[face.length][2] = param[i].defaultVal;
        newval[face.length+1][0] = newval[face.length+1][1] = newval[face.length+1][2] = param[i].defaultVal;
        fvpv.setValue(newval);
      }
    }
    return true;
  }
}