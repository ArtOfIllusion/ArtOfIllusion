/* This is a Module which blurs the signal coming into it. */

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

public class BlurModule extends Module
{
  boolean valueOk;
  double extraBlur, lastBlur;
  
  public BlurModule(Point position)
  {
    super(Translate.text("menu.blurModule"), new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.BOTTOM, new String [] {"Blur", "(0.05)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Input", "(0)"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Output"})}, 
      position);
  }

  /* New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    valueOk = false;
  }

  /* Get the output value. */
  
  public double getAverageValue(int which, double blur)
  {
    if (linkFrom[1] == null)
      return 0.0;
    if (!valueOk || blur != lastBlur)
      extraBlur = (linkFrom[0] == null) ? 0.05 : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    valueOk = true;
    lastBlur = blur;
    return linkFrom[1].getAverageValue(linkFromIndex[1], blur+extraBlur);
  }

  /* Get the output error. */
  
  public double getValueError(int which, double blur)
  {
    if (linkFrom[1] == null)
      return 0.0;
    if (!valueOk || blur != lastBlur)
      extraBlur = (linkFrom[0] == null) ? 0.05 : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    valueOk = true;
    lastBlur = blur;
    return linkFrom[1].getValueError(linkFromIndex[1], blur+extraBlur);
  }

  /* Calculate the gradient. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (linkFrom[1] == null)
      {
	grad.set(0.0, 0.0, 0.0);
	return;
      }
    if (!valueOk || blur != lastBlur)
      extraBlur = (linkFrom[0] == null) ? 0.05 : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    valueOk = true;
    lastBlur = blur;
    linkFrom[1].getValueGradient(linkFromIndex[1], grad, blur+extraBlur);
  }
}
