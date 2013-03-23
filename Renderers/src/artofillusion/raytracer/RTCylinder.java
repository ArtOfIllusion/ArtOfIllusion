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

/** RTCylinder represents an cylinder, tapered cylinder, or cone to be raytraced.  It is 
    defined by specifying a Cylinder object, and the transformations to and from local 
    coordinates. */

public class RTCylinder extends RTObject
{
  Cylinder theCylinder;
  Vec3 topNormal, bottomNormal;
  double rx, rz, height, halfh, rx2, rz2, toprx2, cx, cy, cz, sy, sz, param[];
  boolean bumpMapped, cone, transform, uniform;
  Mat4 toLocal, fromLocal;
  
  public static final double TOL = 1e-12;
  public static final int TOP = 0;
  public static final int BOTTOM = 1;
  public static final int SIDE = 2;

  public RTCylinder(Cylinder cylinder, Mat4 fromLocal, Mat4 toLocal, double param[])
  {
    double ratio = cylinder.getRatio();
    Vec3 vx = toLocal.timesDirection(Vec3.vx()), vy = toLocal.timesDirection(Vec3.vy());
    Vec3 size;
    
    theCylinder = cylinder;
    this.param = param;
    size = cylinder.getBounds().getSize();
    uniform = cylinder.getTextureMapping() instanceof UniformMapping;
    cone = false;
    transform = true;
    if (vy.y == 1.0)
      {
        if (vx.x == 1.0 || vx.x == -1.0)
          {
            rx = size.x/2.0;
            rz = size.z/2.0;
            transform = false;
          }
        else if (vx.z == 1.0 || vx.z == -1.0)
          {
            rx = size.z/2.0;
            rz = size.x/2.0;
            transform = false;
          }
        if (transform == false && ratio == 0.0)
          cone = true;
      }
    else if (vy.y == -1.0 && ratio != 0.0)
      {
        if (vx.x == 1.0 || vx.x == -1.0)
          {
            rx = size.x/2.0;
            rz = size.z/2.0;
            transform = false;
          }
        else if (vx.z == 1.0 || vx.z == -1.0)
          {
            rx = size.z/2.0;
            rz = size.x/2.0;
            transform = false;
          }
        if (transform == false)
          {
            rx *= ratio;
            rz *= ratio;
            ratio = 1.0/ratio;
          }
      }
    height = size.y;
    halfh = height/2.0;
    if (transform)
      {
        rx = size.x/2.0;
        rz = size.z/2.0;
        if (ratio == 0.0)
          cone = true;
        this.fromLocal = fromLocal;
      }
    cx = fromLocal.m14/fromLocal.m44;
    cy = fromLocal.m24/fromLocal.m44;
    cz = fromLocal.m34/fromLocal.m44;
    rx2 = rx*rx;
    rz2 = rz*rz;
    toprx2 = rx2*ratio*ratio;
    sz = rx2/rz2;
    sy = rx*(ratio-1.0)/height;
    bottomNormal = fromLocal.timesDirection(Vec3.vy()).times(-1.0);
    if (!cone)
      topNormal = bottomNormal.times(-1.0);
    bumpMapped = cylinder.getTexture().hasComponent(Texture.BUMP_COMPONENT);
    this.toLocal = toLocal;
  }

  /** Get the MaterialMapping for this object. */
  
  public final MaterialMapping getMaterialMapping()
  {
    return theCylinder.getMaterialMapping();
  }

  /** Get the TextureMapping for this object. */
  
  public final TextureMapping getTextureMapping()
  {
    return theCylinder.getTextureMapping();
  }

  /** Determine whether the given ray intersects this cylinder. */

  public SurfaceIntersection checkIntersection(Ray r)
  {
    Vec3 orig = r.getOrigin(), rdir = r.getDirection();
    Vec3 v1 = r.tempVec1, v2 = r.tempVec2, dir = r.tempVec3;
    double a, b, c, d, e, dist1, dist2, temp1, temp2, mint;
    int intersections, hit = -1;

    if (transform)
      {
        v1.set(cx-orig.x, cy-orig.y, cz-orig.z);
        toLocal.transformDirection(v1);
        v1.y -= halfh;
        dir.set(rdir);
        toLocal.transformDirection(dir);
      }
    else
      {
        v1.set(cx-orig.x, cy-orig.y-halfh, cz-orig.z);
        if (uniform)
          dir = rdir;
        else
          dir.set(rdir);
      }
    mint = Double.MAX_VALUE;
    if (dir.y != 0.0)
      {
        // See if the ray hits the top or bottom face of the cylinder.

        temp1 = v1.y/dir.y;
        if (temp1 > TOL)
          {
            a = temp1*dir.x - v1.x;
            b = temp1*dir.z - v1.z;
            if (a*a+sz*b*b < rx2)
              {
                hit = BOTTOM;
                mint = temp1;
              }
          }
        if (!cone)
          {
            temp1 = (v1.y+height)/dir.y;
            if (temp1 > TOL)
              {
                a = temp1*dir.x - v1.x;
                b = temp1*dir.z - v1.z;
                if (a*a+sz*b*b < toprx2)
                  {
                    if (mint < Double.MAX_VALUE)
                      {
                        // The ray hit both the top and bottom faces, so we know it
                        // didn't hit the sides.

                        intersections = 2;
                        if (temp1 < mint)
                          {
                            hit = TOP;
                            dist1 = temp1;
                            dist2 = mint;
                          }
                        else
                          {
                            dist1 = mint;
                            dist2 = temp1;
                          }
                        v1.set(orig.x+dist1*rdir.x, orig.y+dist1*rdir.y, orig.z+dist1*rdir.z);
                        v2.set(orig.x+dist2*rdir.x, orig.y+dist2*rdir.y, orig.z+dist2*rdir.z);
                        return new CylinderIntersection(this, intersections, hit, v1, v2, dist1, dist2);
                      }
                    else
                      {
                        hit = TOP;
                        mint = temp1;
                      }
                  }
              }
          }
      }
    
    // Now see if it hits the sides of the cylinder.

    if (sy == 0.0)
    {
      // A simple cylinder

      temp1 = sz*dir.z;
      temp2 = 0.0;
      d = rx;
      b = dir.x*v1.x + temp1*v1.z;
      c = v1.x*v1.x + sz*v1.z*v1.z - d*d;
    }
    else
    {
      temp1 = sz*dir.z;
      temp2 = sy*dir.y;
      d = rx - sy*v1.y;
      b = dir.x*v1.x + d*sy*dir.y + temp1*v1.z;
      c = v1.x*v1.x + sz*v1.z*v1.z - d*d;
    }
    dist1 = Double.MAX_VALUE;
    dist2 = mint;
    if (c > TOL)  // Ray origin is outside cylinder.
      {
        if (b > 0.0)  // Ray points toward cylinder.
          {
            a = dir.x*dir.x + temp1*dir.z - temp2*temp2;
            e = b*b - a*c;
            if (e >= 0.0)
              {
                temp1 = Math.sqrt(e);
                dist1 = (b - temp1)/a;
                if (dist2 == Double.MAX_VALUE)
                  dist2 = (b + temp1)/a;
              }
          }
      }
    else if (c < -TOL)  // Ray origin is inside cylinder.
      {
        a = dir.x*dir.x + temp1*dir.z - temp2*temp2;
        e = b*b - a*c;
        if (e >= 0.0)
          dist1 = (b + Math.sqrt(e))/a;
      }
    else  // Ray origin is on the surface of the cylinder.
      {
        if (b > 0.0)  // Ray points into cylinder.
          {
            a = dir.x*dir.x + temp1*dir.z - temp2*temp2;
            e = b*b - a*c;
            if (e >= 0.0)
              dist1 = (b + Math.sqrt(e))/a;
          }
      }
    if (dist1 < mint)
      {
        a = dist1*dir.y-v1.y;
        if (a > 0.0 && a < height)
          {
            hit = SIDE;
            mint = dist1;
          }
      }
    if (mint == Double.MAX_VALUE)
      return SurfaceIntersection.NO_INTERSECTION;
    if (dist2 < mint)
      {
        temp1 = dist2;
        dist2 = mint;
        mint = temp1;
      }
    dist1 = mint;
    v1.set(orig.x+dist1*rdir.x, orig.y+dist1*rdir.y, orig.z+dist1*rdir.z);
    if (hit == SIDE)
      projectPoint(v1);
    if (dist2 == Double.MAX_VALUE)
      intersections = 1;
    else
      {
        intersections = 2;
        v2.set(orig.x+dist2*rdir.x, orig.y+dist2*rdir.y, orig.z+dist2*rdir.z);
      }
    return new CylinderIntersection(this, intersections, hit, v1, v2, dist1, dist2);
  }
  
  /** Given a point, project it onto the surface of the cylinder.  This is necessary to
      prevent roundoff error. */
  
  private void projectPoint(Vec3 pos)
  {
    if (transform)
    {
      toLocal.transform(pos);
      double r = rx + sy*(pos.y+halfh);
      double scale = r/Math.sqrt(pos.x*pos.x+sz*pos.z*pos.z);
      pos.set(pos.x*scale, pos.y, pos.z*scale);
      fromLocal.transform(pos);
    }
    else
    {
      double dx = pos.x-cx, dz = pos.z-cz;
      double r = rx + sy*(pos.y-cy+halfh);
      double scale = r/Math.sqrt(dx*dx+sz*dz*dz);
      pos.set(cx+dx*scale, pos.y, cz+dz*scale);
    }
  }

  /** Get a bounding box for this cylinder. */
  
  public BoundingBox getBounds()
  {
    if (transform)
      return (new BoundingBox(-rx, rx, -halfh, halfh, -rz, rz)).transformAndOutset(fromLocal);
    else if (toprx2 > rx2)
      {
        double xrad = Math.sqrt(toprx2), zrad = Math.sqrt(rz2*toprx2/rx2);
        return new BoundingBox(cx-xrad, cx+xrad, cy-halfh, cy+halfh, cz-zrad, cz+zrad);
      }
    else
      return new BoundingBox(cx-rx, cx+rx, cy-halfh, cy+halfh, cz-rz, cz+rz);
  }

  /** Determine whether any part of the surface of the cylinder lies within a bounding box. */

  public boolean intersectsBox(BoundingBox bb)
  {
    double x, z;
    
    if (transform)
      {
        bb = bb.transformAndOutset(toLocal);
        if (bb.miny > halfh || bb.maxy < -halfh)
          return false;
        x = 0.0;
        z = 0.0;
        if (bb.minx > 0.0)
          x = bb.minx;
        else if (bb.maxx < 0.0)
          x = bb.maxx;
        if (bb.minz > 0.0)
          z = bb.minz;
        else if (bb.maxz < 0.0)
          z = bb.maxz;
        if (x*x + sz*z*z > rx2)
          return false;
        return true;
      }
    if (bb.miny > cy+halfh || bb.maxy < cy-halfh)
      return false;
    x = cx;
    z = cz;
    if (cx < bb.minx)
      x = bb.minx;
    else if (cx > bb.maxx)
      x = bb.maxx;
    if (cz < bb.minz)
      z = bb.minz;
    else if (cz > bb.maxz)
      z = bb.maxz;
    double maxrad2 = (rx2 > toprx2 ? rx2 : toprx2);
    if ((x-cx)*(x-cx) + sz*(z-cz)*(z-cz) > maxrad2)
      return false;
    return true;
  }
  
  /** Get the transformation from world coordinates to the object's local coordinates. */
  
  public Mat4 toLocal()
  {
    return toLocal;
  }

  /**
   * Inner class representing an intersection with an RTCylinder.
   */

  private static class CylinderIntersection implements SurfaceIntersection
  {
    private RTCylinder cylinder;
    private int numIntersections, hit;
    private double dist1, dist2, r1x, r1y, r1z, r2x, r2y, r2z;
    private boolean trueNormValid;
    private Vec3 trueNorm, pos;

    public CylinderIntersection(RTCylinder cylinder, int numIntersections, int hit, Vec3 point1, Vec3 point2, double dist1, double dist2)
    {
      this.cylinder = cylinder;
      this.numIntersections = numIntersections;
      this.hit = hit;
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
      return cylinder;
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
      TextureMapping map = cylinder.theCylinder.getTextureMapping();
      pos.set(r1x, r1y, r1z);
      if (cylinder.uniform)
        map.getTextureSpec(pos, spec, -n.dot(viewDir), size, time, cylinder.param);
      else
      {
        cylinder.toLocal.transform(pos);
        map.getTextureSpec(pos, spec, -n.dot(viewDir), size, time, cylinder.param);
      }
      if (cylinder.bumpMapped)
      {
        if (cylinder.transform)
          cylinder.fromLocal.transformDirection(spec.bumpGrad);
        n.scale(spec.bumpGrad.dot(n)+1.0);
        n.subtract(spec.bumpGrad);
        n.normalize();
      }
    }

    public void intersectionTransparency(int n, RGBColor trans, double angle, double size, double time)
    {
      TextureMapping map = cylinder.theCylinder.getTextureMapping();
      if (n == 0)
        pos.set(r1x, r1y, r1z);
      else
        pos.set(r2x, r2y, r2z);
      if (cylinder.uniform)
        map.getTransparency(pos, trans, angle, size, time, cylinder.param);
      else
        {
          cylinder.toLocal.transform(pos);
          map.getTransparency(pos, trans, angle, size, time, cylinder.param);
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
      if (hit == TOP)
        trueNorm.set(cylinder.topNormal);
      else if (hit == BOTTOM)
        trueNorm.set(cylinder.bottomNormal);
      else
      {
        if (cylinder.transform)
        {
          trueNorm.set(r1x-cylinder.cx, r1y-cylinder.cy, r1z-cylinder.cz);
          cylinder.toLocal.transformDirection(trueNorm);
          trueNorm.set(trueNorm.x, -(cylinder.rx+cylinder.sy*(trueNorm.y+cylinder.halfh))*cylinder.sy, trueNorm.z*cylinder.sz);
          cylinder.fromLocal.transformDirection(trueNorm);
        }
        else
          trueNorm.set(r1x-cylinder.cx, -(cylinder.rx+cylinder.sy*(r1y-cylinder.cy+cylinder.halfh))*cylinder.sy, (r1z-cylinder.cz)*cylinder.sz);
        trueNorm.normalize();
      }
    }
  }
}