/* Copyright (C) 2002-2012 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation.distortion;

import artofillusion.object.*;

/** This interface defines an object that transforms one mesh into another one. */

public abstract class Distortion
{
  protected Distortion previous;

  /** Set another distortion which should be applied before this one.
      This allows Distortions to be chained. */
  
  public void setPreviousDistortion(Distortion previous)
  {
    this.previous = previous;
  }

  /** Get the previous distortion that should be applied before this one.  This may be null. */

  public Distortion getPreviousDistortion()
  {
    return previous;
  }

  /** Determine whether this distortion is identical to another one. */
  
  public abstract boolean isIdenticalTo(Distortion d);
  
  /** Create a duplicate of this object. */
  
  public abstract Distortion duplicate();
  
  /** Apply the Distortion, and return a transformed mesh. */
  
  public abstract Mesh transform(Mesh obj);
}