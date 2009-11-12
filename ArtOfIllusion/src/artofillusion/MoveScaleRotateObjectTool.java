/* Copyright (C) 2006-2009 by Peter Eastman

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
import java.awt.*;
import java.util.*;

import buoy.event.*;
import buoy.widget.*;

/**
 * This editing tool presents a compound interface for moving, scaling, and rotating objects.
 */

public class MoveScaleRotateObjectTool extends EditingTool
{
  private boolean dragInProgress, draggingObjects;
  private ArrayList<ObjectInfo> objects;
  private CoordinateSystem originalCoords[];
  private Object3D originalObjects[];
  private ObjectInfo clickedObject;
  private Point clickPoint;
  private Vec3 rotationCenter[];
  private UndoRecord undo;
  private final Compound3DManipulator manipulator;
  private boolean tooltipsEnabled, tooltipsAdded, applyToChildren = true;
  private int scaleAround = POSITIONS_FIXED, rotateAround = PARENT_CENTER;

  private static BToolTip ROTATE_TIP = new BToolTip(Translate.text("moveScaleRotateObjectTool.rotateTipText"));
  private static BToolTip MOVE_TIP = new BToolTip(Translate.text("moveScaleRotateObjectTool.moveTipText"));
  private static BToolTip SCALE_TIP = new BToolTip(Translate.text("moveScaleRotateObjectTool.scaleTipText"));

  private static final int POSITIONS_FIXED = 0;
  private static final int POSITIONS_SCALE = 1;

  private static final int OBJECT_CENTER = 0;
  private static final int PARENT_CENTER = 1;
  private static final int SELECTION_CENTER = 2;

  public MoveScaleRotateObjectTool(LayoutWindow fr)
  {
    super(fr);
    initButton("moveScaleRotate");
    manipulator = new Compound3DManipulator();
    manipulator.addEventLink(HandlePressedEvent.class, this, "handlePressed");
    manipulator.addEventLink(HandleDraggedEvent.class, this, "handleDragged");
    manipulator.addEventLink(HandleReleasedEvent.class, this, "handleReleased");
  }

  public int whichClicks()
  {
    return OBJECT_CLICKS+ALL_CLICKS;
  }

  public boolean allowSelectionChanges()
  {
    return !dragInProgress;
  }

  public String getToolTipText()
  {
    return Translate.text("moveScaleRotateObjectTool.tipText");
  }

  /** Get the LayoutWindow to which this tool belongs. */

  public LayoutWindow getWindow()
  {
    return (LayoutWindow) theWindow;
  }

  public void drawOverlay(ViewerCanvas view)
  {
    BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
    if (!dragInProgress && manipulator.getViewMode() == Compound3DManipulator.NPQ_MODE && selectionBounds != null)
    {
      // Calculate the axis directions.

      ObjectInfo firstObj = getWindow().getSelectedObjects().iterator().next();
      CoordinateSystem coords = firstObj.getCoords();
      manipulator.setNPQAxes(coords.getUpDirection().cross(coords.getZDirection()), coords.getUpDirection(), coords.getZDirection());
    }
    manipulator.draw(view, selectionBounds);
    if (!dragInProgress)
    {
      if (selectionBounds != null)
        theWindow.setHelpText(Translate.text("moveScaleRotateObjectTool.helpText"));
      else
        theWindow.setHelpText(Translate.text("moveScaleRotateObjectTool.errorText"));
    }
  }

  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
    dragInProgress = false;
    draggingObjects = false;
    if (selectionBounds != null)
      dragInProgress = manipulator.mousePressed(e, view, selectionBounds);
  }

  public void mousePressedOnObject(WidgetMouseEvent e, ViewerCanvas view, int obj)
  {
    draggingObjects = true;
    BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
    Rectangle screenBounds = manipulator.findScreenBounds(selectionBounds, view.getCamera());
    handlePressed(manipulator.new HandlePressedEvent(view, Compound3DManipulator.MOVE, Compound3DManipulator.ALL, screenBounds, selectionBounds, e));
    clickedObject = view.getScene().getObject(obj);
    clickPoint = e.getPoint();
  }

  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    if (draggingObjects)
    {
      BoundingBox selectionBounds = findSelectionBounds(view.getCamera());
      Rectangle screenBounds = manipulator.findScreenBounds(selectionBounds, view.getCamera());
      Point dragPoint = e.getPoint();
      int dx = dragPoint.x - clickPoint.x;
      int dy = dragPoint.y - clickPoint.y;
      if (e.isShiftDown() && !e.isControlDown())
        {
          if (Math.abs(dx) > Math.abs(dy))
            dy = 0;
          else
            dx = 0;
        }
      Vec3 v;
      if (e.isControlDown())
        v = view.getCamera().getCameraCoordinates().getZDirection().times(-dy*0.01);
      else
        v = view.getCamera().findDragVector(clickedObject.getCoords().getOrigin(), dx, dy);
      handleDragged(manipulator.new HandleDraggedEvent(view, Compound3DManipulator.MOVE, Compound3DManipulator.ALL, screenBounds, selectionBounds, e, Mat4.translation(v.x, v.y, v.z)));
    }
    else
      manipulator.mouseDragged(e, view);
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    if (draggingObjects)
      handleReleased(null);
    else
      manipulator.mouseReleased(e, view);
  }

  protected void handlePressed(HandlePressedEvent ev)
  {
    objects = new ArrayList<ObjectInfo>();
    int sel[];
    if (applyToChildren)
      sel = getWindow().getSelectionWithChildren();
    else
      sel = getWindow().getSelectedIndices();
    for (int i = 0; i < sel.length; i++)
      objects.add(theWindow.getScene().getObject(sel[i]));
    originalCoords = new CoordinateSystem[objects.size()];
    for (int i = 0; i < originalCoords.length; i++)
      originalCoords[i] = objects.get(i).getCoords();
    if (ev.getHandleType() == Compound3DManipulator.SCALE)
    {
      originalObjects = new Object3D[objects.size()];
      for (int i = 0; i < originalObjects.length; i++)
        originalObjects[i] = objects.get(i).getObject();
    }
    if (ev.getHandleType() == Compound3DManipulator.ROTATE)
    {
      manipulator.setRotateAroundSelectionCenter(rotateAround == SELECTION_CENTER);
      rotationCenter = new Vec3[objects.size()];
      for (int i = 0; i < rotationCenter.length; i++)
        {
          ObjectInfo parent = objects.get(i);
          if (rotateAround == SELECTION_CENTER)
              rotationCenter[i] = new Vec3();
          else
            rotationCenter[i] = originalCoords[i].getOrigin();
          if (rotateAround == PARENT_CENTER)
            while (parent.getParent() != null)
              {
                parent = parent.getParent();
                if (parent.selected)
                  rotationCenter[i] = parent.getCoords().getOrigin();
              }
        }
    }
  }

  protected void handleDragged(HandleDraggedEvent ev)
  {
    if (undo == null)
    {
      undo = new UndoRecord(theWindow, false);
      for (int i = 0; i < originalCoords.length; i++)
      {
        originalCoords[i] = originalCoords[i].duplicate();
        undo.addCommand(UndoRecord.COPY_COORDS, new Object[] {objects.get(i).getCoords(), originalCoords[i]});
        if (ev.getHandleType() == Compound3DManipulator.SCALE)
        {
          originalObjects[i] = originalObjects[i].duplicate();
          undo.addCommand(UndoRecord.COPY_OBJECT, new Object[] {objects.get(i).getObject(), originalObjects[i]});
        }
      }
    }
    transformObjects(ev);
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
    rotationCenter = null;
    dragInProgress = false;
  }

  /**
   * Apply a transformation to the objects.
   */

  private void transformObjects(HandleDraggedEvent ev)
  {
    for (int i = 0; i < objects.size(); i++)
    {
      Mat4 transform = ev.getTransform();
      if (ev.getHandleType() == Compound3DManipulator.SCALE)
      {
        if (scaleAround == POSITIONS_SCALE)
        {
          // Scale the positions.

          CoordinateSystem coords = originalCoords[i].duplicate();
          coords.transformOrigin(transform);
          objects.get(i).getCoords().copyCoords(coords);
        }

        // Work out the scale factors for the size.

        double xscale = 1.0, yscale = 1.0, zscale = 1.0;
        Vec3 xdir = originalCoords[i].fromLocal().timesDirection(transform.timesDirection(new Vec3(1.0, 0.0, 0.0)));
        Vec3 ydir = originalCoords[i].fromLocal().timesDirection(transform.timesDirection(new Vec3(0.0, 1.0, 0.0)));
        Vec3 zdir = originalCoords[i].fromLocal().timesDirection(transform.timesDirection(new Vec3(0.0, 0.0, 1.0)));
        xdir.normalize();
        ydir.normalize();
        zdir.normalize();
        if (ev.getMouseEvent().isShiftDown())
        {
          xscale = yscale = zscale = ev.getPrimaryScale();
        }
        else if (ev.getAxis() == Compound3DManipulator.UV)
        {
          Vec3 uDir = manipulator.getAxisDirection(Compound3DManipulator.U, ev.getView());
          Vec3 vDir = manipulator.getAxisDirection(Compound3DManipulator.V, ev.getView());
          double xudot = Math.abs(xdir.dot(uDir));
          double yudot = Math.abs(ydir.dot(uDir));
          double zudot = Math.abs(zdir.dot(uDir));
          double xvdot = Math.abs(xdir.dot(vDir));
          double yvdot = Math.abs(ydir.dot(vDir));
          double zvdot = Math.abs(zdir.dot(vDir));
          if (xudot >= yudot && xudot >= zudot)
          {
            xscale = ev.getPrimaryScale();
            if (yvdot >= zvdot)
              yscale = ev.getSecondaryScale();
            else
              zscale = ev.getSecondaryScale();
          }
          else if (yudot >= xudot && yudot >= zudot)
          {
            yscale = ev.getPrimaryScale();
            if (xvdot >= zvdot)
              xscale = ev.getSecondaryScale();
            else
              zscale = ev.getSecondaryScale();
          }
          else
          {
            zscale = ev.getPrimaryScale();
            if (xvdot >= yvdot)
              xscale = ev.getSecondaryScale();
            else
              yscale = ev.getSecondaryScale();
          }
        }
        else
        {
          Vec3 scaleDir = manipulator.getAxisDirection(ev.getAxis(), ev.getView());
          double xdot = Math.abs(xdir.dot(scaleDir));
          double ydot = Math.abs(ydir.dot(scaleDir));
          double zdot = Math.abs(zdir.dot(scaleDir));
          if (xdot >= ydot && xdot >= zdot)
            xscale = ev.getPrimaryScale();
          else if (ydot >= xdot && ydot >= zdot)
            yscale = ev.getPrimaryScale();
          else
            zscale = ev.getPrimaryScale();
        }
        Vec3 size = originalObjects[i].getBounds().getSize();
        size.x *= xscale;
        size.y *= yscale;
        size.z *= zscale;
        objects.get(i).getObject().setSize(size.x, size.y, size.z);
        getWindow().getScene().objectModified(objects.get(i).getObject());
      }
      else
      {
        if (ev.getHandleType() == Compound3DManipulator.ROTATE)
        {
          Vec3 center = rotationCenter[i];
          transform = Mat4.translation(center.x, center.y, center.z).times(transform.times(Mat4.translation(-center.x, -center.y, -center.z)));
        }
        CoordinateSystem coords = originalCoords[i].duplicate();
        coords.transformCoordinates(transform);
        objects.get(i).getCoords().copyCoords(coords);
      }
    }
  }

  public void keyPressed(KeyPressedEvent e, ViewerCanvas view)
  {
    if (e.getKeyCode() == KeyEvent.VK_W && e.getModifiersEx() == 0)
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

    // Handle arrow keys.  This is equivalent to dragging the first selected object by one pixel.

    double dx, dy;
    int key = e.getKeyCode();
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
    int sel[];
    if (applyToChildren)
      sel = getWindow().getSelectionWithChildren();
    else
      sel = getWindow().getSelectedIndices();
    if (sel.length == 0)
      return;  // No objects are selected.
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
    Camera cam = view.getCamera();
    CoordinateSystem cameraCoords = cam.getCameraCoordinates();
    Scene theScene = theWindow.getScene();
    Vec3 v;
    if (e.isControlDown())
      v = cameraCoords.getZDirection().times(-dy*0.01);
    else
    {
      Vec3 origin = theScene.getObject(sel[0]).getCoords().getOrigin();
      if (Math.abs(origin.minus(cameraCoords.getOrigin()).dot(cameraCoords.getZDirection())) < 1e-10)
      {
        // The object being moved is in the plane of the camera, so use a slightly
        // different point to avoid dividing by zero.

        origin = origin.plus(cameraCoords.getZDirection().times(cam.getClipDistance()));
      }
      v = cam.findDragVector(origin, dx, dy);
    }
    theWindow.setUndoRecord(undo = new UndoRecord(getWindow(), false));
    ArrayList<ObjectInfo> toMove = new ArrayList<ObjectInfo>();
    for (int i = 0; i < sel.length; i++)
      toMove.add(theScene.getObject(sel[i]));
    for (int i = 0; i < toMove.size(); i++)
    {
      CoordinateSystem c = toMove.get(i).getCoords();
      undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {c, c.duplicate()});
      c.setOrigin(c.getOrigin().plus(v));
    }
    theWindow.getScene().applyTracksAfterModification(toMove);
    theWindow.updateImage();
    undo = null;
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

  /**
   * This method returns a bounding box for the selected objects in view coordinates,
   * or null if nothing is selected.
   */

  protected BoundingBox findSelectionBounds(Camera cam)
  {
    boolean anything = false;
    BoundingBox bounds = null;
    for (ObjectInfo info : getWindow().getSelectedObjects())
    {
      BoundingBox objBounds = info.getBounds().transformAndOutset(info.getCoords().fromLocal());
      if (!anything)
        bounds = objBounds;
      else
        bounds = bounds.merge(objBounds);
      anything = true;
    }
    return (bounds == null ? null : bounds.transformAndOutset(cam.getWorldToView()));
  }

  public void iconDoubleClicked()
  {
    BCheckBox childrenBox = new BCheckBox(Translate.text("applyToUnselectedChildren"), applyToChildren);
    BComboBox centerChoice = new BComboBox(new String [] {
      Translate.text("individualObjectCenters"),
      Translate.text("parentObject"),
      Translate.text("centerOfSelection")
    });
    centerChoice.setSelectedIndex(rotateAround);
    RowContainer rotateRow = new RowContainer();
    rotateRow.add(Translate.label("rotateAround"));
    rotateRow.add(centerChoice);
    BComboBox scaleChoice = new BComboBox(new String [] {
      Translate.text("remainFixed"),
      Translate.text("scaleWithObjects")
    });
    scaleChoice.setSelectedIndex(scaleAround);
    RowContainer scaleRow = new RowContainer();
    scaleRow.add(Translate.label("objectPositions"));
    scaleRow.add(scaleChoice);
    ComponentsDialog dlg = new ComponentsDialog(theFrame, Translate.text("moveRotateResizeToolTitle"),
                new Widget [] {childrenBox, rotateRow, scaleRow}, new String [] {null, null, null});
    if (!dlg.clickedOk())
      return;
    applyToChildren = childrenBox.getState();
    rotateAround = centerChoice.getSelectedIndex();
    scaleAround = scaleChoice.getSelectedIndex();
  }
}
