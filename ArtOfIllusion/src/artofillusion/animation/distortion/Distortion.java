/* Copyright (C) 2002 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation.distortion;

import artofillusion.object.*;

/** This interface defines an object that transforms one mesh into another one. */

public interface Distortion
{
  /** Set another distortion which should be applied before this one.
      This allows Distortions to be chained. */
  
  public void setPreviousDistortion(Distortion previous);
  
  /** Determine whether this distortion is identical to another one. */
  
  public boolean isIdenticalTo(Distortion d);
  
  /** Create a duplicate of this object. */
  
  public Distortion duplicate();
  
  /** Apply the Distortion, and return a transformed mesh. */
  
  public Mesh transform(Mesh obj);
}