/* Copyright (C) 1999-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import java.awt.*;

/** ReshapeMeshTool is an EditingTool used for moving the vertices of TriangleMesh objects. */

public class ReshapeMeshTool extends MeshEditingTool
{
  private Point clickPoint;
  private Vec3 clickPos, baseVertPos[];
  private UndoRecord undo;

  public ReshapeMeshTool(EditingWindow fr, MeshEditController controller)
  {
    super(fr, controller);
    initButton("movePoints");
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("reshapeMeshTool.helpText"));
  }

  public int whichClicks()
  {
    return HANDLE_CLICKS;
  }

  public boolean allowSelectionChanges()
  {
    return true;
  }

  public String getToolTipText()
  {
    return Translate.text("reshapeMeshTool.tipText");
  }

  public void mousePressedOnHandle(WidgetMouseEvent e, ViewerCanvas view, int obj, int handle)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    MeshVertex v[] = mesh.getVertices();
    
    clickPoint = e.getPoint();
    clickPos = v[handle].r;
    baseVertPos = mesh.getVertexPositions();
  }
  
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    MeshViewer mv = (MeshViewer) view;
    Mesh mesh = (Mesh) controller.getObject().getObject();
    Point dragPoint = e.getPoint();
    Vec3 v[], drag;
    int dx, dy;

    if (undo == null)
      undo = new UndoRecord(theWindow, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {mesh, mesh.getVertexPositions()});
    dx = dragPoint.x - clickPoint.x;
    dy = dragPoint.y - clickPoint.y;
    if (e.isShiftDown())
    {
      if (Math.abs(dx) > Math.abs(dy))
        dy = 0;
      else
        dx = 0;
    }
    v = findDraggedPositions(clickPos, baseVertPos, dx, dy, mv, e.isControlDown(), controller.getSelectionDistance());
    mesh.setVertexPositions(v);
    controller.objectChanged();
    theWindow.updateImage();
    if (e.isControlDown())
      drag = view.getCamera().getCameraCoordinates().getZDirection().times(-dy*0.01);
    else
      drag = view.getCamera().findDragVector(clickPos, dx, dy);
    theWindow.setHelpText(Translate.text("reshapeMeshTool.dragText",
        Math.round(drag.x*1e5)/1e5+", "+Math.round(drag.y*1e5)/1e5+", "+Math.round(drag.z*1e5)/1e5));
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    Point dragPoint = e.getPoint();
    int dx, dy;
    Vec3 v[];

    dx = dragPoint.x - clickPoint.x;
    dy = dragPoint.y - clickPoint.y;
    if (e.isShiftDown())
    {
      if (Math.abs(dx) > Math.abs(dy))
        dy = 0;
      else
        dx = 0;
    }
    if (dx != 0 || dy != 0)
    {
      if (undo != null)
        theWindow.setUndoRecord(undo);
      v = findDraggedPositions(clickPos, baseVertPos, dx, dy, (MeshViewer) view, e.isControlDown(), controller.getSelectionDistance());
      mesh.setVertexPositions(v);
    }
    controller.objectChanged();
    theWindow.updateImage();
    theWindow.setHelpText(Translate.text("reshapeMeshTool.helpText"));
    undo = null;
    baseVertPos = null;
  }

  public void keyPressed(KeyPressedEvent e, ViewerCanvas view)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    Vec3 vert[] = mesh.getVertexPositions();
    int i, selectDist[] = controller.getSelectionDistance();
    int key = e.getKeyCode();
    double dx, dy;
    Vec3 v[];
    
    // Pressing an arrow key is equivalent to dragging the first selected point by one pixel.
 
    if (key == KeyPressedEvent.VK_UP)
    {
      dx = 0;
      dy = -1;
    }
    else if (key == KeyPressedEvent.VK_DOWN)
    {
      dx = 0;
      dy = 1;
    }
    else if (key == KeyPressedEvent.VK_LEFT)
    {
      dx = -1;
      dy = 0;
    }
    else if (key == KeyPressedEvent.VK_RIGHT)
    {
      dx = 1;
      dy = 0;
    }
    else
      return;
    e.consume();
    for (i = 0; i < vert.length && selectDist[i] != 0; i++);
    if (i == vert.length)
      return;
    if (view.getSnapToGrid())
    {
      double scale = view.getGridSpacing()*view.getScale();
      if (!e.isAltDown())
        scale /= view.getSnapToSubdivisions();
      dx *= scale;
      dy *= scale;
    }
    else if (e.isAltDown())
    {
      dx *= 10;
      dy *= 10;
    }
    theWindow.setUndoRecord(new UndoRecord(theWindow, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {mesh, vert}));
    v = findDraggedPositions(vert[i], vert, dx, dy, (MeshViewer) view, e.isControlDown(), selectDist);
    mesh.setVertexPositions(v);
    controller.objectChanged();
    theWindow.updateImage();
  }
  
  private Vec3 [] findDraggedPositions(Vec3 pos, Vec3 vert[], double dx, double dy, MeshViewer view, boolean controlDown, int selectDist[])
  {
    int maxDistance = view.getController().getTensionDistance();
    double tension = view.getController().getMeshTension();
    Vec3 drag[] = new Vec3 [maxDistance+1], v[] = new Vec3 [vert.length];
    
    if (controlDown)
      drag[0] = view.getCamera().getCameraCoordinates().getZDirection().times(-dy*0.01);
    else
      drag[0] = view.getCamera().findDragVector(pos, dx, dy);
    for (int i = 1; i <= maxDistance; i++)
      drag[i] = drag[0].times(Math.pow((maxDistance-i+1.0)/(maxDistance+1.0), tension));
    if (view.getUseWorldCoords())
    {
      Mat4 trans = view.getDisplayCoordinates().toLocal();
      for (int i = 0; i < drag.length; i++)
        trans.transformDirection(drag[i]);
    }
    for (int i = 0; i < vert.length; i++)
    {
      if (selectDist[i] > -1)
        v[i] = vert[i].plus(drag[selectDist[i]]);
      else
        v[i] = new Vec3(vert[i]);
    }
    return v;
  }
}