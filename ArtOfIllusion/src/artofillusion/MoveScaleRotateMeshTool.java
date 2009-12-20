/* Copyright (C) 2006-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.ui.*;
import artofillusion.ui.Compound3DManipulator.*;
import artofillusion.math.*;
import artofillusion.object.*;

import java.awt.event.*;

import buoy.event.*;
import buoy.widget.*;

/**
 * This editing tool presents as a compound interface for move, scale, and rotating parts of
 * a mesh.
 */

public class MoveScaleRotateMeshTool extends MeshEditingTool
{
  private boolean dragInProgress;
  private Vec3 baseVertPos[];
  private UndoRecord undo;
  private final Compound3DManipulator manipulator;
  private boolean tooltipsEnabled, tooltipsAdded;

  private static BToolTip ROTATE_TIP = new BToolTip(Translate.text("moveScaleRotateMeshTool.rotateTipText"));
  private static BToolTip MOVE_TIP = new BToolTip(Translate.text("moveScaleRotateMeshTool.moveTipText"));
  private static BToolTip SCALE_TIP = new BToolTip(Translate.text("moveScaleRotateMeshTool.scaleTipText"));

  public MoveScaleRotateMeshTool(EditingWindow fr, MeshEditController controller)
  {
    super(fr, controller);
    initButton("moveScaleRotate");
    manipulator = new Compound3DManipulator();
    manipulator.addEventLink(HandlePressedEvent.class, this, "handlePressed");
    manipulator.addEventLink(HandleDraggedEvent.class, this, "handleDragged");
    manipulator.addEventLink(HandleReleasedEvent.class, this, "handleReleased");
  }

  public int whichClicks()
  {
    return ALL_CLICKS+HANDLE_CLICKS;
  }

  public boolean allowSelectionChanges()
  {
    return !dragInProgress;
  }

  public String getToolTipText()
  {
    return Translate.text("moveScaleRotateMeshTool.tipText");
  }

  public void drawOverlay(ViewerCanvas view)
  {
    BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
    if (!dragInProgress && manipulator.getViewMode() == Compound3DManipulator.NPQ_MODE && selectionBounds != null)
    {
      // Calculate the axis directions.

      Vec3 avgNorm = new Vec3();
      int selection[] = controller.getSelectionDistance();
      Mesh mesh = (Mesh) controller.getObject().getObject();
      Vec3 normal[] = mesh.getNormals();
      for (int i = 0; i < selection.length; i++)
        if (selection[i] == 0)
          avgNorm.add(normal[i]);
      avgNorm.normalize();
      if (avgNorm.length2() == 0.0)
        avgNorm = Vec3.vx();
      Vec3 updir = Vec3.vx();
      if (Math.abs(updir.dot(avgNorm)) < 0.9)
        updir = avgNorm.cross(updir);
      else
        updir = avgNorm.cross(Vec3.vy());
      updir.normalize();
      manipulator.setNPQAxes(avgNorm, updir, avgNorm.cross(updir));
    }
    manipulator.draw(view, selectionBounds);
    if (!dragInProgress)
    {
      if (selectionBounds != null)
        theWindow.setHelpText(Translate.text("moveScaleRotateMeshTool.helpText"));
      else
        theWindow.setHelpText(Translate.text("moveScaleRotateMeshTool.errorText"));
    }
  }

  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
    dragInProgress = false;
    if (selectionBounds != null)
      dragInProgress = manipulator.mousePressed(e, view, selectionBounds);
  }

  public void mousePressedOnHandle(WidgetMouseEvent e, ViewerCanvas view, int obj, int handle)
  {
    Mesh mesh = (Mesh) controller.getObject().getObject();
    Vec3 vert = mesh.getVertices()[handle].r;
    BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
    if (selectionBounds != null)
    {
      manipulator.mousePressedOnHandle(e, view, selectionBounds, vert);
      dragInProgress = true;
    }
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
    baseVertPos = mesh.getVertexPositions();
  }

  protected void handleDragged(HandleDraggedEvent ev)
  {
    if (undo == null)
    {
      Mesh mesh = (Mesh) controller.getObject().getObject();
      undo = new UndoRecord(theWindow, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {mesh, mesh.getVertexPositions()});
    }

    transformMesh(ev.getTransform());
    theWindow.updateImage();
    if (ev.getHandleType() == Compound3DManipulator.MOVE)
    {
      Vec3 drag = ev.getTransform().times(new Vec3());
      theWindow.setHelpText(Translate.text("reshapeMeshTool.dragText",
          Math.round(drag.x*1e5)/1e5+", "+Math.round(drag.y*1e5)/1e5+", "+Math.round(drag.z*1e5)/1e5));
    }
    else if (ev.getHandleType() == Compound3DManipulator.ROTATE)
      theWindow.setHelpText(Translate.text("rotateMeshTool.dragText", Double.toString(Math.round(ev.getRotationAngle()*1e5*180.0/Math.PI)/1e5)));
    else if (ev.getAxis() == Compound3DManipulator.UV && !ev.getMouseEvent().isShiftDown())
      theWindow.setHelpText(Translate.text("scaleMeshTool.dragText", Math.round(ev.getPrimaryScale()*1e5)/1e5+", "+Math.round(ev.getSecondaryScale()*1e5)/1e5));
    else
      theWindow.setHelpText(Translate.text("scaleMeshTool.dragText", Double.toString(Math.round(ev.getPrimaryScale()*1e5)/1e5)));
  }

  protected void handleReleased(HandleReleasedEvent ev)
  {
    if (undo != null)
      theWindow.setUndoRecord(undo);
    theWindow.updateImage();
    undo = null;
    baseVertPos = null;
    dragInProgress = false;
  }

  /**
   * Apply a transformation to the mesh.
   */

  private void transformMesh(Mat4 transform)
  {
    int selected[] = controller.getSelectionDistance();
    Vec3 v[] = new Vec3 [baseVertPos.length];
    for (int i = 0; i < v.length; i++)
      {
        if (selected[i] == 0)
          v[i] = transform.times(baseVertPos[i]).minus(baseVertPos[i]);
        else
          v[i] = new Vec3();
      }
    if (theFrame instanceof MeshEditorWindow)
      ((MeshEditorWindow) theFrame).adjustDeltas(v);
    for (int i = 0; i < v.length; i++)
      v[i].add(baseVertPos[i]);
    Mesh mesh = (Mesh) controller.getObject().getObject();
    mesh.setVertexPositions(v);
    controller.objectChanged();
  }

  public void keyPressed(KeyPressedEvent e, ViewerCanvas view)
  {
    if (e.getKeyCode() == KeyEvent.VK_W)
    {
      // Change the axis mode.

      ViewMode mode = manipulator.getViewMode();
      if (mode == Compound3DManipulator.XYZ_MODE)
        manipulator.setViewMode(Compound3DManipulator.UV_MODE);
      else if (mode == Compound3DManipulator.UV_MODE)
        manipulator.setViewMode(Compound3DManipulator.NPQ_MODE);
      else
        manipulator.setViewMode(Compound3DManipulator.XYZ_MODE);
      theWindow.updateImage();
      return;
    }
    if (e.getKeyCode() == KeyEvent.VK_F1)
    {
      // Enable tooltips.

      tooltipsEnabled = !tooltipsEnabled;
      if (tooltipsEnabled && !tooltipsAdded)
      {
        ViewerCanvas allViews[] = ((MeshEditorWindow) theWindow).getAllViews();
        for (int i = 0; i < allViews.length; i++)
          allViews[i].addEventLink(ToolTipEvent.class, this, "showToolTip");
        tooltipsAdded = true;
      }
      if (!tooltipsEnabled)
        BToolTip.hide();
      return;
    }

    // Handle arrow keys.

    Mesh mesh = (Mesh) controller.getObject().getObject();
    int i, selectDist[] = controller.getSelectionDistance();
    int key = e.getKeyCode();
    double dx, dy;

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
    baseVertPos = mesh.getVertexPositions();
    for (i = 0; i < baseVertPos.length && selectDist[i] != 0; i++);
    if (i == baseVertPos.length)
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
    Vec3 drag;
    if (e.isControlDown())
      drag = view.getCamera().getCameraCoordinates().getZDirection().times(-dy*0.01);
    else
      drag = view.getCamera().findDragVector(baseVertPos[i], dx, dy);
    theWindow.setUndoRecord(new UndoRecord(theWindow, false, UndoRecord.COPY_VERTEX_POSITIONS, new Object [] {mesh, baseVertPos}));
    transformMesh(Mat4.translation(drag.x, drag.y, drag.z));
    theWindow.updateImage();
  }

  private void showToolTip(ToolTipEvent ev)
  {
    if (!tooltipsEnabled)
      return;
    ViewerCanvas view = (ViewerCanvas) ev.getWidget();
    HandleType type = manipulator.getHandleTypeAtLocation(ev.getPoint(), view, findSelectionBounds(view.getCamera()));
    if (type == Compound3DManipulator.MOVE)
      MOVE_TIP.processEvent(ev);
    else if (type == Compound3DManipulator.ROTATE)
      ROTATE_TIP.processEvent(ev);
    else if (type == Compound3DManipulator.SCALE)
      SCALE_TIP.processEvent(ev);
    else
      BToolTip.hide();
  }
}
