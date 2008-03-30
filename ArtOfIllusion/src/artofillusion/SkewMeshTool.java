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
import artofillusion.ui.NinePointManipulator.*;
import buoy.event.*;
import java.awt.*;

/** SkewMeshTool is an EditingTool used for skewing the vertices of TriangleMesh objects. */

public class SkewMeshTool extends MeshEditingTool
{
  private boolean dragInProgress, skewX, skewY, skewAll;
  private double clickX, clickY, centerX, centerY;
  private Vec3 skewCenter, baseVertPos[];
  private UndoRecord undo;
  private final NinePointManipulator manipulator;

  public static final int HANDLE_SIZE = 5;

  public SkewMeshTool(EditingWindow fr, MeshEditController controller)
  {
    super(fr, controller);
    initButton("skewPoints");
    manipulator = new NinePointManipulator(new Image[] {
      NinePointManipulator.ARROWS_S_E, NinePointManipulator.ARROWS_E_W, NinePointManipulator.ARROWS_S_W,
      NinePointManipulator.ARROWS_N_S, null, NinePointManipulator.ARROWS_N_S,
      NinePointManipulator.ARROWS_N_E, NinePointManipulator.ARROWS_E_W, NinePointManipulator.ARROWS_N_W});
    manipulator.addEventLink(HandlePressedEvent.class, this, "handlePressed");
    manipulator.addEventLink(HandleDraggedEvent.class, this, "handleDragged");
    manipulator.addEventLink(HandleReleasedEvent.class, this, "handleReleased");
  }

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public boolean allowSelectionChanges()
  {
    return !dragInProgress;
  }

  public String getToolTipText()
  {
    return Translate.text("skewMeshTool.tipText");
  }

  public void drawOverlay(ViewerCanvas view)
  {
    BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
    if (selectionBounds != null)
    {
      if (!dragInProgress)
        manipulator.draw(view, selectionBounds);
      theWindow.setHelpText(Translate.text("skewMeshTool.helpText"));
    }
    else
      theWindow.setHelpText(Translate.text("skewMeshTool.errorText"));
  }
  
  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
    dragInProgress = false;
    if (selectionBounds != null)
      dragInProgress = manipulator.mousePressed(e, view, selectionBounds);
  }

  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    manipulator.mouseDragged(e, view);
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    manipulator.mouseReleased(e, view);
  }

  protected void handlePressed(HandlePressedEvent ev)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    Point clickPoint = ev.getMouseEvent().getPoint();
    clickX = (double) clickPoint.x;
    clickY = (double) clickPoint.y;
    BoundingBox bounds = ev.getSelectionBounds();
    Rectangle r = ev.getScreenBounds();
    HandlePosition handle = ev.getHandle();
    skewCenter = new Vec3(0.0, 0.0, (bounds.minz+bounds.maxz)/2.0);
    if (handle.isWest())
      {
        skewY = true;
        centerX = (double) (r.x + r.width);
        skewCenter.x = bounds.minx;
      }
    else if (handle.isEast())
      {
        skewY = true;
        centerX = (double) r.x;
        skewCenter.x = bounds.maxx;
      }
    else
      {
        skewY = false;
        skewCenter.x = (bounds.minx+bounds.maxx);
      }
    if (handle.isNorth())
      {
        skewX = true;
        centerY = (double) (r.y + r.height);
        skewCenter.y = bounds.miny;
      }
    else if (handle.isSouth())
      {
        skewX = true;
        centerY = (double) r.y;
        skewCenter.y = bounds.maxy;
      }
    else
      {
        skewX = false;
        skewCenter.y = (bounds.miny+bounds.maxy);
      }
    if (ev.getMouseEvent().isControlDown())
      {
        centerX = (double) (r.x + (r.width/2));
        centerY = (double) (r.y + (r.height/2));
        skewCenter.x = (bounds.minx+bounds.maxx)/2.0;
        skewCenter.y = (bounds.miny+bounds.maxy)/2.0;
      }
    skewAll = skewX && skewY && ev.getMouseEvent().isShiftDown();
    baseVertPos = mesh.getVertexPositions();
  }
  
  protected void handleDragged(HandleDraggedEvent ev)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    Point dragPoint = ev.getMouseEvent().getPoint();
    double max, xskew, yskew;
    
    if (undo == null)
      undo = new UndoRecord(theWindow, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {mesh, mesh.getVertexPositions()});
    xskew = yskew = 0.0;
    if (skewX)
      xskew = (dragPoint.x-clickX)/(dragPoint.y-centerY);
    if (skewY)
      yskew = (dragPoint.y-clickY)/(dragPoint.x-centerX);
    if (skewAll)
      {
        max = Math.max(Math.abs(xskew), Math.abs(yskew));
        if (xskew != 0.0)
          xskew *= max/Math.abs(xskew);
        if (yskew != 0.0)
          yskew *= max/Math.abs(yskew);
      }
    Vec3 v[] = findSkewedPositions(baseVertPos, xskew, yskew, (MeshViewer) ev.getView());
    mesh.setVertexPositions(v);
    controller.objectChanged();
    theWindow.updateImage();
  }

  protected void handleReleased(HandleReleasedEvent ev)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    Point dragPoint = ev.getMouseEvent().getPoint();
    double max, xskew, yskew;

    xskew = yskew = 0.0;
    if (skewX)
      xskew = (dragPoint.x-clickX)/(dragPoint.y-centerY);
    if (skewY)
      yskew = (dragPoint.y-clickY)/(dragPoint.x-centerX);
    if (skewAll)
      {
        max = Math.max(Math.abs(xskew), Math.abs(yskew));
        if (xskew != 0.0)
          xskew *= max/Math.abs(xskew);
        if (yskew != 0.0)
          yskew *= max/Math.abs(yskew);
      }
    if (undo != null)
      theWindow.setUndoRecord(undo);
    Vec3 v[] = findSkewedPositions(baseVertPos, xskew, yskew, (MeshViewer) ev.getView());
    mesh.setVertexPositions(v);
    controller.objectChanged();
    theWindow.updateImage();
    undo = null;
    baseVertPos = null;
    dragInProgress = false;
  }

  /**
   * Find the new positions of the vertices after scaling.
   */

  private Vec3 [] findSkewedPositions(Vec3 vert[], double xskew, double yskew, MeshViewer view)
  {
    Vec3 v[] = new Vec3 [vert.length];
    int selected[] = controller.getSelectionDistance();
    Camera cam = view.getCamera();
    Mat4 m, s;
    int i;
    
    // Find the transformation matrix.
    
    m = cam.getObjectToView();
    m = Mat4.translation(-skewCenter.x, -skewCenter.y, -skewCenter.z).times(m);
    s = new Mat4(1.0, xskew, 0.0, 0.0,
                yskew, 1.0, 0.0, 0.0,
                0.0, 0.0, 1.0, 0.0,
                0.0, 0.0, 0.0, 1.0);
    m = s.times(m);
    m = Mat4.translation(skewCenter.x, skewCenter.y, skewCenter.z).times(m);
    m = cam.getViewToWorld().times(m);
    m = view.getDisplayCoordinates().toLocal().times(m);
    
    // Determine the deltas.
    
    for (i = 0; i < vert.length; i++)
      {
        if (selected[i] == 0)
          v[i] = m.times(vert[i]).minus(vert[i]);
        else
          v[i] = new Vec3();
      }
    if (theFrame instanceof MeshEditorWindow)
      ((MeshEditorWindow) theFrame).adjustDeltas(v);
    for (i = 0; i < vert.length; i++)
      v[i].add(vert[i]);
    return v;
  }
}