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

public class RTImplicitObject extends RTObject
{
  private ImplicitObject theObject;
  private double minx, miny, minz, maxx, maxy, maxz;
  private double param[];
  private double tol;
  private boolean bumpMapped;
  private Mat4 toLocal, fromLocal;

  public static final double TOL = 1e-12;

  public RTImplicitObject(ImplicitObject implicit, Mat4 fromLocal, Mat4 toLocal, double param[], double tol)
  {
    BoundingBox bounds = implicit.getBounds();
    minx = bounds.minx;
    miny = bounds.miny;
    minz = bounds.minz;
    maxx = bounds.maxx;
    maxy = bounds.maxy;
    maxz = bounds.maxz;
    theObject = implicit;
    this.param = param;
    this.tol = tol;
    bumpMapped = implicit.getTexture().hasComponent(Texture.BUMP_COMPONENT);
    this.toLocal = toLocal;
    this.fromLocal = fromLocal;
  }

  /** Get the TextureMapping for this object. */

  public final TextureMapping getTextureMapping()
  {
    return theObject.getTextureMapping();
  }

  /** Get the MaterialMapping for this object. */

  public final MaterialMapping getMaterialMapping()
  {
    return theObject.getMaterialMapping();
  }

  /** Determine whether the given ray intersects this cube. */

  public SurfaceIntersection checkIntersection(Ray r)
  {
    Vec3 rorig = r.getOrigin(), rdir = r.getDirection();
    Vec3 origin = r.tempVec1;
    origin.set(rorig);
    toLocal.transform(origin);
    Vec3 direction = r.tempVec2;
    direction.set(rdir);
    toLocal.transformDirection(direction);

    // First check for intersections with the bounding box.

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

    // The ray intersects the bounding box, so we need to step along it through the volume
    // looking for actual intersections.

    double time = r.rt.rt.getTime();
    boolean wasInside = false;
    double t = mint;
    double cutoff = theObject.getCutoff();
    double prevValue;
    if (mint < 0.0)
    {
      t = 0.0;
      prevValue = theObject.getFieldValue(origin.x, origin.y, origin.z, tol, time);
      wasInside = (prevValue > cutoff);
    }
    else
    {
      double x = origin.x+t*direction.x;
      double y = origin.y+t*direction.y;
      double z = origin.z+t*direction.z;
      prevValue = theObject.getFieldValue(x, y, z, tol, time);
      if (prevValue > cutoff && mint > tol)
      {
        // The interior extends right up to the bounding box.

        Vec3 pos = r.tempVec3;
        pos.set(x, y, z);
        Vec3 norm = r.tempVec4;
        projectPoint(pos, norm);
        ImplicitIntersection intersect = (ImplicitIntersection) r.rt.rtImplicitPool.getObject();
        intersect.init(this, pos, t, norm, origin, direction, r, maxt);
        return intersect;
      }
    }
    double prevT = t;
    double stepScale = 1.0/theObject.getMaxGradient();
    while (t < maxt)
    {
      double nextStep = Math.abs(stepScale*(prevValue-cutoff));
      if (nextStep < tol)
        nextStep = tol;
      t += nextStep;
      if (t > maxt)
        t = maxt;
      double x = origin.x+t*direction.x;
      double y = origin.y+t*direction.y;
      double z = origin.z+t*direction.z;
      double value = theObject.getFieldValue(x, y, z, tol, time);
      boolean inside = (value > cutoff);
      if (inside != wasInside)
      {
        // We found an intersection.

        double trueT = t;
        if (t != prevT && value != prevValue)
        {
          // Interpolate to find a more accurate position.

          trueT = prevT + (cutoff-prevValue)*(t-prevT)/(value-prevValue);
          x = origin.x+trueT*direction.x;
          y = origin.y+trueT*direction.y;
          z = origin.z+trueT*direction.z;
        }

        // Ignore intersections too close to the ray origin to prevent the surface from
        // shadowing itself.

        if (trueT < tol)
          wasInside = inside;
        else
        {
          Vec3 pos = r.tempVec3;
          pos.set(x, y, z);
          Vec3 norm = r.tempVec4;
          theObject.getFieldGradient(x, y, z, tol, time, norm);
          norm.scale(-1.0/norm.length());
          if (Double.isNaN(norm.x))
            norm.set(1.0, 0.0, 0.0); // Avoid NaNs when the gradient is 0.
          fromLocal.transform(pos);
          fromLocal.transformDirection(norm);
          ImplicitIntersection intersect = (ImplicitIntersection) r.rt.rtImplicitPool.getObject();
          intersect.init(this, pos, trueT, norm, origin, direction, r, maxt);
          return intersect;
        }

      }
      prevT = t;
      prevValue = value;
    }
    if (wasInside)
    {
      // The interior extends right up to the bounding box.

      double x = origin.x+t*direction.x;
      double y = origin.y+t*direction.y;
      double z = origin.z+t*direction.z;
      Vec3 pos = r.tempVec3;
      pos.set(x, y, z);
      Vec3 norm = r.tempVec4;
      projectPoint(pos, norm);
      ImplicitIntersection intersect = (ImplicitIntersection) r.rt.rtImplicitPool.getObject();
      intersect.init(this, pos, t, norm, origin, direction, r, maxt);
      return intersect;
    }
    return SurfaceIntersection.NO_INTERSECTION;
  }

  /** This is called when the bounding box forms part of the surface of the object.  Given a point,
      project it onto the surface of the box.  This is necessary to
      prevent roundoff error.  Also set the true normal. */

  private void projectPoint(Vec3 pos, Vec3 normal)
  {
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
    fromLocal.transform(pos);

    // Set the normal.

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
    fromLocal.transformDirection(normal);
  }

  /** Get a bounding box for this cube. */

  public BoundingBox getBounds()
  {
    BoundingBox bounds = new BoundingBox(minx, maxx, miny, maxy, minz, maxz);
    bounds = bounds.transformAndOutset(fromLocal);
    return bounds;
  }

  /** Determine whether any part of the surface lies within a bounding box. */

  public boolean intersectsBox(BoundingBox bb)
  {
    return bb.intersects(getBounds());
  }

  /** Get the transformation from world coordinates to the object's local coordinates. */

  public Mat4 toLocal()
  {
    return toLocal;
  }

  /**
   * Inner class representing an intersection with an RTImplicitObject.
   */

  public static class ImplicitIntersection implements SurfaceIntersection
  {
    private RTImplicitObject obj;
    private double tint[], maxt;
    private Vec3 rint[], pos, orig, dir, norm;
    private int numIntersections;
    private Ray ray;

    public ImplicitIntersection()
    {
      tint = new double[1];
      rint = new Vec3[] {new Vec3()};
      pos = new Vec3();
      orig = new Vec3();
      dir = new Vec3();
      norm = new Vec3();
    }

    public void init(RTImplicitObject obj, Vec3 point1, double dist1, Vec3 trueNorm, Vec3 origin, Vec3 direction, Ray ray, double maxDist)
    {
      this.obj = obj;
      this.ray = ray;
      numIntersections = -1;
      tint[0] = dist1;
      rint[0].set(point1);
      orig.set(origin);
      dir.set(direction);
      norm.set(trueNorm);
      maxt = maxDist;
    }

    public RTObject getObject()
    {
      return obj;
    }

    public int numIntersections()
    {
      if (numIntersections == -1)
        findAllIntersections();
      return numIntersections;
    }

    public void intersectionPoint(int n, Vec3 p)
    {
      p.set(rint[n]);
    }

    public double intersectionDist(int n)
    {
      return tint[n];
    }

    public void intersectionProperties(TextureSpec spec, Vec3 n, Vec3 viewDir, double size, double time)
    {
      n.set(norm);
      TextureMapping map = obj.theObject.getTextureMapping();
      pos.set(rint[0]);
      if (map instanceof UniformMapping)
        map.getTextureSpec(pos, spec, -n.dot(viewDir), size, time, obj.param);
      else
      {
        obj.toLocal.transform(pos);
        map.getTextureSpec(pos, spec, -n.dot(viewDir), size, time, obj.param);
      }
      if (obj.bumpMapped)
      {
        obj.fromLocal.transformDirection(spec.bumpGrad);
        n.scale(spec.bumpGrad.dot(n)+1.0);
        n.subtract(spec.bumpGrad);
        n.normalize();
      }
    }

    public void intersectionTransparency(int n, RGBColor trans, double angle, double size, double time)
    {
      TextureMapping map = obj.theObject.getTextureMapping();
      pos.set(rint[n]);
      if (map instanceof UniformMapping)
        map.getTransparency(pos, trans, angle, size, time, obj.param);
      else
      {
        obj.toLocal.transform(pos);
        map.getTransparency(pos, trans, angle, size, time, obj.param);
      }
    }

    public void trueNormal(Vec3 n)
    {
      n.set(norm);
    }

    /** Find all intersection points with the ray.  This is called in response to a call to
        numIntersections(), since that indicates that all intersections will be needed, not
        just the first. */

    private void findAllIntersections()
    {
      double cutoff = obj.theObject.getCutoff();
      double time = ray.rt.rt.getTime();
      double t = tint[0];
      double prevT = t;
      double x = orig.x+t*dir.x;
      double y = orig.y+t*dir.y;
      double z = orig.z+t*dir.z;
      double prevValue = obj.theObject.getFieldValue(x, y, z, obj.tol, time);
//      boolean wasInside = (prevValue > cutoff);
      boolean wasInside = (norm.dot(ray.getDirection()) < 0.0);
      numIntersections = 1;
      while (t < maxt)
      {
        t += obj.tol;
        if (t > maxt)
          t = maxt;
        x = orig.x+t*dir.x;
        y = orig.y+t*dir.y;
        z = orig.z+t*dir.z;
        double value = obj.theObject.getFieldValue(x, y, z, obj.tol, time);
        boolean inside = (value > cutoff);
        if (inside != wasInside || (t == maxt && wasInside))
        {
          // We found an intersection.

          if (numIntersections == tint.length)
          {
            double newt[] = new double [numIntersections*2];
            Vec3 newr[] = new Vec3 [numIntersections*2];
            for (int j = 0; j < tint.length; j++)
            {
              newt[j] = tint[j];
              newr[j] = rint[j];
            }
            for (int j = tint.length; j < newt.length; j++)
              newr[j] = new Vec3();
            tint = newt;
            rint = newr;
          }
          double trueT = t;
          if (t != prevT && value != prevValue && inside != wasInside)
          {
            // Interpolate to find a more accurate position.

            trueT = prevT + (cutoff-prevValue)*(t-prevT)/(value-prevValue);
            x = orig.x+trueT*dir.x;
            y = orig.y+trueT*dir.y;
            z = orig.z+trueT*dir.z;
          }
          tint[numIntersections] = trueT;
          rint[numIntersections].set(ray.origin.x+trueT*ray.direction.x,
            ray.origin.y+trueT*ray.direction.y,
            ray.origin.z+trueT*ray.direction.z);
          numIntersections++;
        }
        wasInside = (value > cutoff);
        prevT = t;
        prevValue = value;
      }
    }
  }
}
