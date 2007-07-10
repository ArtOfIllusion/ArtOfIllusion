/* Copyright (C) 2001-2002 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.math.*;
import java.io.*;

/** This class is a vector valued keyframe. */

public class VectorKeyframe extends Vec3 implements Keyframe
{
  public VectorKeyframe()
  {
    super();
  }

  public VectorKeyframe(double xval, double yval, double zval)
  {
    super(xval, yval, zval);
  }

  public VectorKeyframe(Vec3 v)
  {
    super(v);
  }

  public Keyframe duplicate(Object owner)
  {
    return new VectorKeyframe(this);
  }
  
  public Keyframe duplicate()
  {
    return new VectorKeyframe(this);
  }
  
  /* Get the list of graphable values for this keyframe. */
  
  public double [] getGraphValues()
  {
    return new double [] {x, y, z};
  }
  
  /* Set the list of graphable values for this keyframe. */
  
  public void setGraphValues(double values[])
  {
    if (values.length == 3)
      set(values[0], values[1], values[2]);
  }

  public Keyframe blend(Keyframe o2, double weight1, double weight2)
  {
    VectorKeyframe v2 = (VectorKeyframe) o2;
    
    return new VectorKeyframe(weight1*x+weight2*v2.x, weight1*y+weight2*v2.y, weight1*z+weight2*v2.z);
  }

  public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
  {
    VectorKeyframe v2 = (VectorKeyframe) o2, v3 = (VectorKeyframe) o3;
    
    return new VectorKeyframe(weight1*x+weight2*v2.x+weight3*v3.x, 
	weight1*y+weight2*v2.y+weight3*v3.y,
	weight1*z+weight2*v2.z+weight3*v3.z);
  }

  public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
  {
    VectorKeyframe v2 = (VectorKeyframe) o2, v3 = (VectorKeyframe) o3, v4 = (VectorKeyframe) o4;
    
    return new VectorKeyframe(weight1*x+weight2*v2.x+weight3*v3.x+weight4*v4.x, 
	weight1*y+weight2*v2.y+weight3*v3.y+weight4*v4.y, 
	weight1*z+weight2*v2.z+weight3*v3.z+weight4*v4.z);
  }

  /* Determine whether this keyframe is identical to another one. */
  
  public boolean equals(Keyframe k)
  {
    if (!(k instanceof VectorKeyframe))
      return false;
    VectorKeyframe key = (VectorKeyframe) k;
    return equals((Vec3) k);
  }
  
  /* Write out a representation of this keyframe to a stream. */
  
  public void writeToStream(DataOutputStream out) throws IOException
  {
    super.writeToFile(out);
  }

  /* Reconstructs the keyframe from its serialized representation. */

   public VectorKeyframe(DataInputStream in, Object parent) throws IOException
   {
     super(in);
   }
}