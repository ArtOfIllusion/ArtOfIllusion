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

/** This is a Module which takes three numbers, and uses them as the red, green, and blue
    components of a color. */

public class RGBModule extends ProceduralModule
{
  RGBColor color;
  boolean colorOk;
  double lastBlur;

  public RGBModule(Point position)
  {
    super("RGB", new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Red", "(0)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Green", "(0)"}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Blue", "(0)"})},
      new IOPort [] {new IOPort(IOPort.COLOR, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Color"})},
      position);
    color = new RGBColor(0.0f, 0.0f, 0.0f);
  }

  /* New point, so the color will need to be recalculated. */

  @Override
  public void init(PointInfo p)
  {
    colorOk = false;
  }

  /* Calculate the color. */

  @Override
  public void getColor(int which, RGBColor c, double blur)
  {
    if (colorOk && blur == lastBlur)
      {
        c.copy(color);
        return;
      }
    colorOk = true;
    lastBlur = blur;
    float red = (linkFrom[0] == null) ? 0.0f : (float) linkFrom[0].getAverageValue(linkFromIndex[0], blur);
    float green = (linkFrom[1] == null) ? 0.0f : (float) linkFrom[1].getAverageValue(linkFromIndex[1], blur);
    float blue = (linkFrom[2] == null) ? 0.0f : (float) linkFrom[2].getAverageValue(linkFromIndex[2], blur);
    color.setRGB(red, green, blue);
    c.copy(color);
  }
}
