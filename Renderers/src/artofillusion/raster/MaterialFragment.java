/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raster;

import artofillusion.math.*;

public class MaterialFragment extends TransparentFragment
{
  private ObjectMaterialInfo material;
  private boolean isEntering;

  /**
   * Create a MaterialFragment.
   *
   * @param additiveColor        the additive color, in ERGB format
   * @param multiplicativeColor  the multiplicative color, in ERGB format
   * @param depth                the depth of this fragment
   * @param next                 the next fragment behind this one
   * @param material             a description of the material for the object being entered
   * @param isEntering           true if the material is being entered, false if it is being exited
   */

  public MaterialFragment(int additiveColor, int multiplicativeColor, float depth, Fragment next, ObjectMaterialInfo material, boolean isEntering)
  {
    super(additiveColor, multiplicativeColor, depth, next);
    this.material = material;
    this.isEntering = isEntering;
  }

  /**
   * Get the material.
   */

  public ObjectMaterialInfo getMaterialMapping()
  {
    return material;
  }

  /**
   * Get whether the object is being entered or exited.
   */

  public boolean isEntering()
  {
    return isEntering;
  }

  public Fragment insertNextFragment(Fragment fragment)
  {
    if (fragment.getDepth() == getDepth() && fragment.getMaterialMapping() == material && !isEntering && fragment.isEntering())
      return fragment.insertNextFragment(this); // Enter the material before exiting it
    return super.insertNextFragment(fragment);
  }
}
