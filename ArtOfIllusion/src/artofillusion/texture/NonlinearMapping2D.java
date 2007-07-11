/* Copyright (C) 2000-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.math.*;
import artofillusion.object.*;

/** NonlinearMapping2D is an abstract class describing a nonlinear mapping between 2D texture 
    coordinates and 3D space.  When called on to map a triangle, it first performs any initial
    linear transformations on the triangle vertices to yield 3-dimensional "intermediate 
    texture coordinates."  It then provides a callback function which takes an intermediate
    texture coordinate and performs the final nonlinear mapping to yield a 2D texture 
    coordinate. */

public abstract class NonlinearMapping2D extends Mapping2D
{
  boolean coordsFromParams;
  int numTextureParams;
  
  public NonlinearMapping2D(Object3D theObject, Texture theTexture)
  {
    super(theObject, theTexture);
  }

  /** Get the linear transform which maps from object coordinates to intermediate coordinates. */
  
  public abstract Mat4 getPreTransform();
  
  /** Given intermediate texture coordinates, find the surface properties.  size is the width
      of the area over which the properties should be averaged. */
  
  public abstract void getSpecIntermed(TextureSpec spec, double x, double y, double z, double size, double angle, double t, double param[]);

  /** Same as above, except only return the transparent color.  This can save time in cases
      where only the transparency is required, for example, when tracing shadow rays. */

  public abstract void getTransIntermed(RGBColor trans, double x, double y, double z, double size, double angle, double t, double param[]);

  /** Same as above, except only return the displacement. */

  public abstract double getDisplaceIntermed(double x, double y, double z, double size, double t, double param[]);
  
  /** Determine whether this texture is bound to the surface (texture coordinates are determined by parameters,
      not by position). */
  
  public boolean isBoundToSurface()
  {
    return coordsFromParams;
  }

  /** Set whether this texture is bound to the surface (texture coordinates are determined by parameters,
      not by position). */
  
  public void setBoundToSurface(boolean bound)
  {
    coordsFromParams = bound;
  }
}