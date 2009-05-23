/* Copyright (C) 2004-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.image.filter;

import artofillusion.image.*;
import artofillusion.*;
import artofillusion.ui.*;
import artofillusion.math.*;
import artofillusion.object.*;

import java.io.*;

/**
 * This class implements an anisotropic diffusion filter, which is used to reduce noise in images.
 * The algorithm is partly based on the one described in McCool, M.  "Anisotropic diffusion for
 * Monte Carlo noise reduction."  ACM Transactions on Graphics, vol. 18, no. 2, 1999.
 */

public class NoiseReductionFilter extends ImageFilter
{
  public NoiseReductionFilter()
  {
  }

  /** Get the name of this filter.*/

  @Override
  public String getName()
  {
    return Translate.text("NoiseReduction");
  }

  /** Apply the filter to an image.
      @param image      the image to filter
      @param scene      the Scene which was rendered to create the image
      @param camera     the camera from which the Scene was rendered
      @param cameraPos  the position of the camera in the scene
  */

  @Override
  public void filterImage(final ComplexImage image, Scene scene, SceneCamera camera, CoordinateSystem cameraPos)
  {
    final int width = image.getWidth();
    final int height = image.getHeight();
    float cu[][] = new float [width-1][height];
    float cv[][] = new float [width][height-1];
    int iterations = (Integer) getPropertyValue(0);

    for (int i = 0; i < iterations; i++)
    {
      ConductivityFunction fn = new ConductivityFunction() {
        public float evaluate(int x1, int y1, int x2, int y2)
        {
          float object1 = image.getPixelComponent(x1, y1, ComplexImage.OBJECT);
          if (object1 != image.getPixelComponent(x2, y2, ComplexImage.OBJECT) || object1 == 0.0f)
            return 0.0f;
          float err = image.getPixelComponent(x1, y1, ComplexImage.NOISE)+image.getPixelComponent(x2, y2, ComplexImage.NOISE);
          if (err < 1.0e-6)
            return 0.0f;
          float dred = image.getPixelComponent(x1, y1, ComplexImage.RED)-image.getPixelComponent(x2, y2, ComplexImage.RED);
          float dgreen = image.getPixelComponent(x1, y1, ComplexImage.GREEN)-image.getPixelComponent(x2, y2, ComplexImage.GREEN);
          float dblue = image.getPixelComponent(x1, y1, ComplexImage.BLUE)-image.getPixelComponent(x2, y2, ComplexImage.BLUE);
          float d = (dred*dred+dgreen*dgreen+dblue*dblue)/err;
          return (float) (Math.exp(-d*0.2));
        }
      };
      calcConductivity(width, height, cu, cv, fn);
      filterImageComponent(image, cu, cv, ComplexImage.RED);
      filterImageComponent(image, cu, cv, ComplexImage.GREEN);
      filterImageComponent(image, cu, cv, ComplexImage.BLUE);
      filterImageComponent(image, cu, cv, ComplexImage.NOISE);
    }
  }

  /** Get a list of all the image components required by this filter. */

  @Override
  public int getDesiredComponents()
  {
    return ComplexImage.RED+ComplexImage.GREEN+ComplexImage.BLUE+ComplexImage.NOISE+ComplexImage.OBJECT;
  }

  @Override
  public Property[] getProperties()
  {
    return new Property [] {new Property(getName(), 0, Integer.MAX_VALUE, 5)};
  }

  /** Write a serialized description of this filter to a stream. */

  @Override
  public void writeToStream(DataOutputStream out, Scene theScene) throws IOException
  {
    out.writeShort(0);
    out.writeInt((Integer) getPropertyValue(0));
  }

  /** Reconstruct this filter from its serialized representation. */

  @Override
  public void initFromStream(DataInputStream in, Scene theScene) throws IOException
  {
    short version = in.readShort();
    if (version != 0)
      throw new IOException("Unknown version "+version);
    setPropertyValue(0, in.readInt());
  }

  /**
   * Recalculate the conductivity between every pair of pixels.
   */

  private static void calcConductivity(int width, int height, float cu[][], float cv[][], ConductivityFunction fn)
  {
    for (int i = 0; i < width-1; i++)
      for (int j = 0; j < height; j++)
        cu[i][j] = fn.evaluate(i, j, i+1, j);
    for (int i = 0; i < width; i++)
      for (int j = 0; j < height-1; j++)
        cv[i][j] = fn.evaluate(i, j, i, j+1);
  }
  
  /**
   * Apply the filter to one component of an image.
   */

  private static void filterImageComponent(ComplexImage image, float cu[][], float cv[][], int component)
  {
    int width = image.getWidth();
    int height = image.getHeight();
    float [] filtered = new float [width*height];

    for (int i = 0; i < width-1; i++)
      for (int j = 0; j < height; j++)
      {
        float diff = cu[i][j]*(image.getPixelComponent(i+1, j, component)-image.getPixelComponent(i, j, component));
        filtered[i+j*width] += diff;
        filtered[i+1+j*width] -= diff;
      }
    for (int i = 0; i < width; i++)
      for (int j = 0; j < height-1; j++)
      {
        float diff = cv[i][j]*(image.getPixelComponent(i, j+1, component)-image.getPixelComponent(i, j, component));
        filtered[i+j*width] += diff;
        filtered[i+(j+1)*width] -= diff;
      }
    for (int i = 0; i < width; i++)
      for (int j = 0; j < height; j++)
      {
        int index = i+j*width;
        filtered[i+j*width] = 0.1f*filtered[index]+image.getPixelComponent(i, j, component);
      }
    image.setComponentValues(component, filtered);
  }

  /**
   * This interface defines a function for calculating the conductivity between two pixels.
   */

  private static interface ConductivityFunction
  {
    float evaluate(int x1, int y1, int x2, int y2);
  }
}
