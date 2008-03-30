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

/** RotateMeshTool is an EditingTool used for rotating the vertices of TriangleMesh objects. */

public class RotateMeshTool extends MeshEditingTool
{
  private boolean dragInProgress, selectionCanChange;
  private Point clickPoint;
  private Vec3 rotCenter, baseVertPos[];
  private int hdirection, vdirection, whichAxis;
  private UndoRecord undo;
  private final NinePointManipulator manipulator;
  private int lastSelectionDistance[];

  public static final int HANDLE_SIZE = 5;
  private static final double DRAG_SCALE = Math.PI/360.0;
  private static final int XAXIS = 0;
  private static final int YAXIS = 1;
  private static final int ZAXIS = 2;

  public RotateMeshTool(EditingWindow fr, MeshEditController controller, boolean only2D)
  {
    super(fr, controller);
    initButton("rotatePoints");
    if (only2D)
      manipulator = new NinePointManipulator(new Image[] {
        NinePointManipulator.ROTATE_TOPLEFT, null, NinePointManipulator.ROTATE_TOPRIGHT,
        null, null, null,
        NinePointManipulator.ROTATE_BOTTOMLEFT, null, NinePointManipulator.ROTATE_BOTTOMRIGHT});
    else
      manipulator = new NinePointManipulator(new Image[] {
        NinePointManipulator.ROTATE_TOPLEFT, NinePointManipulator.ROTATE_TOP, NinePointManipulator.ROTATE_TOPRIGHT,
        NinePointManipulator.ROTATE_LEFT, null, NinePointManipulator.ROTATE_RIGHT,
        NinePointManipulator.ROTATE_BOTTOMLEFT, NinePointManipulator.ROTATE_BOTTOM, NinePointManipulator.ROTATE_BOTTOMRIGHT});
    manipulator.addEventLink(HandlePressedEvent.class, this, "handlePressed");
    manipulator.addEventLink(HandleDraggedEvent.class, this, "handleDragged");
    manipulator.addEventLink(HandleReleasedEvent.class, this, "handleReleased");
  }

  public void activate()
  {
    super.activate();
    lastSelectionDistance = null;
    checkForSelectionChanged();
  }

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public boolean allowSelectionChanges()
  {
    return selectionCanChange;
  }

  private void checkForSelectionChanged()
  {
    // See whether the selection has changed.

    int selected[] = controller.getSelectionDistance();
    boolean changed = (lastSelectionDistance == null || lastSelectionDistance.length != selected.length);
    for (int i = 0; !changed && i < selected.length; i++)
      changed = (lastSelectionDistance[i] != selected[i]);
    lastSelectionDistance = selected;
    if (!changed)
      return;

    // It has, so update the center point.

    Camera cam = theWindow.getView().getCamera();
    BoundingBox bounds = findSelectionBounds(cam);
    if (bounds != null)
    {
      rotCenter = new Vec3((bounds.minx+bounds.maxx)/2.0, (bounds.miny+bounds.maxy)/2.0, (bounds.minz+bounds.maxz)/2.0);
      rotCenter = cam.getViewToWorld().times(rotCenter);
    }
  }

  public String getToolTipText()
  {
    return Translate.text("rotateMeshTool.tipText");
  }

  public void drawOverlay(ViewerCanvas view)
  {
    checkForSelectionChanged();
    BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
    if (!dragInProgress)
    {
      if (selectionBounds != null)
      {
        manipulator.draw(view, selectionBounds);
        theWindow.setHelpText(Translate.text("rotateMeshTool.helpText"));
      }
      else
        theWindow.setHelpText(Translate.text("rotateMeshTool.errorText"));
    }
    if (selectionBounds != null)
    {
      Vec2 p = view.getCamera().getWorldToScreen().timesXY(rotCenter);
      int px = (int) p.x;
      int py = (int) p.y;
      view.drawLine(new Point(px-HANDLE_SIZE, py-HANDLE_SIZE), new Point(px+HANDLE_SIZE, py+HANDLE_SIZE), ViewerCanvas.handleColor);
      view.drawLine(new Point(px-HANDLE_SIZE, py+HANDLE_SIZE), new Point(px+HANDLE_SIZE, py-HANDLE_SIZE), ViewerCanvas.handleColor);
    }
  }

  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    if (e.isControlDown())
    {
      // For a control-click, just move the center of rotation.

      double depth = view.getCamera().getWorldToView().times(rotCenter).z;
      rotCenter = view.getCamera().convertScreenToWorld(e.getPoint(), depth);
      selectionCanChange = false;
      view.repaint();
      return;
    }
    BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
    dragInProgress = false;
    if (selectionBounds != null)
      dragInProgress = manipulator.mousePressed(e, view, selectionBounds);
    selectionCanChange = !dragInProgress;
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
    clickPoint =  ev.getMouseEvent().getPoint();
    HandlePosition handle = ev.getHandle();
    if (!handle.isEast() && !handle.isWest())
      {
         whichAxis = XAXIS;
         hdirection = 0;
         vdirection = -1;
      }
    else if (!handle.isNorth() && !handle.isSouth())
      {
         whichAxis = YAXIS;
         vdirection = 0;
         hdirection = 1;
      }
    else
      {
         whichAxis = ZAXIS;
         if (handle.isWest())
           vdirection = -1;
         else
           vdirection = 1;
         if (handle.isNorth())
           hdirection = 1;
         else
           hdirection = -1;
      }
    baseVertPos = mesh.getVertexPositions();
  }
  
  protected void handleDragged(HandleDraggedEvent ev)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    Point dragPoint = ev.getMouseEvent().getPoint();
    if (undo == null)
      undo = new UndoRecord(theWindow, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {mesh, mesh.getVertexPositions()});
    double angle = DRAG_SCALE*((dragPoint.x-clickPoint.x)*hdirection + (dragPoint.y-clickPoint.y)*vdirection);
    Vec3 v[] = findRotatedPositions(baseVertPos, angle, (MeshViewer) ev.getView());
    mesh.setVertexPositions(v);
    controller.objectChanged();
    theWindow.updateImage();
    theWindow.setHelpText(Translate.text("rotateMeshTool.dragText", Double.toString(Math.round(angle*1e5*180.0/Math.PI)/1e5)));
  }

  protected void handleReleased(HandleReleasedEvent ev)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    Point dragPoint = ev.getMouseEvent().getPoint();

    double angle = DRAG_SCALE*((dragPoint.x-clickPoint.x)*hdirection + (dragPoint.y-clickPoint.y)*vdirection);
    if (undo != null)
      theWindow.setUndoRecord(undo);
    Vec3 v[] = findRotatedPositions(baseVertPos, angle, (MeshViewer) ev.getView());
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

  private Vec3 [] findRotatedPositions(Vec3 vert[], double angle, MeshViewer view)
  {
    Vec3 v[] = new Vec3 [vert.length], axis;
    int selected[] = controller.getSelectionDistance();
    Camera cam = view.getCamera();
    CoordinateSystem coords = view.getDisplayCoordinates();
    Mat4 m;
    int i;
    
    // Determine whether the coordinate system is right or left handed.
    
    Vec3 xdir = cam.getWorldToView().timesDirection(Vec3.vx());
    Vec3 ydir = cam.getWorldToView().timesDirection(Vec3.vy());
    Vec3 zdir = cam.getWorldToView().timesDirection(Vec3.vz());
    if (xdir.cross(ydir).dot(zdir) < 0.0)
      angle = -angle;
    
    // Find the transformation matrix.
    
    m = coords.fromLocal();
    m = Mat4.translation(-rotCenter.x, -rotCenter.y, -rotCenter.z).times(m);
    if (whichAxis == XAXIS)
      axis = cam.getViewToWorld().timesDirection(Vec3.vx());
    else if (whichAxis == YAXIS)
      axis = cam.getViewToWorld().timesDirection(Vec3.vy());
    else
      axis = cam.getViewToWorld().timesDirection(Vec3.vz());
    m = Mat4.axisRotation(axis, angle).times(m);
    m = Mat4.translation(rotCenter.x, rotCenter.y, rotCenter.z).times(m);
    m = coords.toLocal().times(m);
    
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