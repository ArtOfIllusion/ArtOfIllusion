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
import buoy.widget.*;
import java.awt.*;
import java.util.Vector;

/** RotateObjectTool is an EditingTool used for rotating objects in a scene. */

public class RotateObjectTool extends EditingTool
{
  static final int X_ONLY = 1;
  static final int Y_ONLY = 2;
  static final int Z_ONLY = 3;
  static final int ALL_AXES = 4;

  static final int OBJECT_CENTER = 0;
  static final int PARENT_CENTER = 1;
  static final int SELECTION_CENTER = 2;

  static final double DRAG_SCALE = Math.PI/360.0;

  Point clickPoint;
  Vector<ObjectInfo> toMove;
  ObjectInfo clickedObject;
  int whichAxes, rotateAround = PARENT_CENTER;
  boolean dragged, applyToChildren = true;
  Vec3 rotAxis, rotationCenter[];
  CoordinateSystem objectCoords[];
  
  public RotateObjectTool(EditingWindow fr)
  {
    super(fr);
    initButton("rotate");
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("rotateObjectTool.helpText"));
  }

  public int whichClicks()
  {
    return OBJECT_CLICKS+HANDLE_CLICKS;
  }

  public boolean allowSelectionChanges()
  {
    return true;
  }

  public String getToolTipText()
  {
    return Translate.text("rotateObjectTool.tipText");
  }
  
  public void mousePressedOnObject(WidgetMouseEvent e, ViewerCanvas view, int obj)
  {
    Scene theScene = theWindow.getScene();
    int i, numSelected = 0, sel[];
    Vec3 center = new Vec3();

    toMove = new Vector<ObjectInfo>();
    clickedObject = theScene.getObject(obj);
    if (applyToChildren)
      sel = theScene.getSelectionWithChildren();
    else
      sel = theScene.getSelection();
    for (i = 0; i < sel.length; i++)
      toMove.addElement(theScene.getObject(sel[i]));
    objectCoords = new CoordinateSystem [toMove.size()];
    rotationCenter = new Vec3 [toMove.size()];
    for (i = 0; i < objectCoords.length; i++)
      {
        ObjectInfo info = (ObjectInfo) toMove.elementAt(i), parent = info;
        objectCoords[i] = info.getCoords().duplicate();
        if (rotateAround == SELECTION_CENTER)
          {
            rotationCenter[i] = center;
            if (info.selected)
              {
                center.add(info.getCoords().getOrigin());
                numSelected++;
              }
          }
        else
          rotationCenter[i] = info.getCoords().getOrigin();
        if (rotateAround == PARENT_CENTER)
          while (parent.getParent() != null)
            {
              parent = parent.getParent();
              if (parent.selected)
                rotationCenter[i] = parent.getCoords().getOrigin();
            }
      }
    if (numSelected > 0)
      center.scale(1.0/numSelected);
    clickPoint = e.getPoint();
    dragged = false;
    whichAxes = ALL_AXES;
  }

  public void mousePressedOnHandle(WidgetMouseEvent e, ViewerCanvas view, int obj, int handle)
  {
    mousePressedOnObject(e, view, obj);
    if (handle == 1 || handle == 6)
      {
        whichAxes = X_ONLY;
        rotAxis = new Vec3(1.0, 0.0, 0.0);
      }
    else if (handle == 3 || handle == 4)
      {
        whichAxes = Y_ONLY;
        rotAxis = new Vec3(0.0, -1.0, 0.0);
      }
    else
      {
        whichAxes = Z_ONLY;
        if (handle < 4)
          rotAxis = new Vec3(0.0, 0.0, 1.0);
        else
          rotAxis = new Vec3(0.0, 0.0, -1.0);
      }
    rotAxis = view.getCamera().getViewToWorld().timesDirection(rotAxis);
  }
  
  public void mouseDragged(final WidgetMouseEvent e, final ViewerCanvas view)
  {
    Point dragPoint = e.getPoint();
    CoordinateSystem c;
    UndoRecord undo;
    double angle;
    int i;
    Vec3 origin;
    Mat4 rotMatrix;

    if (!dragged)
      {
        theWindow.setUndoRecord(undo = new UndoRecord(theWindow, false));
        for (i = 0; i < toMove.size(); i++)
          {
            ObjectInfo info = (ObjectInfo) toMove.elementAt(i);
            c = info.getCoords();
            undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {c, c.duplicate()});
          }
        dragged = true;
      }
    if (whichAxes == X_ONLY)
      angle = (clickPoint.y-dragPoint.y)*DRAG_SCALE;
    else if (whichAxes == Y_ONLY)
      angle = (clickPoint.x-dragPoint.x)*DRAG_SCALE;
    else if (whichAxes == Z_ONLY)
      angle = (dragPoint.x-clickPoint.x)*DRAG_SCALE;
    else
      {
        rotAxis = new Vec3((clickPoint.y-dragPoint.y)*DRAG_SCALE, (dragPoint.x-clickPoint.x)*DRAG_SCALE, 0.0);
        angle = rotAxis.length();
        rotAxis = rotAxis.times(1.0/angle);
        rotAxis = view.getCamera().getViewToWorld().timesDirection(rotAxis);
      }
    if (angle != 0.0)
      {
        for (i = 0; i < toMove.size(); i++)
          {
            ObjectInfo info = (ObjectInfo) toMove.elementAt(i);
            c = info.getCoords();
            origin = rotationCenter[i];
            rotMatrix = Mat4.translation(origin.x, origin.y, origin.z).times(Mat4.axisRotation(rotAxis, angle)).times(Mat4.translation(-origin.x, -origin.y, -origin.z));
            c.copyCoords(objectCoords[i]);
            c.transformCoordinates(rotMatrix);
          }
      }
    theWindow.setModified();
    theWindow.updateImage();
    theWindow.setHelpText(Translate.text("rotateMeshTool.dragText", Double.toString(Math.round(angle*1e5*180.0/Math.PI)/1e5)));
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    theWindow.getScene().applyTracksAfterModification(toMove);
    theWindow.setHelpText(Translate.text("rotateObjectTool.helpText"));
    toMove = null;
    objectCoords = null;
    rotationCenter = null;
    theWindow.updateImage();
  }

  public void keyPressed(KeyPressedEvent e, ViewerCanvas view)
  {
    Scene theScene = theWindow.getScene();
    CoordinateSystem c;
    UndoRecord undo;
    Vec3 origin, center = new Vec3();
    Mat4 rotMatrix;
    int key = e.getKeyCode(), numSelected = 0, sel[];

    // Find the axis to rotate around.
 
    if (e.isControlDown())
      {
        if (key == KeyPressedEvent.VK_UP || key == KeyPressedEvent.VK_RIGHT)
          rotAxis = new Vec3(0.0, 0.0, 1.0);
        else if (key == KeyPressedEvent.VK_DOWN || key == KeyPressedEvent.VK_LEFT)
          rotAxis = new Vec3(0.0, 0.0, -1.0);
        else
          return;
      }
    else if (key == KeyPressedEvent.VK_UP)
      rotAxis = new Vec3(1.0, 0.0, 0.0);
    else if (key == KeyPressedEvent.VK_DOWN)
      rotAxis = new Vec3(-1.0, 0.0, 0.0);
    else if (key == KeyPressedEvent.VK_LEFT)
      rotAxis = new Vec3(0.0, -1.0, 0.0);
    else if (key == KeyPressedEvent.VK_RIGHT)
      rotAxis = new Vec3(0.0, 1.0, 0.0);
    else
      return;
    e.consume();
    rotAxis = view.getCamera().getViewToWorld().timesDirection(rotAxis);

    // Find the rotation center for every object.

    if (applyToChildren)
      sel = theScene.getSelectionWithChildren();
    else
      sel = theScene.getSelection();
    toMove = new Vector<ObjectInfo>();
    for (int i = 0; i < sel.length; i++)
      toMove.addElement(theScene.getObject(sel[i]));
    rotationCenter = new Vec3 [toMove.size()];
    for (int i = 0; i < rotationCenter.length; i++)
      {
        ObjectInfo info = (ObjectInfo) toMove.elementAt(i), parent = info;
        if (rotateAround == SELECTION_CENTER)
          {
            rotationCenter[i] = center;
            if (info.selected)
              {
                center.add(info.getCoords().getOrigin());
                numSelected++;
              }
          }
        else
          rotationCenter[i] = info.getCoords().getOrigin();
        if (rotateAround == PARENT_CENTER)
          while (parent.getParent() != null)
            {
              parent = parent.getParent();
              if (parent.selected)
                rotationCenter[i] = parent.getCoords().getOrigin();
            }
      }
    if (numSelected > 0)
      center.scale(1.0/numSelected);

    // Rotate every object by 0.5 degrees.

    theWindow.setUndoRecord(undo = new UndoRecord(theWindow, false));
    double angle = DRAG_SCALE;
    if (e.isAltDown())
      angle *= 10.0;
    for (int i = 0; i < toMove.size(); i++)
      {
        ObjectInfo info = (ObjectInfo) toMove.elementAt(i);
        c = info.getCoords();
        origin = rotationCenter[i];
        rotMatrix = Mat4.translation(origin.x, origin.y, origin.z).times(Mat4.axisRotation(rotAxis, angle)).times(Mat4.translation(-origin.x, -origin.y, -origin.z));
        undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {c, c.duplicate()});
          c.transformCoordinates(rotMatrix);
      }
    theWindow.getScene().applyTracksAfterModification(toMove);
    theWindow.updateImage();
    theWindow.setHelpText(Translate.text("rotateObjectTool.helpText"));
    toMove = null;
    rotationCenter = null;
  }

  /* Allow the user to set options. */
  
  public void iconDoubleClicked()
  {
    BCheckBox childrenBox = new BCheckBox(Translate.text("applyToUnselectedChildren"), applyToChildren);
    BComboBox centerChoice = new BComboBox(new String [] {
      Translate.text("individualObjectCenters"),
      Translate.text("parentObject"),
      Translate.text("centerOfSelection")
    });
    centerChoice.setSelectedIndex(rotateAround);
    RowContainer row = new RowContainer();
    row.add(Translate.label("rotateAround"));
    row.add(centerChoice);
    ComponentsDialog dlg = new ComponentsDialog(theFrame, Translate.text("rotateToolTitle"), 
                new Widget [] {childrenBox, row}, new String [] {null, null});
    if (!dlg.clickedOk())
      return;
    applyToChildren = childrenBox.getState();
    rotateAround = centerChoice.getSelectedIndex();
  }
}