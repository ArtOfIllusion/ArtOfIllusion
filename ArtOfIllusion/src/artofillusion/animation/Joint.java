/* Copyright (C) 2002,2003 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.math.*;
import java.io.*;

/** This class represents a joint in the skeleton of an animated object. */

public class Joint
{
  public CoordinateSystem coords;
  public String name;
  public DOF angle1, angle2, twist, length;
  public Joint parent, children[];
  public int id;
  
  public Joint(CoordinateSystem coords, Joint parentJoint, String name)
  {
    parent = parentJoint;
    children = new Joint [0];
    this.name = name;
    this.coords = coords;
    angle1 = new DOF(-180.0, 180.0, 0.0);
    angle2 = new DOF(-180.0, 180.0, 0.0);
    twist = new DOF(-180.0, 180.0, 0.0);
    twist.fixed = true;
    if (parent == null)
      length = new DOF(0.0, 0.0, 0.0);
    else
    {
      double d = coords.getOrigin().distance(parent.coords.getOrigin());
      length = new DOF(0.0, Double.MAX_VALUE, d);
      calcAnglesFromCoords(false);
    }
    length.fixed = true;
    angle1.loop = angle2.loop = twist.loop = true;
    id = -1;
  }
  
  private Joint()
  {
  }
  
  /** Create an exact duplicate of this joint (except for the parent and children). */
  
  public Joint duplicate()
  {
    Joint j = new Joint();
    
    j.coords = coords.duplicate();
    j.name = name;
    j.angle1 = angle1.duplicate();
    j.angle2 = angle2.duplicate();
    j.twist = twist.duplicate();
    j.length = length.duplicate();
    j.id = id;
    return j;
  }
  
  /** Make this joint identical to another one (except for the parent and children). */
  
  public void copy(Joint j)
  {
    coords.copyCoords(j.coords);
    name = j.name;
    angle1.copy(j.angle1);
    angle2.copy(j.angle2);
    twist.copy(j.twist);
    length.copy(j.length);
    id = j.id;
  }
  
  /** Determine if this joint is identical to another one. */
  
  public boolean equals(Joint j)
  {
    if (!angle1.equals(j.angle1) || !angle2.equals(j.angle2) || !twist.equals(j.twist) || !length.equals(j.length))
      return false;
    if (!name.equals(j.name))
      return false;
    return true;
  }
  
  /** Recalculate the coordinate system for this joint after its parent joint has moved. */
  
  public void recalcCoords(boolean recursive)
  {
    if (parent == null)
      coords.setOrientation(angle1.pos, angle2.pos, twist.pos);
    else
      {
        Mat4 m = getTransform();
        Vec3 parentPos = parent.coords.getOrigin();
        Vec3 zdir = parent.coords.fromLocal().timesDirection(m.timesDirection(Vec3.vz()));
        Vec3 updir = parent.coords.fromLocal().timesDirection(m.timesDirection(Vec3.vy()));
        coords = new CoordinateSystem(parentPos.plus(zdir.times(length.pos)), zdir, updir);
      }
    if (recursive)
      for (int i = 0; i < children.length; i++)
        children[i].recalcCoords(true);
  }
  
  /** Get the matrix which transforms direction vectors from the parent joint's coordinate
      system to this one. */
  
  public Mat4 getTransform()
  {
    double d = Math.PI/180.0;

    return Mat4.yrotation(angle2.pos*d).times(Mat4.xrotation(angle1.pos*d)).times(Mat4.zrotation(twist.pos*d));
  }
  
  /** Get the matrix which transforms direction vectors from this joint's coordinate
      system to its parent's. */
  
  public Mat4 getInverseTransform()
  {
    double d = Math.PI/180.0;

    return Mat4.zrotation(-twist.pos*d).times(Mat4.xrotation(-angle1.pos*d)).times(Mat4.yrotation(-angle2.pos*d));
  }
  
  /** Recalculate the angles based on the coordinate system. */
  
  public void calcAnglesFromCoords(boolean recursive)
  {
    if (parent == null)
    {
      CoordinateSystem c = coords;
      double ang[] = c.getRotationAngles();
      angle1.pos = ang[0];
      angle2.pos = ang[1];
      twist.pos = ang[2];
    }
    else
    {
      CoordinateSystem c = coords.duplicate();
      c.transformAxes(parent.coords.toLocal());
      double ang[] = c.getRotationAngles();
      angle1.pos = -ang[0];
      angle2.pos = -ang[1];
      twist.pos = -ang[2];
    }
    if (recursive)
      for (int i = 0; i < children.length; i++)
        children[i].calcAnglesFromCoords(true);
  }

  /** This is an inner class for storing information about the properties of a particular
      degree of freedom. */
  
  public class DOF
  {
    public double min, max, minComfort, maxComfort, stiffness, pos;
    public boolean fixed, comfort, loop;
    
    public DOF(double min, double max, double pos)
    {
      this.min = minComfort = min;
      this.max = maxComfort = max;
      this.pos = pos;
    }
    
    public DOF duplicate()
    {
      DOF d = new DOF(min, max, pos);

      d.minComfort = minComfort;
      d.maxComfort = maxComfort;
      d.stiffness = stiffness;
      d.fixed = fixed;
      d.comfort = comfort;
      d.loop = loop;
      return d;
    }
    
    public void copy(DOF d)
    {
      pos = d.pos;
      min = d.min;
      max = d.max;
      minComfort = d.minComfort;
      maxComfort = d.maxComfort;
      stiffness = d.stiffness;
      fixed = d.fixed;
      comfort = d.comfort;
      loop = d.loop;
    }
    
    /** Determine if this DOF is identical to another one. */
    
    public boolean equals(DOF d)
    {
      if (fixed != d.fixed || comfort != d.comfort || loop != d.loop)
        return false;
      if (pos != d.pos || min != d.min || max != d.max || minComfort != d.minComfort || maxComfort != d.maxComfort || stiffness != d.stiffness)
        return false;
      return true;
    }
    
    public void set(double val)
    {
      pos = val;
      while (pos > max)
        {
          if (loop)
            pos -= (max-min);
          else
            pos = max;
        }
      while (pos < min)
        {
          if (loop)
            pos += (max-min);
          else
            pos = min;
        }
    }
    
    /** Given a force applied to the degree of freedom, return a new force which is scaled
        based on the stiffness, the direction of the force, and whether it is inside its
        comfort range. */
    
    public double getScaledForce(double f)
    {
      f *= 1.0-stiffness;
      if (!comfort)
        return f;
      if (pos < minComfort && f < 0.0 && minComfort > min)
        f *= (pos-min)/(minComfort-min);
      else if (pos > maxComfort && f > 0.0 && maxComfort < max)
        f *= (max-pos)/(max-maxComfort);
      return f;
    }
    
    /** Given a force applied to the degree of freedom, return a scale factor
        based on the stiffness, the direction of the force, and whether it is inside its
        comfort range. */
    
    public double getForceScale(double f)
    {
      double scale = 1.0-stiffness;
      if (!loop)
        if ((pos == min && f < 0.0) || (pos == max && f > 0.0))
          return 0.0;
      if (!comfort)
        return scale;
      if (pos < minComfort && f < 0.0 && minComfort > min)
        scale *= (pos-min)/(minComfort-min);
      else if (pos > maxComfort && f > 0.0 && maxComfort < max)
        scale *= (max-pos)/(max-maxComfort);
      return scale;
    }

    /** Given a force applied to the degree of freedom, return a new force which is clipped
        so as not to move it beyond the allowed range. */
    
    public double getClippedForce(double f)
    {
      if (loop)
        return f;
      if (pos+f < min)
        f = min-pos;
      else if (pos+f > max)
        f = max-pos;
      return f;
    }

    /** Write a serialized representation of this DOF to a stream. */

    public void writeToStream(DataOutputStream out) throws IOException
    {
      out.writeDouble(pos);
      out.writeDouble(min);
      out.writeDouble(max);
      out.writeDouble(minComfort);
      out.writeDouble(maxComfort);
      out.writeDouble(stiffness);
      out.writeBoolean(fixed);
      out.writeBoolean(comfort);
    }
  
    /** Reconstruct a DOF from its serialized representation. */
  
    public DOF(DataInputStream in) throws IOException
    {
      pos = in.readDouble();
      min = in.readDouble();
      max = in.readDouble();
      minComfort = in.readDouble();
      maxComfort = in.readDouble();
      stiffness = in.readDouble();
      fixed = in.readBoolean();
      comfort = in.readBoolean();
      loop = (max-min == 360.0);
    }
  }
}