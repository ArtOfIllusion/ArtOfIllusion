/* Copyright (C) 2000 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.*;
import artofillusion.ui.*;
import artofillusion.math.*;
import java.awt.*;

/** This is a Module which interpolates between two numbers. */

public class InterpModule extends Module
{
  double value, error, fract, lastBlur;
  boolean valueOk, errorOk, gradOk;
  Vec3 gradient, tempVec;
  
  public InterpModule(Point position)
  {
    super(Translate.text("menu.interpolateModule"), new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.TOP, new String [] {"Value 1", "(0)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.BOTTOM, new String [] {"Value 2", "(1)"}), 
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Fraction", "(0)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Interpolate"})}, 
      position);
    gradient = new Vec3();
    tempVec = new Vec3();
  }

  /* New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    valueOk = errorOk = gradOk = false;
  }

  /* Calculate the value. */
  
  public double getAverageValue(int which, double blur)
  {
    if (valueOk && blur == lastBlur)
      return value;
    valueOk = true;
    lastBlur = blur;
    fract = (linkFrom[2] == null) ? 0.0 : linkFrom[2].getAverageValue(linkFromIndex[2], blur);
    double fractError = (linkFrom[2] == null) ? 0.0 : linkFrom[2].getValueError(linkFromIndex[2], blur);
    double min = fract-fractError, max = fract+fractError;
    
    if (max <= 0.0)
      fract = 0.0;
    else if (min >= 1.0)
      fract = 1.0;
    else if (min < 0.0 || max > 1.0)
      {
	fract = 0.0;
	if (min < 0.0)
	  min = 0.0;
	if (max > 1.0)
	  {
	    fract = max-1.0;
	    max = 1.0;
	  }
	fract += 0.5*(max+min)*(max-min);
	fract /= 2.0*fractError;
      }
    double value1 = (linkFrom[0] == null || fract == 1.0) ? 0.0 : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    double value2 = (linkFrom[1] == null || fract == 0.0) ? 1.0 : linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    value = (1.0-fract)*value1 + fract*value2;
    return value;
  }

  /* Calculate the error. */
  
  public double getValueError(int which, double blur)
  {
    if (errorOk && blur == lastBlur)
      return error;
    errorOk = true;
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    double value1 = (linkFrom[0] == null) ? 0.0 : linkFrom[0].getValueError(linkFromIndex[0], blur);
    double value2 = (linkFrom[1] == null) ? 0.0 : linkFrom[1].getValueError(linkFromIndex[1], blur);

    error = (1.0-fract)*value1 + fract*value2;
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
    gradOk = true;
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    if (linkFrom[0] == null || fract == 1.0)
      grad.set(0.0, 0.0, 0.0);
    else
      {
	linkFrom[0].getValueGradient(linkFromIndex[0], grad, blur);
	grad.scale(1.0-fract);
      }
    if (linkFrom[1] == null || fract == 0.0)
      tempVec.set(0.0, 0.0, 0.0);
    else
      {
	linkFrom[1].getValueGradient(linkFromIndex[1], tempVec, blur);
	tempVec.scale(fract);
      }
    grad.add(tempVec);
    gradient.set(grad);
  }
}
