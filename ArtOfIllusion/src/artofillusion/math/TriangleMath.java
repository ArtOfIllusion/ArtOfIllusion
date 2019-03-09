/* Copyright (C) 2016 - 2017 by Petri Ihalainen

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;

import artofillusion.*;
import artofillusion.ui.*;
import artofillusion.math.*;
import java.awt.*;

/**
 *  TriangleProps contains frequently used calculations about triangles
 */
public class TriangleMath
{
  // Dummy constructor

  private TriangleMath() {};

  //********************************//
  //*   METHODS FOR 3D TRIANGLES   *//
  //********************************//

  /** Cross product of two side vectors in 3D*/

  public static Vec3 triangleCross(Vec3 A, Vec3 B, Vec3 C)
  {
    return B.minus(A).cross(C.minus(A));
  }

  /** Cross product of two side vectors in 3D*/

  public static Vec3 triangleCross(RenderingTriangle t)
  {
    return triangleCross(t.theMesh.vert[t.v1], t.theMesh.vert[t.v2], t.theMesh.vert[t.v3]);
  }

  /** Triangle normal in 3D space */

  public static Vec3 triangleNormal(Vec3 A, Vec3 B, Vec3 C)
  {
    Vec3 n = triangleCross(A, B, C);
    n.normalize();
    return n;
  }

  /** Triangle normal in 3D space */

  public static Vec3 triangleNormal(RenderingTriangle t)
  {
    return triangleNormal(t.theMesh.vert[t.v1], t.theMesh.vert[t.v2], t.theMesh.vert[t.v3]);
  }

  /** Triangle area in 3D-space. Triangle expressed as corner vectors */

  public static double triangleArea(Vec3 A, Vec3 B, Vec3 C)
  {
    return  triangleCross(A, B, C).length() * 0.5;
  }

  /** Triangle area in 3D-space. Triangle as a RenderingTriangle. */
  public static double triangleArea(RenderingTriangle t)
  {
    return triangleArea(t.theMesh.vert[t.v1], t.theMesh.vert[t.v2], t.theMesh.vert[t.v3]);
  }

  /** Triangle center in 3D-space. Triangle as corner vectors. */

  public static Vec3 triangleCenter(Vec3 A, Vec3 B, Vec3 C)
  {
    return A.plus(B).plus(C).times(1.0/3.0);
  }

  /** Triangle center in 3D-space. Triangle as RenderingTriangle. */

  public static Vec3 triangleCenter(RenderingTriangle t)
  {
    return triangleCenter(t.theMesh.vert[t.v1], t.theMesh.vert[t.v2], t.theMesh.vert[t.v3]);
  }

  /**
   *  Barycentric coordinates in 3D space. The point can be anywhere in the space. It will be
   *  treated as projected on the triangle plane.
   */

  public static double[] baryCoordinates(Vec3 A, Vec3 B, Vec3 C, Vec3 p)
  {
    double[] bary = new double[3];
    Vec3 triCross = triangleCross(A, B, C);
    double tcl = triCross.length();
    bary[0]   = triangleCross(p, B, C).dot(triCross)/tcl/6.0; // w -- weigth for A
    bary[1]   = triangleCross(p, C, A).dot(triCross)/tcl/6.0; // v -- weigth for B
    bary[2]   = triangleCross(p, A, B).dot(triCross)/tcl/6.0; // u -- weigth for C
    return bary;
  }

  /**
   *  Barycentric coordinates in 3D space. The point can be anywhere in the space. It will be
   *  treated as projected on the triangle plane.
   */

  public static double[] baryCoordinates(RenderingTriangle t, Vec3 p)
  {
    return baryCoordinates(t.theMesh.vert[t.v1], t.theMesh.vert[t.v2], t.theMesh.vert[t.v3], p);
  }

  /**
   *  Interpolated point on triangle plane in 3D by given barycentic coordinates.
   */

  public static Vec3 interpolate(Vec3 A, Vec3 B, Vec3 C, double[] bary)
  {
    return A.times(bary[0]).plus(B.times(bary[1])).plus(C.times(bary[3]));
  }

  /**
   *  Interpolated point on a RenderingTriangle plane by given barycentic coordinates.
   */

  public static Vec3 interpolate(RenderingTriangle t, double[] bary)
  {
    return interpolate(t.theMesh.vert[t.v1], t.theMesh.vert[t.v2], t.theMesh.vert[t.v3], bary);
  }

  /**
   *  Projection of a point on a triangle plane in 3D. The point can be anywhere in the space.
   */
  public static Vec3 project(Vec3 A, Vec3 B, Vec3 C, Vec3 p)
  {
    double[] bary = new double[3];
    bary = baryCoordinates(A, B, C, p);
    return interpolate(A, B, C, bary);
  }

  //********************************//
  //*   METHODS FOR 2D TRIANGLES   *//
  //********************************//

  /** Triangle area in 2D-space. Triangle as corner vectors. */

  public static double triangleArea(Vec2 A, Vec2 B, Vec2 C)
  {
    return (B.minus(A).cross(C.minus(A)) * 0.5);
  }

  /** Triangle area in 2D-space. The triangle as Vec2[3]. */

  public static double triangleArea(Vec2[] t)
  {
    return (triangleArea(t[0], t[1], t[2]));
  }

  /** Barycentric coordinates in 2D space. The triangle as corner vectors. */

  public static double[] baryCoordinates(Vec2 A, Vec2 B, Vec2 C, Vec2 p)
  {
    double[] bary = new double[3];
    double wholeArea = triangleArea(A, B, C);
    bary[0]   = triangleArea(p, B, C)/wholeArea; // w -- weigth for A
    bary[1]   = triangleArea(p, C, A)/wholeArea; // v -- weigth for B
    bary[2]   = triangleArea(p, A, B)/wholeArea; // u -- weigth for C
    return bary;
  }

  /** Barycentric coordinates in 2D space. The triangle as Vec2[3]. */

  public static double[] baryCoordinates(Vec2[] t, Vec2 p)
  {
    return baryCoordinates(t[0], t[1], t[2], p);
  }

  /** Barycentric coordinates in 2D space. The triangle as Vec2[3], point as Point */

  public static double[] baryCoordinates(Vec2[] t, Point p)
  {
    return baryCoordinates(t[0], t[1], t[2], new Vec2(p.x, p.y));
  }
}