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
 * This interface defines a fragment (a piece of a rasterized polygon covering a single pixel).
 * A fragment defines a color by which anything behind it should be multiplied, and a color which
 * should be added to anything behind it.  Fragments can be chained together, when several
 * polygons contribute to the final color of a pixel.
 */

public interface Fragment
{
  /**
   * Get this fragment's additive color.
   *
   * @param color      the color is stored in this
   */

  void getAdditiveColor(RGBColor color);

  /**
   * Get this fragment's multiplicative color.
   *
   * @param color      the color is stored in this
   */

  void getMultiplicativeColor(RGBColor color);

  /**
   * Get whether this fragment is completely opaque, such that anything behind it has no effect
   * on the final color of the pixel.
   */

  boolean isOpaque();

  /**
   * Get the depth at which this fragment is located.
   */

  float getDepth();

  /**
   * Get the depth of the frontmost opaque fragment.  Anythinig beyond this depth has no
   * effect on the final color.
   */

  float getOpaqueDepth();

  /**
   * Get the material for the object this fragment is part of.  This may return null.
   */

  public ObjectMaterialInfo getMaterialMapping();

  /**
   * Get whether the object is being entered or exited in this fragment.
   */

  public boolean isEntering();
  
  /**
   * Get the next fragment behind this one.
   */

  Fragment getNextFragment();

  /**
   * Insert another fragment behind this one.  If there is already another fragment behind this
   * one, this method is responsible for inserting the new fragment into the list at the correct
   * point.
   *
   * @param fragment        the new fragment to insert
   * @return this fragment, or another equivalent fragment it should be replaced by.  The return
   * value is to permit certain optimizations.
   */

  Fragment insertNextFragment(Fragment fragment);
}
