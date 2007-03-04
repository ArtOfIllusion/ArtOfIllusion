/* This is a Module which outputs the lighter of two colors. */

/* Copyright (C) 2001 by David M. Turner <novalis@novalis.org> and Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import java.awt.*;

public class ColorLightenModule extends Module
{
  RGBColor color;
  boolean colorOk;
  double lastBlur;
  
  public ColorLightenModule(Point position)
  {
    super(Translate.text("menu.lighterModule"), new IOPort [] {new IOPort(IOPort.COLOR, IOPort.INPUT, IOPort.TOP, new String [] {"Color 1", '('+Translate.text("white")+')'}),
      new IOPort(IOPort.COLOR, IOPort.INPUT, IOPort.BOTTOM, new String [] {"Color 2", '('+Translate.text("white")+')'})}, 
      new IOPort [] {new IOPort(IOPort.COLOR, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Lighter"})}, 
      position);
    color = new RGBColor(0.0f, 0.0f, 0.0f);
  }

  /* New point, so the color will need to be recalculated. */

  public void init(PointInfo p)
  {
    colorOk = false;
  }

  /* Calculate the lighter color. */
  
  public void getColor(int which, RGBColor c, double blur)
  {
    if (colorOk && blur == lastBlur)
      {
        c.copy(color);
        return;
      }
    float brightness1, brightness2;
    colorOk = true;
    lastBlur = blur;
    if (linkFrom[0] == null)
      {
        color.setRGB(1.0f, 1.0f, 1.0f);
        brightness1 = 1.0f;
      }
    else
      {
        linkFrom[0].getColor(linkFromIndex[0], color, blur);
        brightness1 = color.getBrightness();
      }

    if (linkFrom[1] == null)
      {
        c.setRGB(1.0f, 1.0f, 1.0f);
        brightness2 = 1.0f;
      }
    else
      {
        linkFrom[1].getColor(linkFromIndex[1], c, blur);
        brightness2 = c.getBrightness();
      }
    if (brightness1 > brightness2)
      c.copy(color);
    else
      color.copy(c);
  }
}