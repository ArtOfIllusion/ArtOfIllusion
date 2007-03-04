/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raster;

import artofillusion.math.*;

public class TransparentFragment implements Fragment
{
  private int additiveColor, multiplicativeColor;
  private float depth;
  private Fragment next;

  /**
   * Create a TransparentFragment.
   *
   * @param additiveColor        the additive color, in ERGB format
   * @param multiplicativeColor  the multiplicative color, in ERGB format
   * @param depth                the depth of this fragment
   * @param next                 the next fragment behind this one
   */

  public TransparentFragment(int additiveColor, int multiplicativeColor, float depth, Fragment next)
  {
    this.additiveColor = additiveColor;
    this.multiplicativeColor = multiplicativeColor;
    this.depth = depth;
    this.next = next;
  }

  public void getAdditiveColor(RGBColor color)
  {
    color.setERGB(additiveColor);
  }

  public void getMultiplicativeColor(RGBColor color)
  {
    color.setERGB(multiplicativeColor);
  }

  public boolean isOpaque()
  {
    return false;
  }

  public float getDepth()
  {
    return depth;
  }

  public float getOpaqueDepth()
  {
    return next.getOpaqueDepth();
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
    return next;
  }

  public Fragment insertNextFragment(Fragment fragment)
  {
    if (fragment.getDepth() < next.getDepth())
      fragment = fragment.insertNextFragment(next);
    else
      fragment = next.insertNextFragment(fragment);
    next = fragment;
    return this;
  }
}
