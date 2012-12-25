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
import artofillusion.ui.*;
import buoy.event.*;
import java.awt.*;

/** RotateViewTool is an EditingTool for rotating the viewpoint around the origin. */

public class RotateViewTool extends EditingTool
{
  private static final double DRAG_SCALE = 0.01;

  private Point clickPoint;
  private Mat4 viewToWorld;
  private boolean controlDown;
  private CoordinateSystem oldCoords;
  private Vec3 rotationCenter;
  
  public RotateViewTool(EditingWindow fr)
  {
    super(fr);
    initButton("rotateView");
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("rotateViewTool.helpText"));
  }

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public boolean hilightSelection()
  {
      return true;
  }

  public String getToolTipText()
  {
    return Translate.text("rotateViewTool.tipText");
  }

  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    Camera cam = view.getCamera();

    controlDown = e.isControlDown();
    clickPoint = e.getPoint();
    oldCoords = cam.getCameraCoordinates().duplicate();
    viewToWorld = cam.getViewToWorld();
    
    // Find the center point to rotate around.

    rotationCenter = view.getRotationCenter();
    if (rotationCenter == null)
      rotationCenter = view.getDefaultRotationCenter();
  }

  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    // Compute the vertical axis to rotate around.

    Vec3 cameray = viewToWorld.timesDirection(Vec3.vy());
    Vec3 cameraz = viewToWorld.timesDirection(Vec3.vz());
    Vec3 vertical = cameray.times(cameray.y).plus(cameraz.times(cameraz.y));
    if (vertical.length2() < 1e-5)
      vertical = cameray;
    else
      vertical.normalize();

    // Compute the rotation matrix.

    Point dragPoint = e.getPoint();
    int dx = dragPoint.x-clickPoint.x;
    int dy = dragPoint.y-clickPoint.y;
    Mat4 rotation;
    if (controlDown)
      rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vz()), -dx*DRAG_SCALE);
    else if (e.isShiftDown())
      {
        if (Math.abs(dx) > Math.abs(dy))
          rotation = Mat4.axisRotation(vertical, -dx*DRAG_SCALE);
        else
          rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vx()), dy*DRAG_SCALE);
      }
    else
      {
        rotation = Mat4.axisRotation(viewToWorld.timesDirection(Vec3.vx()), dy*DRAG_SCALE);
        rotation = Mat4.axisRotation(vertical, -dx*DRAG_SCALE).times(rotation);
      }
    if (!rotation.equals(Mat4.identity()))
      {
        CoordinateSystem c = oldCoords.duplicate();
        c.transformCoordinates(Mat4.translation(-rotationCenter.x, -rotationCenter.y, -rotationCenter.z));
        c.transformCoordinates(rotation);
        c.transformCoordinates(Mat4.translation(rotationCenter.x, rotationCenter.y, rotationCenter.z));
        view.getCamera().setCameraCoordinates(c);
        view.viewChanged(false);
        view.repaint();
      }
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    mouseDragged(e, view);
    Point dragPoint = e.getPoint();
    if ((dragPoint.x != clickPoint.x || dragPoint.y != clickPoint.y) && view.getBoundCamera() == null)
      view.setOrientation(ViewerCanvas.VIEW_OTHER);
    if (theWindow != null)
      {
        ObjectInfo bound = view.getBoundCamera();
        if (bound != null)
          {
            // This view corresponds to an actual camera in the scene.  Create an undo record, and move any children of
            // the camera.
            
            UndoRecord undo = new UndoRecord(theWindow, false, UndoRecord.COPY_COORDS, new Object [] {bound.getCoords(), oldCoords});
            moveChildren(bound, bound.getCoords().fromLocal().times(oldCoords.toLocal()), undo);
            theWindow.setUndoRecord(undo);
          }
        theWindow.updateImage();
      }
  }
  
  /** This is called recursively to move any children of a bound camera. */
  
  private void moveChildren(ObjectInfo parent, Mat4 transform, UndoRecord undo)
  {
    for (int i = 0; i < parent.getChildren().length; i++)
      {
        CoordinateSystem coords = parent.getChildren()[i].getCoords();
        CoordinateSystem oldCoords = coords.duplicate();
        coords.transformCoordinates(transform);
        undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {coords, oldCoords});
        moveChildren(parent.getChildren()[i], transform, undo);
      }
  }
}