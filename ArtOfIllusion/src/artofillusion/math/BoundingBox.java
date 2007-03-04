/* Copyright (C) 1999-2000 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;

/** The BoundingBox class describes a 3-dimensional rectangular box which is aligned with the
    coordinate axes. */

public class BoundingBox
{
  public double minx, maxx, miny, maxy, minz, maxz;

  /** Create a BoundingBox by specifying the upper and lower values along the X, Y, and Z axes. */

  public BoundingBox(double x1, double x2, double y1, double y2, double z1, double z2)
  {
    minx = Math.min(x1, x2);
    maxx = Math.max(x1, x2);
    miny = Math.min(y1, y2);
    maxy = Math.max(y1, y2);
    minz = Math.min(z1, z2);
    maxz = Math.max(z1, z2);
  }
  
  /** Create a BoundingBox by specifying two opposite corners. */

  public BoundingBox(Vec3 p1, Vec3 p2)
  {
    minx = Math.min(p1.x, p2.x);
    maxx = Math.max(p1.x, p2.x);
    miny = Math.min(p1.y, p2.y);
    maxy = Math.max(p1.y, p2.y);
    minz = Math.min(p1.z, p2.z);
    maxz = Math.max(p1.z, p2.z);
  }

  /** Create a new BoundingBox identical to another one. */

  public BoundingBox(BoundingBox b)
  {
    minx = b.minx;
    miny = b.miny;
    minz = b.minz;
    maxx = b.maxx;
    maxy = b.maxy;
    maxz = b.maxz;
  }
  
  /** Get a vector containing the dimensions of the box. */
  
  public Vec3 getSize()
  {
    return new Vec3(maxx-minx, maxy-miny, maxz-minz);
  }
  
  /** Get a vector to the center of the box. */
  
  public Vec3 getCenter()
  {
    return new Vec3((maxx+minx)/2.0, (maxy+miny)/2.0, (maxz+minz)/2.0);
  }
  
  /** Get an array containing the coordinates of the corners of the box. */
  
  public Vec3 [] getCorners()
  {
    return new Vec3 [] {
      new Vec3(minx, miny, minz),
      new Vec3(minx, miny, maxz),
      new Vec3(minx, maxy, minz),
      new Vec3(minx, maxy, maxz),
      new Vec3(maxx, miny, minz),
      new Vec3(maxx, miny, maxz),
      new Vec3(maxx, maxy, minz),
      new Vec3(maxx, maxy, maxz)
    };
  }
  
  /** Return a new bounding box which contains both this box and another specified one. */
  
  public BoundingBox merge(BoundingBox b)
  {
    return new BoundingBox(Math.min(minx, b.minx), Math.max(maxx, b.maxx), 
      Math.min(miny, b.miny), Math.max(maxy, b.maxy), Math.min(minz, b.minz), 
      Math.max(maxz, b.maxz));
  }

  /** Extend this bounding box to also contain the contents of another one. */

  public void extend(BoundingBox b)
  {
    if (b.minx < minx)
      minx = b.minx;
    if (b.miny < miny)
      miny = b.miny;
    if (b.minz < minz)
      minz = b.minz;
    if (b.maxx > maxx)
      maxx = b.maxx;
    if (b.maxy > maxy)
      maxy = b.maxy;
    if (b.maxz > maxz)
      maxz = b.maxz;
  }
  
  /** Determine whether the given point lies inside the box. */
  
  public final boolean contains(Vec3 p)
  {
    if (p.x < minx || p.x > maxx || p.y < miny || p.y > maxy || p.z < minz || p.z > maxz)
      return false;
    return true;
  }
  
  /** Determine whether two bounding boxes intersect each other. */
  
  public final boolean intersects(BoundingBox b)
  {
    if (minx > b.maxx || maxx < b.minx || miny > b.maxy || maxy < b.miny || minz > b.maxz || maxz < b.minz)
      return false;
    return true;
  }

  /** Determine the distance between a point and the closest point in the box. */
  
  public final double distanceToPoint(Vec3 p)
  {
    double x, y, z;
    
    if (p.x < minx)
      x = minx-p.x;
    else if (p.x > maxx)
      x = p.x-maxx;
    else
      x = 0.0;
    if (p.y < miny)
      y = miny-p.y;
    else if (p.y > maxy)
      y = p.y-maxy;
    else
      y = 0.0;
    if (p.z < minz)
      z = minz-p.z;
    else if (p.z > maxz)
      z = p.z-maxz;
    else
      z = 0.0;
    return Math.sqrt(x*x + y*y + z*z);
  }
  
  /** Outset the bounding box by a fixed amount in every direction. */
  
  public final void outset(double dist)
  {
    minx -= dist;
    miny -= dist;
    minz -= dist;
    maxx += dist;
    maxy += dist;
    maxz += dist;
  }
  
  /** Return a new bounding box which is translated from this one by the specified amount. */
  
  public final BoundingBox translate(double dx, double dy, double dz)
  {
    return new BoundingBox(minx+dx, maxx+dx, miny+dy, maxy+dy, minz+dz, maxz+dz);
  }

  /** This method applies a transformation matrix M to each of the eight corners of the box,
      then generates a new BoundingBox which is large enough to contain the transformed box. */
  
  public final BoundingBox transformAndOutset(Mat4 m)
  {
    Vec3 p, corner[] = getCorners();
    double newminx, newmaxx, newminy, newmaxy, newminz, newmaxz;

    p = m.times(corner[0]);
    newminx = newmaxx = p.x;
    newminy = newmaxy = p.y;
    newminz = newmaxz = p.z;
    for (int i = 1; i < 8; i++)
      {
        p = m.times(corner[i]);
        if (p.x < newminx) newminx = p.x;
        if (p.x > newmaxx) newmaxx = p.x;
        if (p.y < newminy) newminy = p.y;
        if (p.y > newmaxy) newmaxy = p.y;
        if (p.z < newminz) newminz = p.z;
        if (p.z > newmaxz) newmaxz = p.z;
      }
    return new BoundingBox(newminx, newmaxx, newminy, newmaxy, newminz, newmaxz);
  }

  public String toString()
  {
    return "Box: {"+minx+", "+maxx+"}   {"+miny+", "+maxy+"}   {"+minz+", "+maxz+"}";
  }
}