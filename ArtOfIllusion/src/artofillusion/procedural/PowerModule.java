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

/** This is a Module which raises a number to a power. */

public class PowerModule extends Module
{
  boolean valueOk, errorOk, gradOk, powerIsInteger;
  double value, error, valueIn, errorIn, power, lastBlur;
  Vec3 gradient;
  
  public PowerModule(Point position)
  {
    super("Pow", new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.TOP, new String [] {"Exponent", "(1)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Input", "(0)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Output"})}, 
      position);
    gradient = new Vec3();
  }

  /* New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    valueOk = errorOk = gradOk = false;
  }

  /* Calculate the output value. */
  
  public double getAverageValue(int which, double blur)
  {
    if (valueOk && blur == lastBlur)
      return value;
    valueOk = true;
    lastBlur = blur;
    if (linkFrom[1] == null)
      {
        value = error = 0.0;
        errorOk = true;
        return 0.0;
      }
    if (linkFrom[0] == null)
    {
      power = 1.0;
      powerIsInteger = true;
    }
    else
    {
      power = linkFrom[0].getAverageValue(linkFromIndex[0], blur);
      powerIsInteger = ((int) power == power && linkFrom[0].getValueError(linkFromIndex[0], blur) == 0.0);
    }
    valueIn = linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    errorIn = linkFrom[1].getValueError(linkFromIndex[1], blur);
    if (errorIn == 0.0)
      {
        if (valueIn < 0.0 && !powerIsInteger)
          value = -Math.pow(-valueIn, power);
        else
          value = Math.pow(valueIn, power);
        error = 0.0;
        errorOk = true;
        return value;
      }
    value = (integral(valueIn+errorIn, power)-integral(valueIn-errorIn, power))/(2.0*errorIn);
    return value;
  }
  
  /* This calculates the integral of the power function. */
  
  private double integral(double x, double y)
  {
    if (y == -1.0)
    {
      if (x > 0.0 || powerIsInteger)
        return Math.log(x);
      else
        return Math.log(-x);
    }
    double d = y+1.0;
    if (x > 0.0 || powerIsInteger)
      return Math.pow(x, d)/d;
    else
      return Math.pow(-x, d)/d;
  }

  /* Estimate the error from the derivative of the function. */
  
  public double getValueError(int which, double blur)
  {
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    if (errorOk)
      return error;
    errorOk = true;
    if (errorIn == 0.0 || power == 0.0)
      {
        error = 0.0;
        return 0.0;
      }
    if (valueIn >= 0.0 || powerIsInteger)
      error = errorIn*Math.abs(power*Math.pow(valueIn, power-1.0));
    else
      error = errorIn*Math.abs(power*Math.pow(-valueIn, power-1.0));
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
    if (linkFrom[1] == null)
      {
        grad.set(0.0, 0.0, 0.0);
        return;
      }
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    gradOk = true;
    if (power == 0.0)
      gradient.set(0.0, 0.0, 0.0);
    else
      {
        linkFrom[1].getValueGradient(linkFromIndex[1], gradient, blur);
        if (valueIn > 0.0 || powerIsInteger)
          gradient.scale(power*Math.pow(valueIn, power-1.0));
        else
          gradient.scale(power*Math.pow(-valueIn, power-1.0));
      }
    grad.set(gradient);
  }
}
