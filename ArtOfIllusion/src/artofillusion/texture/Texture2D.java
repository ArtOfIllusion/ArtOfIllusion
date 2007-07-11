/* Copyright (C) 2000-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.math.*;
import artofillusion.util.*;
import artofillusion.object.*;

import java.awt.*;
import java.awt.image.*;

/** Texture2D represents a Texture whose surface properties are defined in 2D.  This 2D
    surface can be mapped onto a 3D object by a variety of different mappings. */

public abstract class Texture2D extends Texture
{
  /** Get the surface properties at point in the texture.  The properties should be averaged over a region 
      around the point.
      @param spec     the surface properties will be stored in this
      @param x        the x coordinate at which to evaluate the texture
      @param y        the y coordinate at which to evaluate the texture
      @param xsize    the range of x over which to average the surface properties
      @param ysize    the range of z over which to average the surface properties
      @param angle    the dot product of the view direction with the surface normal
      @param t        the time at which to evaluate the surface properties
      @param param    the texture parameter values at the point
  */

  public abstract void getTextureSpec(TextureSpec spec, double x, double y, double xsize, double ysize, double angle, double t, double param[]);

  /** Same as above, except only return the transparent color.  This can save time in cases
      where only the transparency is required, for example, when tracing shadow rays. */

  public abstract void getTransparency(RGBColor trans, double x, double y, double xsize, double ysize, double angle, double t, double param[]);

  /** For the default mapping, use a basic projection. */
  
  public TextureMapping getDefaultMapping(Object3D object)
  {
    return new ProjectionMapping(object, this);
  }
  
  /** Textures which use displacement mapping should override this method to return the
      displacement at the given point. */

  public double getDisplacement(double x, double y, double xsize, double ysize, double t, double param[])
  {
    return Double.NaN;
  }

  /** Determine whether the texture is displacement mapped based on the value returned by
      getDisplacement(). */

  public boolean displacementMapped()
  {
    return !Double.isNaN(getDisplacement(0.0, 0.0, 0.0, 0.0, 0.0, null));
  }
  
  /** Create an Image which represents a particular component of this texture.
      The arguments specify the region of the texture to represent (U and V ranges),
      the image size, the component to represent (one of the constants defined
      in the Texture class), and the time and texture parameters. */
  
  public Image createComponentImage(final double minu, double maxu, double minv, final double maxv,
      final int width, final int height, final int component, final double time, final double param[])
  {
    final int pixel[] = new int [width*height];
    final double uscale = (maxu-minu)/width;
    final double vscale = (maxv-minv)/height;
    final ThreadLocal textureSpec = new ThreadLocal() {
      protected Object initialValue()
      {
        return new TextureSpec();
      }
    };
    ThreadManager threads = new ThreadManager(width, new ThreadManager.Task()
    {
      public void execute(int i)
      {
        TextureSpec spec = (TextureSpec) textureSpec.get();
        for (int j = 0; j < height; j++)
        {
          double u = minu+i*uscale;
          double v = maxv-j*vscale;
          getTextureSpec(spec, u, v, uscale, vscale, 1.0, time, param);
          int index = i+j*width;
          switch (component)
          {
            case DIFFUSE_COLOR_COMPONENT:
              pixel[index] = spec.diffuse.getARGB();
              break;
            case SPECULAR_COLOR_COMPONENT:
              pixel[index] = spec.specular.getARGB();
              break;
            case TRANSPARENT_COLOR_COMPONENT:
              pixel[index] = spec.transparent.getARGB();
              break;
            case HILIGHT_COLOR_COMPONENT:
              pixel[index] = spec.hilight.getARGB();
              break;
            case EMISSIVE_COLOR_COMPONENT:
              pixel[index] = spec.emissive.getARGB();
              break;
          }
        }
      }
      public void cleanup()
      {
      }
    });
    threads.run();
    threads.finish();
    MemoryImageSource src = new MemoryImageSource(width, height, pixel, 0, width);
    return Toolkit.getDefaultToolkit().createImage(src);
  }
}