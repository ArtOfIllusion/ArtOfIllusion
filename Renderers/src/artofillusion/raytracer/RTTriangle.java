/* Copyright (C) 1999-2013 by Peter Eastman
   Editions copyright (C) by Petri Ihalainen 2020

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

/** RTTriangle represents a triangle to be raytraced.  It is defined by specifying a
    RenderingMesh, the index of the RenderingTriangle within the mesh, and a matrix which
    gives the transformation from the mesh's local coordinates to world coordinates.  To
    save time and memory, the constructor is also passed two arrays which contain the
    vertices and normals for the mesh, transformed into world coordinates. */

public class RTTriangle extends RTObject
{
  RenderingTriangle tri;
  Vec3 vert1, trueNorm;
  short dropAxis, flags;
  Mat4 toLocal, fromLocal;
  double d, edge2d1x, edge2d1y, edge2d2x, edge2d2y;

  public static final double TOL = 1e-12;
  private static final short BUMP_MAPPED = 1;
  private static final short INTERP_NORMALS = 2;

  private double triangleTol;

  public RTTriangle(RenderingMesh mesh, int which, Mat4 fromLocal, Mat4 toLocal)
  {
    tri = mesh.triangle[which];
    vert1 = mesh.vert[tri.v1];
    Vec3 vert2 = mesh.vert[tri.v2];
    Vec3 vert3 = mesh.vert[tri.v3];
    trueNorm = mesh.faceNorm[which];
    this.fromLocal = fromLocal;
    this.toLocal = toLocal;
    if (tri.n1 != tri.n2 || tri.n1 != tri.n3)
    {
      flags |= INTERP_NORMALS;
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
    }
    d = -trueNorm.dot(vert1);
    if (Math.abs(trueNorm.y) > Math.abs(trueNorm.x))
    {
      if (Math.abs(trueNorm.z) > Math.abs(trueNorm.y))
        dropAxis = 2;
      else
        dropAxis = 1;
    }
    else
    {
      if (Math.abs(trueNorm.z) > Math.abs(trueNorm.x))
        dropAxis = 2;
      else
        dropAxis = 0;
    }
    switch (dropAxis)
    {
      case 0:
        edge2d1x = vert1.y-vert2.y;
        edge2d1y = vert1.z-vert2.z;
        edge2d2x = vert1.y-vert3.y;
        edge2d2y = vert1.z-vert3.z;
        break;
      case 1:
        edge2d1x = vert1.x-vert2.x;
        edge2d1y = vert1.z-vert2.z;
        edge2d2x = vert1.x-vert3.x;
        edge2d2y = vert1.z-vert3.z;
        break;
      default:
        edge2d1x = vert1.x-vert2.x;
        edge2d1y = vert1.y-vert2.y;
        edge2d2x = vert1.x-vert3.x;
        edge2d2y = vert1.y-vert3.y;
    }
    double denom = 1.0/(edge2d1x*edge2d2y-edge2d1y*edge2d2x);
    edge2d1x *= denom;
    edge2d1y *= denom;
    edge2d2x *= denom;
    edge2d2y *= denom;
    if (tri.theMesh.mapping.getTexture().hasComponent(Texture.BUMP_COMPONENT))
      flags |= BUMP_MAPPED;

    triangleTol = (Math.max(Math.max(Math.abs(fromLocal.m14), Math.abs(fromLocal.m24)), Math.abs(fromLocal.m34)) +
                   Math.max(Math.max(Math.max(Math.max(vert1.x, vert2.x), vert3.x) - Math.min(Math.min(vert1.x, vert2.x), vert3.x),
                                     Math.max(Math.max(vert1.y, vert2.y), vert3.y) - Math.min(Math.min(vert1.y, vert2.y), vert3.y)),
                                     Math.max(Math.max(vert1.z, vert2.z), vert3.z) - Math.min(Math.min(vert1.z, vert2.z), vert3.z))
                  )*TOL;
  }

  /** Get the TextureMapping for this object. */

  @Override
  public final TextureMapping getTextureMapping()
  {
    return tri.getTextureMapping();
  }

  /** Get the MaterialMapping for this object. */

  @Override
  public final MaterialMapping getMaterialMapping()
  {
    return tri.theMesh.matMapping;
  }

  /** Determine whether the given ray intersects this triangle. */

  @Override
  public SurfaceIntersection checkIntersection(Ray r)
  {
    double vd, v0, vx, vy;
    Vec3 orig = r.getOrigin(), dir = r.getDirection();

    vd = trueNorm.dot(dir);
    if (vd == 0.0)
      return SurfaceIntersection.NO_INTERSECTION;  // The ray is parallel to the plane.
    v0 = -(trueNorm.dot(orig)+d);
    double t = v0/vd;
    if (t < triangleTol)
      return SurfaceIntersection.NO_INTERSECTION;  // Ray points away from plane of triangle.

    // Determine whether the intersection point is inside the triangle.

    double rix = orig.x+dir.x*t;
    double riy = orig.y+dir.y*t;
    double riz = orig.z+dir.z*t;
    switch (dropAxis)
    {
      case 0:
        vx = riy - vert1.y;
        vy = riz - vert1.z;
        break;
      case 1:
        vx = rix - vert1.x;
        vy = riz - vert1.z;
        break;
      default:
        vx = rix - vert1.x;
        vy = riy - vert1.y;
    }
    double v = edge2d2x*vy - edge2d2y*vx;
    if (v < -TOL || v > 1.0+TOL)
      return SurfaceIntersection.NO_INTERSECTION;
    double w = vx*edge2d1y - vy*edge2d1x;
    if (w < -TOL || w > 1.0+TOL)
      return SurfaceIntersection.NO_INTERSECTION;
    double u = 1.0-v-w;
    if (u < -TOL || u > 1.0+TOL)
      return SurfaceIntersection.NO_INTERSECTION;
    TriangleIntersection intersection = (r.rt == null ? new TriangleIntersection() : (TriangleIntersection) r.rt.rtTriPool.getObject());
    intersection.init(this, t, u, v, rix, riy, riz);
    return intersection;
  }

  /** Get a bounding box for this triangle. */

  @Override
  public BoundingBox getBounds()
  {
    double minx, maxx, miny, maxy, minz, maxz;
    Vec3 vert2 = tri.theMesh.vert[tri.v2];
    Vec3 vert3 = tri.theMesh.vert[tri.v3];

    minx = maxx = vert1.x;
    miny = maxy = vert1.y;
    minz = maxz = vert1.z;
    if (vert2.x < minx)
      minx = vert2.x;
    if (vert2.x > maxx)
      maxx = vert2.x;
    if (vert2.y < miny)
      miny = vert2.y;
    if (vert2.y > maxy)
      maxy = vert2.y;
    if (vert2.z < minz)
      minz = vert2.z;
    if (vert2.z > maxz)
      maxz = vert2.z;
    if (vert3.x < minx)
      minx = vert3.x;
    if (vert3.x > maxx)
      maxx = vert3.x;
    if (vert3.y < miny)
      miny = vert3.y;
    if (vert3.y > maxy)
      maxy = vert3.y;
    if (vert3.z < minz)
      minz = vert3.z;
    if (vert3.z > maxz)
      maxz = vert3.z;
    return new BoundingBox(minx, maxx, miny, maxy, minz, maxz);
  }

  /** Determine whether any part of the triangle lies within a bounding box. */

  @Override
  public boolean intersectsNode(OctreeNode node)
  {
    Vec3 vert2 = tri.theMesh.vert[tri.v2];
    Vec3 vert3 = tri.theMesh.vert[tri.v3];

    // First see if any vertex is inside the box.

    if (node.contains(vert1) || node.contains(vert2) || node.contains(vert3))
      return true;

    // If any edge intersects the box, return true.

    if (edgeIntersectsBox(vert1, vert2, node) || edgeIntersectsBox(vert2, vert3, node) || edgeIntersectsBox(vert3, vert1, node))
      return true;

    // None of the vertices is inside the box.  However, it is possible that the
    // triangle cuts through the entire box.  Check each of the diagonals of
    // the box to see if one intersects the triangle.

    Ray r = new Ray(null);
    Vec3 orig = r.getOrigin(), dir = r.getDirection();
    double len;

    orig.set(node.minx, node.miny, node.minz);
    dir.set(node.maxx-node.minx, node.maxy-node.miny, node.maxz-node.minz);
    len = dir.length();
    dir.scale(1.0/len);
    SurfaceIntersection intersect = checkIntersection(r);
    if (intersect != SurfaceIntersection.NO_INTERSECTION && intersect.intersectionDist(0) < len)
      return true;
    orig.set(node.maxx, node.miny, node.minz);
    dir.set(node.minx-node.maxx, node.maxy-node.miny, node.maxz-node.minz);
    len = dir.length();
    dir.scale(1.0/len);
    intersect = checkIntersection(r);
    if (intersect != SurfaceIntersection.NO_INTERSECTION && intersect.intersectionDist(0) < len)
      return true;
    orig.set(node.minx, node.maxy, node.minz);
    dir.set(node.maxx-node.minx, node.miny-node.maxy, node.maxz-node.minz);
    len = dir.length();
    dir.scale(1.0/len);
    intersect = checkIntersection(r);
    if (intersect != SurfaceIntersection.NO_INTERSECTION && intersect.intersectionDist(0) < len)
      return true;
    orig.set(node.minx, node.miny, node.maxz);
    dir.set(node.maxx-node.minx, node.maxy-node.miny, node.minz-node.maxz);
    len = dir.length();
    dir.scale(1.0/len);
    intersect = checkIntersection(r);
    if (intersect != SurfaceIntersection.NO_INTERSECTION && intersect.intersectionDist(0) < len)
      return true;
    return false;
  }

  /** Determine whether a particular edge of the triangle intersects a bounding box. */

  boolean edgeIntersectsBox(Vec3 p1, Vec3 p2, OctreeNode node)
  {
    double t1, t2, mint = -Double.MAX_VALUE, maxt = Double.MAX_VALUE;
    double dirx = p2.x-p1.x, diry = p2.y-p1.y, dirz = p2.z-p1.z;
    double len = Math.sqrt(dirx*dirx + diry*diry + dirz*dirz);
    if (dirx == 0.0)
    {
      if (p1.x < node.minx || p1.x > node.maxx)
        return false;
    }
    else
    {
      t1 = (node.minx-p1.x)*len/dirx;
      t2 = (node.maxx-p1.x)*len/dirx;
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
      if (mint > maxt || mint > len || maxt < 0.0)
        return false;
    }
    if (diry == 0.0)
    {
      if (p1.y < node.miny || p1.y > node.maxy)
        return false;
    }
    else
    {
      t1 = (node.miny-p1.y)*len/diry;
      t2 = (node.maxy-p1.y)*len/diry;
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
      if (mint > maxt || mint > len || maxt < 0.0)
        return false;
    }
    if (dirz == 0.0)
    {
      if (p1.z < node.minz || p1.z > node.maxz)
        return false;
    }
    else
    {
      t1 = (node.minz-p1.z)*len/dirz;
      t2 = (node.maxz-p1.z)*len/dirz;
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
      if (mint > maxt || mint > len || maxt < 0.0)
        return false;
    }
    return true;
  }

  /** Get the transformation from world coordinates to the object's local coordinates. */

  @Override
  public Mat4 toLocal()
  {
    return toLocal;
  }

  /** Get the mesh this triangle is part of. */

  @Override
  public Object getObject()
  {
    return tri.theMesh;
  }

  /**
   * Inner class representing an intersection with an RTTriangle.
   */

  public static class TriangleIntersection implements SurfaceIntersection
  {
    private RTTriangle rtTri;
    private double dist, u, v, rix, riy, riz;

    public TriangleIntersection()
    {
    }

    public void init(RTTriangle rtTri, double dist, double u, double v, double rix, double riy, double riz)
    {
      this.rtTri = rtTri;
      this.dist = dist;
      this.u = u;
      this.v = v;
      this.rix = rix;
      this.riy = riy;
      this.riz = riz;
    }

    @Override
    public RTObject getObject()
    {
      return rtTri;
    }

    @Override
    public int numIntersections()
    {
      return 1;
    }

    @Override
    public void intersectionPoint(int n, Vec3 p)
    {
      p.set(rix, riy, riz);
    }

    @Override
    public double intersectionDist(int n)
    {
      return dist;
    }

    @Override
    public void intersectionProperties(TextureSpec spec, Vec3 n, Vec3 viewDir, double size, double time)
    {
      double w = 1.0-u-v;
      if ((rtTri.flags&INTERP_NORMALS) == 0)
        n.set(rtTri.trueNorm);
      else
      {
        Vec3 normals[] = rtTri.tri.theMesh.norm;
        Vec3 norm1 = normals[rtTri.tri.n1];
        Vec3 norm2 = normals[rtTri.tri.n2];
        Vec3 norm3 = normals[rtTri.tri.n3];
        n.x = u*norm1.x + v*norm2.x + w*norm3.x;
        n.y = u*norm1.y + v*norm2.y + w*norm3.y;
        n.z = u*norm1.z + v*norm2.z + w*norm3.z;
        n.normalize();
      }
      rtTri.tri.getTextureSpec(spec, -n.dot(viewDir), u, v, w, size, time);
      if ((rtTri.flags&BUMP_MAPPED) != 0)
      {
        rtTri.fromLocal.transformDirection(spec.bumpGrad);
        n.scale(spec.bumpGrad.dot(n)+1.0);
        n.subtract(spec.bumpGrad);
        n.normalize();
      }
    }

    @Override
    public void intersectionTransparency(int n, RGBColor trans, double angle, double size, double time)
    {
      rtTri.tri.getTransparency(trans, angle, u, v, 1.0-u-v, size, time);
    }

    @Override
    public void trueNormal(Vec3 n)
    {
      n.set(rtTri.trueNorm);
    }
  }
}
