/* Texture3D represents a Texture whose surface properties are defined in 3D. */

/* Copyright (C) 2000 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.math.*;
import artofillusion.object.*;

public abstract class Texture3D extends Texture
{  
  /** Get the surface properties at point in the texture.  The properties should be averaged over a region 
      around the point.
      @param spec     the surface properties will be stored in this
      @param x        the x coordinate at which to evaluate the texture
      @param y        the y coordinate at which to evaluate the texture
      @param z        the z coordinate at which to evaluate the texture
      @param xsize    the range of x over which to average the surface properties
      @param ysize    the range of y over which to average the surface properties
      @param zsize    the range of z over which to average the surface properties
      @param angle    the dot product of the view direction with the surface normal
      @param t        the time at which to evaluate the surface properties
      @param param    the texture parameter values at the point
  */

  public abstract void getTextureSpec(TextureSpec spec, double x, double y, double z, double xsize, double ysize, double zsize, double angle, double t, double param[]);

  /* Same as above, except only return the transparent color.  This can save time in cases
     where only the transparency is required, for example, when tracing shadow rays. */

  public abstract void getTransparency(RGBColor trans, double x, double y, double z, double xsize, double ysize, double zsize, double angle, double t, double param[]);

  /** For the default mapping, use a basic projection. */
  
  public TextureMapping getDefaultMapping(Object3D object)
  {
    return new LinearMapping3D(object, this);
  }
  
  /** Textures which use displacement mapping should override this method to return the
      displacement at the given point. */

  public double getDisplacement(double x, double y, double z, double xsize, double ysize, double zsize, double t, double param[])
  {
    return Double.NaN;
  }

  /** Determine whether the texture is displacement mapped based on the value returned by
      getDisplacement(). */

  public boolean displacementMapped()
  {
    return !Double.isNaN(getDisplacement(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null));
  }
}