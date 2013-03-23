/* Copyright (C) 2005-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.math.Vec3;
import artofillusion.math.RGBColor;
import artofillusion.texture.TextureSpec;

/**
 * A SurfaceIntersection records information about where an RTObject was hit by a Ray.
 */

public interface SurfaceIntersection
{
  /**
   * This is a single global object that is always returned to indicate no intersections.
   */

  public static final SurfaceIntersection NO_INTERSECTION = new SurfaceIntersection() {
    public RTObject getObject()
    {
      return null;
    }
    public int numIntersections()
    {
      return 0;
    }
    public void intersectionPoint(int n, Vec3 p)
    {
    }
    public double intersectionDist(int n)
    {
      return 0.0;
    }
    public void intersectionProperties(TextureSpec spec, Vec3 n, Vec3 viewDir, double size, double time)
    {
    }
    public void intersectionTransparency(int n, RGBColor trans, double angle, double size, double time)
    {
    }
    public void trueNormal(Vec3 n)
    {
    }
  };

  /** Get the object that was hit by the ray.  If no object was hit, this will be null. */

  public RTObject getObject();

  /** Get the number of times the ray intersects the object. */

  public int numIntersections();

  /** Get the point of intersection.
      @param n      the index of the intersection point (0 to numIntersections())
      @param p      the intersection point is returned in this
   */

  public void intersectionPoint(int n, Vec3 p);

  /** Get the distance from the ray origin to the nth point of intersection. */

  public double intersectionDist(int n);

  /** Get the surface properties at the point of intersection.
      @param spec        the texture properties will be stored in this
      @param n           the surface normal will be stored in this
      @param viewDir     the direction from which the surface is being viewed
      @param size        the width of the region over which the texture should be averaged
      @param time        the time for which the image is being rendered
  */

  public void intersectionProperties(TextureSpec spec, Vec3 n, Vec3 viewDir, double size, double time);

  /** Same as intersectionProperties(), except only return the transparent color.  This can save time in cases
      where only the transparency is required, for example, when tracing shadow rays.  This
      also allows you to specify which intersection to get the transparency for. */

  public void intersectionTransparency(int n, RGBColor trans, double angle, double size, double time);

  /** Get the normal of the "true" surface (without interpolation, bump mapping, etc.)
      at the nth point of intersection. */

  public void trueNormal(Vec3 n);
}
