/* Copyright (C) 2000-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.*;
import artofillusion.material.*;
import artofillusion.math.*;
import artofillusion.texture.*;

/** RTDisplacedTriangle represents a displacement mapped triangle to be raytraced.  It is 
    defined by specifying a
    RenderingMesh, the index of the RenderingTriangle within the mesh, and a matrix which 
    gives the transformation from the mesh's local coordinates to world coordinates.  To
    save time and memory, the constructor is also passed two arrays which contain the
    vertices and normals for the mesh, transformed into world coordinates. */

public class RTDisplacedTriangle extends RTObject
{
  RenderingTriangle tri;
  Vec3 trueNorm;
  double minheight, maxheight;
  private double tol;
  private boolean bumpMapped;
  private BoundingBox bounds;
  private Mat4 toLocal, fromLocal;
  private volatile ExtraInfo extraInfo;

  /**
   * This inner class holds many internal fields that are only used after the triangle is initialized.
   * Putting them in a separate object saves memory on triangles that never get hit by a ray (and hence
   * never get initialized).
   */
  
  private static class ExtraInfo
  {
    Mat4 trans;
    BoundingBox bounds2;
    double v1x, v1y, v2y;
    double n1x, n1y, n1z;
    double n2x, n2y, n2z;
    double n3x, n3y, n3z;
    double dn1x, dn1y, dn2x, dn2y;
    double minscale, maxscale;
  }
  
  public RTDisplacedTriangle(RenderingMesh mesh, int which, Mat4 fromLocal, Mat4 toLocal, double tol, double time)
  {
    tri = mesh.triangle[which];
    Vec3 vert1 = mesh.vert[tri.v1];
    Vec3 vert2 = mesh.vert[tri.v2];
    Vec3 vert3 = mesh.vert[tri.v3];
    trueNorm = mesh.faceNorm[which];
    this.fromLocal = fromLocal;
    this.toLocal = toLocal;
    this.tol = tol;
    Vec3 norm1 = mesh.norm[tri.n1];
    Vec3 norm2 = mesh.norm[tri.n2];
    Vec3 norm3 = mesh.norm[tri.n3];

    // Make sure trueNorm points in the same direction as the vertex normals.

    int i = 0;
    if (trueNorm.dot(norm1) < 0.0) i++;
    if (trueNorm.dot(norm2) < 0.0) i++;
    if (trueNorm.dot(norm3) < 0.0) i++;
    if (i > 1)
      trueNorm.scale(-1.0);
    
    // Evaluate the displacement at many points over the triangle.  Use this to determine
    // the maximum and minimum displacements, as well as a bounding box for the object.

    minheight = Double.MAX_VALUE;
    maxheight = -Double.MAX_VALUE;
    bounds = new BoundingBox(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    bounds.minx = bounds.miny = bounds.minz = Double.MAX_VALUE;
    bounds.maxx = bounds.maxy = bounds.maxz = -Double.MAX_VALUE;
    double d = 2.0*Math.max(Math.max(vert1.minus(vert2).length2(), vert1.minus(vert3).length2()), 
        vert2.minus(vert3).length2());
    int j, divisions = FastMath.ceil(Math.sqrt(d)/tol);
    if (divisions > 499)
      divisions = 499; // Otherwise, we could spend hours just estimating the displacement range.  499 is prime, to avoid aliasing.
    Vec3 temp = new Vec3();
    d = 1.0/divisions;
    for (i = 0; i <= divisions; i++)
      for (j = 0; j <= divisions-i; j++)
      {
        double u = i*d;
        double v = j*d;
        double w = (divisions-i-j)*d;
        double disp = tri.getDisplacement(u, v, w, tol, time);
        temp.set(u*norm1.x+v*norm2.x+w*norm3.x, u*norm1.y+v*norm2.y+w*norm3.y, u*norm1.z+v*norm2.z+w*norm3.z);
        temp.normalize();
        double h = disp*temp.dot(trueNorm);
        if (h < minheight)
          minheight = h;
        if (h > maxheight)
          maxheight = h;
        temp.set(u*vert1.x+v*vert2.x+w*vert3.x+disp*temp.x,
            u*vert1.y+v*vert2.y+w*vert3.y+disp*temp.y,
            u*vert1.z+v*vert2.z+w*vert3.z+disp*temp.z);
        if (temp.x < bounds.minx)
          bounds.minx = temp.x;
        if (temp.x > bounds.maxx)
          bounds.maxx = temp.x;
        if (temp.y < bounds.miny)
          bounds.miny = temp.y;
        if (temp.y > bounds.maxy)
          bounds.maxy = temp.y;
        if (temp.z < bounds.minz)
          bounds.minz = temp.z;
        if (temp.z > bounds.maxz)
          bounds.maxz = temp.z;
      }
    bounds.outset(0.5*tol);
    bumpMapped = tri.theMesh.mapping.getTexture().hasComponent(Texture.BUMP_COMPONENT);
  }

  /** Determine whether this triangle is really displaced.  If the displacement map is completely flat
      over it, return false.  Otherwise, return true. */
  
  public boolean isReallyDisplaced()
  {
    return (minheight != 0.0 || maxheight != 0.0);
  }
  
  /** Set the tolerance which should be used for evaluating the surface. */
  
  public void setTolerance(double tol)
  {
    this.tol = tol;
  }
  
  /** Part of the initialization is done lazily to save time and memory, since some triangles may never
      be hit by any ray. */

  private synchronized void init()
  {
    if (extraInfo != null)
      return;
    ExtraInfo ex = new ExtraInfo();
    
    // Determine a coordinate transformation which places the triangle in the xy plane, with
    // vertex 3 at the origin and vertex 2 on the y axis.
    
    Vec3 vert[] = tri.theMesh.vert;
    Vec3 vert1 = vert[tri.v1];
    Vec3 vert2 = vert[tri.v2];
    Vec3 vert3 = vert[tri.v3];
    ex.trans = Mat4.viewTransform(vert3, trueNorm, vert2.minus(vert3));
    
    // Find the vertex positions and normals in the coordinate system described above.
    
    Vec3 v1 = ex.trans.times(vert1);
    Vec3 v2 = ex.trans.times(vert2);
    ex.v1x = v1.x;
    ex.v1y = v1.y;
    ex.v2y = v2.y;
    Vec3 norm[] = tri.theMesh.norm;
    Vec3 n1 = ex.trans.timesDirection(norm[tri.n1]);
    Vec3 n2 = ex.trans.timesDirection(norm[tri.n2]);
    Vec3 n3 = ex.trans.timesDirection(norm[tri.n3]);
    ex.n1x = n1.x;
    ex.n1y = n1.y;
    ex.n1z = n1.z;
    ex.n2x = n2.x;
    ex.n2y = n2.y;
    ex.n2z = n2.z;
    ex.n3x = n3.x;
    ex.n3y = n3.y;
    ex.n3z = n3.z;

    // Find the bounding box of the volume swept out by the triangle, in the coordinate
    // system described above.
    
    Vec3 pos[] = new Vec3 [6];
    double scale1 = 1.0/norm[tri.n1].dot(trueNorm);
    if (Double.isInfinite(scale1))
      scale1 = 0.0;
    double scale2 = 1.0/norm[tri.n2].dot(trueNorm);
    if (Double.isInfinite(scale2))
      scale2 = 0.0;
    double scale3 = 1.0/norm[tri.n3].dot(trueNorm);
    if (Double.isInfinite(scale3))
      scale3 = 0.0;
    pos[0] = new Vec3(v1.x+n1.x*minheight*scale1, v1.y+n1.y*minheight*scale1, n1.z*minheight*scale1);
    pos[1] = new Vec3(v1.x+n1.x*maxheight*scale1, v1.y+n1.y*maxheight*scale1, n1.z*maxheight*scale1);
    pos[2] = new Vec3(n2.x*minheight*scale2, v2.y+n2.y*minheight*scale2, n2.z*minheight*scale2);
    pos[3] = new Vec3(n2.x*maxheight*scale2, v2.y+n2.y*maxheight*scale2, n2.z*maxheight*scale2);
    pos[4] = new Vec3(n3.x*minheight*scale3, n3.y*minheight*scale3, n3.z*minheight*scale3);
    pos[5] = new Vec3(n3.x*maxheight*scale3, n3.y*maxheight*scale3, n3.z*maxheight*scale3);
    BoundingBox bounds2 = new BoundingBox(pos[0].x, pos[0].x, pos[0].y, pos[0].y, pos[0].z, pos[0].z);
    for (int i = 1; i < 6; i++)
      {
        if (pos[i].x < bounds2.minx)
          bounds2.minx = pos[i].x;
        if (pos[i].x > bounds2.maxx)
          bounds2.maxx = pos[i].x;
        if (pos[i].y < bounds2.miny)
          bounds2.miny = pos[i].y;
        if (pos[i].y > bounds2.maxy)
          bounds2.maxy = pos[i].y;
        if (pos[i].z < bounds2.minz)
          bounds2.minz = pos[i].z;
        if (pos[i].z > bounds2.maxz)
          bounds2.maxz = pos[i].z;
      }
    bounds2.outset(0.5*tol);
    ex.bounds2 = bounds2;

    // Create various other objects which will be needed.
    
    ex.dn1x = n1.x-n3.x;
    ex.dn1y = n1.y-n3.y;
    ex.dn2x = n2.x-n3.x;
    ex.dn2y = n2.y-n3.y;
    ex.minscale = 1.0/Math.max(Math.max(n1.z, n2.z), n3.z);
    ex.maxscale = 1.0/Math.min(Math.min(n1.z, n2.z), n3.z);
    extraInfo = ex;
  }

  /** Get the TextureMapping for this object. */
  
  public final TextureMapping getTextureMapping()
  {
    return tri.getTextureMapping();
  }

  /** Get the MaterialMapping for this object. */
  
  public final MaterialMapping getMaterialMapping()
  {
    return tri.theMesh.matMapping;
  }

  /** Determine whether the given ray intersects this triangle. */

  public SurfaceIntersection checkIntersection(Ray r)
  {
    DisplacedTriangleIntersection dti = (DisplacedTriangleIntersection) r.rt.rtDispTriPool.getObject();
    dti.init(this, r);
    if (!rayIntersectsBounds(r.origin, r.direction, bounds, dti))
      return SurfaceIntersection.NO_INTERSECTION;
    double mint = dti.mint, maxt = dti.maxt;
    if (extraInfo == null)
      init();
    ExtraInfo extra = extraInfo;

    // Transform the ray to the local coordinate system, and check it against the bounding box.

    Vec3 orig = dti.orig;
    Vec3 dir = dti.dir;
    extra.trans.transform(orig);
    extra.trans.transformDirection(dir);

    if (!rayIntersectsBounds(orig, dir, extra.bounds2, dti))
      return SurfaceIntersection.NO_INTERSECTION;
    if (mint < dti.mint)
      mint = dti.mint;
    if (maxt > dti.maxt)
      maxt = dti.maxt;
    
    // See if the ray is entirely outside the triangle.
    
    if (mint < 0.0)
      mint = 0.0;
    double x, y, z;
    x = orig.x+maxt*dir.x;
    y = orig.y+maxt*dir.y;
    z = orig.z+maxt*dir.z;
    calcCoords(dti, x, y, z);
    double lastu = dti.u, lastv = dti.v, lastw = dti.w;
    x = orig.x+mint*dir.x;
    y = orig.y+mint*dir.y;
    z = orig.z+mint*dir.z;
    calcCoords(dti, x, y, z);
    double u = dti.u, v = dti.v, w = dti.w;
    if ((u < 0.0 && lastu < 0.0) || (u > 1.0 && lastu > 1.0) ||
        (v < 0.0 && lastv < 0.0) || (v > 1.0 && lastv > 1.0) ||
        (w < 0.0 && lastw < 0.0) || (w > 1.0 && lastw > 1.0))
      return SurfaceIntersection.NO_INTERSECTION;
    Vec3 temp1 = r.tempVec1;
    temp1.set(u*extra.n1x+v*extra.n2x+w*extra.n3x, u*extra.n1y+v*extra.n2y+w*extra.n3y, u*extra.n1z+v*extra.n2z+w*extra.n3z);

    // The ray intersects the volume, so step along it and check for intersections.  Determine
    // how many steps to use.

    double time = r.rt.rt.getTime();
    double disp = tri.getDisplacement(u, v, w, tol, time), height = disp*temp1.z/temp1.length();
    double prevDelta = z-height;
    boolean above = (z > height);
    boolean wasOutsideU = (u < 0.0 || u > 1.0), wasOutsideV = (v < 0.0 || v > 1.0), wasOutsideW = (w < 0.0 || w > 1.0);
    boolean outsideU, outsideV, outsideW;
    double t = mint;
    double prevt = t;
    dti.numIntersections = -1;
    dti.tint[0] = Double.MAX_VALUE;
    while (t < maxt)
      {
        t += tol;
        if (t >= maxt)
          {
            t = maxt;
            x = orig.x+t*dir.x;
            y = orig.y+t*dir.y;
            z = orig.z+t*dir.z;
            u = lastu;
            v = lastv;
            w = lastw;
          }
        else
          {
            x = orig.x+t*dir.x;
            y = orig.y+t*dir.y;
            z = orig.z+t*dir.z;
            calcCoords(dti, x, y, z);
            u = dti.u;
            v = dti.v;
            w = dti.w;
          }
        disp = tri.getDisplacement(u, v, w, tol, time);
        temp1.set(u*extra.n1x+v*extra.n2x+w*extra.n3x, u*extra.n1y+v*extra.n2y+w*extra.n3y, u*extra.n1z+v*extra.n2z+w*extra.n3z);
        height = disp*temp1.z/temp1.length();

        outsideU = (u < 0.0 || u > 1.0);
        outsideV = (v < 0.0 || v > 1.0);
        outsideW = (w < 0.0 || w > 1.0);
        if ((outsideU && wasOutsideU) || (outsideV && wasOutsideV) || (outsideW && wasOutsideW))
          {
            // The ray is currently outside the triangle.
            
            above = (z > height);
            prevDelta = z-height;
            prevt = t;
            wasOutsideU = outsideU;
            wasOutsideV = outsideV;
            wasOutsideW = outsideW;
            continue;
          }
        if ((above && z <= height) || (!above && z >= height))
          {
            // The ray intersects the surface.
            
            double truet = t-(t-prevt)*(z-height)/(z-height-prevDelta);
            if (truet <= tol)
              {
                // Ignore intersections too close to the ray origin to prevent the surface from
                // shadowing itself.
                
                above = (z > height);
                prevDelta = z-height;
                prevt = t;
                wasOutsideU = outsideU;
                wasOutsideV = outsideV;
                wasOutsideW = outsideW;
                continue;
              }
            x = orig.x+truet*dir.x;
            y = orig.y+truet*dir.y;
            z = orig.z+truet*dir.z;
            calcCoords(dti, x, y, z);
            u = dti.u;
            v = dti.v;
            w = dti.w;
            dti.tint[0] = truet;
            dti.uint[0] = u;
            dti.vint[0] = v;
            break;
          }
        prevDelta = z-height;
        above = (z > height);
        prevt = t;
        wasOutsideU = outsideU;
        wasOutsideV = outsideV;
        wasOutsideW = outsideW;
      }
    if (dti.tint[0] == Double.MAX_VALUE)
      return SurfaceIntersection.NO_INTERSECTION;
    
    // Find the point of intersection.
    
    disp = tri.getDisplacement(u, v, w, tol, time);
    dti.rint[0].set(r.origin.x+dti.tint[0]*r.direction.x, r.origin.y+dti.tint[0]*r.direction.y, r.origin.z+dti.tint[0]*r.direction.z);

    // Calculate the derivates of the displacement function.
    
    double dhdu = (tri.getDisplacement(u+(1e-5), v, w-(1e-5), tol, time)-disp)*1e5;
    double dhdv = (tri.getDisplacement(u, v+(1e-5), w-(1e-5), tol, time)-disp)*1e5;

    // Use them to find the local normal vector.

    Vec3 norm[] = tri.theMesh.norm;
    Vec3 norm1 = norm[tri.n1];
    Vec3 norm2 = norm[tri.n2];
    Vec3 norm3 = norm[tri.n3];
    Vec3 interp = dti.interp;
    interp.set(u*norm1.x+v*norm2.x+w*norm3.x, u*norm1.y+v*norm2.y+w*norm3.y, u*norm1.z+v*norm2.z+w*norm3.z);
    interp.normalize();
    Vec3 vert[] = tri.theMesh.vert;
    Vec3 vert1 = vert[tri.v1];
    Vec3 vert2 = vert[tri.v2];
    Vec3 vert3 = vert[tri.v3];
    Vec3 temp2 = r.tempVec2;
    Vec3 temp3 = r.tempVec3;
    Vec3 temp4 = r.tempVec4;
    temp1.set(vert1.x+disp*norm1.x, vert1.y+disp*norm1.y, vert1.z+disp*norm1.z);
    temp2.set(vert2.x+disp*norm2.x, vert2.y+disp*norm2.y, vert2.z+disp*norm2.z);
    temp3.set(vert3.x+disp*norm3.x, vert3.y+disp*norm3.y, vert3.z+disp*norm3.z);
    temp1.set(temp1.x-temp3.x, temp1.y-temp3.y, temp1.z-temp3.z);
    temp2.set(temp3.x-temp2.x, temp3.y-temp2.y, temp3.z-temp2.z);
    temp3.set(temp1.y*interp.z-temp1.z*interp.y, temp1.z*interp.x-temp1.x*interp.z, temp1.x*interp.y-temp1.y*interp.x);
    temp4.set(temp2.y*interp.z-temp2.z*interp.y, temp2.z*interp.x-temp2.x*interp.z, temp2.x*interp.y-temp2.y*interp.x);
    temp3.scale(-1.0/temp3.dot(temp2));
    temp4.scale(1.0/temp4.dot(temp1));
    temp1.set(dhdu*temp4.x+dhdv*temp3.x, dhdu*temp4.y+dhdv*temp3.y, dhdu*temp4.z+dhdv*temp3.z);
    interp.scale(temp1.dot(interp)+1.0);
    interp.subtract(temp1);
    interp.normalize();
    return dti;
  }

  /** Given the coordinates of a point along the ray, calculate the barycentric coordinates
      (u, v, w) cooresponding to it.  Also calculate the displacement h of that point on the
      triangle, and return the height (the distance along the non-interpolated normal).  If
      u, v, or w is outside the range [0, 1], the return value is meaningless. */

  private void calcCoords(DisplacedTriangleIntersection dti, double x, double y, double z)
  {
    ExtraInfo extra = extraInfo;
    double dmax = z*extra.maxscale, dmin = z*extra.minscale;
    guessCoords(dti, x, y, dmax);
    double ua = dti.u, va = dti.v, wa = dti.w;
    double zmax = dmax*(dti.u*extra.n1z + dti.v*extra.n2z + dti.w*extra.n3z);
    guessCoords(dti, x, y, dmin);
    double zmin = dmin*(dti.u*extra.n1z + dti.v*extra.n2z + dti.w*extra.n3z);
    if (zmax == zmin)
      return;
    double fract = (z-zmin)/(zmax-zmin);
    double fract2 = 1.0-fract;
    dti.u = fract*ua + fract2*dti.u;
    dti.v = fract*va + fract2*dti.v;
    dti.w = fract*wa + fract2*dti.w;
  }
  
  /** Given the coordinates of a point along the ray, and a guess about the displacement h,
      find the corresponding guesses about u, v, and w. */
  
  private void guessCoords(DisplacedTriangleIntersection dti, double x, double y, double disp)
  {
    ExtraInfo extra = extraInfo;
    double a = extra.v1x+disp*extra.dn1x;
    double b = disp*extra.dn2x;
    double c = x-disp*extra.n3x;
    double e = extra.v1y+disp*extra.dn1y;
    double f = extra.v2y+disp*extra.dn2y;
    double g = y-disp*extra.n3y;
    double m = 1.0/(a*f-b*e);
    dti.u = (c*f-b*g)*m;
    dti.v = (a*g-c*e)*m;
    dti.w = 1.0-dti.u-dti.v;
  }

  /** Get a bounding box for this triangle. */
  
  public BoundingBox getBounds()
  {
    return bounds;
  }

  /** Determine whether any part of the triangle lies within a bounding box. */

  public boolean intersectsBox(BoundingBox bb)
  {
    if (!bb.intersects(bounds))
      return false;
    double dot = tri.theMesh.vert[tri.v1].dot(trueNorm);
    double mindot = dot+minheight, maxdot = dot+maxheight;
    boolean anyBelow = false, anyAbove = false;
    dot = trueNorm.x*bb.minx + trueNorm.y*bb.miny + trueNorm.z*bb.minz;
    if (dot < mindot)
      anyBelow = true;
    else if (dot > maxdot)
      anyAbove = true;
    else
      return true;
    dot = trueNorm.x*bb.minx + trueNorm.y*bb.miny + trueNorm.z*bb.maxz;
    if (dot < mindot)
    {
      if (anyAbove)
        return true;
      anyBelow = true;
    }
    else if (dot > maxdot)
    {
      if (anyBelow)
        return true;
      anyAbove = true;
    }
    else
      return true;
    dot = trueNorm.x*bb.minx + trueNorm.y*bb.maxy + trueNorm.z*bb.minz;
    if (dot < mindot)
    {
      if (anyAbove)
        return true;
      anyBelow = true;
    }
    else if (dot > maxdot)
    {
      if (anyBelow)
        return true;
      anyAbove = true;
    }
    else
      return true;
    dot = trueNorm.x*bb.minx + trueNorm.y*bb.maxy + trueNorm.z*bb.maxz;
    if (dot < mindot)
    {
      if (anyAbove)
        return true;
      anyBelow = true;
    }
    else if (dot > maxdot)
    {
      if (anyBelow)
        return true;
      anyAbove = true;
    }
    else
      return true;
    dot = trueNorm.x*bb.maxx + trueNorm.y*bb.miny + trueNorm.z*bb.minz;
    if (dot < mindot)
    {
      if (anyAbove)
        return true;
      anyBelow = true;
    }
    else if (dot > maxdot)
    {
      if (anyBelow)
        return true;
      anyAbove = true;
    }
    else
      return true;
    dot = trueNorm.x*bb.maxx + trueNorm.y*bb.miny + trueNorm.z*bb.maxz;
    if (dot < mindot)
    {
      if (anyAbove)
        return true;
      anyBelow = true;
    }
    else if (dot > maxdot)
    {
      if (anyBelow)
        return true;
      anyAbove = true;
    }
    else
      return true;
    dot = trueNorm.x*bb.maxx + trueNorm.y*bb.maxy + trueNorm.z*bb.minz;
    if (dot < mindot)
    {
      if (anyAbove)
        return true;
      anyBelow = true;
    }
    else if (dot > maxdot)
    {
      if (anyBelow)
        return true;
      anyAbove = true;
    }
    else
      return true;
    dot = trueNorm.x*bb.maxx + trueNorm.y*bb.maxy + trueNorm.z*bb.maxz;
    if (dot < mindot)
    {
      if (anyAbove)
        return true;
    }
    else if (dot > maxdot)
    {
      if (anyBelow)
        return true;
    }
    else
      return true;
    return false;
  }
  
  /** Determine whether a ray intersects the bounding box.  If so, return true and set mint and
      maxt to the points where the ray enters and exits the box. */
  
  private static boolean rayIntersectsBounds(Vec3 origin, Vec3 direction, BoundingBox bb, DisplacedTriangleIntersection dti)
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
    dti.mint = mint;
    dti.maxt = maxt;
    return true;
  }
  
  /** Get the transformation from world coordinates to the object's local coordinates. */
  
  public Mat4 toLocal()
  {
    return toLocal;
  }
  
  /** Get the mesh this triangle is part of. */
  
  public Object getObject()
  {
    return tri.theMesh;
  }

  /**
   * Inner class representing an intersection with an RTTriangle.
   */

  public static class DisplacedTriangleIntersection implements SurfaceIntersection
  {
    public RTDisplacedTriangle rtTri;
    public double t, u, v, w, mint, maxt;
    public double tint[], uint[], vint[];
    public Vec3 orig, dir, interp, rint[];
    public short numIntersections;
    public Ray ray;

    public RTObject getObject()
    {
      return rtTri;
    }

    public DisplacedTriangleIntersection()
    {
      tint = new double [1];
      uint = new double [1];
      vint = new double [1];
      rint = new Vec3 [] {new Vec3()};
      orig = new Vec3();
      dir = new Vec3();
      interp = new Vec3();
    }

    public void init(RTDisplacedTriangle tri, Ray r)
    {
      rtTri = tri;
      ray = r;
      orig.set(r.origin);
      dir.set(r.direction);
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
      n.set(interp);
      rtTri.tri.getTextureSpec(spec, -n.dot(viewDir), uint[0], vint[0], 1.0-uint[0]-vint[0], size, time);
      if (rtTri.bumpMapped)
      {
        rtTri.fromLocal.transformDirection(spec.bumpGrad);
        n.scale(spec.bumpGrad.dot(n)+1.0);
        n.subtract(spec.bumpGrad);
        n.normalize();
      }
    }

    public void intersectionTransparency(int n, RGBColor trans, double angle, double size, double time)
    {
      rtTri.tri.getTransparency(trans, angle, uint[n], vint[n], 1.0-uint[0]-vint[0], size, time);
    }

    public void trueNormal(Vec3 n)
    {
      n.set(interp);
    }

    /** Find all intersection points with the ray.  This is called in response to a call to
        numIntersections(), since that indicates that all intersections will be needed, not
        just the first. */

    private void findAllIntersections()
    {
      ExtraInfo extra = rtTri.extraInfo;
      double x, y, z;
      x = orig.x+t*dir.x;
      y = orig.y+t*dir.y;
      z = orig.z+t*dir.z;
      Vec3 temp1 = ray.tempVec1;
      temp1.set(u*extra.n1x+v*extra.n2x+w*extra.n3x, u*extra.n1y+v*extra.n2y+w*extra.n3y, u*extra.n1z+v*extra.n2z+w*extra.n3z);
      double time = ray.rt.rt.getTime();
      double disp = rtTri.tri.getDisplacement(u, v, w, rtTri.tol, time), height = disp*temp1.z/temp1.length();
      double prevDelta = z-height, prevt = t;
      boolean above = (z > height);
      boolean wasOutsideU = (u < 0.0 || u > 1.0), wasOutsideV = (v < 0.0 || v > 1.0), wasOutsideW = (w < 0.0 || w > 1.0);
      boolean outsideU, outsideV, outsideW;

      numIntersections = 1;
      while (t < maxt)
      {
        t += rtTri.tol;
        if (t >= maxt)
          t = maxt;
        x = orig.x+t*dir.x;
        y = orig.y+t*dir.y;
        z = orig.z+t*dir.z;
        rtTri.calcCoords(this, x, y, z);
        disp = rtTri.tri.getDisplacement(u, v, w, rtTri.tol, time);
        temp1.set(u*extra.n1x+v*extra.n2x+w*extra.n3x, u*extra.n1y+v*extra.n2y+w*extra.n3y, u*extra.n1z+v*extra.n2z+w*extra.n3z);
        height = disp*temp1.z/temp1.length();

        outsideU = (u < 0.0 || u > 1.0);
        outsideV = (v < 0.0 || v > 1.0);
        outsideW = (w < 0.0 || w > 1.0);
        if ((outsideU && wasOutsideU) || (outsideV && wasOutsideV) || (outsideW && wasOutsideW))
        {
          // The ray is currently outside the triangle.

          above = (z > height);
          prevDelta = z-height;
          prevt = t;
          wasOutsideU = outsideU;
          wasOutsideV = outsideV;
          wasOutsideW = outsideW;
          continue;
        }
        if ((above && z <= height) || (!above && z >= height))
        {
          // The ray intersects the surface.

          if (numIntersections == tint.length)
          {
            double newt[] = new double [numIntersections*2];
            double newu[] = new double [numIntersections*2];
            double newv[] = new double [numIntersections*2];
            Vec3 newr[] = new Vec3 [numIntersections*2];
            for (int j = 0; j < tint.length; j++)
            {
              newt[j] = tint[j];
              newu[j] = uint[j];
              newv[j] = vint[j];
              newr[j] = rint[j];
            }
            for (int j = tint.length; j < newt.length; j++)
              newr[j] = new Vec3();
            tint = newt;
            uint = newu;
            vint = newv;
            rint = newr;
          }
          double oldz = z;
          double truet = t-(t-prevt)*(z-height)/(z-height-prevDelta);
          x = orig.x+truet*dir.x;
          y = orig.y+truet*dir.y;
          z = orig.z+truet*dir.z;
          rtTri.calcCoords(this, x, y, z);
          tint[numIntersections] = truet;
          uint[numIntersections] = u;
          vint[numIntersections] = v;
          rint[numIntersections].set(ray.origin.x+truet*ray.direction.x,
            ray.origin.y+truet*ray.direction.y,
            ray.origin.z+truet*ray.direction.z);
          numIntersections++;
          z = oldz;
        }
        prevDelta = z-height;
        above = (z > height);
        prevt = t;
        wasOutsideU = outsideU;
        wasOutsideV = outsideV;
        wasOutsideW = outsideW;
      }
    }
  }
}