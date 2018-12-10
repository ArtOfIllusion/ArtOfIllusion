/* Copyright (C) 2008-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.object.*;
import artofillusion.math.*;

/**
 * RTLight represents a light source in the scene to be raytraced.  It is an abstract
 * class, with subclasses for particular types of lights.
 */

public abstract class RTLight
{
  private final Light light;
  private final CoordinateSystem coords;

  /**
   * Create an RTLight.
   *
   * @param light    the Light object
   * @param coords   the location and orientation of the light
   */
  public RTLight(Light light, CoordinateSystem coords)
  {
    this.light = light;
    this.coords = coords;
  }

  public Light getLight()
  {
    return light;
  }

  public CoordinateSystem getCoords()
  {
    return coords;
  }

  /**
   * This is called by the raytracer to generate a shadow ray from a point in the scene
   * to the light.
   *
   * @param origin      the location from which to trace a ray
   * @param ray         this should be configured to be a ray pointing from the specified
   *                    origin to a randomly chosen point inside the light
   * @param renderer    the renderer being used to render an image
   * @param rayNumber   a number which will be different for different rays, and can be used
   *                    for stratified sampling.  If this is -1, that indicates that soft
   *                    shadows should be disabled.
   * @return the distance from the ray origin to the light
   */
  public abstract double findRayToLight(Vec3 origin, Ray ray, RaytracerRenderer renderer, int rayNumber);

  /**
   * Get whether this light generates soft shadows.
   */
  public abstract boolean getSoftShadows();
}
