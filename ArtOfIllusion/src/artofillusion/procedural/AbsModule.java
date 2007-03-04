/* This is a Module which outputs the absolute value of a number. */

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

public class AbsModule extends Module
{
  private boolean signOk, positive;
  private double lastBlur;
  
  public AbsModule(Point position)
  {
    super(Translate.text("menu.absModule"), new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Input", "(0"})}, 
      new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Output"})}, 
      position);
  }

  /* New point, so the value will need to be recalculated. */

  public void init(PointInfo p)
  {
    signOk = false;
  }

  /* Calculate the output value. */
  
  public double getAverageValue(int which, double blur)
  {
    double value = (linkFrom[0] == null) ? 0.0 : linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    positive = value > 0.0;
    signOk = true;
    lastBlur = blur;
    return positive ? value : -value;
  }
  /* The error is unchanged by this module. */
  
  public double getValueError(int which, double blur)
  {
    return (linkFrom[0] == null) ? 0.0 : linkFrom[0].getValueError(linkFromIndex[0], blur);
  }

  /* Calculate the gradient. */

  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (!signOk || blur != lastBlur)
      getAverageValue(which, blur);
    linkFrom[0].getValueGradient(linkFromIndex[0], grad, blur);
    if (!positive)
      grad.scale(-1.0);
  }
}
