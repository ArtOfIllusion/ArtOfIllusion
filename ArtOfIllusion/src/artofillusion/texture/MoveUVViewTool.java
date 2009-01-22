/* Copyright (C) 2003-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.*;
import artofillusion.ui.*;
import buoy.event.*;
import java.awt.*;

/** MoveUVViewTool is an EditingTool used for moving the viewpoint in the UV editing window. */

public class MoveUVViewTool extends EditingTool
{
  private Point clickPoint;
  private boolean controlDown;
  private double minu, maxu, minv, maxv;
  private int vwidth, vheight; 

  public MoveUVViewTool(EditingWindow fr)
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
    return true;
  }

  public String getToolTipText()
  {
    return Translate.text("moveViewTool.tipText");
  }

  public void mousePressed(WidgetMouseEvent e, ViewerCanvas view)
  {
    UVMappingViewer uvview = (UVMappingViewer) view;

    controlDown = e.isControlDown();
    clickPoint = e.getPoint();
    
    minu = uvview.getMinU();
    maxu = uvview.getMaxU();
    minv = uvview.getMinV();
    maxv = uvview.getMaxV();
    
    Rectangle d = uvview.getBounds();
    vwidth = d.width;
    vheight = d.height;
  }

  public void mouseDragged(WidgetMouseEvent e, ViewerCanvas view)
  {
    Point dragPoint = e.getPoint();
    UVMappingViewer uvview = (UVMappingViewer) view;
    int dx, dy;

    dx = dragPoint.x-clickPoint.x;
    dy = dragPoint.y-clickPoint.y;
    if (controlDown)
    {
      double factor = Math.pow(1.01, dy);
      double midu = (minu+maxu)/2;
      double midv = (minv+maxv)/2;
      
      double newminu = ((minu - midu)/factor) + midu;
      double newmaxu = ((maxu - midu)/factor) + midu;
      double newminv = ((minv - midv)/factor) + midv;
      double newmaxv = ((maxv - midv)/factor) + midv;
      
      uvview.setParameters(newminu, newmaxu, newminv, newmaxv);
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
      double du = (minu - maxu)*dx/vwidth;
      double dv = (maxv - minv)*dy/vheight;
      uvview.setParameters(minu + du, maxu + du, minv + dv, maxv + dv);
    }
  }

  public void mouseReleased(WidgetMouseEvent e, ViewerCanvas view)
  {
    mouseDragged(e, view);
    theWindow.updateImage();
  }
}