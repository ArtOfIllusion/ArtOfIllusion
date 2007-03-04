/* This is a Module which returns the minimum of two numbers. */

/* Copyright (C) 2000 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.*;
import artofillusion.math.*;
import java.awt.*;
import java.io.*;

public class MinModule extends Module
{
  double lastBlur;
  int which;
  boolean whichOk;
  
  public MinModule(Point position)
  {
    super("Min", new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.TOP, new String [] {"Value 1", "(0)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.BOTTOM, new String [] {"Value 2", "(0)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Minimum"})}, 
      position);
  }

  /* New point, so the inputs will need to be compared again. */

  public void init(PointInfo p)
  {
    whichOk = false;
  }

  /* Compare the two inputs. */
  
  public double getAverageValue(int which, double blur)
  {
    if (whichOk && blur == lastBlur)
      return ((linkFrom[which] == null) ? 0.0 : linkFrom[which].getAverageValue(linkFromIndex[which], blur));
    whichOk = true;
    lastBlur = blur;
    
    double value1 = (linkFrom[0] == null) ? 0.0 : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    double value2 = (linkFrom[1] == null) ? 0.0 : linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    if (value1 < value2)
      {
	which = 0;
	return value1;
      }
    which = 1;
    return value2;
  }

  /* Determine which input to use, and get its error. */
  
  public double getValueError(int which, double blur)
  {
    if (!whichOk || blur != lastBlur)
      getAverageValue(which, blur);
    return ((linkFrom[which] == null) ? 0.0 : linkFrom[which].getValueError(linkFromIndex[which], blur));
  }

  /* Determine which input to use, and get its gradient. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (!whichOk || blur != lastBlur)
      getAverageValue(which, blur);
    if (linkFrom[which] == null)
      grad.set(0.0, 0.0, 0.0);
    else
      linkFrom[which].getValueGradient(linkFromIndex[which], grad, blur);
  }
}
