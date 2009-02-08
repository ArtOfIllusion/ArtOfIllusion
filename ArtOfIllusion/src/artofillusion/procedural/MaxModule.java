/* Copyright (C) 2000-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.math.*;
import java.awt.*;

/** This is a Module which returns the maximum of two numbers. */

public class MaxModule extends Module
{
  double lastBlur, value, error;
  int which;
  boolean valueOk;

  public MaxModule(Point position)
  {
    super("Max", new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.TOP, new String [] {"Value 1", "(0)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.BOTTOM, new String [] {"Value 2", "(0)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Maximum"})}, 
      position);
  }

  /* New point, so the inputs will need to be compared again. */

  public void init(PointInfo p)
  {
    valueOk = false;
  }

  /* Compare the two inputs. */
  
  public double getAverageValue(int which, double blur)
  {
    if (valueOk && blur == lastBlur)
      return value;
    valueOk = true;
    double value1 = (linkFrom[0] == null) ? 0.0 : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    double value2 = (linkFrom[1] == null) ? 0.0 : linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    double error1 = (linkFrom[0] == null) ? 0.0 : linkFrom[0].getValueError(linkFromIndex[0], blur);
    double error2 = (linkFrom[1] == null) ? 0.0 : linkFrom[1].getValueError(linkFromIndex[1], blur);
    double min1 = value1-error1;
    double max1 = value1+error1;
    double min2 = value2-error2;
    double max2 = value2+error2;
    if (max1 < min2)
    {
      value = value2;
      error = error2;
      which = 1;
    }
    else if (max2 < min1)
    {
      value = value1;
      error = error1;
      which = 0;
    }
    else if (value1 > value2)
    {
      value = value1;
      double maxmin = (min1 < min2 ? min2 : min1);
      error = Math.abs(value-maxmin);
      which = 0;
    }
    else
    {
      value = value2;
      double maxmin = (min1 < min2 ? min2 : min1);
      error = Math.abs(value-maxmin);
      which = 1;
    }
    return value;
  }

  /* Determine which input to use, and get its error. */

  public double getValueError(int which, double blur)
  {
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    return error;
  }

  /* Determine which input to use, and get its gradient. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    if (linkFrom[which] == null)
      grad.set(0.0, 0.0, 0.0);
    else
      linkFrom[which].getValueGradient(linkFromIndex[which], grad, blur);
  }
}
