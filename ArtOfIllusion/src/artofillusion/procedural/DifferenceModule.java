/* This is a Module which outputs the difference of two numbers. */

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

public class DifferenceModule extends Module
{
  Vec3 tempVec;
  
  public DifferenceModule(Point position)
  {
    super("-", new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.TOP, new String [] {"Value 1", "(0)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.BOTTOM, new String [] {"Value 2", "(0)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Difference"})}, 
      position);
    tempVec = new Vec3();
  }

  /* This module outputs the difference of the two values. */
  
  public double getAverageValue(int which, double blur)
  {
    double value1 = (linkFrom[0] == null) ? 0.0 : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    double value2 = (linkFrom[1] == null) ? 0.0 : linkFrom[1].getAverageValue(linkFromIndex[1], blur);

    return value1-value2;
  }

  /* The errors add in quadrature, which involves a square root.  This is a faster
     approximation to it. */
  
  public double getValueError(int which, double blur)
  {
    double value1 = (linkFrom[0] == null) ? 0.0 : linkFrom[0].getValueError(linkFromIndex[0], blur);
    double value2 = (linkFrom[1] == null) ? 0.0 : linkFrom[1].getValueError(linkFromIndex[1], blur);
    double min, max, ratio;
    
    if (value1 < value2)
      {
	min = value1;
	max = value2;
      }
    else
      {
	min = value2;
	max = value1;
      }
    if (min == 0.0)
      return max;
    ratio = min/max;
    return max*(1.0+0.5*ratio*ratio);
  }

  /* The gradient is the difference of the two gradients. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (linkFrom[0] == null)
      grad.set(0.0, 0.0, 0.0);
    else
      linkFrom[0].getValueGradient(linkFromIndex[0], grad, blur);
    if (linkFrom[1] == null)
      tempVec.set(0.0, 0.0, 0.0);
    else
      linkFrom[1].getValueGradient(linkFromIndex[1], tempVec, blur);
    grad.subtract(tempVec);
  }
}
