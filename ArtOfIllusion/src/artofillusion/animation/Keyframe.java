/* Copyright (C) 2001-2002 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import java.io.*;

/** This interface represents any object which can be used to represent a keyframe on an
    animation track.
    <p>
    Every Keyframe class should also provide a constructor of the following form, which
    reconstructs the keyframe from its serialized representation.

    public KeyframeClass(DataInputStream in, Object parent) throws IOException, InvalidObjectException
*/

public interface Keyframe
{
  /** Create a duplicate of this keyframe. */
  
  public Keyframe duplicate();

  /** Create a duplicate of this keyframe for a (possibly different) object. */
  
  public Keyframe duplicate(Object owner);
  
  /** Get the list of graphable values for this keyframe. */
  
  public double [] getGraphValues();
  
  /** Set the list of graphable values for this keyframe. */
  
  public void setGraphValues(double values[]);

  /** Return a new Keyframe which is a weighted average of this one and one other. */
  
  public Keyframe blend(Keyframe o2, double weight1, double weight2);

  /** Return a new Keyframe which is a weighted average of this one and two others. */

  public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3);

  /** Return a new Keyframe which is a weighted average of this one and three others. */

  public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4);

  /** Determine whether this keyframe is identical to another one. */
  
  public boolean equals(Keyframe k);
  
  /** Write out a representation of this keyframe to a stream. */
  
  public void writeToStream(DataOutputStream out) throws IOException;
}