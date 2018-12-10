/* Copyright (C) 2005-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.math.*;
import artofillusion.raytracer.Raytracer.*;

import java.util.*;

/**
 * This class holds temporary information used during raytracing.  One instance of it is
 * created for every worker thread.
 */

public class RaytracerContext
{
  public Raytracer rt;
  public Vec3 tempVec;
  public RayIntersection intersect;
  public int lastRayID[];
  public SurfaceIntersection lastRayResult[];
  public ResourcePool rtTriPool, rtDispTriPool, rtImplicitPool;
  public Random random;

  public RaytracerContext(Raytracer rt)
  {
    this.rt = rt;
    tempVec = new Vec3();
    intersect = new Raytracer.RayIntersection();
    random = new FastRandom(0);
    if (rt.getUseReducedMemory())
      rtTriPool = new ResourcePool(RTTriangleLowMemory.TriangleIntersection.class);
    else
      rtTriPool = new ResourcePool(RTTriangle.TriangleIntersection.class);
    rtDispTriPool = new ResourcePool(RTDisplacedTriangle.DisplacedTriangleIntersection.class);
    rtImplicitPool = new ResourcePool(RTImplicitObject.ImplicitIntersection.class);
    lastRayID = new int [rt.getObjects().length];
    lastRayResult = new SurfaceIntersection [rt.getObjects().length];
  }

  /**
   * This is called when rendering is finished.  It nulls out fields to help garbage collection.
   */

  public void cleanup()
  {
    intersect = null;
    lastRayID = null;
    lastRayResult = null;
    rtTriPool = null;
    rtDispTriPool = null;
    rtImplicitPool = null;
  }
}
