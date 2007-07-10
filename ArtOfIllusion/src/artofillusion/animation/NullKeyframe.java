/* Copyright (C) 2001-2002 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import java.io.*;

/** This is a keyframes which contains no information.  It is occasionally useful as a
    placeholder. */

public class NullKeyframe implements Keyframe
{
  public NullKeyframe()
  {
  }

  /* Create a duplicate of this keyframe. */
  
  public Keyframe duplicate()
  {
    return new NullKeyframe();
  }

  /* Create a duplicate of this keyframe for a (possibly different) object. */
  
  public Keyframe duplicate(Object owner)
  {
    return new NullKeyframe();
  }
  
  /* Get the list of graphable values for this keyframe. */
  
  public double [] getGraphValues()
  {
    return new double [0];
  }
  
  /* Set the list of graphable values for this keyframe. */
  
  public void setGraphValues(double values[])
  {
  }

  /* These methods return a new Keyframe which is a weighted average of this one and one,
     two, or three others. */
  
  public Keyframe blend(Keyframe o2, double weight1, double weight2)
  {
    return new NullKeyframe();
  }

  public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3)
  {
    return new NullKeyframe();
  }

  public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4)
  {
    return new NullKeyframe();
  }

  /* Determine whether this keyframe is identical to another one. */
  
  public boolean equals(Keyframe k)
  {
    return (k instanceof NullKeyframe);
  }
  
  /* Write out a representation of this keyframe to a stream. */
  
  public void writeToStream(DataOutputStream out) throws IOException
  {
  }

  /* Reconstructs the keyframe from its serialized representation. */

   public NullKeyframe(DataInputStream in, Object parent) throws IOException
   {
   }
}