/* Copyright (C) 2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

/**
 * This class implements an anisotropic diffusion filter, which is used to reduce noise in images.
 * The algorithm is partly based on the one described in McCool, M.  "Anisotropic diffusion for
 * Monte Carlo noise reduction."  ACM Transactions on Graphics, vol. 18, no. 2, 1999.
 */

public class NoiseReductionFilter
{
  private int width, height;
  private float cu[][], cv[][];
  
  /**
   * Filter an image.
   * 
   * @param iterations   the number of iterations of diffusion to use
   * @param width        the width of the image, in pixels
   * @param height       the height of the image, in pixels
   * @param red          contains the red component for each pixel in the image
   * @param green        contains the green component for each pixel in the image
   * @param blue         contains the blue component for each pixel in the image
   * @param error        contains the square of the standard error for each pixel in the image
   * @param object       contains the object covered by each pixel in the image
   */

  public static void filter(int iterations, final int width, final int height, float red[], float green[], float blue[], float error[], final Object object[])
  {
    NoiseReductionFilter filter = new NoiseReductionFilter(iterations, width, height);
    float origRed[] = red, origGreen[] =  green, origBlue[] = blue, origVariance[] = error;

    for (int i = 0; i < iterations; i++)
    {
      final float r[] = red;
      final float g[] = green;
      final float b[] = blue;
      final float e[] = error;
      ConductivityFunction fn = new ConductivityFunction() {
        public float evaluate(int x1, int y1, int x2, int y2)
        {
          int index1 = x1+y1*width;
          int index2 = x2+y2*width;
          if (object[index1] != object[index2] || object[index1] == null)
            return 0.0f;
          float err = e[index1]+e[index2];
          if (err < 1.0e-6)
            return 0.0f;
          float dred = r[index1]-r[index2];
          float dgreen = g[index1]-g[index2];
          float dblue = b[index1]-b[index2];
          float d = (float) ((dred*dred+dgreen*dgreen+dblue*dblue)/err);
          return (float) (Math.exp(-d*0.2));
        }
      };
      filter.calcConductivity(fn);
      red = filter.filterImageComponent(red);
      green = filter.filterImageComponent(green);
      blue = filter.filterImageComponent(blue);
      error = filter.filterImageComponent(error);
    }
    System.arraycopy(red, 0, origRed, 0, red.length);
    System.arraycopy(green, 0, origGreen, 0, green.length);
    System.arraycopy(blue, 0, origBlue, 0, blue.length);
  }
  
  /**
   * The constructor is private.  Use the static filter() method instead.
   */
  
  private NoiseReductionFilter(int iterations, int width, int height)
  {
    this.width = width;
    this.height = height;
    cu = new float [width-1][height];
    cv = new float [width][height-1];
  }
  
  /**
   * Recalculate the conductivity between every pair of pixels.
   */

  private void calcConductivity(ConductivityFunction fn)
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

  private float [] filterImageComponent(float original[])
  {
    float [] filtered = new float [width*height];

    for (int i = 0; i < width-1; i++)
      for (int j = 0; j < height; j++)
      {
        int index1 = i+j*width;
        int index2 = i+1+j*width;
        float diff = cu[i][j]*(original[index2]-original[index1]);
        filtered[index1] += diff;
        filtered[index2] -= diff;
      }
    for (int i = 0; i < width; i++)
      for (int j = 0; j < height-1; j++)
      {
        int index1 = i+j*width;
        int index2 = i+(j+1)*width;
        float diff = cv[i][j]*(original[index2]-original[index1]);
        filtered[index1] += diff;
        filtered[index2] -= diff;
      }
    for (int i = 0; i < width; i++)
      for (int j = 0; j < height; j++)
      {
        int index = i+j*width;
        filtered[index] = 0.1f*filtered[index]+original[index];
      }
    return filtered;
  }

  /**
   * This interface defines a function for calculating the conductivity between two pixels.
   */

  private static interface ConductivityFunction
  {
    float evaluate(int x1, int y1, int x2, int y2);
  }
}
