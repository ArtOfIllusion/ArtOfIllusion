/* This is a Module which outputs the cosine of a number. */

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

public class CosineModule extends Module
{
  boolean valueOk, errorOk, gradOk;
  double value, error, valueIn, errorIn, lastBlur;
  Vec3 gradient;
  
  public CosineModule(Point position)
  {
    super("Cos", new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Value", "(0)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Cosine"})}, 
      position);
    gradient = new Vec3();
  }

  /* New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    valueOk = errorOk = gradOk = false;
  }

  /* This module outputs the sine of the input value. */
  
  public double getAverageValue(int which, double blur)
  {
    if (valueOk && blur == lastBlur)
      return value;
    if (linkFrom[0] == null)
      {
	value = 1.0;
	error = 0.0;
	valueOk = errorOk = true;
	return 1.0;
      }
    valueIn = linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    errorIn = linkFrom[0].getValueError(linkFromIndex[0], blur);
    if (errorIn == 0.0)
      {
	value = Math.cos(valueIn);
	error = 0.0;
	valueOk = errorOk = true;
	return value;
      }
    value = (Math.sin(valueIn+errorIn)-Math.sin(valueIn-errorIn))/(2.0*errorIn);
    valueOk = true;
    lastBlur = blur;
    return value;
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
    error = Math.abs(Math.sin(valueIn)*errorIn);
    if (error > 0.5)
      error = 0.5;
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
    gradient.scale(-Math.sin(valueIn));
    grad.set(gradient);
  }
}
