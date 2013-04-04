/* Copyright (C) 1999-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.math.*;

/**
 * A Ray is defined by two vectors: an origin and a direction.  In addition, every ray has
 * a unique ID number which can be used to avoid repeated intersection tests.  Call newID()
 * to update the ID number any time the ray has been modified.
 *
 * Each Ray is bound to a particular {@link RaytracerContext} which is specified when it is created.
 * This means it is specific to that context's thread, and should only ever be used on that thread.
 */

public class Ray
{
  public final Vec3 origin, direction;
  public final Vec3 tempVec1, tempVec2, tempVec3, tempVec4;
  public final RaytracerContext rt;
  private int id;
  private static int nextid = 0;
  
  public Ray(RaytracerContext rt)
  {
    this.rt = rt;
    origin = new Vec3();
    direction = new Vec3();
    tempVec1 = new Vec3();
    tempVec2 = new Vec3();
    tempVec3 = new Vec3();
    tempVec4 = new Vec3();
    id = nextid++;
  }

  /**
   * Get the origin for this ray.
   */

  public Vec3 getOrigin()
  {
    return origin;
  }

  /**
   * Get the direction for this ray.
   */
  
  public Vec3 getDirection()
  {
    return direction;
  }

  /**
   * Get this ray's current ID.
   */
  
  public int getID()
  {
    return id;
  }

  /**
   * Assign a new ID to this ray.  This should be called any time it has been modified.
   */
  
  public void newID()
  {
    id = getNextID();
    rt.rtTriPool.reset();
    rt.rtDispTriPool.reset();
    rt.rtImplicitPool.reset();
  }

  private synchronized static int getNextID()
  {
    return nextid++;
  }

  /** Determine whether this ray intersects an object.  This returns a SurfaceIntersection describing the
      intersection, or SurfaceIntersection.NO_INTERSECTION if it does not intersect. */

  public SurfaceIntersection findIntersection(RTObject object)
  {
    int index = object.index;
    if (rt.lastRayID[index] != id)
    {
      rt.lastRayID[index] = id;
      rt.lastRayResult[index] = object.checkIntersection(this);
    }
    return rt.lastRayResult[index];
  }

  /** Determine whether this ray intersects a BoundingBox. */
  
  public boolean intersects(BoundingBox bb)
  {
    double t1, t2;
    double mint = -Double.MAX_VALUE;
    double maxt = Double.MAX_VALUE;
    if (direction.x == 0.0)
    {
      if (origin.x < bb.minx || origin.x > bb.maxx)
        return false;
    }
    else
    {
      t1 = (bb.minx-origin.x)/direction.x;
      t2 = (bb.maxx-origin.x)/direction.x;
      if (t1 < t2)
      {
        if (t1 > mint)
          mint = t1;
        if (t2 < maxt)
          maxt = t2;
      }
      else
      {
        if (t2 > mint)
          mint = t2;
        if (t1 < maxt)
          maxt = t1;
      }
      if (mint > maxt || maxt < 0.0)
        return false;
    }
    if (direction.y == 0.0)
    {
      if (origin.y < bb.miny || origin.y > bb.maxy)
        return false;
    }
    else
    {
      t1 = (bb.miny-origin.y)/direction.y;
      t2 = (bb.maxy-origin.y)/direction.y;
      if (t1 < t2)
      {
        if (t1 > mint)
          mint = t1;
        if (t2 < maxt)
          maxt = t2;
      }
      else
      {
        if (t2 > mint)
          mint = t2;
        if (t1 < maxt)
          maxt = t1;
      }
      if (mint > maxt || maxt < 0.0)
        return false;
    }
    if (direction.z == 0.0)
    {
      if (origin.z < bb.minz || origin.z > bb.maxz)
        return false;
    }
    else
    {
      t1 = (bb.minz-origin.z)/direction.z;
      t2 = (bb.maxz-origin.z)/direction.z;
      if (t1 < t2)
      {
        if (t1 > mint)
          mint = t1;
        if (t2 < maxt)
          maxt = t2;
      }
      else
      {
        if (t2 > mint)
          mint = t2;
        if (t1 < maxt)
          maxt = t1;
      }
      if (mint > maxt || maxt < 0.0)
        return false;
    }
    return true;
  }
}