/* Copyright (C) 2000-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.math.*;
import java.awt.*;

/** This is a Module which outputs one number mod another. */

public class ModModule extends Module
{
  double value, error, gradScale, lastBlur;
  boolean valueOk;
  
  public ModModule(Point position)
  {
    super("Mod", new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Input", "(0)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.BOTTOM, new String [] {"Modulus", "(1)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Output"})}, 
      position);
  }

  /* New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    valueOk = false;
  }

  /* Calculate the average value. */
  
  public double getAverageValue(int which, double blur)
  {
    if (valueOk && blur == lastBlur)
      return value;
    valueOk = true;
    lastBlur = blur;
    if (linkFrom[0] == null)
      {
        value = error = 0.0;
        return 0.0;
      }
    value = linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    error = linkFrom[0].getValueError(linkFromIndex[0], blur);
    double m = (linkFrom[1] == null) ? 1.0 : Math.abs(linkFrom[1].getAverageValue(linkFromIndex[1], blur));
    double minv = 1.0/m;
    
    if (error == 0.0)
      {
        value *= minv;
        value -= FastMath.floor(value);
        value *= m;
        gradScale = 1.0;
      }
    else if (error >= 0.5*m)
      {
/*        value *= minv;
        value -= Math.floor(value);
        value *= m;
        error = 0.5*m;*/
        value = error = 0.5*m;
        gradScale = 0.0;
      }
    else
      {
        value *= minv;
        error *= minv;
        double min = value-error;
        double max = value+error;
        min -= FastMath.floor(min);
        max -= FastMath.floor(max);
        if (max > min)
          {
            value = 0.5*(max+min);
            gradScale = 1.0;
          }
        else
          {
            value = (0.5*max*max + 0.5*(1.0+min)*(1.0-min))/(1.0-min+max);
            gradScale = (max-min)/(2.0*error*m);
            error = 0.5;
          }
        value *= m;
        error *= m;
      }
    return value;
  }

  /* The error is calculate at the same time as the value. */
  
  public double getValueError(int which, double blur)
  {
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    return error;
  }

  /* Calculate the gradient. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (linkFrom[0] == null)
      {
        grad.set(0.0, 0.0, 0.0);
        return;
      }
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    linkFrom[0].getValueGradient(linkFromIndex[0], grad, blur);
    grad.scale(gradScale);
  }
}
