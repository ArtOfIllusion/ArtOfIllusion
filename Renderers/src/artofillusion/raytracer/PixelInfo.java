/* Copyright (C) 2003-2011 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.math.*;

/** A PixelInfo is used during raytracing to store information about the rays that have been
    sent out through a particular pixel. */

public class PixelInfo
{
  public float red, green, blue, transparency, depth;
  public float redSquare, greenSquare, blueSquare;
  public int raysSent;
  public boolean needsMore, converged;
  public float object;
  
  public PixelInfo()
  {
    needsMore = true;
    depth = Float.MAX_VALUE;
  }
  
  /** Reinitialize all of the fields for the PixelInfo. */
  
  public final void clear()
  {
    red = green = blue = redSquare = greenSquare = blueSquare = transparency = 0.0f;
    depth = Float.MAX_VALUE;
    object = 0.0f;
    raysSent = 0;
    needsMore = true;
  }
  
  /** Make this PixelInfo identical to another one. */
  
  public final void copy(PixelInfo info)
  {
    red = info.red;
    green = info.green;
    blue = info.blue;
    redSquare = info.redSquare;
    greenSquare = info.greenSquare;
    blueSquare = info.blueSquare;
    transparency = info.transparency;
    depth = info.depth;
    object = info.object;
    raysSent = info.raysSent;
    needsMore = info.needsMore;
    converged = info.converged;
  }
  
  /** Add another ray to this pixel. */
  
  public final void add(RGBColor col, float t)
  {
    red += col.getRed();
    green += col.getGreen();
    blue += col.getBlue();
    redSquare += col.getRed()*col.getRed();
    greenSquare += col.getGreen()*col.getGreen();
    blueSquare += col.getBlue()*col.getBlue();
    transparency += t;
    raysSent++;
  }

  /** Add the rays from another PixelInfo to this one. */

  public final void add(PixelInfo info)
  {
    red += info.red;
    green += info.green;
    blue += info.blue;
    redSquare += info.redSquare;
    greenSquare += info.greenSquare;
    blueSquare += info.blueSquare;
    transparency += info.transparency;
    if (info.depth < depth)
    {
      depth = info.depth;
      object = info.object;
    }
    raysSent += info.raysSent;
  }
  
  /** Decide whether the color of this pixel is identical to another one, within either of the specified
      tolerances (relative and absolute differences) */
  
  public final boolean matches(PixelInfo info, float maxAbsDiff, float maxRelDiff)
  {
    float scale1 = 1.0f/raysSent;
    float scale2 = 1.0f/info.raysSent;
    if (valuesDifferent(red*scale1, info.red*scale2, maxAbsDiff, maxRelDiff))
      return false;
    if (valuesDifferent(green*scale1, info.green*scale2, maxAbsDiff, maxRelDiff))
      return false;
    if (valuesDifferent(blue*scale1, info.blue*scale2, maxAbsDiff, maxRelDiff))
      return false;
    if (valuesDifferent(transparency*scale1, info.transparency*scale2, maxAbsDiff, maxRelDiff))
      return false;
    return true;
  }
  
  /** Decide whether particular values are different. */
  
  private final boolean valuesDifferent(float val1, float val2, float maxAbsDiff, float maxRelDiff)
  {
    float diff = val1-val2;
    if (diff < 0.0f)
      diff = -diff;
    if (diff < maxAbsDiff)
      return false;
    float sum = val1+val2;
    return (diff >= maxRelDiff*sum);
  }
  
  /** Calculate the ARGB value for this pixel. */

  public final int calcARGB()
  {
    float ninv = 1.0f/raysSent;
    float t = transparency*ninv;
    if (t >= 1.0f)
      return 0;
    float scale = ninv*255.0f/(1.0f-t);
    int a, r, g, b;
    a = (int) (255.0f*(1.0f-t));
    r = (int) (red*scale);
    g = (int) (green*scale);
    b = (int) (blue*scale);
    if (r < 0) r = 0;
    if (r > 255) r = 255;
    if (g < 0) g = 0;
    if (g > 255) g = 255;
    if (b < 0) b = 0;
    if (b > 255) b = 255;
    return (a<<24) + (r<<16) + (g<<8) + b;
  }
  
  /** Get the variance of the red component for this pixel. */
  
  public final float getRedVariance()
  {
    float ninv = 1.0f/raysSent;
    	return (redSquare-red*red*ninv)*ninv;
  }
  
  /** Get the variance of the green component for this pixel. */
  
  public final float getGreenVariance()
  {
    float ninv = 1.0f/raysSent;
    	return (greenSquare-green*green*ninv)*ninv;
  }
  
  /** Get the variance of the blue component for this pixel. */
  
  public final float getBlueVariance()
  {
    float ninv = 1.0f/raysSent;
    	return (blueSquare-blue*blue*ninv)*ninv;
  }
}