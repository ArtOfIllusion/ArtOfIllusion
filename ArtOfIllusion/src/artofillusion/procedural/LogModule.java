/* This is a Module which outputs the natural log of a number. */

/* Copyright (C) 2000 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.math.*;
import java.awt.*;

public class LogModule extends Module
{
  boolean valueOk, errorOk, gradOk;
  double value, error, valueIn, errorIn, lastBlur;
  Vec3 gradient;
  
  public LogModule(Point position)
  {
    super("Log", new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Value", "(1)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Log"})}, 
      position);
    gradient = new Vec3();
  }

  /* New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    valueOk = errorOk = gradOk = false;
  }

  /* This module outputs the log of the input value. */
  
  public double getAverageValue(int which, double blur)
  {
    if (valueOk && blur == lastBlur)
      return value;
    valueOk = true;
    lastBlur = blur;
    if (linkFrom[0] == null)
      {
        value = error = 0.0;
        errorOk = true;
        return 0.0;
      }
    valueIn = linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    errorIn = linkFrom[0].getValueError(linkFromIndex[0], blur);
    if (errorIn == 0.0)
      {
        if (valueIn < 0.0)
          value = -Math.log(-valueIn);
        else
          value = Math.log(valueIn);
        error = 0.0;
        errorOk = true;
        return value;
      }
    value = (integral(valueIn+errorIn)-integral(valueIn-errorIn))/(2.0*errorIn);
    return value;
  }
  
  /* This calculates the integral of the logarithm. */
  
  private double integral(double x)
  {
    if (x > 0.0)
      return x*Math.log(x)-x;
    else
      return -x*Math.log(-x)+x;
  }

  /* Estimate the error from the derivative of the function. */
  
  public double getValueError(int which, double blur)
  {
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    if (errorOk)
      return error;
    errorOk = true;
    if (errorIn == 0.0)
      {
        error = 0.0;
        return 0.0;
      }
    error = errorIn/Math.abs(valueIn);
    return error;
  }

  /* Calculate the gradient. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (gradOk && blur == lastBlur)
      {
        grad.set(gradient);
        return;
      }
    if (linkFrom[0] == null)
      {
        grad.set(0.0, 0.0, 0.0);
        return;
      }
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    gradOk = true;
    linkFrom[0].getValueGradient(linkFromIndex[0], gradient, blur);
    if (valueIn > 0.0)
      gradient.scale(1.0/valueIn);
    else
      gradient.scale(-1.0/valueIn);
    grad.set(gradient);
  }
}
