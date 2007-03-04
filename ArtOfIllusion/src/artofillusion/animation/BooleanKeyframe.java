/* This class is a boolean valued keyframe. */

/* Copyright (C) 2001-2002 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import java.io.*;

public class BooleanKeyframe implements Keyframe
{
  public boolean val;
  
  public BooleanKeyframe(boolean b)
  {
    val = b;
  }

  public Keyframe duplicate()
  {
    return new BooleanKeyframe(val);
  }

  public Keyframe duplicate(Object owner)
  {
    return new BooleanKeyframe(val);
  }
  
  /* Get the list of graphable values for this keyframe. */
  
  public double [] getGraphValues()
  {
    return new double [] {val ? 1.0 : 0.0};
  }
  
  /* Set the list of graphable values for this keyframe. */
  
  public void setGraphValues(double values[])
  {
    if (values.length == 1)
      val = (values[0] > 0.5);
  }

  public Keyframe blend(Keyframe o2, double weight1, double weight2)
  {
    if (weight1 < 1e-10)
      return new BooleanKeyframe(((BooleanKeyframe) o2).val);
    return new BooleanKeyframe(val);
  }

  public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
  {
    return new BooleanKeyframe(((BooleanKeyframe) o2).val);
  }

  public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
  {
    if (weight2 < 1e-10)
      return new BooleanKeyframe(((BooleanKeyframe) o3).val);
    return new BooleanKeyframe(((BooleanKeyframe) o2).val);
  }

  /* Determine whether this keyframe is identical to another one. */
  
  public boolean equals(Keyframe k)
  {
    if (!(k instanceof BooleanKeyframe))
      return false;
    BooleanKeyframe key = (BooleanKeyframe) k;
    return (key.val == val);
  }
  
  /* Write out a representation of this keyframe to a stream. */
  
  public void writeToStream(DataOutputStream out) throws IOException
  {
    out.writeBoolean(val);
  }

  /* Reconstructs the keyframe from its serialized representation. */

   public BooleanKeyframe(DataInputStream in, Object parent) throws IOException
   {
     val = in.readBoolean();
   }
}