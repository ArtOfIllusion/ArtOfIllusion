/* Copyright (C) 1999-2003 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;

import java.io.*;

/** The CoordinateSystem class describes the position and orientation of one coordinate
    system relative to another one.  It is defined by three vectors.  orig defines the position
    of the origin of the new coordinate system, while zdir and updir define the directions
    of the z and y axes, respectively.  (Note that the y axis will only be parallel to
    updir in the special case that zdir and updir are perpendicular to each other.) 
    Alternatively, the orientation can be represented by three rotation angles about
    the (global) x, y, and z axes.  Both representations are maintained internally. */

public class CoordinateSystem
{
  Vec3 orig, zdir, updir;
  Mat4 transformTo, transformFrom;
  double xrot, yrot, zrot;
  
  /** Create a new CoordinateSystem which represents an identity transformation (i.e. no
      translation or rotation). */
  
  public CoordinateSystem()
  {
    this(new Vec3(), Vec3.vz(), Vec3.vy());
  }
  
  /** Create a new coordinate system.
      @param orig     the origin of the new coordinate system
      @param zdir     the direction of the new coordinate system's z axis
      @param updir    defines the "up" direction.  If this is perpendicular to zdir, this will be
                      the y axis direction of the new coordinate system.
  */
  
  public CoordinateSystem(Vec3 orig, Vec3 zdir, Vec3 updir)
  {
    this.orig = orig;
    this.zdir = zdir;
    this.updir = updir;
    zdir.normalize();
    updir.normalize();
    findRotationAngles();
  }
  
  /** Create a new coordinate system.
      @param orig     the origin of the new coordinate system
      @param x        the rotation angle around the x axis
      @param y        the rotation angle around the y axis
      @param z        the rotation angle around the z axis
  */

  public CoordinateSystem(Vec3 orig, double x, double y, double z)
  {
    this.orig = orig;
    setOrientation(x, y, z);
  }

  /** Create an exact duplicate of this CoordinateSystem. */

  public final CoordinateSystem duplicate()
  {
    CoordinateSystem coords = new CoordinateSystem(new Vec3(orig.x, orig.y, orig.z), new Vec3(zdir.x, zdir.y, zdir.z), new Vec3(updir.x, updir.y, updir.z));
    coords.xrot = xrot;
    coords.yrot = yrot;
    coords.zrot = zrot;
    return coords;
  }
  
  /** Make this CoordianteSystem identical to another one. */
  
  public final void copyCoords(CoordinateSystem c)
  {
    setOrigin(new Vec3(c.orig.x, c.orig.y, c.orig.z));
    setOrientation(new Vec3(c.zdir), new Vec3(c.updir));
    xrot = c.xrot;
    yrot = c.yrot;
    zrot = c.zrot;
  }
  
  /** Determine whether this coordinate system is identical to another one. */
  
  public final boolean equals(Object coords)
  {
    CoordinateSystem c = (CoordinateSystem) coords;
    if (!orig.equals(c.orig))
      return false;
    if (!zdir.equals(c.zdir))
      return false;
    if (!updir.equals(c.updir))
      return false;
    return true;
  }
  
  /** Set the position of this CoordinateSystem's origin. */
  
  public final void setOrigin(Vec3 orig)
  {
    this.orig = orig;
    transformTo = transformFrom = null;
  }
  
  /** Set the orientation of this CoordinateSystem.
      @param zdir     the direction of this coordinate system's z axis
      @param updir    defines the "up" direction.  If this is perpendicular to zdir, this will become
                      the y axis direction.
  */

  public final void setOrientation(Vec3 zdir, Vec3 updir)
  {
    this.zdir = zdir;
    this.updir = updir;
    zdir.normalize();
    updir.normalize();
    findRotationAngles();
    transformTo = transformFrom = null;
  }
  
  /** Set the orientation of this CoordinateSystem.
      @param x        the rotation angle around the x axis
      @param y        the rotation angle around the y axis
      @param z        the rotation angle around the z axis
  */

  public final void setOrientation(double x, double y, double z)
  {
    Mat4 m;
    
    xrot = x*Math.PI/180.0;
    yrot = y*Math.PI/180.0;
    zrot = z*Math.PI/180.0;
    m = Mat4.yrotation(-yrot).times(Mat4.xrotation(-xrot)).times(Mat4.zrotation(-zrot));
    zdir = m.times(Vec3.vz());
    updir = m.times(Vec3.vy());
    transformTo = transformFrom = null;
  }

  /** Get the origin of this CoordinateSystem. */

  public final Vec3 getOrigin()
  {
    return orig;
  }
  
  /** Get this CoordinateSystem's z axis direction. */

  public final Vec3 getZDirection()
  {
    return zdir;
  }
  
  /** Get the vector used to define "up" in this CoordinateSystem (usually but not always the y axis direction). */

  public final Vec3 getUpDirection()
  {
    return updir;
  }

  /** Return the x, y, and z rotation angles.  */

  public final double [] getRotationAngles()
  {
    return new double [] {xrot*180.0/Math.PI, yrot*180.0/Math.PI, zrot*180.0/Math.PI};
  }

  /** Transform this CoordinateSystem's orientation by applying a matrix to its axis directions. */
  
  public final void transformAxes(Mat4 m)
  {
    zdir = m.timesDirection(zdir);
    updir = m.timesDirection(updir);
    findRotationAngles();
    transformTo = transformFrom = null;
  }
  
  /** Transform this CoordinateSystem's position by applying a matrix to its origin. */

  public final void transformOrigin(Mat4 m)
  {
    orig = m.times(orig);
    transformTo = transformFrom = null;
  }
  
  /** Transform this CoordinateSystem's position and orientation by applying a matrix to its origin
      and axis directions. */

  public final void transformCoordinates(Mat4 m)
  {
    orig = m.times(orig);
    zdir = m.timesDirection(zdir);
    updir = m.timesDirection(updir);
    findRotationAngles();
    transformTo = transformFrom = null;
  }
  
  /** Return a matrix which will transform points from this coordinate system to the outside
      coordinate system with respect to which it is defined. */

  public final Mat4 fromLocal()
  {
    if (transformFrom == null)
      transformFrom = Mat4.objectTransform(orig, zdir, updir);
    return transformFrom;
  }

  /** Return a matrix which will transform points from the outside coordinate system to 
      this local coordinate system. */

  public final Mat4 toLocal()
  {
    if (transformTo == null)
      transformTo = Mat4.viewTransform(orig, zdir, updir);
    return transformTo;
  }
  
  /** Calculate the x, y, and z rotation angles given the current values for zdir and updir. */
  
  private void findRotationAngles()
  {
    Vec3 v;
    double d;
    Mat4 m;
    
    if (zdir.x == 0.0 && zdir.z == 0.0)
      {
        d = Math.sqrt(updir.x*updir.x+updir.z*updir.z);
        yrot = Math.acos(updir.z/d);
        if (Double.isNaN(yrot))
          yrot = Math.acos(updir.z > 0.0 ? 1.0 : -1.0);
        if (zdir.y > 0.0)
          yrot *= -1.0;
      }
    else
      {
        d = Math.sqrt(zdir.x*zdir.x+zdir.z*zdir.z);
        yrot = Math.acos(zdir.z/d);
        if (Double.isNaN(yrot))
          yrot = Math.acos(zdir.z > 0.0 ? 1.0 : -1.0);
        if (zdir.x > 0.0)
          yrot *= -1.0;
      }
    m = Mat4.yrotation(yrot);
    v = m.times(zdir);
    d = zdir.length();
    xrot = Math.acos(v.z/d);
    if (Double.isNaN(xrot))
      xrot = Math.acos(v.z > 0.0 ? 1.0 : -1.0);
    if (v.y < 0.0)
      xrot *= -1.0;
    m = Mat4.xrotation(xrot).times(m);
    v = m.times(updir);
    d = Math.sqrt(v.x*v.x+v.y*v.y);
    zrot = Math.acos(v.y/d);
    if (Double.isNaN(zrot))
      zrot = Math.acos(v.y > 0.0 ? 1.0 : -1.0);
    if (v.x < 0.0)
      zrot *= -1.0;
  }

  /** A rotation can also be described by specifying a rotation axis, and a rotation angle
      about that axis.  This method calculates this alternate representation of the to-local
      rotation.  The from-local transformation is found by simply reversing the sign of the
      rotation angle.  It returns the rotation angle, and overwrites axis with a unit vector
      along the rotation axis. */
  
  public final double getAxisAngleRotation(Vec3 axis)
  {
    double a[][] = new double [4][], b[] = new double [4], ctheta2, cphi, sphi, phi;
    Vec3 v, xdir;
    Mat4 m = toLocal();

    if (zdir.z == 1.0)
      {
        // If the z-axis is unchanged, it must be the rotation axis.
        
        axis.set(0.0, 0.0, 1.0);
        if (updir.y == 1.0)
          return 0.0;
      }
    else if (updir.y == 1.0)
      axis.set(0.0, 1.0, 0.0);  // Same for the y-axis;
    else
      {
        // The rotation axis is the cross product of (newy-oldy) and (newz-oldz).
        
        axis.set((updir.y-1.0)*(zdir.z-1.0) - updir.z*zdir.y, 
                updir.z*zdir.x - updir.x*(zdir.z-1.0), 
                updir.x*zdir.y - (updir.y-1.0)*zdir.x);
        if (axis.length2() < 1e-6)
          {
            xdir = updir.cross(zdir);
            axis.set(xdir.y*(zdir.z-1.0) - xdir.z*zdir.y, 
                xdir.z*zdir.x - (xdir.x-1.0)*(zdir.z-1.0), 
                (xdir.x-1.0)*zdir.y - xdir.y*zdir.x);
          }
        axis.normalize();
      }

    // Now calculate the rotation angle about the axis.

    if (Math.abs(axis.z) < Math.abs(axis.y))
      {
        v = axis.cross(zdir);
        ctheta2 = axis.z*axis.z;
        sphi = v.z/(ctheta2-1.0);
        cphi = (zdir.z-ctheta2)/(1.0-ctheta2);
      }
    else
      {
        v = axis.cross(updir);
        ctheta2 = axis.y*axis.y;
        sphi = v.y/(ctheta2-1.0);
        cphi = (updir.y-ctheta2)/(1.0-ctheta2);
      }
    if (cphi > 0.0)
      return Math.asin(sphi);
    else if (sphi > 0.0)
      return Math.PI-Math.asin(sphi);
    else
      return -(Math.PI+Math.asin(sphi));
  }

  /** Create a CoordinateSystem by reading the information that was written by writeToFile(). */
  
  public CoordinateSystem(DataInputStream in) throws IOException
  {
    orig = new Vec3(in);
    xrot = in.readDouble();
    yrot = in.readDouble();
    zrot = in.readDouble();
    Mat4 m = Mat4.yrotation(-yrot).times(Mat4.xrotation(-xrot)).times(Mat4.zrotation(-zrot));
    zdir = m.times(Vec3.vz());
    updir = m.times(Vec3.vy());
  }

  /** Write out a serialized representation of this CoordinateSystem. */
  
  public void writeToFile(DataOutputStream out) throws IOException
  {
    orig.writeToFile(out);
    out.writeDouble(xrot);
    out.writeDouble(yrot);
    out.writeDouble(zrot);
  }
}