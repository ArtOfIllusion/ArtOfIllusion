/* Copyright (C) 2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.math.*;
import artofillusion.object.*;

/**
 * This subclass of RTLight is used for DirectionalLights.
 */

public class RTDirectionalLight extends RTLight
{
  public RTDirectionalLight(DirectionalLight light, CoordinateSystem coords)
  {
    super(light, coords);
  }

  public double findRayToLight(Vec3 origin, Ray ray, int rayNumber)
  {
    ray.getOrigin().set(origin);
    ray.getDirection().set(getCoords().getZDirection());
    ray.getDirection().scale(-1.0);
    return Double.MAX_VALUE;
  }
}
