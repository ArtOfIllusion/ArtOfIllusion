/* Copyright (C) 2001-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.*;
import artofillusion.texture.*;

/** This interface represents an object which can be used to represent a predefined gesture for
    an Actor. */

public interface Gesture extends Keyframe
{
  /** Return a new gesture which is the weighted average of an arbitrary list of gestures.
      The gestures are averaged around this gesture.  That is, the returned gesture
      is represented symbolically as
      <p>
      result = this + sum(weight[i]*(p[i]-this))
      
      */
  
  public Gesture blend(Gesture p[], double weight[]);

  /** Get the skeleton for this gesture (or null if it doesn't have one). */
  
  public Skeleton getSkeleton();
  
  /** Set the skeleton for this gesture. */
  
  public void setSkeleton(Skeleton s);
  
  /** Update the texture parameter values when the texture is changed. */

  public void textureChanged(TextureParameter oldParams[], TextureParameter newParams[]);
  
  /** Get the value of a per-vertex texture parameter. */
  
  public ParameterValue getTextureParameter(TextureParameter param);
  
  /** Set the value of a per-vertex texture parameter. */
  
  public void setTextureParameter(TextureParameter param, ParameterValue value);
}