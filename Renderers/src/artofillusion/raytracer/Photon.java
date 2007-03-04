/* Copyright (C) 2003 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.math.*;

/** This class represents a single photon used for photon mapping.  It stores the light intensity
    and color, position, incident direction, and a flag used for building the photon map.
    
    This is a slightly improved version of the photon data structure described in:
    
    Henrick Wann Jensen, "Realistic Image Synthesis Using Photon Mapping", A K Peters, Natick, MA, 2001. */

public class Photon
{
  public float x, y, z;
  public int ergb;
  public short direction, axis;
    
  /** Create a new Photon. */
  
  public Photon(Vec3 pos, Vec3 dir, RGBColor color)
  {
    x = (float) pos.x;
    y = (float) pos.y;
    z = (float) pos.z;
    int phi = (int) (Math.atan2(dir.z, dir.x)*128/Math.PI);
    if (phi < 0)
      phi += 256;
    int theta = (int) (Math.acos(dir.y)*256/Math.PI);
    direction = (short) ((phi<<8)+theta);
    ergb = color.getERGB();
  }
}