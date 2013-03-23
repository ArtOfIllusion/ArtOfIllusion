/* Copyright (C) 1999-2013 by Peter Eastman

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

/** RTEllipsoid represents an ellipsoid to be raytraced.  It is defined by specifying a Sphere 
    object, and the transformations to and from local coordinates. */

public class RTEllipsoid extends RTObject
{
  Sphere theSphere;
  double rx, ry, rz, rx2, ry2, rz2, cx, cy, cz, sy, sz, param[];
  boolean bumpMapped, transform, uniform;
  Mat4 toLocal, fromLocal;
  
  public static final double TOL = 1e-12;

  public RTEllipsoid(Sphere sphere, Mat4 fromLocal, Mat4 toLocal, double param[])
  {
    Vec3 radii = sphere.getRadii();
    Vec3 vx = toLocal.timesDirection(Vec3.vx()), vy = toLocal.timesDirection(Vec3.vy());
    theSphere = sphere;
    this.param = param;
    uniform = sphere.getTextureMapping() instanceof UniformMapping;
    transform = true;
    if (vx.x == 1.0 || vx.x == -1.0)
      {
        if (vy.y == 1.0 || vy.y == -1.0)
          {
            rx = radii.x;
            ry = radii.y;
            rz = radii.z;
            transform = false;
          }
        else if (vy.z == 1.0 || vy.z == -1.0)
          {
            rx = radii.x;
            ry = radii.z;
            rz = radii.y;
            transform = false;
          }
      }
    else if (vx.y == 1.0 || vx.y == -1.0)
      {
        if (vy.x == 1.0 || vy.x == -1.0)
          {
            rx = radii.y;
            ry = radii.x;
            rz = radii.z;
            transform = false;
          }
        else if (vy.z == 1.0 || vy.z == -1.0)
          {
            rx = radii.y;
            ry = radii.z;
            rz = radii.x;
            transform = false;
          }
      }
    else if (vx.z == 1.0 || vx.z == -1.0)
      {
        if (vy.x == 1.0 || vy.x == -1.0)
          {
            rx = radii.z;
            ry = radii.x;
            rz = radii.y;
            transform = false;
          }
        else if (vy.y == 1.0 || vy.y == -1.0)
          {
            rx = radii.z;
            ry = radii.y;
            rz = radii.x;
            transform = false;
          }
      }
    if (transform)
      {
        rx = radii.x;
        ry = radii.y;
        rz = radii.z;
        this.fromLocal = fromLocal;
      }
    cx = fromLocal.m14/fromLocal.m44;
    cy = fromLocal.m24/fromLocal.m44;
    cz = fromLocal.m34/fromLocal.m44;
    rx2 = rx*rx;
    ry2 = ry*ry;
    rz2 = rz*rz;
    sy = rx2/ry2;
    sz = rx2/rz2;
    bumpMapped = sphere.getTexture().hasComponent(Texture.BUMP_COMPONENT);
    this.toLocal = toLocal;
  }

  /** Get the TextureMapping for this object. */
  
  public final TextureMapping getTextureMapping()
  {
    return theSphere.getTextureMapping();
  }

  /** Get the MaterialMapping for this object. */
  
  public final MaterialMapping getMaterialMapping()
  {
    return theSphere.getMaterialMapping();
  }  

  /** Determine whether the given ray intersects this sphere. */

  public SurfaceIntersection checkIntersection(Ray r)
  {
    Vec3 orig = r.getOrigin(), rdir = r.getDirection();
    Vec3 v1 = r.tempVec1, v2 = r.tempVec2, dir = r.tempVec3;
    double a, b, c, d, temp1, temp2;
    double dist1, dist2 = 0;

    v1.set(cx-orig.x, cy-orig.y, cz-orig.z);
    if (transform)
      {
        toLocal.transformDirection(v1);
        dir.set(rdir);
        toLocal.transformDirection(dir);
      }
    else if (uniform)
      dir = rdir;
    else
      dir.set(rdir);
    temp1 = sy*dir.y;
    temp2 = sz*dir.z;
    b = dir.x*v1.x + temp1*v1.y + temp2*v1.z;
    c = v1.x*v1.x + sy*v1.y*v1.y + sz*v1.z*v1.z - rx2;
    int numIntersections;
    if (c > TOL*b)
      {
        // Ray origin is outside ellipsoid.

        if (b <= 0.0)
          return SurfaceIntersection.NO_INTERSECTION;  // Ray points away from the ellipsoid.
        a = dir.x*dir.x + temp1*dir.y + temp2*dir.z;
        d = b*b - a*c;
        if (d < 0.0)
          return SurfaceIntersection.NO_INTERSECTION;
        numIntersections = 2;
        temp1 = Math.sqrt(d);
        dist1 = (b - temp1)/a;
        dist2 = (b + temp1)/a;
        v2.set(orig.x+dist2*dir.x, orig.y+dist2*dir.y, orig.z+dist2*dir.z);
        projectPoint(v2);
      }
    else if (c < -TOL*b)
      {
        // Ray origin is inside ellipsoid.

        a = dir.x*dir.x + temp1*dir.y + temp2*dir.z;
        d = b*b - a*c;
        if (d < 0.0)
          return SurfaceIntersection.NO_INTERSECTION;
        numIntersections = 1;
        dist1 = (b + Math.sqrt(d))/a;
      }
    else
      {
        // Ray origin is on the surface of the ellipsoid.

        if (b <= 0.0)
          return SurfaceIntersection.NO_INTERSECTION;  // Ray points away from the ellipsoid.
        a = dir.x*dir.x + temp1*dir.y + temp2*dir.z;
        d = b*b - a*c;
        if (d < 0.0)
          return SurfaceIntersection.NO_INTERSECTION;
        numIntersections = 1;
        dist1 = (b + Math.sqrt(d))/a;
      }
    v1.set(orig.x+dist1*rdir.x, orig.y+dist1*rdir.y, orig.z+dist1*rdir.z);
    projectPoint(v1);
    return new EllipsoidIntersection(this, numIntersections, v1, v2, dist1, dist2);
  }
  
  /** Given a point, project it onto the surface of the ellipsoid.  This is necessary to
      prevent roundoff error. */
  
  private void projectPoint(Vec3 pos)
  {
    if (transform)
    {
      toLocal.transform(pos);
      double scale = rx/Math.sqrt(pos.x*pos.x+sy*pos.y*pos.y+sz*pos.z*pos.z);
      pos.scale(scale);
      fromLocal.transform(pos);
    }
    else
    {
      double dx = pos.x-cx, dy = pos.y-cy, dz = pos.z-cz;
      double scale = rx/Math.sqrt(dx*dx+sy*dy*dy+sz*dz*dz);
      pos.set(cx+dx*scale, cy+dy*scale, cz+dz*scale);
    }
  }

  /** Get a bounding box for this ellipsoid. */
  
  public BoundingBox getBounds()
  {
    if (transform)
      return (new BoundingBox(-rx, rx, -ry, ry, -rz, rz)).transformAndOutset(fromLocal);
    else
      return new BoundingBox(cx-rx, cx+rx, cy-ry, cy+ry, cz-rz, cz+rz);
  }

  /** Determine whether any part of the surface of the ellipsoid lies within a bounding box. */

  public boolean intersectsBox(BoundingBox bb)
  {
    double dx, dy, dz, centerx, centery, centerz;

    if (transform)
      {
        bb = bb.transformAndOutset(toLocal);
        centerx = centery = centerz = 0.0;
      }
    else
      {
        centerx = cx;
        centery = cy;
        centerz = cz;
      }
    Vec3 c = new Vec3(centerx, centery, centerz);

    // Find the nearest point of the box to the ellipsoid.

    if (centerx < bb.minx)
      c.x = bb.minx;
    else if (centerx > bb.maxx)
      c.x = bb.maxx;
    if (centery < bb.miny)
      c.y = bb.miny;
    else if (centery > bb.maxy)
      c.y = bb.maxy;
    if (centerz < bb.minz)
      c.z = bb.minz;
    else if (centerz > bb.maxz)
      c.z = bb.maxz;
    
    // If the ellipsoid lies entirely outside the box, return false.
    
    if (!transform)
      c.set(c.x-centerx, c.y-centery, c.z-centerz);
    if (c.x*c.x + c.y*c.y*sy + c.z*c.z*sz > rx2)
      return false;

    // If the box is completely inside the ellipsoid, return false.  Otherwise, return true.

    dx = 1.0/rx;
    dy = 1.0/ry;
    dz = 1.0/rz;
    c.set((bb.minx-centerx)*dx, (bb.miny-centery)*dy, (bb.minz-centerz)*dz);
    if (c.length2() > 1.0)
      return true;
    c.set((bb.minx-centerx)*dx, (bb.miny-centery)*dy, (bb.maxz-centerz)*dz);
    if (c.length2() > 1.0)
      return true;
    c.set((bb.minx-centerx)*dx, (bb.maxy-centery)*dy, (bb.minz-centerz)*dz);
    if (c.length2() > 1.0)
      return true;
    c.set((bb.minx-centerx)*dx, (bb.maxy-centery)*dy, (bb.maxz-centerz)*dz);
    if (c.length2() > 1.0)
      return true;
    c.set((bb.maxx-centerx)*dx, (bb.miny-centery)*dy, (bb.minz-centerz)*dz);
    if (c.length2() > 1.0)
      return true;
    c.set((bb.maxx-centerx)*dx, (bb.miny-centery)*dy, (bb.maxz-centerz)*dz);
    if (c.length2() > 1.0)
      return true;
    c.set((bb.maxx-centerx)*dx, (bb.maxy-centery)*dy, (bb.minz-centerz)*dz);
    if (c.length2() > 1.0)
      return true;
    c.set((bb.maxx-centerx)*dx, (bb.maxy-centery)*dy, (bb.maxz-centerz)*dz);
    if (c.length2() > 1.0)
      return true;
    return false;
  }
  
  /** Get the transformation from world coordinates to the object's local coordinates. */
  
  public Mat4 toLocal()
  {
    return toLocal;
  }


  /**
   * Inner class representing an intersection with an RTEllipsoid.
   */

  private static class EllipsoidIntersection implements SurfaceIntersection
  {
    private RTEllipsoid ellipse;
    private int numIntersections;
    private double dist1, dist2, r1x, r1y, r1z, r2x, r2y, r2z;
    private boolean trueNormValid;
    private Vec3 trueNorm, pos;

    public EllipsoidIntersection(RTEllipsoid ellipse, int numIntersections, Vec3 point1, Vec3 point2, double dist1, double dist2)
    {
      this.ellipse = ellipse;
      this.numIntersections = numIntersections;
      this.dist1 = dist1;
      this.dist2 = dist2;
      r1x = point1.x;
      r1y = point1.y;
      r1z = point1.z;
      r2x = point2.x;
      r2y = point2.y;
      r2z = point2.z;
      trueNorm = new Vec3();
      pos = new Vec3();
    }

    public RTObject getObject()
    {
      return ellipse;
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
      calcTrueNorm();
      n.set(trueNorm);
      TextureMapping map = ellipse.theSphere.getTextureMapping();
      pos.set(r1x, r1y, r1z);
      if (map instanceof UniformMapping)
        map.getTextureSpec(pos, spec, -n.dot(viewDir), size, time, ellipse.param);
      else
      {
        ellipse.toLocal.transform(pos);
        map.getTextureSpec(pos, spec, -n.dot(viewDir), size, time, ellipse.param);
      }
      if (ellipse.bumpMapped)
      {
        if (ellipse.transform)
          ellipse.fromLocal.transformDirection(spec.bumpGrad);
        n.scale(spec.bumpGrad.dot(n)+1.0);
        n.subtract(spec.bumpGrad);
        n.normalize();
      }
    }

    public void intersectionTransparency(int n, RGBColor trans, double angle, double size, double time)
    {
      TextureMapping map = ellipse.theSphere.getTextureMapping();
      if (n == 0)
        pos.set(r1x, r1y, r1z);
      else
        pos.set(r2x, r2y, r2z);
      if (map instanceof UniformMapping)
        map.getTransparency(pos, trans, angle, size, time, ellipse.param);
      else
      {
        ellipse.toLocal.transform(pos);
        map.getTransparency(pos, trans, angle, size, time, ellipse.param);
      }
    }

    public void trueNormal(Vec3 n)
    {
      calcTrueNorm();
      n.set(trueNorm);
    }

    /** Calculate the true normal of the point of intersection. */

    private final void calcTrueNorm()
    {
      if (trueNormValid)
        return;
      trueNormValid = true;
      if (ellipse.transform)
      {
        trueNorm.set(r1x-ellipse.cx, r1y-ellipse.cy, r1z-ellipse.cz);
        ellipse.toLocal.transformDirection(trueNorm);
        trueNorm.set(trueNorm.x, trueNorm.y*ellipse.sy, trueNorm.z*ellipse.sz);
        ellipse.fromLocal.transformDirection(trueNorm);
      }
      else
        trueNorm.set(r1x-ellipse.cx, (r1y-ellipse.cy)*ellipse.sy, (r1z-ellipse.cz)*ellipse.sz);
      trueNorm.normalize();
    }
  }
}