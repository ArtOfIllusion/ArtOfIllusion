/* Copyright (C) 2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;

/**
 * This class provides faster replacements for some of the methods of java.lang.Math.
 * Read the comments for each method before using it, since in some cases they return
 * different results from java.lang.Math.
 */

public class FastMath
{
  /**
   * This method produces identical results to Math.floor() for all normal input values
   * (one which fall inside the allowed range of the int type).
   */

  public static int floor(double d)
  {
    if (d < 0.0)
    {
      int f = (int) d;
      if (f != d)
        f -= 1;
      return f;
    }
    return (int) d;
  }

  /**
   * This method produces identical results to Math.ceil() for all normal input values
   * (one which fall inside the allowed range of the int type).
   */

  public static int ceil(double d)
  {
    if (d > 0.0)
    {
      int f = (int) d;
      if (f != d)
        f += 1;
      return f;
    }
    return ((int) d);
  }

  /**
   * This method produces identical results to Math.round() for all normal input values
   * (one which fall inside the allowed range of the int type).
   */

  public static int round(double d)
  {
    return floor(d+0.5);
  }

  /**
   * This method produces results which are nearly identical to Math.pow(), although the
   * last few digits may be different due to numerical error.  Unlike Math.pow(), this
   * method requires the exponent to be an integer.
   */

  public static double pow(double base, int exponent)
  {
    if (exponent < 0)
    {
      exponent = -exponent;
      base = 1.0/base;
    }
    double result = 1.0;
    while (exponent != 0)
    {
      if ((exponent&1) == 1)
        result *= base;
      base *= base;
      exponent = exponent>>1;
    }
    return result;
  }

  /**
   * This method calculates a fast approximation to the arctan function.  It differs from
   * the true value by no more than 0.005 for any input value.
   * <p>
   * I found this formula on an internet discussion board post by Ranko Bojanic.  The reference
   * cited in that post was
   * <p>
   * Approximation Theory (C. Hastings, Jr., Note 143, Math. Tables Aids. Comp 6, 68 (1953))
   */

  public static double atan(double d)
  {
    if (d >= 1.0)
      return (0.5*Math.PI-d/(d*d+0.28));
    if (d <= -1.0)
      return (-0.5*Math.PI-d/(d*d+0.28));
    return (d/(1.0+0.28*d*d));
  }
}
