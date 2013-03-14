/* Copyright (C) 2008-2013 by Peter Eastman

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
  private final double radius;

  public RTDirectionalLight(DirectionalLight light, CoordinateSystem coords, boolean softShadows)
  {
    super(light, coords);
    radius = (softShadows ? Math.tan(light.getRadius()*Math.PI/180.0) : 0.0);
  }

  public double findRayToLight(Vec3 origin, Ray ray, RaytracerRenderer renderer, int rayNumber)
  {
    ray.getOrigin().set(origin);
    Vec3 dir = ray.getDirection();
    dir.set(getCoords().getZDirection());
    dir.scale(-1.0);
    if (rayNumber != -1)
    {
      renderer.randomizePoint(dir, ray.rt.random, radius, rayNumber);
      dir.normalize();
    }
    return Double.MAX_VALUE;
  }

  @Override
  public boolean getSoftShadows()
  {
    return radius != 0.0 && getLight().getType() == Light.TYPE_NORMAL;
  }
}
