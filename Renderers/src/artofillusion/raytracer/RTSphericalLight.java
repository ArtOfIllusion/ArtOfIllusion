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
 * This subclass of RTLight is used for PointLights and SpotLights.  It distributes ray
 * targets uniformly over the volume of a sphere.
 */

public class RTSphericalLight extends RTLight
{
  private final double radius;

  public RTSphericalLight(PointLight light, CoordinateSystem coords, boolean softShadows)
  {
    super(light, coords);
    radius = (softShadows ? light.getRadius() : 0.0);
  }

  public RTSphericalLight(SpotLight light, CoordinateSystem coords, boolean softShadows)
  {
    super(light, coords);
    radius = (softShadows ? light.getRadius() : 0.0);
  }

  public double findRayToLight(Vec3 origin, Ray ray, int rayNumber)
  {
    ray.getOrigin().set(origin);
    Vec3 dir = ray.getDirection();
    dir.set(getCoords().getOrigin());
    if (rayNumber != -1)
      ray.rt.rt.randomizePoint(dir, ray.rt.random, radius, rayNumber);
    dir.subtract(origin);
    double distToLight = dir.length();
    dir.normalize();
    return distToLight;
  }
}
