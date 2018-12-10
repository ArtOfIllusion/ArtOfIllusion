/* This is a Module which represents one of the output values of a procedure. */

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

public class OutputModule extends ProceduralModule
{
  int width;
  double defaultValue;
  RGBColor defaultColor;

  public OutputModule(String name, String defaultLabel, double defaultValue, RGBColor defaultColor, int type)
  {
    super(name, new IOPort [] {new IOPort(type, IOPort.INPUT, IOPort.LEFT, new String [] {name, "("+defaultLabel+")"})},
      new IOPort [] {}, new Point(0, 0));
    this.defaultValue = defaultValue;
    this.defaultColor = defaultColor;
  }

  /* All output modules should be the same width. */

  public void setWidth(int w)
  {
    width = w;
  }

  @Override
  public void calcSize()
  {
    super.calcSize();
    if (width > 0)
      bounds.width = width;
  }

  /* Get the output value for this module. */

  @Override
  public double getAverageValue(int which, double blur)
  {
    if (linkFrom[0] == null)
      return defaultValue;
    return linkFrom[0].getAverageValue(linkFromIndex[0], blur);
  }

  /* Get the gradient of the output value for this module. */

  @Override
  public void getValueGradient(int which, Vec3 grad, double blur)
  {
    if (linkFrom[0] == null)
      grad.set(0.0, 0.0, 0.0);
    else
      linkFrom[0].getValueGradient(linkFromIndex[0], grad, blur);
  }

  /* Get the output color for this module. */

  @Override
  public void getColor(int which, RGBColor color, double blur)
  {
    if (linkFrom[0] == null)
      color.copy(defaultColor);
    else
      linkFrom[0].getColor(linkFromIndex[0], color, blur);
  }
}
