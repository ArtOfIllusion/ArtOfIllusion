/* Copyright (C) 1999-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.material.*;
import artofillusion.math.*;
import artofillusion.texture.*;

/** RTObject represents an object which can be raytraced. */

public abstract class RTObject
{
  protected int index;

  /** Get the TextureMapping for this object. */
  
  public abstract TextureMapping getTextureMapping();

  /** Get the MaterialMapping for this object. */
  
  public abstract MaterialMapping getMaterialMapping();
  
  /** Determine whether a ray intersects this object.  In most cases, this method should not
      be called directly.  Instead, call {@link Ray#findIntersection(RTObject)} on a Ray,
      which provides caching to avoid repeated intersection calculations. */

  public abstract SurfaceIntersection checkIntersection(Ray r);

  /** Get a bounding box for this object. */
  
  public abstract BoundingBox getBounds();

  /** Determine whether any part of the object lies within a bounding box. */

  public abstract boolean intersectsBox(BoundingBox bb);
  
  /** Get the transformation from world coordinates to the object's local coordinates. */
  
  public abstract Mat4 toLocal();
  
  /** Get the object represented by this RTObject.  The default implementation simply
      returns this, but subclasses may override it to return something more appropriate.
      For example, if the RTObject is a triangle in a mesh, it may return the mesh. */
  
  public Object getObject()
  {
    return this;
  }

  /** Get the index of this object in the Raytracer's list of RTObjects. */

  public int getIndex()
  {
    return index;
  }
}