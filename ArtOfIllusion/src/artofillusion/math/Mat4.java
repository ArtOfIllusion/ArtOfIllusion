/* Copyright (C) 1999,2000,2003 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;

import java.io.*;

/** This class represents a 4x4 matrix.  It is typically used for representing coordinate transformations. */

public class Mat4
{
  public final double m11, m12, m13, m14;
  public final double m21, m22, m23, m24;
  public final double m31, m32, m33, m34;
  public final double m41, m42, m43, m44;
  
  /** Create a new Mat4 by explicitly setting its components. */
  
  public Mat4 (double e11, double e12, double e13, double e14, 
  		double e21, double e22, double e23, double e24, 
  		double e31, double e32, double e33, double e34, 
  		double e41, double e42, double e43, double e44)
  {
    m11 = e11;  m12 = e12;  m13 = e13;  m14 = e14;
    m21 = e21;  m22 = e22;  m23 = e23;  m24 = e24;
    m31 = e31;  m32 = e32;  m33 = e33;  m34 = e34;
    m41 = e41;  m42 = e42;  m43 = e43;  m44 = e44;
  }

  /** Create a new Mat4 by multiplying two matrices. */

  public final Mat4 times(Mat4 a)
  {
    return new Mat4(m11*a.m11 + m12*a.m21 + m13*a.m31 + m14*a.m41,
    	m11*a.m12 + m12*a.m22 + m13*a.m32 + m14*a.m42,
    	m11*a.m13 + m12*a.m23 + m13*a.m33 + m14*a.m43,
    	m11*a.m14 + m12*a.m24 + m13*a.m34 + m14*a.m44,
    	m21*a.m11 + m22*a.m21 + m23*a.m31 + m24*a.m41,
    	m21*a.m12 + m22*a.m22 + m23*a.m32 + m24*a.m42,
    	m21*a.m13 + m22*a.m23 + m23*a.m33 + m24*a.m43,
    	m21*a.m14 + m22*a.m24 + m23*a.m34 + m24*a.m44,
    	m31*a.m11 + m32*a.m21 + m33*a.m31 + m34*a.m41,
    	m31*a.m12 + m32*a.m22 + m33*a.m32 + m34*a.m42,
    	m31*a.m13 + m32*a.m23 + m33*a.m33 + m34*a.m43,
    	m31*a.m14 + m32*a.m24 + m33*a.m34 + m34*a.m44,
    	m41*a.m11 + m42*a.m21 + m43*a.m31 + m44*a.m41,
    	m41*a.m12 + m42*a.m22 + m43*a.m32 + m44*a.m42,
    	m41*a.m13 + m42*a.m23 + m43*a.m33 + m44*a.m43,
    	m41*a.m14 + m42*a.m24 + m43*a.m34 + m44*a.m44);
  }
  
  /** Multiply this matrix (M) by a vector (v) and return the product
      Mv.  Both the input and output vectors are assumed to be in homogeneous coordinates,
      and to have an implicit fourth element equal to 1. */
  
  public final Vec3 times(Vec3 v)
  {
    double w = m41*v.x + m42*v.y + m43*v.z + m44;
    
    return new Vec3((m11*v.x + m12*v.y + m13*v.z + m14)/w,
    		    (m21*v.x + m22*v.y + m23*v.z + m24)/w,
    		    (m31*v.x + m32*v.y + m33*v.z + m34)/w);
  }
  
  /** This method is identical to times(), except that v is assumed to be a direction vector.
      That is, the implicit fourth element is assumed to be 0. */

  public final Vec3 timesDirection(Vec3 v)
  {
    return new Vec3(m11*v.x + m12*v.y + m13*v.z,
    		    m21*v.x + m22*v.y + m23*v.z,
    		    m31*v.x + m32*v.y + m33*v.z);
  }

  /** This method works exactly like the times() method, except that it only calculates
      the x and y components of the output vector.  This can save several operations when
      the z component is not of interest, for example, when applying a perspective
      projection. */
     
  public final Vec2 timesXY(Vec3 v)
  {
    double w = m41*v.x + m42*v.y + m43*v.z + m44;
    
    return new Vec2((m11*v.x + m12*v.y + m13*v.z + m14)/w,
    		    (m21*v.x + m22*v.y + m23*v.z + m24)/w);
  }

  /** This method works exactly like the times() method, except that it only calculates
      the x and y components of the output vector.  This can save several operations when
      the z component is not of interest, for example, when applying a perspective
      projection.  Also, rather than creating a new object, it allows you to pass in an
      existing object to receive the results. */
     
  public final Vec2 timesXY(Vec3 v, Vec2 result)
  {
    double w = m41*v.x + m42*v.y + m43*v.z + m44;
    
    result.set((m11*v.x + m12*v.y + m13*v.z + m14)/w,
    		    (m21*v.x + m22*v.y + m23*v.z + m24)/w);
    return result;
  }
  
  /** This method works like the times() method, except that it only calculates the z
      component of the output vector.  This is useful, for example, for determining whether
      a point lies in front of or behind the viewer. */

  public final double timesZ(Vec3 v)
  {
    double w = m41*v.x + m42*v.y + m43*v.z + m44;
    
    return (m31*v.x + m32*v.y + m33*v.z + m34)/w;
  }
  
  /** This method works like times(), except that the calculation is done in place. */

  public final void transform(Vec3 v)
  {
    double w = m41*v.x + m42*v.y + m43*v.z + m44;
    
    v.set((m11*v.x + m12*v.y + m13*v.z + m14)/w,
	(m21*v.x + m22*v.y + m23*v.z + m24)/w,
	(m31*v.x + m32*v.y + m33*v.z + m34)/w);
  }
  
  /** This method works like timesDirection(), except that the calculation is done in place. */

  public final void transformDirection(Vec3 v)
  {
    v.set(m11*v.x + m12*v.y + m13*v.z,
	m21*v.x + m22*v.y + m23*v.z,
	m31*v.x + m32*v.y + m33*v.z);
  }

  /** Create an identity matrix. */
  
  public static Mat4 identity()
  {
    return new Mat4(1.0, 0.0, 0.0, 0.0,
    		0.0, 1.0, 0.0, 0.0,
    		0.0, 0.0, 1.0, 0.0,
    		0.0, 0.0, 0.0, 1.0);
  }

  /** Create a matrix to scale x, y, and z by sx, sy, and sz respectively. */
  
  public static Mat4 scale(double sx, double sy, double sz)
  {
    return new Mat4(sx, 0.0, 0.0, 0.0,
    		0.0, sy, 0.0, 0.0,
    		0.0, 0.0, sz, 0.0,
    		0.0, 0.0, 0.0, 1.0);
  }

  /** Create a matrix to translate a vector by (dx, dy, dz). */

  public static Mat4 translation(double dx, double dy, double dz)
  {
    return new Mat4(1.0, 0.0, 0.0, dx,
    		0.0, 1.0, 0.0, dy,
    		0.0, 0.0, 1.0, dz,
    		0.0, 0.0, 0.0, 1.0);
  }
  
  /** Create a matrix that rotates a vector around the X axis.
      @param angle     the rotation angle, in radians
   */
  
  public static Mat4 xrotation(double angle)
  {
    double c = Math.cos(angle), s = Math.sin(angle);
    
    return new Mat4(1.0, 0.0, 0.0, 0.0,
    		0.0, c, -s, 0.0,
    		0.0, s, c, 0.0,
    		0.0, 0.0, 0.0, 1.0);
  }

  /** Create a matrix that rotates a vector around the Y axis.
      @param angle     the rotation angle, in radians
   */
  
  public static Mat4 yrotation(double angle)
  {
    double c = Math.cos(angle), s = Math.sin(angle);
    
    return new Mat4(c, 0.0, s, 0.0,
    		0.0, 1.0, 0.0, 0.0,
    		-s, 0.0, c, 0.0,
    		0.0, 0.0, 0.0, 1.0);
  }

  /** Create a matrix that rotates a vector around the Z axis.
      @param angle     the rotation angle, in radians
   */
  
  public static Mat4 zrotation(double angle)
  {
    double c = Math.cos(angle), s = Math.sin(angle);
    
    return new Mat4(c, -s, 0.0, 0.0,
    		s, c, 0.0, 0.0,
    		0.0, 0.0, 1.0, 0.0,
    		0.0, 0.0, 0.0, 1.0);
  }
  
  /** This routine creates a matrix to rotate a vector around an arbitrary axis.
      @param axis      the axis around which to rotate
      @param angle     the rotation angle, in radians
   */
  
  public static Mat4 axisRotation(Vec3 axis, double angle)
  {
    double c = Math.cos(angle), s = Math.sin(angle);
    double t = 1.0 - c;
    
    return new Mat4(t*axis.x*axis.x + c,
	t*axis.x*axis.y - s*axis.z,
	t*axis.x*axis.z + s*axis.y,
	0.0,
	t*axis.x*axis.y + s*axis.z,
	t*axis.y*axis.y + c,
	t*axis.y*axis.z - s*axis.x,
	0.0,
	t*axis.x*axis.z - s*axis.y,
	t*axis.y*axis.z + s*axis.x,
	t*axis.z*axis.z + c,
	0.0,
	0.0, 0.0, 0.0, 1.0);
  }

  /** Create a matrix for transforming from world coordinates to viewing coordinates.  This
      matrix transforms coordinates such that the position vector orig is translated to
      the origin, the direction vector zdir lies along the positive z axis, and the
      direction vection updir lies the positive y half of the yz plane. */

  public static Mat4 viewTransform(Vec3 orig, Vec3 zdir, Vec3 updir)
  {
    Vec3 rx, ry, rz;

    rz = zdir.times(1.0/zdir.length());
    rx = updir.cross(zdir);
    rx.normalize();
    ry = rz.cross(rx);
    return new Mat4(rx.x, rx.y, rx.z, -(rx.x*orig.x + rx.y*orig.y + rx.z*orig.z),
		ry.x, ry.y, ry.z, -(ry.x*orig.x + ry.y*orig.y + ry.z*orig.z),
		rz.x, rz.y, rz.z, -(rz.x*orig.x + rz.y*orig.y + rz.z*orig.z),
		0.0, 0.0, 0.0, 1.0);
  }
  
  /** Create a matrix which is the inverse of the viewTransform matrix.  That is, it first
      rotates the rotates the z axis to lie along the direction of zdir and the y axis to
      lie in the updir direction, then translates the origin to the point orig.  This is
      useful for transforming from object coordinates to world coordinates. */
     
  public static Mat4 objectTransform(Vec3 orig, Vec3 zdir, Vec3 updir)
  {
    Vec3 rx, ry, rz;

    rz = zdir.times(1.0/zdir.length());
    rx = updir.cross(zdir);
    rx.normalize();
    ry = rz.cross(rx);
    return new Mat4(rx.x, ry.x, rz.x, orig.x,
		rx.y, ry.y, rz.y, orig.y,
		rx.z, ry.z, rz.z, orig.z,
		0.0, 0.0, 0.0, 1.0);
  }

  /** Create a matrix to implement a perspective projection.  The center of projection is
      at (0, 0, -d), and the projection plane is given by z=1. */

  public static Mat4 perspective(double d)
  {
    double e33 = 1.0/((d+1.0)*(d+1.0));
    
    return new Mat4(1.0, 0.0, 0.0, 0.0,
		0.0, 1.0, 0.0, 0.0,
		0.0, 0.0, e33, 1.0-e33,
		0.0, 0.0, e33, 1.0-e33);
  }
  
  public String toString()
  {
    return "Mat4: {"+m11+", "+m12 +", "+m13+", "+m14+"}, {"+m21+", "+m22 +", "+m23+", "+m24+"}, {"+m31+", "+m32 +", "+m33+", "+m34+"}, {"+m41+", "+m42 +", "+m43+", "+m44+"}";
  }
  
  public boolean equals(Object o)
  {
    if (!(o instanceof Mat4))
      return false;
    Mat4 m = (Mat4) o;
    return (m11 == m.m11 && m12 == m.m12 && m13 == m.m13 && m14 == m.m14 &&
      m21 == m.m21 && m22 == m.m22 && m23 == m.m23 && m24 == m.m24 &&
      m31 == m.m31 && m32 == m.m32 && m33 == m.m33 && m34 == m.m34 &&
      m41 == m.m41 && m42 == m.m42 && m43 == m.m43 && m44 == m.m44);
  }
  
  public int hashCode()
  {
    double d = (m11+m12+m13+m14) + 2.2*(m21+m22+m23+m24) + 3.3*(m31+m32+m33+m34) + 4.4*(m41+m42+m43+m44);
    return Float.floatToIntBits((float) d);
  }
  
  /** Create a Mat4 by reading in information that was written by writeToFile(). */
  
  public Mat4(DataInputStream in) throws IOException
  {
    m11 = in.readDouble(); m12 = in.readDouble(); m13 = in.readDouble(); m14 = in.readDouble();
    m21 = in.readDouble(); m22 = in.readDouble(); m23 = in.readDouble(); m24 = in.readDouble();
    m31 = in.readDouble(); m32 = in.readDouble(); m33 = in.readDouble(); m34 = in.readDouble();
    m41 = in.readDouble(); m42 = in.readDouble(); m43 = in.readDouble(); m44 = in.readDouble();
  }
  
  /** Write out a serialized representation of this object. */
  
  public void writeToFile(DataOutputStream out) throws IOException
  {
    out.writeDouble(m11); out.writeDouble(m12); out.writeDouble(m13); out.writeDouble(m14);
    out.writeDouble(m21); out.writeDouble(m22); out.writeDouble(m23); out.writeDouble(m24);
    out.writeDouble(m31); out.writeDouble(m32); out.writeDouble(m33); out.writeDouble(m34);
    out.writeDouble(m41); out.writeDouble(m42); out.writeDouble(m43); out.writeDouble(m44);
  }
}