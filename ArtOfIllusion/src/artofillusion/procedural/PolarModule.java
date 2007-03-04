/* Copyright (C) 2000-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.ui.*;
import artofillusion.math.*;
import java.awt.*;

/** This is a Module which converts from rectangular to polar coordinates. */

public class PolarModule extends Module
{
  double value[], error[], lastBlur;
  boolean valueOk[];
  Vec3 gradient[], tempVec1, tempVec2;
  PointInfo point;
  
  public PolarModule(Point position)
  {
    super(Translate.text("menu.polarModule"), new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"X", "(X)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Y", "(Y)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"R"}),
      new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Theta"})}, 
      position);
    value = new double [2];
    error = new double [2];
    valueOk = new boolean [2];
    gradient = new Vec3 [] {new Vec3(), new Vec3()};
    tempVec1 = new Vec3();
    tempVec2 = new Vec3();
  }

  /* New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    point = p;
    valueOk[0] = valueOk[1] = false;
  }

  /* Calculate the output values. */
  
  public double getAverageValue(int which, double blur)
  {
    if (valueOk[which] && blur == lastBlur)
      return value[which];
    lastBlur = blur;
    double x, y, xerror, yerror;
    
    // Determine the input values.
    
    if (linkFrom[0] == null)
      {
        x = point.x;
        xerror = 0.5*point.xsize+blur;
        tempVec1.set(1.0, 0.0, 0.0);
      }
    else
      {
        x = linkFrom[0].getAverageValue(linkFromIndex[0], blur);
        xerror = linkFrom[0].getValueError(linkFromIndex[0], blur);
        linkFrom[0].getValueGradient(linkFromIndex[0], tempVec1, blur);
      }
    if (linkFrom[1] == null)
      {
        y = point.y;
        yerror = 0.5*point.ysize+blur;
        tempVec2.set(0.0, 1.0, 0.0);
      }
    else
      {
        y = linkFrom[1].getAverageValue(linkFromIndex[1], blur);
        yerror = linkFrom[1].getValueError(linkFromIndex[1], blur);
        linkFrom[1].getValueGradient(linkFromIndex[1], tempVec2, blur);
      }
    if (!valueOk[0])
      {
        // Calculate R.

        value[0] = Math.sqrt(x*x + y*y);
        if (value[0] == 0.0)
        {
          gradient[0].set(0.0, 0.0, 0.0);
          error[0] = Math.sqrt(xerror*xerror + yerror*yerror);
        }
        else
        {
          gradient[0].set(x*tempVec1.x + y*tempVec2.x, x*tempVec1.y + y*tempVec2.y, x*tempVec1.z + y*tempVec2.z);
          gradient[0].scale(1.0/value[0]);
          error[0] = (Math.abs(x*xerror) + Math.abs(y*yerror))/value[0];
        }
        valueOk[0] = true;
      }
    if (which == 1)
      {
        // Calculate Theta.

        value[1] = Math.atan2(y, x);
        gradient[1].set(-y*tempVec1.x + x*tempVec2.x, -y*tempVec1.y + x*tempVec2.y, -y*tempVec1.z + x*tempVec2.z);
        double inv = 1.0/(value[0]*value[0]);
        gradient[1].scale(inv);
        error[1] = 1.5*(Math.abs(y*xerror) + Math.abs(x*yerror))*inv;
        valueOk[1] = true;
      }
    return value[which];
  }

  /* The errors are calculated at the same time as the values. */
  
  public double getValueError(int which, double blur)
  {
    if (!valueOk[which] || blur != lastBlur)
      getAverageValue(which, blur);
    return error[which];
  }

  /* The gradients are calculated at the same time as the values. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (!valueOk[which] || blur != lastBlur)
      getAverageValue(which, blur);
    grad.set(gradient[which]);
  }
}
