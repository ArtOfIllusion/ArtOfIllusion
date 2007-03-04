/* MaterialSpec describes the properties of a point in the interior of an object. */

/* Copyright (C) 2000 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.material;

import artofillusion.math.*;

public class MaterialSpec
{
  public RGBColor transparency, color, scattering;
  public double eccentricity;

  public MaterialSpec()
  {
    transparency = new RGBColor(0.0f, 0.0f, 0.0f);
    color = new RGBColor(0.0f, 0.0f, 0.0f);
    scattering = new RGBColor(0.0f, 0.0f, 0.0f);
    eccentricity = 0.0;
  }
}