/* Copyright (C) 2000-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.math.*;
import java.awt.*;

/** This is a Module which compares two numbers, and outputs either 0 or 1 depending on
    which is greater. */

public class CompareModule extends Module
{
  double value, error, deriv, lastBlur;
  boolean valueOk, gradOk;
  Vec3 gradient;
  
  public CompareModule(Point position)
  {
    super(">", new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.TOP, new String [] {"Value 1", "(0)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.BOTTOM, new String [] {"Value 2", "(0)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Comparison"})}, 
      position);
    gradient = new Vec3();
  }

  /* New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    valueOk = gradOk = false;
  }

  /* Compare the two inputs. */
  
  public double getAverageValue(int which, double blur)
  {
    if (valueOk && blur == lastBlur)
      return value;
    valueOk = true;
    lastBlur = blur;
    
    double value1 = (linkFrom[0] == null) ? 0.0 : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    double value2 = (linkFrom[1] == null) ? 0.0 : linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    double error1 = (linkFrom[0] == null) ? 0.0 : linkFrom[0].getValueError(linkFromIndex[0], blur);
    double error2 = (linkFrom[1] == null) ? 0.0 : linkFrom[1].getValueError(linkFromIndex[1], blur);
    double min1 = value1-error1, max1 = value1+error1;
    double min2 = value2-error2, max2 = value2+error2;

    if (error1 == 0.0 && error2 == 0.0)
    {
      value = (value1 > value2 ? 1.0 : 0.0);
      error = 0.0;
    }
    else if (min1 > max2)
    {
      value = 1.0;
      error = 0.0;
    }
    else if (min2 > max1)
    {
      value = 0.0;
      error = 0.0;
    }
    else if (value1 > value2)
    {
      error = 0.5*(max2-min1)/(max1-min2);
      value = 1.0-error;
      deriv = 1.0/(error1 > error2 ? error1 : error2);
    }
    else
    {
      value = error = 0.5*(max1-min2)/(max2-min1);
      deriv = 1.0/(error1 > error2 ? error1 : error2);
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
    if (error == 0.0)
      {
        grad.set(0.0, 0.0, 0.0);
        return;
      }
    if (gradOk)
      {
        grad.set(gradient);
        return;
      }
    gradOk = true;
    if (linkFrom[0] == null)
      grad.set(0.0, 0.0, 0.0);
    else
      linkFrom[0].getValueGradient(linkFromIndex[0], grad, blur);
    if (linkFrom[1] == null)
      gradient.set(0.0, 0.0, 0.0);
    else
      linkFrom[1].getValueGradient(linkFromIndex[1], gradient, blur);
    grad.subtract(gradient);
    grad.scale(deriv);
    gradient.set(grad);
  }
}
