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

/** ScaleObjectTool is an EditingTool used for resizing objects in a scene.  For convenience, it also
    allows users to move objects by clicking on the object itself rather than on a handle.*/

public class ScaleObjectTool extends EditingTool
{
  static final int TOP = 1;
  static final int BOTTOM = 2;
  static final int LEFT = 4;
  static final int RIGHT = 8;

  static final int POSITIONS_FIXED = 0;
  static final int POSITIONS_SCALE = 1;
  
  static final int OPMODE_SCALE = 0;
  static final int OPMODE_MOVE = 1;

  private BoundingBox bounds[];
  private Point clickPoint;
  private Vec3 objectPos[], scaleCenter[];
  private Object3D oldObj[];
  private CoordinateSystem oldCoords[];
  private Vector<ObjectInfo> toMove;
  private double halfx, halfy, centerx, centery;
  private ObjectInfo clickedObject;
  private int whichSides;
  private int opmode;
  private int haxis[], vaxis[], haxisDir[], vaxisDir[], scaleAround = POSITIONS_FIXED;
  private boolean scaleAll, dragged, applyToChildren = true;
  
  public ScaleObjectTool(EditingWindow fr)
  {
    super(fr);
    initButton("resize");
  }

  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("scaleObjectTool.helpText"));
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
    return Translate.text("scaleObjectTool.tipText");
  }
  
  public void mousePressedOnHandle(WidgetMouseEvent e, ViewerCanvas view, int obj, int handle)
  {
    Scene theScene = theWindow.getScene();
    Camera cam = view.getCamera();
    Rectangle r;
    Vec3 screenx, screeny, screenz;
    int i, sel[];

    opmode = OPMODE_SCALE;
    toMove = new Vector<ObjectInfo>();
    clickedObject = theScene.getObject(obj);
    if (applyToChildren)
      sel = theScene.getSelectionWithChildren();
    else
      sel = theScene.getSelection();
    for (i = 0; i < sel.length; i++)
      toMove.addElement(theScene.getObject(sel[i]));
    bounds = new BoundingBox [toMove.size()];
    scaleCenter = new Vec3 [toMove.size()];
    haxis = new int [toMove.size()];
    vaxis = new int [toMove.size()];
    haxisDir = new int [toMove.size()];
    vaxisDir = new int [toMove.size()];
    objectPos = new Vec3 [toMove.size()];
    for (i = 0; i < objectPos.length; i++)
      {
        ObjectInfo info = (ObjectInfo) toMove.elementAt(i);
        objectPos[i] = info.getCoords().getOrigin();
      }
    
    // Figure out the correspondence between the object's x, y, and z axes, on the
    // horizontal and vertical axes on the screen.
    
    for (i = 0; i < bounds.length; i++)
      {
        bounds[i] = ((ObjectInfo) toMove.elementAt(i)).getBounds();
        cam.setObjectTransform(((ObjectInfo) toMove.elementAt(i)).getCoords().fromLocal());
        screenx = cam.getObjectToView().timesDirection(Vec3.vx());
        screeny = cam.getObjectToView().timesDirection(Vec3.vy());
        screenz = cam.getObjectToView().timesDirection(Vec3.vz());
        if (Math.abs(screenx.x) > Math.abs(screeny.x))
          {
            if (Math.abs(screenz.x) > Math.abs(screenx.x))
              haxis[i] = 2;
            else
              haxis[i] = 0;
          }
        else
          {
            if (Math.abs(screenz.x) > Math.abs(screeny.x))
              haxis[i] = 2;
            else
              haxis[i] = 1;
          }
        if (Math.abs(screenx.y) > Math.abs(screeny.y))
          {
            if (Math.abs(screenz.y) > Math.abs(screenx.y))
              vaxis[i] = 2;
            else
              vaxis[i] = 0;
          }
        else
          {
            if (Math.abs(screenz.y) > Math.abs(screeny.y))
              vaxis[i] = 2;
            else
              vaxis[i] = 1;
          }
        if (vaxis[i] == haxis[i])
          vaxis[i] = (vaxis[i]+1)%3;
        Vec3 dirs[] = new Vec3 [] {screenx, screeny, screenz};
        haxisDir[i] = (dirs[haxis[i]].x > 0.0 ? 1 : -1);
        vaxisDir[i] = (dirs[vaxis[i]].y > 0.0 ? 1 : -1);
      }
    
    // Figure out how the position of each object will scale.
    
    if (e.isControlDown())
      {
        if (scaleAround == POSITIONS_FIXED)
          for (i = 0; i < scaleCenter.length; i++)
            scaleCenter[i] = ((ObjectInfo) toMove.elementAt(i)).getCoords().getOrigin();
        else
          for (i = 0; i < scaleCenter.length; i++)
            scaleCenter[i] = clickedObject.getCoords().getOrigin();
        for (i = 0; i < scaleCenter.length; i++)
          scaleCenter[i] = ((ObjectInfo) toMove.elementAt(i)).getCoords().toLocal().times(scaleCenter[i]);
        cam.setObjectTransform(clickedObject.getCoords().fromLocal());
        r = cam.findScreenBounds(clickedObject.getBounds());
        halfx = r.width/2.0;
        halfy = r.height/2.0;
        centerx = r.x+halfx;
        centery = r.y+halfy;
      }
    else
      {
        if (scaleAround == POSITIONS_FIXED)
          for (i = 0; i < scaleCenter.length; i++)
            {
              ObjectInfo info = (ObjectInfo) toMove.elementAt(i);
              scaleCenter[i] = findBorderPos(info.getBounds(), 
                info.getCoords().getOrigin(), haxis[i], vaxis[i], haxisDir[i], vaxisDir[i], handle);
            }
        else
          {
            for (i = 0; toMove.elementAt(i) != clickedObject; i++);
            Vec3 center = findBorderPos(clickedObject.getBounds(), 
                clickedObject.getCoords().getOrigin(), haxis[i], vaxis[i], haxisDir[i], vaxisDir[i], handle);
            center = clickedObject.getCoords().fromLocal().times(center);
            for (i = 0; i < scaleCenter.length; i++)
              scaleCenter[i] = ((ObjectInfo) toMove.elementAt(i)).getCoords().toLocal().times(center);
          }
        cam.setObjectTransform(clickedObject.getCoords().fromLocal());
        r = cam.findScreenBounds(clickedObject.getBounds());
        halfx = r.width/2.0;
        halfy = r.height/2.0;
        if (handle == 0 || handle == 3 || handle == 5)
          centerx = r.x+r.width;
        else if (handle == 1 || handle == 6)
          centerx = r.x+halfx;
        else
          centerx = r.x;
        if (handle == 0 || handle == 1 || handle == 2)
          centery = r.y+r.height;
        else if (handle == 3 || handle == 4)
          centery = r.y+halfy;
        else
          centery = r.y;
        halfx *= 2;
        halfy *= 2;
      }
    dragged = false;
    scaleAll = e.isShiftDown();
    whichSides = 0;
    if (handle < 3)
      whichSides += TOP;
    if (handle > 4)
      whichSides += BOTTOM;
    if (handle == 0 || handle == 3 || handle == 5)
      whichSides += LEFT;
    if (handle == 2 || handle == 4 || handle == 7)
      whichSides += RIGHT;
  }
  
  public void mousePressedOnObject(WidgetMouseEvent e, ViewerCanvas view, int obj)
  {
    Scene theScene = theWindow.getScene();
    int i, sel[];

    opmode = OPMODE_MOVE;
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
        ObjectInfo info = (ObjectInfo) toMove.elementAt(i);
        objectPos[i] = info.getCoords().getOrigin();
      }
    clickPoint = e.getPoint();
    dragged = false;
  }
  
  /** Given a bounding box, the definitions of its horizontal and vertical axes, and
      the index of a handle, return the corresponding point on the border of the box. */
  
  private Vec3 findBorderPos(BoundingBox bb, Vec3 center, int h, int v, int hdir, int vdir, int handle)
  {
    double minh, centerh, maxh, minv, centerv, maxv;
    
    // Translate from x,y,z to h,v.
    
    if (h == 0)
      {
        minh = bb.minx;
        centerh = center.x;
        maxh = bb.maxx;
      }
    else if (h == 1)
      {
        minh = bb.miny;
        centerh = center.y;
        maxh = bb.maxy;
      }
    else
      {
        minh = bb.minz;
        centerh = center.z;
        maxh = bb.maxz;
      }
    if (v == 0)
      {
        minv = bb.minx;
        centerv = center.x;
        maxv = bb.maxx;
      }
    else if (v == 1)
      {
        minv = bb.miny;
        centerv = center.y;
        maxv = bb.maxy;
      }
    else
      {
        minv = bb.minz;
        centerv = center.z;
        maxv = bb.maxz;
      }
    if (hdir == -1)
      {
        double swap = minh;
        minh = maxh;
        maxh = swap;
      }
    if (vdir == -1)
      {
        double swap = minv;
        minv = maxv;
        maxv = swap;
      }
    
    // Find the appropriate point.
    
    double borderh, borderv;
    if (handle == 0 || handle == 3 || handle == 5)
      borderh = minh;
    else if (handle == 1 || handle == 6)
      borderh = centerh;
    else
      borderh = maxh;
    if (handle == 0 || handle == 1 || handle == 2)
      borderv = minv;
    else if (handle == 3 || handle == 4)
      borderv = centerv;
    else
      borderv = maxv;
    
    // Translate back to x,y,z.
    
    Vec3 pos = new Vec3(center);
    if (h == 0)
      pos.x = borderh;
    else if (v == 0)
      pos.x = borderv;
    if (h == 1)
      pos.y = borderh;
    else if (v == 1)
      pos.y = borderv;
    if (h == 2)
      pos.z = borderh;
    else if (v == 2)
      pos.z = borderv;
    return pos;
  }
  
  public void mouseDraggedMoveOp(final WidgetMouseEvent e, final ViewerCanvas view)
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
            ObjectInfo info = (ObjectInfo) toMove.elementAt(i);
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
        ObjectInfo info = (ObjectInfo) toMove.elementAt(i);
        c = info.getCoords();
        c.setOrigin(objectPos[i].plus(v));
      }
    theWindow.setModified();
    theWindow.updateImage();
    theWindow.setHelpText(Translate.text("moveObjectTool.dragText", 
      Math.round(v.x*1e5)/1e5+", "+Math.round(v.y*1e5)/1e5+", "+Math.round(v.z*1e5)/1e5));
  }
  
  public void mouseDraggedScaleOp(WidgetMouseEvent e, ViewerCanvas view)
  {
    Scene theScene = theWindow.getScene();
    Point dragPoint = e.getPoint();
    double size, hscale, vscale, scale[] = new double [3];
    
    if (!dragged)
    {
      oldObj = new Object3D [toMove.size()];
      oldCoords = new CoordinateSystem [toMove.size()];
      UndoRecord undo;
      theWindow.setUndoRecord(undo = new UndoRecord(theWindow, false));
      for (int i = 0; i < toMove.size(); i++)
      {
        ObjectInfo info = (ObjectInfo) toMove.elementAt(i);
        oldObj[i] = info.getObject().duplicate();
        oldCoords[i] = info.getCoords().duplicate();
        undo.addCommand(UndoRecord.COPY_COORDS, new Object [] {info.getCoords(), oldCoords[i]});
        undo.addCommand(UndoRecord.COPY_OBJECT, new Object [] {info.getObject(), oldObj[i]});
      }
      dragged = true;
    }
    if (scaleAll)
      hscale = vscale = 0.0;
    else
      hscale = vscale = 1.0;
    if ((whichSides & TOP) > 0)
    {
      size = centery-dragPoint.y;
      if (size < 1.0)
        size = 1.0;
      vscale = ((double) size) / ((double) halfy);
    }
    if ((whichSides & BOTTOM) > 0)
    {
      size = dragPoint.y-centery;
      if (size < 1.0)
        size = 1.0;
      vscale = ((double) size) / ((double) halfy);
    }
    if ((whichSides & LEFT) > 0)
    {
      size = centerx-dragPoint.x;
      if (size < 1.0)
        size = 1.0;
      hscale = ((double) size) / ((double) halfx);
    }
    if ((whichSides & RIGHT) > 0)
    {
      size = dragPoint.x-centerx;
      if (size < 1.0)
        size = 1.0;
      hscale = ((double) size) / ((double) halfx);
    }
    for (int i = 0; i < toMove.size(); i++)
    {
      if (scaleAll)
        scale[0] = scale[1] = scale[2] = Math.max(hscale, vscale);
      else
      {
        scale[0] = scale[1] = scale[2] = 1.0;
        scale[haxis[i]] = hscale;
        scale[vaxis[i]] = vscale;
      }
      Vec3 oldsize = bounds[i].getSize();
      ObjectInfo info = (ObjectInfo) toMove.elementAt(i);
      Object3D obj = info.getObject();
      obj.copyObject(oldObj[i]);
      obj.setSize(scale[0]*oldsize.x, scale[1]*oldsize.y, scale[2]*oldsize.z);
      Vec3 offset = new Vec3(scaleCenter[i]);
      offset.x *= 1.0-scale[0];
      offset.y *= 1.0-scale[1];
      offset.z *= 1.0-scale[2];
      info.getCoords().setOrigin(oldCoords[i].fromLocal().times(offset));
      theScene.objectModified(obj);
    }
    theWindow.setModified();
    theWindow.updateImage();
    if (scaleAll)
      theWindow.setHelpText(Translate.text("scaleMeshTool.dragText", Double.toString(Math.round(Math.max(hscale, vscale)*1e5)/1e5)));
    else if (whichSides == RIGHT || whichSides == LEFT)
      theWindow.setHelpText(Translate.text("scaleMeshTool.dragText", Double.toString(Math.round(hscale*1e5)/1e5)));
    else if (whichSides == TOP || whichSides == BOTTOM)
      theWindow.setHelpText(Translate.text("scaleMeshTool.dragText", Double.toString(Math.round(vscale*1e5)/1e5)));
    else
      theWindow.setHelpText(Translate.text("scaleMeshTool.dragText", Math.round(hscale*1e5)/1e5+", "+Math.round(vscale*1e5)/1e5));
  }
  
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    if (opmode == OPMODE_SCALE)
      mouseDraggedScaleOp(e, view);
    else
      mouseDraggedMoveOp(e, view);
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    theWindow.getScene().applyTracksAfterModification(toMove);
    toMove = null;
    objectPos = null;
    oldObj = null;
    oldCoords = null;
    bounds = null;
    scaleCenter = null;
    theWindow.updateImage();
    theWindow.setHelpText(Translate.text("scaleObjectTool.helpText"));
  }
  
  public void iconDoubleClicked()
  {
    BCheckBox childrenBox = new BCheckBox(Translate.text("applyToUnselectedChildren"), applyToChildren);
    BComboBox centerChoice = new BComboBox(new String [] {
      Translate.text("remainFixed"),
      Translate.text("scaleWithObjects")
    });
    centerChoice.setSelectedIndex(scaleAround);
    RowContainer row = new RowContainer();
    row.add(Translate.label("objectPositions"));
    row.add(centerChoice);
    ComponentsDialog dlg = new ComponentsDialog(theFrame, Translate.text("resizeToolTitle"), 
                new Widget [] {childrenBox, row}, new String [] {null, null});
    if (!dlg.clickedOk())
      return;
    applyToChildren = childrenBox.getState();
    scaleAround = centerChoice.getSelectedIndex();
  }
}