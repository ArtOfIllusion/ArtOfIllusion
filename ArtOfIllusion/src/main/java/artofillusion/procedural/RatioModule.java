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

/** This is a Module which outputs the ratio of two numbers. */

public class RatioModule extends ProceduralModule
{
  boolean valueOk, errorOk;
  double value, error, valueIn1, valueIn2, errorIn1, errorIn2, lastBlur;
  Vec3 tempVec;

  public RatioModule(Point position)
  {
    super("\u00F7", new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.TOP, new String [] {"Value 1", "(0)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.BOTTOM, new String [] {"Value 2", "(0)"})},
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Ratio"})},
      position);
    tempVec = new Vec3();
  }

  /* New point, so the value will need to be recalculated. */

  @Override
  public void init(PointInfo p)
  {
    valueOk = errorOk = false;
  }

  /* This module outputs the ratio of the two values. */

  @Override
  public double getAverageValue(int which, double blur)
  {
    if (valueOk && blur == lastBlur)
      return value;
    valueOk = true;
    lastBlur = blur;
    valueIn1 = (linkFrom[0] == null) ? 0.0 : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    valueIn2 = (linkFrom[1] == null) ? 0.0 : linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    if (valueIn2 == 0.0)
      {
        if (valueIn1 < 0.0)
          value = -Double.MAX_VALUE;
        else
          value = Double.MAX_VALUE;
        error = Double.MAX_VALUE;
        errorOk = true;
        return value;
      }
    value = valueIn1/valueIn2;
    return value;
  }

  /* Calculate the error. */

  @Override
  public double getValueError(int which, double blur)
  {
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    if (errorOk)
      return error;
    valueOk = errorOk = true;
    errorIn1 = (linkFrom[0] == null) ? 0.0 : linkFrom[0].getValueError(linkFromIndex[0], blur);
    errorIn2 = linkFrom[1].getValueError(linkFromIndex[1], blur);
    error = Math.abs(errorIn1/valueIn2) + Math.abs(valueIn1*errorIn2/(valueIn2*valueIn2));
    return error;
  }

  /* Calculate the gradient. */

  @Override
  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (!valueOk || blur != lastBlur)
      getAverageValue(which, blur);
    if (linkFrom[0] == null)
      {
	grad.set(0.0, 0.0, 0.0);
	return;
      }
    if (linkFrom[1] == null)
      {
	grad.set(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
	return;
      }
    linkFrom[0].getValueGradient(linkFromIndex[0], grad, blur);
    linkFrom[1].getValueGradient(linkFromIndex[1], tempVec, blur);
    double d1 = 1.0/valueIn2, d2 = valueIn1/(valueIn2*valueIn2);
    grad.x = grad.x*d1 - tempVec.x*d2;
    grad.y = grad.y*d1 - tempVec.y*d2;
    grad.z = grad.z*d1 - tempVec.z*d2;
  }
}
