/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raster;

import artofillusion.math.*;

/**
 * This class represents a fully opaque fragment.
 */

public class OpaqueFragment implements Fragment
{
  private int additiveColor;
  private float depth;

  /**
   * Create an OpaqueFragment.
   *
   * @param additiveColor     the additive color, in ERGB format
   * @param depth             the depth of this fragment
   */

  public OpaqueFragment(int additiveColor, float depth)
  {
    this.additiveColor = additiveColor;
    this.depth = depth;
  }

  public void getAdditiveColor(RGBColor color)
  {
    color.setERGB(additiveColor);
  }

  public void getMultiplicativeColor(RGBColor color)
  {
    color.setRGB(0.0f, 0.0f, 0.0f);
  }

  public boolean isOpaque()
  {
    return true;
  }

  public float getDepth()
  {
    return depth;
  }

  public float getOpaqueDepth()
  {
    return depth;
  }

  public ObjectMaterialInfo getMaterialMapping()
  {
    return null;
  }

  public boolean isEntering()
  {
    return false;
  }

  public Fragment getNextFragment()
  {
    return Raster.BACKGROUND_FRAGMENT;
  }

  public Fragment insertNextFragment(Fragment fragment)
  {
    return this;
  }
}
