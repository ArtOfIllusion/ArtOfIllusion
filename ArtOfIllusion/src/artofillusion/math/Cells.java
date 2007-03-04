/* Copyright (C) 2001-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;

import java.util.*;

/** This class provides an implementation of Steven Worley's cellular texture basis function
    as described in
    <p>
    Worley, S. "A Cellular Texture Basis Function." Siggraph Proceedings, pp. 291-294, 1996.
    <p>
    This function scatters "feature points" at random locations throughout 3D space.  At
    any point (x, y, z), it defines the function f1(x, y, z) to be the distance to the
    nearest feature point.  It similarly defines f2(x, y, z) as the distance to the second
    nearest feature point, and similarly for f3(x, y, z), etc. */

public class Cells
{
  private double dx[], dy[], dz[];
  private Vec3 feature, diff;
  private Random random = new FastRandom(0);
  private int metric;

  private static double prob[];
  
  public static final int EUCLIDEAN = 0;
  public static final int CITY_BLOCK = 1;
  public static final int CHESS_BOARD = 2;

  private static final double DENSITY = 3.0;
  private static final int cellIndex[][] = new int [][] {{0, 1, 1}, {2, 1, 1}, {1, 0, 1},
    {1, 2, 1}, {1, 1, 0}, {1, 1, 2}, {0, 0, 1}, {0, 2, 1}, {0, 1, 0}, {0, 1, 2}, 
    {2, 0, 1}, {2, 2, 1}, {2, 1, 0}, {2, 1, 2}, {1, 0, 0}, {1, 0, 2}, {1, 2, 0}, 
    {1, 2, 2}, {0, 0, 0}, {0, 0, 2}, {0, 2, 0}, {0, 2, 2}, {2, 0, 0}, {2, 0, 2}, 
    {2, 2, 0}, {2, 2, 2}};

  static
  {
    // Calculate the probabilities for different numbers of points to be in a cell.
    
    prob = new double [10];
    for (int i = 0; i < 10; i++)
      {
        prob[i] = FastMath.pow(DENSITY, i) * Math.exp(-DENSITY);
        for (int j = 2; j <= i; j++)
            prob[i] /= (double) j;
      }
    for (int i = 1; i < 10; i++)
      prob[i] += prob[i-1];
  }
  
  public Cells()
  {
    dx = new double [3];
    dy = new double [3];
    dz = new double [3];
    feature = new Vec3();
    diff = new Vec3();
    metric = EUCLIDEAN;
  }

  /** Get which distance metric is being used.  Allowed values are EUCLIDEAN, CITY_BLOCK,
      or CHESS_BOARD. */

  public int getMetric()
  {
    return metric;
  }

  /** Set which distance metric to use.  Allowed values are EUCLIDEAN, CITY_BLOCK, 
      or CHESS_BOARD. */
  
  public void setMetric(int metric)
  {
    this.metric = metric;
  }

  /** Calculate the various functions at a specified point.  The number of functions to
      evaluate is determined by the length of value[].  The values of the functions are
      returned in value[], and their gradients in grad[].  A "unique identifier" for the
      nearest, second nearest, etc. feature point is returned in id.  If gradients are 
      not needed, null may be passed for grad[]. */

  public void calcFunctions(Vec3 p, double value[], Vec3 grad[], int id[])
  {
    int i, j, k, m, num;
    int a, b, c;
    int seed;
    double x, y, z, r, dist2, rand;

    a = floor(p.x);
    b = floor(p.y);
    c = floor(p.z);
    x = p.x - (double) a;
    y = p.y - (double) b;
    z = p.z - (double) c;
    for (i = 0; i < value.length; i++)
      value[i] = Double.MAX_VALUE;

    // Find the number of features in this cell.

    seed = randSeed(a, b, c);
    random.setSeed(seed);
    random.nextInt();
    rand = random.nextDouble();
    for (num = 1; num < 9 && rand > prob[num]; num++);

    // Find the nearest features in this cell.

    for (i = 0; i < num; i++)
      {
        feature.x = random.nextDouble();
        feature.y = random.nextDouble();
        feature.z = random.nextDouble();
        diff.x = x-feature.x;
        diff.y = y-feature.y;
        diff.z = z-feature.z;
        dist2 = distance(diff);
        sortIntoList(dist2, seed+i, value, grad, id);
      }

    // Check feature points in all the surrounding cells.

    dx[0] = x*x;
    dy[0] = y*y;
    dz[0] = z*z;
    dx[2] = (1.0-x)*(1.0-x);
    dy[2] = (1.0-y)*(1.0-y);
    dz[2] = (1.0-z)*(1.0-z);
    r = value[value.length-1];
    for (int cell = 0; cell < cellIndex.length; cell++)
      {
        i = cellIndex[cell][0];
        j = cellIndex[cell][1];
        k = cellIndex[cell][2];
        if (r < dx[i]+dy[j]+dz[k])
          continue;
        i--;
        j--;
        k--;
        seed = randSeed(a+i, b+j, c+k);
        random.setSeed(seed);
        random.nextInt();
        rand = random.nextDouble();
        for (num = 1; num < 9 && rand > prob[num]; num++);

        // Find the nearest features in this cell.

        for (m = 0; m < num; m++)
          {
            feature.x = random.nextDouble() + (double) i;
            feature.y = random.nextDouble() + (double) j;
            feature.z = random.nextDouble() + (double) k;
            diff.x = x-feature.x;
            diff.y = y-feature.y;
            diff.z = z-feature.z;
            dist2 = distance(diff);
            if (dist2 < value[value.length-1])
              sortIntoList(dist2, seed+m, value, grad, id);
          }
        r = value[value.length-1];
      }
    if (metric == EUCLIDEAN)
      for (i = 0; i < value.length; i++)
        {
          value[i] = Math.sqrt(value[i]);
          if (grad != null)
            grad[i].scale(1.0/value[i]);
        }
  }

  /** Calculate the random number seed to use for cell (i, j, k). */

  private static final int randSeed(int i, int j, int k)
  {
    return i*10 + j*1000 + k*100000;
  }

  /** Given the squared distance to a feature point, sort that point into the list. */

  private final void sortIntoList(double dist2, int newid, double value[], Vec3 grad[], int id[])
  {
    for (int i = 0; i < value.length; i++)
      if (dist2 < value[i])
        {
          for (int j = value.length-1; j > i; j--)
            {
              value[j] = value[j-1];
              id[j] = id[j-1];
              if (grad != null)
                grad[j].set(grad[j-1]);
            }
          value[i] = dist2;
          id[i] = newid;
          if (grad != null)
            {
              if (metric == EUCLIDEAN)
                grad[i].set(diff);
              else if (metric == CITY_BLOCK)
                grad[i].set(diff.x > 0.0 ? 1.0 : -1.0, diff.y > 0.0 ? 1.0 : -1.0, diff.z > 0.0 ? 1.0 : -1.0);
              else
                {
                  double ax = Math.abs(diff.x);
                  double ay = Math.abs(diff.y);
                  double az = Math.abs(diff.z);
                  if (ax > ay)
                    {
                      if (ax > az)
                        grad[i].set(diff.x > 0.0 ? 1.0 : -1.0, 0.0, 0.0);
                      else
                        grad[i].set(0.0, 0.0, diff.z > 0.0 ? 1.0 : -1.0);
                    }
                  else
                    {
                      if (ay > az)
                        grad[i].set(0.0, diff.y > 0.0 ? 1.0 : -1.0, 0.0);
                      else
                        grad[i].set(0.0, 0.0, diff.z > 0.0 ? 1.0 : -1.0);
                    }
                }
            }
          return;
        }
  }
  
  /** Given the vector between two points, calculate the distance measure for the 
      desired metric. */
  
  private final double distance(Vec3 diff)
  {
    if (metric == EUCLIDEAN)
      return diff.length2();
    double ax = Math.abs(diff.x);
    double ay = Math.abs(diff.y);
    double az = Math.abs(diff.z);
    if (metric == CITY_BLOCK)
      return ax+ay+az;
    if (ax > ay)
      return (ax > az ? ax : az);
    return (ay > az ? ay : az);
  }
  
  /** A faster replacement for Math.floor(). */
  
  private static int floor(double d)
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
}



