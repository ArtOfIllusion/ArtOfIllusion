/* Copyright (C) 2004-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.material.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;

/** RTCube represents a cube (or more generally, any rectangular box) to be raytraced.  It is
    defined by specifying a Cube object, and the transformations to and from local coordinates. */

public class RTCube extends RTObject
{
  private Cube theCube;
  protected double minx, miny, minz, maxx, maxy, maxz;
  protected double param[];
  private boolean bumpMapped, transform;
  protected Mat4 toLocal, fromLocal;
  
  public static final double TOL = 1e-12;

  public RTCube(Cube cube, Mat4 fromLocal, Mat4 toLocal, double param[])
  {
    Vec3 size = cube.getBounds().getSize();
    Vec3 vx = toLocal.timesDirection(Vec3.vx()), vy = toLocal.timesDirection(Vec3.vy());
    theCube = cube;
    this.param = param;
    double xsize = size.x, ysize = size.y, zsize = size.z;
    transform = true;
    if (vx.x == 1.0 || vx.x == -1.0)
    {
      if (vy.y == 1.0 || vy.y == -1.0)
      {
        transform = false;
      }
      else if (vy.z == 1.0 || vy.z == -1.0)
      {
        ysize = size.z;
        zsize = size.y;
        transform = false;
      }
    }
    else if (vx.y == 1.0 || vx.y == -1.0)
    {
      if (vy.x == 1.0 || vy.x == -1.0)
      {
        xsize = size.y;
        ysize = size.x;
        transform = false;
      }
      else if (vy.z == 1.0 || vy.z == -1.0)
      {
        xsize = size.y;
        ysize = size.z;
        zsize = size.x;
        transform = false;
      }
    }
    else if (vx.z == 1.0 || vx.z == -1.0)
    {
      if (vy.x == 1.0 || vy.x == -1.0)
      {
        xsize = size.z;
        ysize = size.x;
        zsize = size.y;
        transform = false;
      }
      else if (vy.y == 1.0 || vy.y == -1.0)
      {
        xsize = size.z;
        zsize = size.x;
        transform = false;
      }
    }
    if (transform)
    {
      this.fromLocal = fromLocal;
      minx = -0.5*xsize;
      miny = -0.5*ysize;
      minz = -0.5*zsize;
      maxx = 0.5*xsize;
      maxy = 0.5*ysize;
      maxz = 0.5*zsize;
    }
    else
    {
      Vec3 center = fromLocal.times(new Vec3());
      minx = center.x-0.5*xsize;
      miny = center.y-0.5*ysize;
      minz = center.z-0.5*zsize;
      maxx = center.x+0.5*xsize;
      maxy = center.y+0.5*ysize;
      maxz = center.z+0.5*zsize;
    }
    bumpMapped = cube.getTexture().hasComponent(Texture.BUMP_COMPONENT);
    this.toLocal = toLocal;
  }

  /** Get the TextureMapping for this object. */
  
  public final TextureMapping getTextureMapping()
  {
    return theCube.getTextureMapping();
  }

  /** Get the MaterialMapping for this object. */
  
  public final MaterialMapping getMaterialMapping()
  {
    return theCube.getMaterialMapping();
  }  

  /** Determine whether the given ray intersects this cube. */

  public SurfaceIntersection checkIntersection(Ray r)
  {
    Vec3 rorig = r.getOrigin(), rdir = r.getDirection();
    Vec3 origin, direction;
    if (transform)
    {
      origin = r.tempVec1;
      origin.set(rorig);
      toLocal.transform(origin);
      direction = r.tempVec2;
      direction.set(rdir);
      toLocal.transformDirection(direction);
    }
    else
    {
      origin = rorig;
      direction = rdir;
    }
    double mint = -Double.MAX_VALUE;
    double maxt = Double.MAX_VALUE;
    if (direction.x == 0.0)
    {
      if (origin.x < minx || origin.x > maxx)
        return SurfaceIntersection.NO_INTERSECTION;
    }
    else
    {
      double t1 = (minx-origin.x)/direction.x;
      double t2 = (maxx-origin.x)/direction.x;
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
      if (mint > maxt || maxt < TOL)
        return SurfaceIntersection.NO_INTERSECTION;
    }
    if (direction.y == 0.0)
    {
      if (origin.y < miny || origin.y > maxy)
        return SurfaceIntersection.NO_INTERSECTION;
    }
    else
    {
      double t1 = (miny-origin.y)/direction.y;
      double t2 = (maxy-origin.y)/direction.y;
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
      if (mint > maxt || maxt < TOL)
        return SurfaceIntersection.NO_INTERSECTION;
    }
    if (direction.z == 0.0)
    {
      if (origin.z < minz || origin.z > maxz)
        return SurfaceIntersection.NO_INTERSECTION;
    }
    else
    {
      double t1 = (minz-origin.z)/direction.z;
      double t2 = (maxz-origin.z)/direction.z;
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
      if (mint > maxt || maxt < TOL)
        return SurfaceIntersection.NO_INTERSECTION;
    }
    int numIntersections;
    Vec3 v1 = r.tempVec1, v2 = r.tempVec2, trueNorm = r.tempVec3;
    if (mint < TOL)
    {
      v1.set(rorig.x+maxt*rdir.x, rorig.y+maxt*rdir.y, rorig.z+maxt*rdir.z);
      mint = maxt;
      numIntersections = 1;
    }
    else
    {
      v2.set(rorig.x+maxt*rdir.x, rorig.y+maxt*rdir.y, rorig.z+maxt*rdir.z);
      projectPoint(v2, null);
      v1.set(rorig.x+mint*rdir.x, rorig.y+mint*rdir.y, rorig.z+mint*rdir.z);
      numIntersections = 2;
    }
    projectPoint(v1, trueNorm);
    return new CubeIntersection(this, numIntersections, v1, v2, mint, maxt, trueNorm);
  }
  
  /** Given a point, project it onto the surface of the cube.  This is necessary to
      prevent roundoff error.  This routine can optionally set the true normal, since
      it is easy to do at the same time. */
  
  private void projectPoint(Vec3 pos, Vec3 normal)
  {
    if (transform)
      toLocal.transform(pos);
    
    // Determine which side it is closest to.
    
    int side = 0;
    double mindist = Math.abs(pos.x-minx), dist;
    dist = Math.abs(pos.x-maxx);
    if (dist < mindist)
    {
      mindist = dist;
      side = 1;
    }
    dist = Math.abs(pos.y-miny);
    if (dist < mindist)
    {
      mindist = dist;
      side = 2;
    }
    dist = Math.abs(pos.y-maxy);
    if (dist < mindist)
    {
      mindist = dist;
      side = 3;
    }
    dist = Math.abs(pos.z-minz);
    if (dist < mindist)
    {
      mindist = dist;
      side = 4;
    }
    dist = Math.abs(pos.z-maxz);
    if (dist < mindist)
    {
      mindist = dist;
      side = 5;
    }
    
    // Project it onto the appropriate side.
    
    if (side == 0)
      pos.x = minx;
    else if (side == 1)
      pos.x = maxx;
    else if (side == 2)
      pos.y = miny;
    else if (side == 3)
      pos.y = maxy;
    else if (side == 4)
      pos.z = minz;
    else if (side == 5)
      pos.z = maxz;
    if (transform)
      fromLocal.transform(pos);
    
    // If requested, also set the normal.
    
    if (normal != null)
    {
      if (side == 0)
        normal.set(-1.0, 0.0, 0.0);
      if (side == 1)
        normal.set(1.0, 0.0, 0.0);
      if (side == 2)
        normal.set(0.0, -1.0, 0.0);
      if (side == 3)
        normal.set(0.0, 1.0, 0.0);
      if (side == 4)
        normal.set(0.0, 0.0, -1.0);
      if (side == 5)
        normal.set(0.0, 0.0, 1.0);
      if (transform)
        fromLocal.transformDirection(normal);
    }
  }

  /** Get a bounding box for this cube. */
  
  public BoundingBox getBounds()
  {
    BoundingBox bounds = new BoundingBox(minx, maxx, miny, maxy, minz, maxz);
    if (transform)
      bounds = bounds.transformAndOutset(fromLocal);
    return bounds;
  }

  /** Determine whether any part of the surface of the cube lies within a bounding box. */

  public boolean intersectsBox(BoundingBox bb)
  {
    if (!bb.intersects(getBounds()))
      return false;
    
    // Check whether the box is entirely contained within this object.
    
    if (transform)
      bb = bb.transformAndOutset(toLocal);
    if (bb.minx > minx && bb.maxx < maxx && bb.miny > miny && bb.maxy < maxy && bb.minz > minz && bb.maxz < maxz)
      return false;
    return true;
  }
  
  /** Get the transformation from world coordinates to the object's local coordinates. */
  
  public Mat4 toLocal()
  {
    return toLocal;
  }

  /**
   * Inner class representing an intersection with an RTCube.
   */

  private static class CubeIntersection implements SurfaceIntersection
  {
    private RTCube cube;
    private int numIntersections;
    private double dist1, dist2, r1x, r1y, r1z, r2x, r2y, r2z, normx, normy, normz;
    private Vec3 pos;

    public CubeIntersection(RTCube cube, int numIntersections, Vec3 point1, Vec3 point2, double dist1, double dist2, Vec3 trueNorm)
    {
      this.cube = cube;
      this.numIntersections = numIntersections;
      this.dist1 = dist1;
      this.dist2 = dist2;
      r1x = point1.x;
      r1y = point1.y;
      r1z = point1.z;
      r2x = point2.x;
      r2y = point2.y;
      r2z = point2.z;
      normx = trueNorm.x;
      normy = trueNorm.y;
      normz = trueNorm.z;
      pos = new Vec3();
    }

    public RTObject getObject()
    {
      return cube;
    }

    public int numIntersections()
    {
      return numIntersections;
    }

    public void intersectionPoint(int n, Vec3 p)
    {
      if (n == 0)
        p.set(r1x, r1y, r1z);
      else
        p.set(r2x, r2y, r2z);
    }

    public double intersectionDist(int n)
    {
      if (n == 0)
        return dist1;
      else
        return dist2;
    }

    public void intersectionProperties(TextureSpec spec, Vec3 n, Vec3 viewDir, double size, double time)
    {
      n.set(normx, normy, normz);
      TextureMapping map = cube.theCube.getTextureMapping();
      pos.set(r1x, r1y, r1z);
      if (map instanceof UniformMapping)
        map.getTextureSpec(pos, spec, -n.dot(viewDir), size, time, cube.param);
      else
      {
        cube.toLocal.transform(pos);
        map.getTextureSpec(pos, spec, -n.dot(viewDir), size, time, cube.param);
      }
      if (cube.bumpMapped)
      {
        if (cube.transform)
          cube.fromLocal.transformDirection(spec.bumpGrad);
        n.scale(spec.bumpGrad.dot(n)+1.0);
        n.subtract(spec.bumpGrad);
        n.normalize();
      }
    }

    public void intersectionTransparency(int n, RGBColor trans, double angle, double size, double time)
    {
      TextureMapping map = cube.theCube.getTextureMapping();
      if (n == 0)
        pos.set(r1x, r1y, r1z);
      else
        pos.set(r2x, r2y, r2z);
      if (map instanceof UniformMapping)
        map.getTransparency(pos, trans, angle, size, time, cube.param);
      else
      {
        cube.toLocal.transform(pos);
        map.getTransparency(pos, trans, angle, size, time, cube.param);
      }
    }

    public void trueNormal(Vec3 n)
    {
      n.set(normx, normy, normz);
    }
  }
}
