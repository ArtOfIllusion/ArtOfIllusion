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

/** This is a distortion which twists an object. */

public class TwistDistortion extends Distortion
{
  private int axis;
  private double angle;
  private Mat4 preTransform, postTransform;
  private boolean forward;

  public static final int X_AXIS = 0;
  public static final int Y_AXIS = 1;
  public static final int Z_AXIS = 2;
  
  public TwistDistortion(int axis, double angle, boolean forward, Mat4 preTransform, Mat4 postTransform)
  {
    this.axis = axis;
    this.angle = angle;
    this.forward = forward;
    this.preTransform = preTransform;
    this.postTransform = postTransform;
  }

  /** Determine whether this distortion is identical to another one. */
  
  public boolean isIdenticalTo(Distortion d)
  {
    if (!(d instanceof TwistDistortion))
      return false;
    TwistDistortion s = (TwistDistortion) d;
    if (previous != null && !previous.isIdenticalTo(s.previous))
      return false;
    if (previous == null && s.previous != null)
      return false;
    if (axis != s.axis || angle != s.angle || forward != s.forward)
      return false;
    if (preTransform == s.preTransform && postTransform == s.postTransform)
      return true;
    return (preTransform != null && preTransform.equals(s.preTransform) &&
      postTransform != null && postTransform.equals(s.postTransform));
  }
  
  /** Create a duplicate of this object. */
  
  public Distortion duplicate()
  {
    TwistDistortion d = new TwistDistortion(axis, angle, forward, preTransform, postTransform);
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
    
    for (int i = 0; i < newvert.length; i++)
      {
        newvert[i] = vert[i].r;
        if (preTransform != null)
          preTransform.transform(newvert[i]);
      }
    
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
    double scale = angle*(Math.PI/180.0);
    if (axis == X_AXIS)
      for (int i = 0; i < newvert.length; i++)
        {
          double c = Math.cos(scale*(newvert[i].x-min));
          double s = Math.sin(scale*(newvert[i].x-min));
          newvert[i].set(newvert[i].x, newvert[i].y*c-newvert[i].z*s, newvert[i].y*s+newvert[i].z*c);
        }
    else if (axis == Y_AXIS)
      for (int i = 0; i < newvert.length; i++)
        {
          double c = Math.cos(scale*(newvert[i].y-min));
          double s = Math.sin(scale*(newvert[i].y-min));
          newvert[i].set(newvert[i].x*c-newvert[i].z*s, newvert[i].y, newvert[i].x*s+newvert[i].z*c);
        }
    else
      for (int i = 0; i < newvert.length; i++)
        {
          double c = Math.cos(scale*(newvert[i].z-min));
          double s = Math.sin(scale*(newvert[i].z-min));
          newvert[i].set(newvert[i].x*c-newvert[i].y*s, newvert[i].x*s+newvert[i].y*c, newvert[i].z);
        }
    if (postTransform != null)
      for (int i = 0; i < newvert.length; i++)
        postTransform.transform(newvert[i]);
    newmesh.setVertexPositions(newvert);
    return newmesh;
  }
}