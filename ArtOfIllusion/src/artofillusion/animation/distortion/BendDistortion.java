/* Copyright (C) 2002-2012 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation.distortion;

import artofillusion.math.*;
import artofillusion.object.*;

/** This is a distortion which bends an object. */

public class BendDistortion extends Distortion
{
  private int axis, direction;
  private double angle;
  private Mat4 preTransform, postTransform;
  private boolean forward;

  public static final int POS_X = 0;
  public static final int NEG_X = 1;
  public static final int POS_Y = 2;
  public static final int NEG_Y = 3;
  public static final int POS_Z = 4;
  public static final int NEG_Z = 5;

  public static final int X_AXIS = 0;
  public static final int Y_AXIS = 1;
  public static final int Z_AXIS = 2;
  
  public BendDistortion(int axis, int direction, double angle, boolean forward, Mat4 preTransform, Mat4 postTransform)
  {
    this.axis = axis;
    this.direction = direction;
    this.angle = angle;
    this.forward = forward;
    this.preTransform = preTransform;
    this.postTransform = postTransform;
  }

  /** Determine whether this distortion is identical to another one. */
  
  public boolean isIdenticalTo(Distortion d)
  {
    if (!(d instanceof BendDistortion))
      return false;
    BendDistortion s = (BendDistortion) d;
    if (previous != null && !previous.isIdenticalTo(s.previous))
      return false;
    if (previous == null && s.previous != null)
      return false;
    if (axis != s.axis || direction != s.direction || angle != s.angle || forward != s.forward)
      return false;
    if (preTransform == s.preTransform && postTransform == s.postTransform)
      return true;
    return (preTransform != null && preTransform.equals(s.preTransform) &&
      postTransform != null && postTransform.equals(s.postTransform));
  }
  
  /** Create a duplicate of this object. */
  
  public Distortion duplicate()
  {
    BendDistortion d = new BendDistortion(axis, direction, angle, forward, preTransform, postTransform);
    if (previous != null)
      d.previous = previous.duplicate();
    return d;
  }
  
  /** Apply the Distortion, and return a transformed mesh. */

  public Mesh transform(Mesh obj)
  {
    if (previous != null)
      obj = previous.transform(obj);
    Mesh newmesh = (Mesh) obj.duplicate();
    MeshVertex[] vert = newmesh.getVertices();
    Vec3 newvert[] = new Vec3 [vert.length];
    Vec3 origin = new Vec3();
    
    for (int i = 0; i < newvert.length; i++)
      {
        newvert[i] = vert[i].r;
        if (preTransform != null)
          preTransform.transform(newvert[i]);
      }
    if (preTransform != null)
      preTransform.transform(origin);
    
    // Find the range along the appropriate axis.

    double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
    for (int i = 0; i < newvert.length; i++)
      {
        double value;
        if (axis == X_AXIS)
          value = newvert[i].x;
        else if (axis == Y_AXIS)
          value = newvert[i].y;
        else
          value = newvert[i].z;
        if (value < min)
          min = value;
        if (value > max)
          max = value;
      }
    if (min >= max)
      return obj;
    if (!forward)
      {
        double temp = min;
        min = max;
        max = temp;
      }
    double theta = angle*(Math.PI/180.0);
    double scale = theta/(max-min);
    double radius = (max-min)/theta;
    if (Math.abs(theta) > 1e-10)
    {
      if (axis == X_AXIS)
        {
          if (direction == Y_AXIS)
            for (int i = 0; i < newvert.length; i++)
              {
                double a = scale*(newvert[i].x-min);
                double b = newvert[i].y-origin.y-radius;
                newvert[i].set(min-Math.sin(a)*b, origin.y+radius+Math.cos(a)*b, newvert[i].z);
              }
          else
            for (int i = 0; i < newvert.length; i++)
              {
                double a = scale*(newvert[i].x-min);
                double b = newvert[i].z-origin.z-radius;
                newvert[i].set(min-Math.sin(a)*b, newvert[i].y, origin.z+radius+Math.cos(a)*b);
              }
        }
      else if (axis == Y_AXIS)
        {
          if (direction == X_AXIS)
            for (int i = 0; i < newvert.length; i++)
              {
                double a = scale*(newvert[i].y-min);
                double b = newvert[i].x-origin.x-radius;
                newvert[i].set(origin.x+radius+Math.cos(a)*b, min-Math.sin(a)*b, newvert[i].z);
              }
          else
            for (int i = 0; i < newvert.length; i++)
              {
                double a = scale*(newvert[i].y-min);
                double b = newvert[i].z-origin.z-radius;
                newvert[i].set(newvert[i].x, min-Math.sin(a)*b, origin.z+radius+Math.cos(a)*b);
              }
        }
      else
        {
          if (direction == X_AXIS)
            for (int i = 0; i < newvert.length; i++)
              {
                double a = scale*(newvert[i].z-min);
                double b = newvert[i].x-origin.x-radius;
                newvert[i].set(origin.x+radius+Math.cos(a)*b, newvert[i].y, min-Math.sin(a)*b);
              }
          else
            for (int i = 0; i < newvert.length; i++)
              {
                double a = scale*(newvert[i].z-min);
                double b = newvert[i].y-origin.y-radius;
                newvert[i].set(newvert[i].x, origin.y+radius+Math.cos(a)*b, min-Math.sin(a)*b);
              }
        }
    }
    if (postTransform != null)
      for (int i = 0; i < newvert.length; i++)
        postTransform.transform(newvert[i]);
    newmesh.setVertexPositions(newvert);
    return newmesh;
  }
}