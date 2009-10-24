/* This is a Module which outputs a weighted average of two colors. */

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

public class BlendModule extends Module
{
  RGBColor blendColor;
  boolean colorOk;
  double lastBlur;
  
  public BlendModule(Point position)
  {
    super(Translate.text("menu.blendModule"), new IOPort [] {new IOPort(IOPort.COLOR, IOPort.INPUT, IOPort.TOP, new String [] {"Color 1", '('+Translate.text("black")+')'}),
      new IOPort(IOPort.COLOR, IOPort.INPUT, IOPort.BOTTOM, new String [] {"Color 2", '('+Translate.text("white")+')'}),
      new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Fraction", "(0.5)"})},
      new IOPort [] {new IOPort(IOPort.COLOR, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Blend"})}, 
      position);
    blendColor = new RGBColor(0.0f, 0.0f, 0.0f);
  }

  /* New point, so the color will need to be recalculated. */

  public void init(PointInfo p)
  {
    colorOk = false;
  }

  /* Calculate the blended color. */
  
  public void getColor(int which, RGBColor c, double blur)
  {
    if (colorOk && blur == lastBlur)
      {
        c.copy(blendColor);
        return;
      }
    colorOk = true;
    lastBlur = blur;
    double fract = (linkFrom[2] == null) ? 0.5 : linkFrom[2].getAverageValue(linkFromIndex[2], blur);
    double error = (linkFrom[2] == null) ? 0.0 : linkFrom[2].getValueError(linkFromIndex[2], blur);
    double min = fract-error, max = fract+error;
    if (min < 1.0 && max > 0.0)
      if (min < 0.0 || max > 1.0)
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
	  fract /= 2.0*error;
	}
    if (fract < 1.0)
      {
        if (linkFrom[0] == null)
          blendColor.setRGB(0.0f, 0.0f, 0.0f);
        else
          linkFrom[0].getColor(linkFromIndex[0], blendColor, blur);
      }
    if (fract > 0.0)
      {
        if (linkFrom[1] == null)
          c.setRGB(1.0f, 1.0f, 1.0f);
        else
          linkFrom[1].getColor(linkFromIndex[1], c, blur);
      }
    if (fract <= 0.0)
      {
        c.copy(blendColor);
        return;
      }
    if (fract >= 1.0)
      {
        blendColor.copy(c);
        return;
      }
    blendColor.scale((float) (1.0-fract));
    c.scale((float) fract);
    blendColor.add(c);
    c.copy(blendColor);
  }
}
