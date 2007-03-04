/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;

/**
 * This class provides an interface for accessing a three dimensional noise function.
 * By default it uses the {@link SimplexNoise} class to calculate the function, but it is
 * possible to specify an alternate noise generator.  This allows plugins to replace the
 * noise function used throughout the application, for example to provide a faster implementation
 * using native code.
 * <p>
 * Regardless of the implementation, this should be a function which varies smoothly between
 * -1.0 and 1.0, with all of the variation happening on a length scale of about 1.  Typically,
 * you will want to add several octaves of this function together to create a fractal noise
 * function.  Methods are also provided for calculating the gradient of the noise function, and
 * a vector valued noise function.
 */

public class Noise
{
  private static NoiseGenerator generator = new NoiseGenerator()
  {
    public double getValue(double x, double y, double z)
    {
      return SimplexNoise.noise(x, y, z);
    }

    public void getGradient(Vec3 gradient, double x, double y, double z)
    {
      SimplexNoise.noiseGradient(gradient, x, y, z);
    }

    public void getVector(Vec3 v, double x, double y, double z)
    {
      SimplexNoise.noiseVector(v, x, y, z);
    }
  };

  /** Given a point in 3D space, return the value of the scalar noise 
      function at that point. */
  
  public static double value(double x, double y, double z)
  {
    return generator.getValue(x, y, z);
  }

  /** Given a point in 3D space, calculate the gradient of the scalar noise
      function at that point.  This is necessary when using noise for bump mapping. */

  public static void calcGradient(Vec3 gradient, double x, double y, double z)
  {
    generator.getGradient(gradient, x, y, z);
  }

  /** Given a point (x,y,z) in 3D space, set v to the value of the vector
      noise function at that point. */

  public static void calcVector(Vec3 v, double x, double y, double z)
  {
    generator.getVector(v, x, y, z);
  }

  /**
   * Get the generator used to calculate the noise function.
   */

  public NoiseGenerator getGenerator()
  {
    return generator;
  }

  /**
   * Set the generator used to calculate the noise function.
   */

  public void setGenerator(NoiseGenerator gen)
  {
    generator = gen;
  }

  /**
   * This interface defines an object which can be used to calculate the noise function.
   */

  public static interface NoiseGenerator
  {
    public double getValue(double x, double y, double z);

    public void getGradient(Vec3 gradient, double x, double y, double z);

    public void getVector(Vec3 v, double x, double y, double z);
  }
}