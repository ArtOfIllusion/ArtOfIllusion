/* Copyright (C) 1999-2009 by Peter Eastman

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
import buoy.widget.*;
import java.awt.*;
import java.util.Vector;

/** MoveObjectTool is an EditingTool used for moving objects in a scene. */

public class MoveObjectTool extends EditingTool
{
  Point clickPoint;
  Vec3 objectPos[];
  Vector<ObjectInfo> toMove;
  ObjectInfo clickedObject;
  boolean dragged, applyToChildren = true;
  
  public MoveObjectTool(EditingWindow fr)
  {
    super(fr);
    initButton("move");
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("moveObjectTool.helpText"));
  }

  public int whichClicks()
  {
    return OBJECT_CLICKS;
  }

  public boolean allowSelectionChanges()
  {
    return true;
  }

  public String getToolTipText()
  {
    return Translate.text("moveObjectTool.tipText");
  }

  public void mousePressedOnObject(WidgetMouseEvent e, ViewerCanvas view, int obj)
  {
    Scene theScene = theWindow.getScene();
    int i, sel[];

    toMove = new Vector<ObjectInfo>();
    clickedObject = theScene.getObject(obj);
    if (applyToChildren)
      sel = theScene.getSelectionWithChildren();
    else
      sel = theScene.getSelection();
    for (i = 0; i < sel.length; i++)
      toMove.addElement(theScene.getObject(sel[i]));
    objectPos = new Vec3 [toMove.size()];
    for (i = 0; i < objectPos.length; i++)
      {
        ObjectInfo info = toMove.elementAt(i);
        objectPos[i] = info.getCoords().getOrigin();
      }
    clickPoint = e.getPoint();
    dragged = false;
  }

  public void mouseDragged(final WidgetMouseEvent e, final ViewerCanvas view)
  {
    Camera cam = view.getCamera();
    Point dragPoint = e.getPoint();
    CoordinateSystem c;
    int i, dx, dy;
    Vec3 v;

    if (!dragged)
      {
        UndoRecord undo;
        theWindow.setUndoRecord(undo = new UndoRecord(theWindow, false));
        for (i = 0; i < toMove.size(); i++)
          {
            ObjectInfo info = toMove.elementAt(i);
            c = info.getCoords();
            undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {c, c.duplicate()});
          }
        dragged = true;
      }
    dx = dragPoint.x - clickPoint.x;
    dy = dragPoint.y - clickPoint.y;
    if (e.isShiftDown() && !e.isControlDown())
      {
        if (Math.abs(dx) > Math.abs(dy))
          dy = 0;
        else
          dx = 0;
      }
    if (e.isControlDown())
      v = cam.getCameraCoordinates().getZDirection().times(-dy*0.01);
    else
      v = cam.findDragVector(clickedObject.getCoords().getOrigin(), dx, dy);
    for (i = 0; i < toMove.size(); i++)
      {
        ObjectInfo info = toMove.elementAt(i);
        c = info.getCoords();
        c.setOrigin(objectPos[i].plus(v));
      }
    theWindow.updateImage();
    theWindow.setHelpText(Translate.text("moveObjectTool.dragText", 
      Math.round(v.x*1e5)/1e5+", "+Math.round(v.y*1e5)/1e5+", "+Math.round(v.z*1e5)/1e5));
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    theWindow.getScene().applyTracksAfterModification(toMove);
    theWindow.setHelpText(Translate.text("moveObjectTool.helpText"));
    toMove = null;
    objectPos = null;
    theWindow.updateImage();
    theWindow.setModified();
  }

  public void keyPressed(KeyPressedEvent e, ViewerCanvas view)
  {
    Scene theScene = theWindow.getScene();
    Camera cam = view.getCamera();
    CoordinateSystem c;
    UndoRecord undo;
    int i, sel[];
    double dx, dy;
    Vec3 v;
    int key = e.getKeyCode();

    // Pressing an arrow key is equivalent to dragging the first selected object by one pixel.
 
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
    if (applyToChildren)
      sel = theScene.getSelectionWithChildren();
    else
      sel = theScene.getSelection();
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
    CoordinateSystem cameraCoords = cam.getCameraCoordinates();
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
    theWindow.setUndoRecord(undo = new UndoRecord(theWindow, false));
    toMove = new Vector<ObjectInfo>();
    for (i = 0; i < sel.length; i++)
      toMove.addElement(theScene.getObject(sel[i]));
    for (i = 0; i < toMove.size(); i++)
    {
      c = toMove.elementAt(i).getCoords();
      undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {c, c.duplicate()});
      c.setOrigin(c.getOrigin().plus(v));
    }
    theWindow.getScene().applyTracksAfterModification(toMove);
    theWindow.updateImage();
  }
  
  /* Allow the user to set options. */
  
  public void iconDoubleClicked()
  {
    BCheckBox childrenBox = new BCheckBox(Translate.text("applyToUnselectedChildren"), applyToChildren);
    ComponentsDialog dlg = new ComponentsDialog(theFrame, Translate.text("moveToolTitle"), 
		new Widget [] {childrenBox}, new String [] {null});
    if (!dlg.clickedOk())
      return;
    applyToChildren = childrenBox.getState();
  }
}