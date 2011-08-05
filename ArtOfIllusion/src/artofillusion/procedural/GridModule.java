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

/** This is a Module which generates a grid of dots. */

public class GridModule extends Module
{
  boolean valueOk, errorOk, gradOk;
  double value, error, xspace, yspace, zspace, lastBlur;
  double xinv, yinv, zinv;
  PointInfo point;
  Vec3 gradient;
  
  public GridModule(Point position)
  {
    super(Translate.text("menu.gridModule"), new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"X", "(X)"}), 
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Y", "(Y)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Z", "(Z)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Value"})}, 
      position);
    xspace = yspace = zspace = 1.0;
    xinv = yinv = zinv = 1.0;
    gradient = new Vec3();
  }
  
  /** Get the X spacing. */
  
  public double getXSpacing()
  {
    return xspace;
  }
  
  /** Set the X spacing. */
  
  public void setXSpacing(double space)
  {
    xspace = space;
    xinv = 1.0/space;
  }
  
  /** Get the Y spacing. */
  
  public double getYSpacing()
  {
    return yspace;
  }
  
  /** Set the Y spacing. */
  
  public void setYSpacing(double space)
  {
    yspace = space;
    yinv = 1.0/space;
  }
  
  /** Get the Z spacing. */
  
  public double getZSpacing()
  {
    return zspace;
  }
  
  /** Set the Z spacing. */
  
  public void setZSpacing(double space)
  {
    zspace = space;
    zinv = 1.0/space;
  }

  /* New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    point = p;
    valueOk = errorOk = gradOk = false;
  }
  
  /* Calculate the average value of the function. */

  public double getAverageValue(int which, double blur)
  {
    if (valueOk && blur == lastBlur)
      return value;
    valueOk = true;
    lastBlur = blur;

    double x = (linkFrom[0] == null) ? point.x : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    double y = (linkFrom[1] == null) ? point.y : linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    double z = (linkFrom[2] == null) ? point.z : linkFrom[2].getAverageValue(linkFromIndex[2], blur);    
    double xi = xspace*FastMath.round(x*xinv), yi = yspace*FastMath.round(y*yinv), zi = zspace*FastMath.round(z*zinv);
    double xf = x-xi, yf = y-yi, zf = z-zi;
    
    value = Math.sqrt(xf*xf + yf*yf + zf*zf);
    gradient.set(xf, yf, zf);
    gradient.scale(1.0/value);
    return value;
  }
  
  /* The error is calculated at the same time as the value. */
  
  public double getValueError(int which, double blur)
  {
    if (errorOk && blur == lastBlur)
      return error;
    double xsize = (linkFrom[0] == null) ? 0.5*point.xsize+blur : linkFrom[0].getValueError(linkFromIndex[0], blur);
    double ysize = (linkFrom[1] == null) ? 0.5*point.ysize+blur : linkFrom[1].getValueError(linkFromIndex[1], blur);
    double zsize = (linkFrom[2] == null) ? 0.5*point.zsize+blur : linkFrom[2].getValueError(linkFromIndex[2], blur);
    
    error = Math.max(Math.max(xsize, ysize), zsize);
    errorOk = true;
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
    final ValueField xField = new ValueField(xspace, ValueField.POSITIVE, 5);
    final ValueField yField = new ValueField(yspace, ValueField.POSITIVE, 5);
    final ValueField zField = new ValueField(zspace, ValueField.POSITIVE, 5);
    Object listener = new Object() {
      void processEvent()
      {
        xspace = xField.getValue();
        yspace = yField.getValue();
        zspace = zField.getValue();
        xinv = 1.0/xspace;
        yinv = 1.0/yspace;
        zinv = 1.0/zspace;
        editor.updatePreview();
      }
    };
    xField.addEventLink(ValueChangedEvent.class, listener);
    yField.addEventLink(ValueChangedEvent.class, listener);
    zField.addEventLink(ValueChangedEvent.class, listener);
    ComponentsDialog dlg = new ComponentsDialog(editor.getParentFrame(), "Set Grid Spacing:", new Widget [] {xField, yField, zField},
      new String [] {"X", "Y", "Z"});
    if (!dlg.clickedOk())
      return false;
    return true;
  }
  
  /* Create a duplicate of this module. */
  
  public Module duplicate()
  {
    GridModule mod = new GridModule(new Point(bounds.x, bounds.y));
    
    mod.xspace = xspace;
    mod.yspace = yspace;
    mod.zspace = zspace;
    mod.xinv = xinv;
    mod.yinv = yinv;
    mod.zinv = zinv;
    return mod;
  }

  /* Write out the parameters. */

  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeDouble(xspace);
    out.writeDouble(yspace);
    out.writeDouble(zspace);
  }
  
  /* Read in the parameters. */
  
  public void readFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    xspace = in.readDouble();
    yspace = in.readDouble();
    zspace = in.readDouble();
    xinv = 1.0/xspace;
    yinv = 1.0/yspace;
    zinv = 1.0/zspace;
  }
}
