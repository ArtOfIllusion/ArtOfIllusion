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

/** TaperMeshTool is an EditingTool used for tapering the vertices of TriangleMesh objects. */

public class TaperMeshTool extends MeshEditingTool
{
  private boolean dragInProgress, taperAll, towardCenter;
  private Point clickPoint;
  private HandlePosition whichHandle;
  private double boundsHeight, boundsWidth;
  private Vec3 baseVertPos[];
  private UndoRecord undo;
  private final NinePointManipulator manipulator;

  public static final int HANDLE_SIZE = 5;

  public TaperMeshTool(EditingWindow fr, MeshEditController controller)
  {
    super(fr, controller);
    initButton("taperPoints");
    manipulator = new NinePointManipulator(new Image[] {
      NinePointManipulator.ARROWS_S_E, null, NinePointManipulator.ARROWS_S_W,
      null, null, null,
      NinePointManipulator.ARROWS_N_E, null, NinePointManipulator.ARROWS_N_W});
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
    return Translate.text("taperMeshTool.tipText");
  }

  public void drawOverlay(ViewerCanvas view)
  {
    BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
    if (selectionBounds != null)
    {
      if (!dragInProgress)
        manipulator.draw(view, selectionBounds);
      theWindow.setHelpText(Translate.text("taperMeshTool.helpText"));
    }
    else
      theWindow.setHelpText(Translate.text("taperMeshTool.errorText"));
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
    clickPoint = ev.getMouseEvent().getPoint();
    whichHandle = ev.getHandle();
    taperAll = ev.getMouseEvent().isShiftDown();
    towardCenter = ev.getMouseEvent().isControlDown();
    Rectangle r = ev.getScreenBounds();
    boundsHeight = (double) r.height;
    boundsWidth = (double) r.width;
    baseVertPos = mesh.getVertexPositions();
  }
  
  protected void handleDragged(HandleDraggedEvent ev)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    if (undo == null)
      undo = new UndoRecord(theWindow, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {mesh, mesh.getVertexPositions()});
    Vec3 v[] = findTaperedPositions(baseVertPos, ev.getMouseEvent().getPoint(), ev.getSelectionBounds(), (MeshViewer) ev.getView());
    mesh.setVertexPositions(v);
    controller.objectChanged();
    theWindow.updateImage();
  }

  protected void handleReleased(HandleReleasedEvent ev)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    if (undo != null)
      theWindow.setUndoRecord(undo);
    Vec3 v[] = findTaperedPositions(baseVertPos, ev.getMouseEvent().getPoint(), ev.getSelectionBounds(), (MeshViewer) ev.getView());
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

  private Vec3 [] findTaperedPositions(Vec3 vert[], Point pos, BoundingBox bounds, MeshViewer view)
  {
    Vec3 v[] = new Vec3 [vert.length];
    int selected[] = controller.getSelectionDistance();
    Camera cam = view.getCamera();
    double clickX, clickY, posX, posY, taper;
    Vec3 center;
    Mat4 m1, m2;
    int i, direction;
    
    clickX = (double) clickPoint.x;
    clickY = (double) clickPoint.y;
    posX = (double) pos.x;
    posY = (double) pos.y;
    
    // Figure out which way to taper the mesh, and toward which point.
    
    direction = (Math.abs(posX-clickX) > Math.abs(posY-clickY)) ? 0 : 1;
    center = new Vec3(0.0, 0.0, (bounds.minz+bounds.maxz)/2.0);
    if (bounds.minx == bounds.maxx || bounds.miny == bounds.maxy)
      taper = 0.0;
    else if (direction == 0)
      {
        if (towardCenter)
          center.x = (bounds.minx+bounds.maxx)/2.0;
        else if (whichHandle.isWest())
          center.x = bounds.minx;
        else
          center.x = bounds.maxx;
        if (whichHandle.isNorth())
          center.y = bounds.miny;
        else
          center.y = bounds.maxy;
        if (whichHandle.isWest())
          taper = (posX-clickX)/boundsWidth;
        else
          taper = (clickX-posX)/boundsWidth;
        if (taper > 1.0)
          taper = 1.0;
        if (whichHandle.isSouth())
          taper *= -1.0;
      }
    else
      {
        if (towardCenter)
          center.y = (bounds.miny+bounds.maxy)/2.0;
        else if (whichHandle.isNorth())
          center.y = bounds.miny;
        else
          center.y = bounds.maxy;
        if (whichHandle.isWest())
          center.x = bounds.minx;
        else
          center.x = bounds.maxx;
        if (whichHandle.isNorth())
          taper = (posY-clickY)/boundsHeight;
        else
          taper = (clickY-posY)/boundsHeight;
        if (taper > 1.0)
          taper = 1.0;
        if (whichHandle.isEast())
          taper *= -1.0;
      }

    // If the points are not being tapered, just copy them over.
    
    if (taper == 0.0)
      {
        for (i = 0; i < vert.length; i++)
          v[i] = new Vec3(vert[i]);
        return v;
      }

    // Find the transformation matrix.
    
    m1 = cam.getObjectToView();
    m1 = Mat4.translation(-center.x, -center.y, -center.z).times(m1);
    m2 = Mat4.translation(center.x, center.y, center.z);
    m2 = cam.getViewToWorld().times(m2);
    m2 = view.getDisplayCoordinates().toLocal().times(m2);
    
    // Determine the deltas.
    
    for (i = 0; i < vert.length; i++)
      {
        if (selected[i] == 0)
          {
            v[i] = m1.times(vert[i]);
            if (direction == 0)
              {
                v[i].x *= 1.0 - taper*v[i].y/(bounds.maxy-bounds.miny);
                if (taperAll)
                  v[i].z *= 1.0 - taper*v[i].y/(bounds.maxy-bounds.miny);
              }
            else
              {
                v[i].y *= 1.0 - taper*v[i].x/(bounds.maxx-bounds.minx);
                if (taperAll)
                  v[i].z *= 1.0 - taper*v[i].x/(bounds.maxx-bounds.minx);
              }
            v[i] = m2.times(v[i]).minus(vert[i]);
          }
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