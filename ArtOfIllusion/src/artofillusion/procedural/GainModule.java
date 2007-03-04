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

/** This is a Module which calculates Perlin's gain function. */

public class GainModule extends Module
{
  boolean valueOk, errorOk, gradOk;
  double value, error, valueIn, errorIn, gain, power, lastBlur;
  Vec3 gradient;
  
  static final double scale = 1.0/Math.log(0.5);

  public GainModule(Point position)
  {
    super(Translate.text("menu.gainModule"), new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.BOTTOM, new String [] {"Gain", "(0.5)"}),
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
    gain = (linkFrom[0] == null) ? 0.5 : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    valueIn = linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    errorIn = linkFrom[1].getValueError(linkFromIndex[1], blur);
    if (gain <= 0.0)
      {
        gain = 0.0;
        value = 0.5;
        error = 0.0;
        errorOk = true;
        return value;
      }
    double sign, v;
    if (valueIn < 0.0)
      {
        v = -valueIn;
        sign = -1.0;
      }
    else
      {
        v = valueIn;
        sign = 1.0;
      }
    if (gain >= 1.0)
      {
        gain = 1.0;
        value = sign*(v < 0.5 ? 0.0 : 1.0);
        error = 0.0;
        errorOk = true;
        return value;
      }
    power = Math.log(1.0-gain)*scale;
    if (errorIn == 0.0)
      {
        if (v < 0.5)
          value = 1.0-0.5*Math.pow(2.0-2.0*v, power);
        else
          value = 0.5*Math.pow(2.0*v, power);
        error = 0.0;
        errorOk = true;
        return value;
      }
    value = (integral(valueIn+errorIn, power)-integral(valueIn-errorIn, power))/(2.0*errorIn);
    return value;
  }
  
  /* This calculates the integral of the gain function. */
  
  private double integral(double x, double y)
  {
    double d = y+1.0, sign = 1.0;
    
    if (x < 0.0)
      {
        x = -x;
        sign = -1.0;
      }
    if (x < 0.5)
      return sign*(x + (Math.pow(2.0-2.0*x, d)-Math.pow(2.0, d))/(4.0*d));
    else
      return sign*(0.5 + (Math.pow(2.0*x, d)-Math.pow(2.0, d))/(4.0*d));
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
    double v = valueIn < 0.0 ? -valueIn : valueIn;
    if (valueIn < 0.5)
      error *= Math.abs(power*Math.pow(2.0-2.0*v, power-1.0));
    else
      error *= Math.abs(power*Math.pow(2.0*v, power-1.0));
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
    if (gain == 0.0 || gain == 1.0)
      gradient.set(0.0, 0.0, 0.0);
    else
      {
        linkFrom[1].getValueGradient(linkFromIndex[1], gradient, blur);
        double v = (valueIn < 0.0) ? -valueIn : valueIn;
        if (v < 0.5)
          gradient.scale(power*Math.pow(2.0-2.0*v, power-1.0));
        else
          gradient.scale(power*Math.pow(2.0*v, power-1.0));
      }
    grad.set(gradient);
  }
}
