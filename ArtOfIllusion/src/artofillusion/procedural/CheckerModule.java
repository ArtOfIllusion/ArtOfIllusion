/* This is a Module which generates a checkerboard pattern. */

/* Copyright (C) 2000 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.math.*;
import artofillusion.ui.*;

import java.awt.*;

public class CheckerModule extends Module
{
  boolean valueOk, gradOk;
  double value, error, lastBlur;
  PointInfo point;
  Vec3 gradient;

  public CheckerModule(Point position)
  {
    super(Translate.text("menu.checkerModule"), new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"X", "(X)"}), 
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Y", "(Y)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Z", "(Z)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Value"})}, 
      position);
    gradient = new Vec3();
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

    double xsize = (linkFrom[0] == null) ? 0.5*point.xsize+blur : linkFrom[0].getValueError(linkFromIndex[0], blur);
    double ysize = (linkFrom[1] == null) ? 0.5*point.ysize+blur : linkFrom[1].getValueError(linkFromIndex[1], blur);
    double zsize = (linkFrom[2] == null) ? 0.5*point.zsize+blur : linkFrom[2].getValueError(linkFromIndex[2], blur);
    if (xsize >= 0.5 || ysize >= 0.5 || zsize >= 0.5)
      {
        value = error = 0.5;
        gradient.set(0.0, 0.0, 0.0);
        gradOk = true;
        return 0.5;
      }
    double x = (linkFrom[0] == null) ? point.x : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    double y = (linkFrom[1] == null) ? point.y : linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    double z = (linkFrom[2] == null) ? point.z : linkFrom[2].getAverageValue(linkFromIndex[2], blur);
    double xi = Math.rint(x), yi = Math.rint(y), zi = Math.rint(z);
    double xf = 0.5-Math.abs(x-xi), yf = 0.5-Math.abs(y-yi), zf = 0.5-Math.abs(z-zi);
    int i = (int) xi, j = (int) yi, k = (int) zi;

    value = ((i+j+k)&1) == 0 ? 1.0 : 0.0;
    if (xf > xsize && yf > ysize && zf > zsize)
      {
        error = 0.0;
        gradient.set(0.0, 0.0, 0.0);
        gradOk = true;
        return value;
      }
    double e1 = xf/xsize, e2 = yf/ysize, e3 = zf/zsize;
    if (e1 < e2 && e1 < e3)
      {
        error = 0.5-0.5*e1;
        gradient.set((x > xi ? 1.0-2.0*value : 2.0*value-1.0)/xsize, 0.0, 0.0);
      }
    else if (e2 < e1 && e2 < e3)
      {
        error = 0.5-0.5*e2;
        gradient.set(0.0, (y > yi ? 1.0-2.0*value : 2.0*value-1.0)/ysize, 0.0);
      }
    else
      {
        error = 0.5-0.5*e3;
        gradient.set(0.0, 0.0, (z > zi ? 1.0-2.0*value : 2.0*value-1.0)/zsize);
      }
    value = error*(1.0-value) + (1.0-error)*value;
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
}
