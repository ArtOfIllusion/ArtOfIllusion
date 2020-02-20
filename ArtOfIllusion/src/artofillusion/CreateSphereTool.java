/* Copyright (C) 1999-2008 by Peter Eastman
   Changes Copyright (C) 2016, 2019 by Petri Ihalainen
   Changes copyright (C) 2020 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import java.awt.*;

/** CreateSphereTool is an EditingTool used for creating Sphere objects. */

public class CreateSphereTool extends EditingTool
{
  static int counter = 1;
  private boolean equilateral, centered;
  private Point clickPoint;
  private ObjectInfo objInfo;
  private Vec3 ydir, zdir;

  public CreateSphereTool(LayoutWindow fr)
  {
    super(fr);
    initButton("sphere");
  }

  @Override
  public void activate()
  {
    super.activate();
    theWindow.setHelpText(Translate.text("createSphereTool.helpText"));
  }

  @Override
  public int whichClicks()
  {
    return ALL_CLICKS;
  }

  @Override
  public String getToolTipText()
  {
    return Translate.text("createSphereTool.tipText");
  }

  @Override
  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    clickPoint  = e.getPoint();
    equilateral = e.isShiftDown();
    centered    = e.isControlDown();
    ydir = Vec3.vy();
    zdir = Vec3.vz();
  }

  @Override
  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    Camera cam = view.getCamera();
    Point dragPoint = e.getPoint();
    ydir.set(cam.getCameraCoordinates().getUpDirection());
    zdir.set(cam.getCameraCoordinates().getZDirection());
    zdir.scale(-1.0);

    if (objInfo == null)
    {
      // Create the initial sphere, if the mouse has moved enough. The limit is there to reduce 
      // the probability of accidentally creating zero size objects.

      if (Math.abs(dragPoint.x-clickPoint.x) + Math.abs(dragPoint.y-clickPoint.y) > 3)
      {
        Scene theScene = ((LayoutWindow) theWindow).getScene();
        objInfo = new ObjectInfo(new Sphere(1.0, 1.0, 1.0), new CoordinateSystem(), "Sphere "+(counter++));
        objInfo.addTrack(new PositionTrack(objInfo), 0);
        objInfo.addTrack(new RotationTrack(objInfo), 1);
        UndoRecord undo = new UndoRecord(theWindow, false);
        int sel[] = ((LayoutWindow) theWindow).getSelectedIndices();
        ((LayoutWindow) theWindow).addObject(objInfo, undo);
        undo.addCommand(UndoRecord.SET_SCENE_SELECTION, sel);
        theWindow.setUndoRecord(undo);
        ((LayoutWindow) theWindow).setSelection(theScene.getNumObjects()-1);
      }
      else
        return;
    }

    // Determine the size and position for the sphere.

    Vec3 v1, v2, v3, orig;
    double xsize, ysize, zsize;

    if (equilateral)
    {
      if (Math.abs(dragPoint.x-clickPoint.x) > Math.abs(dragPoint.y-clickPoint.y))
      {
        if (dragPoint.y < clickPoint.y)
          dragPoint.y = clickPoint.y - Math.abs(dragPoint.x-clickPoint.x);
        else
          dragPoint.y = clickPoint.y + Math.abs(dragPoint.x-clickPoint.x);
      }
      else
      {
        if (dragPoint.x < clickPoint.x)
          dragPoint.x = clickPoint.x - Math.abs(dragPoint.y-clickPoint.y);
        else
          dragPoint.x = clickPoint.x + Math.abs(dragPoint.y-clickPoint.y);
      }
    }
    v1 = cam.convertScreenToWorld(clickPoint, view.getDistToPlane());
    v2 = cam.convertScreenToWorld(new Point(dragPoint.x, clickPoint.y), view.getDistToPlane());
    v3 = cam.convertScreenToWorld(dragPoint, view.getDistToPlane());

    if (centered)
    {
      orig  = v1;
      xsize = v2.minus(v1).length()*2.0; 
      ysize = v2.minus(v3).length()*2.0;
    }
    else
    {
      orig  = v1.plus(v3).times(0.5);
      xsize = v2.minus(v1).length(); 
      ysize = v2.minus(v3).length();
    }
    zsize = Math.min(xsize, ysize);

    // Update the size and position, and redraw the display.

    ((Sphere) objInfo.getObject()).setSize(xsize, ysize, zsize);
    objInfo.getCoords().setOrigin(orig);
    objInfo.getCoords().setOrientation(zdir, ydir);
    objInfo.clearCachedMeshes();
    theWindow.setModified();
    theWindow.updateImage();
  }

  @Override
  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    objInfo = null;
  }
}
