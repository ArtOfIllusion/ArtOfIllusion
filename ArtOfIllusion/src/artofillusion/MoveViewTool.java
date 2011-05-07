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

/** MoveViewTool is an EditingTool used for moving the viewpoint. */

public class MoveViewTool extends EditingTool
{
  private Point clickPoint;
  private Mat4 viewToWorld;
  private Vec3 clickPos;
  private boolean controlDown;
  private CoordinateSystem oldCoords;
  private double oldScale;
  
  public MoveViewTool(EditingWindow fr)
  {
    super(fr);
    initButton("moveView");
  }
  
  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("moveViewTool.helpText"));
  }

  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  public boolean hilightSelection()
  {
/*    if (theWindow instanceof LayoutWindow)
      return false;
    else*/
      return true;
  }

  public String getToolTipText()
  {
    return Translate.text("moveViewTool.tipText");
  }

  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    Camera cam = view.getCamera();

    controlDown = e.isControlDown();
    clickPoint = e.getPoint();
    clickPos = cam.convertScreenToWorld(clickPoint, cam.getDistToScreen());
    oldCoords = cam.getCameraCoordinates().duplicate();
    viewToWorld = cam.getViewToWorld();
    oldScale = view.getScale();
  }

  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    Camera cam = view.getCamera();
    Point dragPoint = e.getPoint();
    CoordinateSystem c = oldCoords.duplicate();
    int dx, dy;
    double dist;
    Vec3 move;
    Mat4 m;
    
    dx = dragPoint.x-clickPoint.x;
    dy = dragPoint.y-clickPoint.y;
    cam.setCameraCoordinates(c);
    if (controlDown)
      {
        if (view.isPerspective())
          {
            move = cam.findDragVector(clickPos, 0, dy);
            dist = dy > 0 ? -move.length() : move.length();
            move = (viewToWorld.timesDirection(Vec3.vz())).times(dist*2.0);
          }
        else
          {
            if (dy < 0.0)
              view.setScale(oldScale/(1.0-dy*0.01));
            else
              view.setScale(oldScale*(1.0+dy*0.01));
            move = new Vec3();
          }
      }
    else
      {
        if (e.isShiftDown())
          {
            if (Math.abs(dx) > Math.abs(dy))
              dy = 0;
            else
              dx = 0;
          }
        move = cam.findDragVector(clickPos, dx, dy);
      }
    m = Mat4.translation(-move.x, -move.y, -move.z);
    c.transformOrigin(m);
    cam.setCameraCoordinates(c);
    view.viewChanged(false);
    view.repaint();
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    mouseDragged(e, view);
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