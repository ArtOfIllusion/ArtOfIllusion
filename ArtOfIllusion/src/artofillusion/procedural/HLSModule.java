/* Copyright (C) 2005 by Peter Eastman

This program is free software; you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY 
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.procedural;

import artofillusion.math.*;
import java.awt.*;

/** This is a Module which takes three numbers, and uses them as the hue, saturation, and 
 value components of a color. */

public class HLSModule extends Module
{
  RGBColor color;
  boolean colorOk;
  double lastBlur;
  
  public HLSModule(Point position)
  {
   super("HLS", new IOPort [] {new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Hue", "(1)"}),
     new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Lightness", "(1)"}),
     new IOPort(IOPort.NUMBER, IOPort.INPUT, IOPort.LEFT, new String [] {"Saturation", "(1)"})}, 
     new IOPort [] {new IOPort(IOPort.COLOR, IOPort.OUTPUT, IOPort.RIGHT, new String [] {"Color"})}, 
     position);
   color = new RGBColor(0.0f, 0.0f, 0.0f);
  }
  
  /** New point, so the color will need to be recalculated. */
  
  public void init(PointInfo p)
  {
   colorOk = false;
  }
  
  /** Calculate the color. */
  
  public void getColor(int which, RGBColor c, double blur)
  {
   if (colorOk && blur == lastBlur)
   {
     c.copy(color);
     return;
   }
   colorOk = true;
   lastBlur = blur;
   float hue = (linkFrom[0] == null) ? 1.0f : (float) linkFrom[0].getAverageValue(linkFromIndex[0], blur);
   float lightness = (linkFrom[1] == null) ? 1.0f : (float) linkFrom[1].getAverageValue(linkFromIndex[1], blur);
   float saturation = (linkFrom[2] == null) ? 1.0f : (float) linkFrom[2].getAverageValue(linkFromIndex[2], blur);
   float hueError = (linkFrom[0] == null) ? 0.0f : (float) linkFrom[0].getValueError(linkFromIndex[0], blur)*0.5f;
   if (saturation < 0.0f) saturation = 0.0f;
   if (saturation > 1.0f) saturation = 1.0f;
   if (hueError == 0.0)
     color.setHLS((hue-(float) FastMath.floor(hue))*360.0f, lightness, saturation);
   else
   {
     if (hueError > 0.25f)
       hueError = 0.25f;
     float min = hue-hueError, max = hue+hueError;
     min -= FastMath.floor(min);
     max -= FastMath.floor(max);
     color.setHSV(min*360.0f, lightness, saturation);
     float r1 = color.getRed(), g1 = color.getGreen(), b1 = color.getBlue();
     color.setHSV(max*360.0f, lightness, saturation);
     color.setRGB(0.5f*(r1+color.getRed()), 0.5f*(g1+color.getGreen()), 0.5f*(b1+color.getBlue()));
   }
   c.copy(color);
  }
}
