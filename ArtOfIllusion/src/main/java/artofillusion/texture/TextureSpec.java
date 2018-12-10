/* TextureSpec describes the properties of a point on the surface of an object. */

/* Copyright (C) 1999,2000,2002 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.math.*;

public class TextureSpec
{
  public final RGBColor diffuse, specular, transparent, emissive, hilight;
  public double roughness, cloudiness;
  public final Vec3 bumpGrad;

  public TextureSpec()
  {
    diffuse = new RGBColor(0.0f, 0.0f, 0.0f);
    specular = new RGBColor(0.0f, 0.0f, 0.0f);
    transparent = new RGBColor(0.0f, 0.0f, 0.0f);
    emissive = new RGBColor(0.0f, 0.0f, 0.0f);
    hilight = new RGBColor(0.0f, 0.0f, 0.0f);
    roughness = cloudiness = 0.0;
    bumpGrad = new Vec3();
  }
}