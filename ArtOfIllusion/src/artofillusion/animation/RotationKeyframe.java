/* Copyright (C) 2001-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.math.*;
import java.io.*;

/** This class is a keyframe for a rotation track.  It can represent rotations both in
    terms of rotation angles and in terms of quaternions. */

public class RotationKeyframe implements Keyframe
{
  public double x, y, z;
  private double q[];
  private boolean useQuaternion;

  private static final double SCALE = Math.PI/180.0;

  public RotationKeyframe(double xrot, double yrot, double zrot)
  {
    set(xrot, yrot, zrot);
  }

  public RotationKeyframe(CoordinateSystem coords)
  {
    double angle[] = coords.getRotationAngles();
    
    set(angle[0], angle[1], angle[2]);
  }

  /** Set the rotation angles. */
  
  public void set(double xrot, double yrot, double zrot)
  {
    x = xrot;
    y = yrot;
    z = zrot;
    q = null;
  }
  
  /** Get the list of graphable values for this keyframe. */
  
  public double [] getGraphValues()
  {
    return new double [] {x, y, z};
  }
  
  /** Set the list of graphable values for this keyframe. */
  
  public void setGraphValues(double values[])
  {
    if (values.length == 3)
      set(values[0], values[1], values[2]);
  }

  /** Get which method will be used for interpolating. */
  
  public boolean getUseQuaternion()
  {
    return useQuaternion;
  }

  /** Set which method will be used for interpolating. */
  
  public void setUseQuaternion(boolean use)
  {
    useQuaternion = use;
    q = null;
  }
  
  /** Get the quaternion representation of this keyframe. */
  
  public double [] getQuaternion()
  {
    makeQuaternion();
    return new double [] {q[0], q[1], q[2], q[3]};
  }
  
  /** Apply this rotation to a coordinate system. */

  public void applyToCoordinates(CoordinateSystem coords, double weight, Mat4 preTransform, 
        Mat4 postTransform, boolean relative, boolean enablex, boolean enabley, boolean enablez)
  {
    if (preTransform != null)
      coords.transformAxes(preTransform);
    double v[] = coords.getRotationAngles();
    if (!relative)
      {
        double w = 1.0-weight;
        if (enablex || useQuaternion)
          v[0] *= w;
        if (enabley || useQuaternion)
          v[1] *= w;
        if (enablez || useQuaternion)
          v[2] *= w;
        if (!useQuaternion && preTransform == null && postTransform == null)
          {
            if (enablex)
              v[0] += x*weight;
            if (enabley)
              v[1] += y*weight;
            if (enablez)
              v[2] += z*weight;
            coords.setOrientation(v[0], v[1], v[2]);
            return;
          }
        coords.setOrientation(v[0], v[1], v[2]);
      }
    Mat4 trans;
    if (useQuaternion)
      {
        makeQuaternion();
        double angle = 2.0*Math.acos(q[3]);
        Vec3 axis = new Vec3(q[0], q[1], q[2]);
        if (axis.length2() > 1e-10)
          {
            axis.scale(1.0/Math.sin(0.5*angle));
            trans = Mat4.axisRotation(axis, -weight*angle);
          }
        else
          {
            if (preTransform == null && postTransform == null)
              return;
            trans = Mat4.identity();
          }
      }
    else
      {
        double xt = enablex ? x : 0.0;
        double yt = enabley ? y : 0.0;
        double zt = enablez ? z : 0.0;
        trans = Mat4.yrotation(-yt*weight*SCALE).times(Mat4.xrotation(-xt*weight*SCALE)).times(Mat4.zrotation(-zt*weight*SCALE));
      }
    coords.transformAxes(trans);
    if (postTransform != null)
      coords.transformAxes(postTransform);
  }

  /** Build the quaternion representation of the rotation. */

  private void makeQuaternion()
  {
    if (q != null)
      return;
    double qx[] = new double [] {Math.sin(0.5*SCALE*x), 0.0, 0.0, Math.cos(0.5*SCALE*x)};
    double qy[] = new double [] {0.0, Math.sin(0.5*SCALE*y), 0.0, Math.cos(0.5*SCALE*y)};
    double qz[] = new double [] {0.0, 0.0, Math.sin(0.5*SCALE*z), Math.cos(0.5*SCALE*z)};
    q = multiply(qz, multiply(qx, qy));
  }

  /** Multiply two quaternions together. */
  
  private double [] multiply(double q1[], double q2[])
  {
    double result[] = new double [4];

    result[0] = q1[3]*q2[0] + q1[0]*q2[3] + q1[1]*q2[2] - q1[2]*q2[1];
    result[1] = q1[3]*q2[1] + q1[1]*q2[3] + q1[2]*q2[0] - q1[0]*q2[2];
    result[2] = q1[3]*q2[2] + q1[2]*q2[3] + q1[0]*q2[1] - q1[1]*q2[0];
    result[3] = q1[3]*q2[3] - q1[0]*q2[0] - q1[1]*q2[1] - q1[2]*q2[2];
    return result;
  }

  public Keyframe duplicate()
  {
    RotationKeyframe f = new RotationKeyframe(x, y, z);
    f.setUseQuaternion(useQuaternion);
    return f;
  }

  public Keyframe duplicate(Object owner)
  {
    RotationKeyframe f = new RotationKeyframe(x, y, z);
    f.setUseQuaternion(useQuaternion);
    return f;
  }

  public Keyframe blend(Keyframe o2, double weight1, double weight2)
  {
    RotationKeyframe v2 = (RotationKeyframe) o2;
    RotationKeyframe r = new RotationKeyframe(weight1*x+weight2*v2.x, weight1*y+weight2*v2.y, weight1*z+weight2*v2.z);

    if (!useQuaternion)
      return r;
    makeQuaternion();
    v2.makeQuaternion();
    r.q = slerp(q, v2.q, weight2);
    r.useQuaternion = true;
    return r;
  }

  public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
  {
    RotationKeyframe v2 = (RotationKeyframe) o2, v3 = (RotationKeyframe) o3;
    RotationKeyframe r = new RotationKeyframe(weight1*x+weight2*v2.x+weight3*v3.x, 
        weight1*y+weight2*v2.y+weight3*v3.y, weight1*z+weight2*v2.z+weight3*v3.z);

    if (!useQuaternion)
      return r;
    makeQuaternion();
    v2.makeQuaternion();
    v3.makeQuaternion();
    if (weight2 == 1.0)
      r.q = new double [] {v2.q[0], v2.q[1], v2.q[2], v2.q[3]};
    else
      {
        double p1[] = slerp(q, v2.q, weight2);
        double p2[] = slerp(v3.q, v2.q, weight2);
        r.q = slerp(p1, p2, weight3/(1.0-weight2));
      }
    r.useQuaternion = true;
    return r;
  }

  public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
  {
    RotationKeyframe v2 = (RotationKeyframe) o2, v3 = (RotationKeyframe) o3, v4 = (RotationKeyframe) o4;
    RotationKeyframe r = new RotationKeyframe(weight1*x+weight2*v2.x+weight3*v3.x+weight4*v4.x, 
        weight1*y+weight2*v2.y+weight3*v3.y+weight4*v4.y,
        weight1*z+weight2*v2.z+weight3*v3.z+weight4*v4.z);

    if (!useQuaternion)
      return r;
    makeQuaternion();
    v2.makeQuaternion();
    v3.makeQuaternion();
    v4.makeQuaternion();
    double p1[] = slerp(q, v2.q, weight2/(weight1+weight2));
    double p2[] = slerp(v3.q, v4.q, weight4/(weight3+weight4));
    r.q = slerp(p1, p2, weight3+weight4);
    r.useQuaternion = true;
    return r;
  }
  
  /** This performs spherical linear interpolation (slerp) between two quaternions. */
  
  private static final double [] slerp(double q1[], double q2[], double t)
  {
    double dot = q1[0]*q2[0] + q1[1]*q2[1] + q1[2]*q2[2] + q1[3]*q2[3], sign;
    if (dot < 0.0)
      {
        dot = -dot;
        sign = -1.0;
      }
    else
      sign = 1.0;
    double angle = Math.acos(dot);
    if (angle < 1e-10 || Double.isNaN(angle))
      return new double [] {q1[0], q1[1], q1[2], q1[3]};
    double denom = Math.sin(angle);
    double weight1 = Math.sin((1.0-t)*angle)/denom, weight2 = sign*Math.sin(t*angle)/denom;
    
    return new double [] {weight1*q1[0] + weight2*q2[0], weight1*q1[1] + weight2*q2[1], 
        weight1*q1[2] + weight2*q2[2], weight1*q1[3] + weight2*q2[3]};
  }

  /** Determine whether this keyframe is identical to another one. */
  
  public boolean equals(Keyframe k)
  {
    if (!(k instanceof RotationKeyframe))
      return false;
    RotationKeyframe key = (RotationKeyframe) k;
    if (useQuaternion)
      {
        double q1[] = getQuaternion(), q2[] = key.getQuaternion();
        double dot = q1[0]*q2[0] + q1[1]*q2[1] + q1[2]*q2[2] + q1[3]*q2[3];
        return (1.0-dot < 1e-10);
      }
    return (key.x == x && key.y == y && key.z == z);
  }
  
  /** Write out a representation of this keyframe to a stream. */
  
  public void writeToStream(DataOutputStream out) throws IOException
  {
    out.writeDouble(x);
    out.writeDouble(y);
    out.writeDouble(z);
  }

  /** Reconstructs the keyframe from its serialized representation. */

  public RotationKeyframe(DataInputStream in, Object parent) throws IOException
  {
    this(in.readDouble(), in.readDouble(), in.readDouble());
  }
}