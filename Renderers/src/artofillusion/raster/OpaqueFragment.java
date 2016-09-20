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

  @Override
  public void getAdditiveColor(RGBColor color)
  {
    color.setERGB(additiveColor);
  }

  @Override
  public void getMultiplicativeColor(RGBColor color)
  {
    color.setRGB(0.0f, 0.0f, 0.0f);
  }

  @Override
  public boolean isOpaque()
  {
    return true;
  }

  @Override
  public float getDepth()
  {
    return depth;
  }

  @Override
  public float getOpaqueDepth()
  {
    return depth;
  }

  @Override
  public ObjectMaterialInfo getMaterialMapping()
  {
    return null;
  }

  @Override
  public boolean isEntering()
  {
    return false;
  }

  @Override
  public Fragment getNextFragment()
  {
    return Raster.BACKGROUND_FRAGMENT;
  }

  @Override
  public Fragment insertNextFragment(Fragment fragment)
  {
    return this;
  }
}
