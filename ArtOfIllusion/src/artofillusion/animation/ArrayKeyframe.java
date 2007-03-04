/* This class is an array valued keyframe. */

/* Copyright (C) 2001-2002 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import java.io.*;

public class ArrayKeyframe implements Keyframe
{
  public double val[];
  
  public ArrayKeyframe(double values[])
  {
    val = values;
  }

  public Keyframe duplicate()
  {
    return new ArrayKeyframe(val);
  }

  public Keyframe duplicate(Object owner)
  {
    return new ArrayKeyframe(val);
  }
  
  /* Get the list of graphable values for this keyframe. */
  
  public double [] getGraphValues()
  {
    return val;
  }
  
  /* Set the list of graphable values for this keyframe. */
  
  public void setGraphValues(double values[])
  {
    val = new double [values.length];
    System.arraycopy(values, 0, val, 0, values.length);
  }

  public Keyframe blend(Keyframe o2, double weight1, double weight2)
  {
    double d[] = new double [val.length];
    ArrayKeyframe key2 = (ArrayKeyframe) o2;

    for (int i = 0; i < val.length; i++)
      d[i] = (weight1*val[i] + weight2*key2.val[i]);
    return new ArrayKeyframe(d);
  }

  public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
  {
    double d[] = new double [val.length];
    ArrayKeyframe key2 = (ArrayKeyframe) o2;
    ArrayKeyframe key3 = (ArrayKeyframe) o3;

    for (int i = 0; i < val.length; i++)
      d[i] = (weight1*val[i] + weight2*key2.val[i] + weight3*key3.val[i]);
    return new ArrayKeyframe(d);
  }

  public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
  {
    double d[] = new double [val.length];
    ArrayKeyframe key2 = (ArrayKeyframe) o2;
    ArrayKeyframe key3 = (ArrayKeyframe) o3;
    ArrayKeyframe key4 = (ArrayKeyframe) o4;

    for (int i = 0; i < val.length; i++)
      d[i] = (weight1*val[i] + weight2*key2.val[i] + weight3*key3.val[i] + weight4*key4.val[i]);
    return new ArrayKeyframe(d);
  }

  /* Determine whether this keyframe is identical to another one. */
  
  public boolean equals(Keyframe k)
  {
    if (!(k instanceof ArrayKeyframe))
      return false;
    ArrayKeyframe key = (ArrayKeyframe) k;
    for (int i = 0; i < val.length; i++)
      if (val[i] != key.val[i])
        return false;
    return true;
  }
  
  /* Write out a representation of this keyframe to a stream. */
  
  public void writeToStream(DataOutputStream out) throws IOException
  {
    out.writeShort(val.length);
    for (int i = 0; i < val.length; i++)
      out.writeDouble(val[i]);
  }

  /* Reconstructs the keyframe from its serialized representation. */

   public ArrayKeyframe(DataInputStream in, Object parent) throws IOException
   {
     val = new double [in.readShort()];
     for (int i = 0; i < val.length; i++)
       val[i] = in.readDouble();
   }
}