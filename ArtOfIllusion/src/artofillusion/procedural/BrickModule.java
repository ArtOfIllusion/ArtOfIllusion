/* Copyright (C) 2000-2011 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** This is a Module which generates a brick pattern. */

public class BrickModule extends Module
{
  boolean valueOk, gradOk;
  double value, error, gap, offset, height, lastBlur;
  PointInfo point;
  Vec3 gradient;
  
  public BrickModule(Point position)
  {
    super(Translate.text("menu.bricksModule"), new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"X", "(X)"}), 
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Y", "(Y)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Z", "(Z)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Value"})}, 
      position);
    gap = .1;
    offset = 0.5;
    height = 0.5;
    gradient = new Vec3();
  }
  
  /** Get the brick height. */
  
  public double getBrickHeight()
  {
    return height;
  }
  
  /** Set the brick height. */
  
  public void setBrickHeight(double h)
  {
    height = h;
  }
  
  /** Get the gap between bricks. */
  
  public double getGap()
  {
    return gap;
  }
  
  /** Set the gap between bricks. */
  
  public void setGap(double g)
  {
    gap = g;
  }
  
  /** Get the offset for alternating rows. */
  
  public double getOffset()
  {
    return offset;
  }
  
  /** Set the offset for alternating rows. */
  
  public void setOffset(double o)
  {
    offset = o;
  }

  /* New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    point = p;
    valueOk = gradOk = false;
  }
  
  /* Calculate the average value of the function. */

  public double getAverageValue(int which, double blur)
  {
    if (valueOk && blur == lastBlur)
      return value;
    valueOk = true;
    lastBlur = blur;

    double hinv = 1.0/height;
    double xsize = (linkFrom[0] == null) ? 0.5*point.xsize+blur : linkFrom[0].getValueError(linkFromIndex[0], blur);
    double ysize = (linkFrom[1] == null) ? 0.5*hinv*point.ysize+blur : hinv*linkFrom[1].getValueError(linkFromIndex[1], blur);
    double zsize = (linkFrom[2] == null) ? 0.5*point.zsize+blur : linkFrom[2].getValueError(linkFromIndex[2], blur);
    if (xsize >= 0.5 || ysize >= 0.5 || zsize >= 0.5)
      {
        value = 1.0-3.0*gap;
        error = 0.5;
        gradient.set(0.0, 0.0, 0.0);
        gradOk = true;
        return value;
      }
    double x = (linkFrom[0] == null) ? point.x : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    double y = (linkFrom[1] == null) ? hinv*point.y : hinv*linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    double z = (linkFrom[2] == null) ? point.z : linkFrom[2].getAverageValue(linkFromIndex[2], blur);    
    double d = ((FastMath.floor(y)&1) == 0 ? 0.5*offset : -0.5*offset);
    x += d;
    z += d+0.5;
    double x1 = x-FastMath.round(x), y1 = y-FastMath.round(y), z1 = z-FastMath.round(z);
    double xf = Math.abs(x1), yf = Math.abs(y1), zf = Math.abs(z1);
    double halfgap = 0.5*gap;
    if (xf > halfgap+xsize && yf > hinv*halfgap+ysize && zf > halfgap+zsize)
      {
        value = 1.0;
        error = 0;
        gradient.set(0.0, 0.0, 0.0);
        gradOk = true;
      }
    else if (xf < halfgap-xsize || yf < hinv*halfgap-ysize || zf < halfgap-zsize)
      {
        value = error = 0.0;
        gradient.set(0.0, 0.0, 0.0);
        gradOk = true;
      }
    else
      {
        double e1 = (xf-halfgap)/xsize, e2 = (yf-hinv*halfgap)/ysize, e3 = (zf-halfgap)/zsize;
        double weight;
        if (e1 < e2 && e1 < e3)
          {
            weight = 0.5-0.5*e1;
            gradient.set((x1 > 0.0 ? 1.0 : -1.0)/xsize, 0.0, 0.0);
          }
        else if (e2 < e1 && e2 < e3)
          {
            weight = 0.5-0.5*e2;
            gradient.set(0.0, (y1 > 0.0 ? 1.0 : -1.0)/ysize, 0.0);
          }
        else
          {
            weight = 0.5-0.5*e3;
            gradient.set(0.0, 0.0, (z1 > 0.0 ? 1.0 : -1.0)/zsize);
          }
        error = Math.abs(error);
        value = 1.0-weight;
      }
    return value;
  }
  
  /* The error is calculated at the same time as the value. */
  
  public double getValueError(int which, double blur)
  {
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    return error;
  }
  
  /* Calculate the gradient. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    if (gradOk)
      {
        grad.set(gradient);
        return;
      }
    double dx = gradient.x, dy = gradient.y, dz = gradient.z;
    if (dx != 0.0)
      {
        if (linkFrom[0] == null)
          gradient.set(dx, 0.0, 0.0);
        else
          {
            linkFrom[0].getValueGradient(linkFromIndex[0], grad, blur);
            gradient.x = dx*grad.x;
            gradient.y = dx*grad.y;
            gradient.z = dx*grad.z;
          }
      }
    else
      gradient.set(0.0, 0.0, 0.0);
    if (dy != 0.0)
      {
        if (linkFrom[1] == null)
          gradient.y += dy;
        else
          {
            linkFrom[1].getValueGradient(linkFromIndex[1], grad, blur);
            gradient.x += dy*grad.x;
            gradient.y += dy*grad.y;
            gradient.z += dy*grad.z;
          }
      }
    if (dz != 0.0)
      {
        if (linkFrom[2] == null)
          gradient.z += dz;
        else
          {
            linkFrom[2].getValueGradient(linkFromIndex[2], grad, blur);
            gradient.x += dz*grad.x;
            gradient.y += dz*grad.y;
            gradient.z += dz*grad.z;
          }
      }
    gradOk = true;
    grad.set(gradient);
  }
  
  /* Allow the user to set the parameters. */
  
  public boolean edit(final ProcedureEditor editor, Scene theScene)
  {
    final ValueSlider heightSlider = new ValueSlider(0.0, 1.0, 100, height);
    final ValueSlider gapSlider = new ValueSlider(0.0, 0.5, 50, gap);
    final ValueSlider offsetSlider = new ValueSlider(0.0, 0.5, 50, offset);
    Object listener = new Object() {
      void processEvent()
      {
        height = heightSlider.getValue();
        gap = gapSlider.getValue();
        offset = offsetSlider.getValue();
        editor.updatePreview();
      }
    };
    heightSlider.addEventLink(ValueChangedEvent.class, listener);
    gapSlider.addEventLink(ValueChangedEvent.class, listener);
    offsetSlider.addEventLink(ValueChangedEvent.class, listener);
    ComponentsDialog dlg = new ComponentsDialog(editor.getParentFrame(), "Set brick properties:", new Widget [] {heightSlider, gapSlider, offsetSlider},
      new String [] {"Brick Height", "Gap Width", "Row Offset"});
    if (!dlg.clickedOk())
      return false;
    height = heightSlider.getValue();
    gap = gapSlider.getValue();
    offset = offsetSlider.getValue();
    return true;
  }
  
  /* Create a duplicate of this module. */
  
  public Module duplicate()
  {
    BrickModule mod = new BrickModule(new Point(bounds.x, bounds.y));
    
    mod.height = height;
    mod.gap = gap;
    mod.offset = offset;
    return mod;
  }

  /* Write out the parameters. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeDouble(height);
    out.writeDouble(gap);
    out.writeDouble(offset);
  }
  
  /* Read in the parameters. */
  
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    height = in.readDouble();
    gap = in.readDouble();
    offset = in.readDouble();
  }
}
