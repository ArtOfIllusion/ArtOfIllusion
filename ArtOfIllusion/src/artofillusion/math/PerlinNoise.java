/* Copyright (C) 1999-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;

import java.util.*;

/** This class provides an implementation of Ken Perlin's noise function, based on the
    description (but not on the sample code) in "Texturing & Modeling: A Procedural Approach"
    (second edition), by Ebert, Musgrave, Peachey, Perlin, and Worley (Academic Press, 1998).
    It also includes the improvements described in "Improving Noise", by Ken Perlin,
    published in the 2002 SIGGRAPH Proceedings, pp. 681-2.
    <p>
    This is a function which varies smoothly between -1.0 and 1.0, with all of the variation
    happening on a length scale of about 1.  Typically, you will want to add several octaves
    of this function together to create a fractal noise function.  Methods are also provided
    for calculating the gradient of the noise function, and a closely related noise function
    which is vector valued.
    <p>
    Note: it is generally recommended to use {@link SimplexNoise} instead of this class,
    since it is faster and produces more isotropic noise.  Alternatively, use the
    {@link Noise} class, which allows the preferred noise generator to be changed.
 */

public class PerlinNoise
{
  private static short permute[];
  private static short grad[];
  private static Vec3 vect[];

  private static final int TABLE_SIZE = 256;
  private static final double GRAD_SCALE = 2.0/Math.sqrt(2.0);

  static
  {
    int i, j;
    short temp;
    Random random = new Random();
    Vec3 v;
    double len;

    // Precalculate the permutation table.

    random.setSeed(0);
    permute = new short [TABLE_SIZE*2];
    grad = new short [TABLE_SIZE*2];
    for (i = 0; i < TABLE_SIZE; i++)
      permute[i] = (short) i;
    for (i = 0; i < TABLE_SIZE; i++)
      {
        j = Math.abs(random.nextInt()) % TABLE_SIZE;
        temp = permute[i];
        permute[i] = permute[j];
        permute[j] = temp;
      }
    for (i = 0; i < TABLE_SIZE; i++)
      grad[i] = (short) (permute[i] % 12);

    // Precalculate the random vectors for the vector noise function.

    vect = new Vec3 [TABLE_SIZE*2];
    for (i = 0; i < TABLE_SIZE; i++)
      {
        do
          {
            v = new Vec3(2.0*random.nextDouble()-1.0, 2.0*random.nextDouble()-1.0, 2.0*random.nextDouble()-1.0);
            len = v.length2();
          } while (len > 1.0);
        v.scale(1.0/Math.sqrt(len));
        vect[i] = v;
      }

    // Extend all the tables to be twice as long.

    for (i = 0; i < TABLE_SIZE; i++)
      {
        permute[TABLE_SIZE+i] = permute[i];
        grad[TABLE_SIZE+i] = grad[i];
        vect[TABLE_SIZE+i] = vect[i];
      }
  }

  /** Given a point in 3D space, return the value of the scalar noise
      function at that point. */

  public static double value(double x, double y, double z)
  {
    double xi = FastMath.floor(x), yi = FastMath.floor(y), zi = FastMath.floor(z);
    double u1 = x-xi, v1 = y-yi, w1 = z-zi;
    double u2 = 1.0-u1, v2 = 1.0-v1, w2 = 1.0-w1;
    int i = ((int) xi)&0xFF, j = ((int) yi)&0xFF, k = ((int) zi)&0xFF;

    return GRAD_SCALE*(getWavelet(i, j, k, u1, v1, w1) +
        getWavelet(i+1, j, k, u2, v1, w1) +
        getWavelet(i, j+1, k, u1, v2, w1) +
        getWavelet(i+1, j+1, k, u2, v2, w1) +
        getWavelet(i, j, k+1, u1, v1, w2) +
        getWavelet(i+1, j, k+1, u2, v1, w2) +
        getWavelet(i, j+1, k+1, u1, v2, w2) +
        getWavelet(i+1, j+1, k+1, u2, v2, w2));
  }

  /** Evaluate the wavelet at node (i, j, k) for the point (u, v, w). */

  private static final double getWavelet(int i, int j, int k, double u, double v, double w)
  {
    double u2 = u*u, v2 = v*v, w2 = w*w, drop;

    drop = 1.0 - u2*(10.0*u+(6.0*u-15.0)*u2);
    drop *= 1.0 - v2*(10.0*v+(6.0*v-15.0)*v2);
    drop *= 1.0 - w2*(10.0*w+(6.0*w-15.0)*w2);
    switch (grad[permute[permute[i]+j]+k])
      {
        case 0:
          return drop*(u+v);
        case 1:
          return drop*(-u+v);
        case 2:
          return drop*(u-v);
        case 3:
          return drop*(-u-v);
        case 4:
          return drop*(u+w);
        case 5:
          return drop*(-u+w);
        case 6:
          return drop*(u-w);
        case 7:
          return drop*(-u-w);
        case 8:
          return drop*(v+w);
        case 9:
          return drop*(-v+w);
        case 10:
          return drop*(v-w);
        case 11:
          return drop*(-v-w);
        default:
          return 0.0;  // Should never get here
      }
  }

  /** Given a point in 3D space, calculate the gradient of the scalar noise
      function at that point.  This is necessary when using noise for bump mapping. */

  public static void calcGradient(Vec3 gradient, double x, double y, double z)
  {
    double xi = FastMath.floor(x), yi = FastMath.floor(y), zi = FastMath.floor(z);
    double u1 = x-xi, v1 = y-yi, w1 = z-zi;
    double u2 = 1.0-u1, v2 = 1.0-v1, w2 = 1.0-w1;
    int i = ((int) xi)&0xFF, j = ((int) yi)&0xFF, k = ((int) zi)&0xFF;

    gradient.set(0.0, 0.0, 0.0);
    addWaveletGradient(gradient, i, j, k, u1, v1, w1);
    addWaveletGradient(gradient, i+1, j, k, u2, v1, w1);
    addWaveletGradient(gradient, i, j+1, k, u1, v2, w1);
    addWaveletGradient(gradient, i+1, j+1, k, u2, v2, w1);
    addWaveletGradient(gradient, i, j, k+1, u1, v1, w2);
    addWaveletGradient(gradient, i+1, j, k+1, u2, v1, w2);
    addWaveletGradient(gradient, i, j+1, k+1, u1, v2, w2);
    addWaveletGradient(gradient, i+1, j+1, k+1, u2, v2, w2);
    gradient.scale(2.0);
  }

  /** Add the gradient of the wavelet at node (i, j, k) for the point (u, v, w). */

  private static final void addWaveletGradient(Vec3 gradient, int i, int j, int k, double u, double v, double w)
  {
    double dropu, dropv, dropw, product, dot;
    double u2 = u*u, v2 = v*v, w2 = w*w;

    dropu = 1.0 - u2*(10.0*u+(6.0*u-15.0)*u2);
    dropv = 1.0 - v2*(10.0*v+(6.0*v-15.0)*v2);
    dropw = 1.0 - w2*(10.0*w+(6.0*w-15.0)*w2);
    product = dropu*dropv*dropw;
    switch (grad[permute[permute[i]+j]+k])
      {
        case 0:
          dot = 30.0*(u+v);
          gradient.x += -dropv*dropw*u2*(u2-2.0*u+1.0)*dot + product;
          gradient.y += -dropw*dropu*v2*(v2-2.0*v+1.0)*dot + product;
          gradient.z += -dropu*dropv*w2*(w2-2.0*w+1.0)*dot;
          return;
        case 1:
          dot = 30.0*(-u+v);
          gradient.x += -dropv*dropw*u2*(u2-2.0*u+1.0)*dot - product;
          gradient.y += -dropw*dropu*v2*(v2-2.0*v+1.0)*dot + product;
          gradient.z += -dropu*dropv*w2*(w2-2.0*w+1.0)*dot;
          return;
        case 2:
          dot = 30.0*(u-v);
          gradient.x += -dropv*dropw*u2*(u2-2.0*u+1.0)*dot + product;
          gradient.y += -dropw*dropu*v2*(v2-2.0*v+1.0)*dot - product;
          gradient.z += -dropu*dropv*w2*(w2-2.0*w+1.0)*dot;
          return;
        case 3:
          dot = -30.0*(u+v);
          gradient.x += -dropv*dropw*u2*(u2-2.0*u+1.0)*dot - product;
          gradient.y += -dropw*dropu*v2*(v2-2.0*v+1.0)*dot - product;
          gradient.z += -dropu*dropv*w2*(w2-2.0*w+1.0)*dot;
          return;
        case 4:
          dot = 30.0*(u+w);
          gradient.x += -dropv*dropw*u2*(u2-2.0*u+1.0)*dot + product;
          gradient.y += -dropw*dropu*v2*(v2-2.0*v+1.0)*dot;
          gradient.z += -dropu*dropv*w2*(w2-2.0*w+1.0)*dot + product;
          return;
        case 5:
          dot = 30.0*(-u+w);
          gradient.x += -dropv*dropw*u2*(u2-2.0*u+1.0)*dot - product;
          gradient.y += -dropw*dropu*v2*(v2-2.0*v+1.0)*dot;
          gradient.z += -dropu*dropv*w2*(w2-2.0*w+1.0)*dot + product;
          return;
        case 6:
          dot = 30.0*(u-w);
          gradient.x += -dropv*dropw*u2*(u2-2.0*u+1.0)*dot + product;
          gradient.y += -dropw*dropu*v2*(v2-2.0*v+1.0)*dot;
          gradient.z += -dropu*dropv*w2*(w2-2.0*w+1.0)*dot - product;
          return;
        case 7:
          dot = -30.0*(u+w);
          gradient.x += -dropv*dropw*u2*(u2-2.0*u+1.0)*dot - product;
          gradient.y += -dropw*dropu*v2*(v2-2.0*v+1.0)*dot;
          gradient.z += -dropu*dropv*w2*(w2-2.0*w+1.0)*dot - product;
          return;
        case 8:
          dot = 30.0*(v+w);
          gradient.x += -dropv*dropw*u2*(u2-2.0*u+1.0)*dot;
          gradient.y += -dropw*dropu*v2*(v2-2.0*v+1.0)*dot + product;
          gradient.z += -dropu*dropv*w2*(w2-2.0*w+1.0)*dot + product;
          return;
        case 9:
          dot = 30.0*(-v+w);
          gradient.x += -dropv*dropw*u2*(u2-2.0*u+1.0)*dot;
          gradient.y += -dropw*dropu*v2*(v2-2.0*v+1.0)*dot - product;
          gradient.z += -dropu*dropv*w2*(w2-2.0*w+1.0)*dot + product;
          return;
        case 10:
          dot = 30.0*(v-w);
          gradient.x += -dropv*dropw*u2*(u2-2.0*u+1.0)*dot;
          gradient.y += -dropw*dropu*v2*(v2-2.0*v+1.0)*dot + product;
          gradient.z += -dropu*dropv*w2*(w2-2.0*w+1.0)*dot - product;
          return;
        case 11:
          dot = -30.0*(v+w);
          gradient.x += -dropv*dropw*u2*(u2-2.0*u+1.0)*dot;
          gradient.y += -dropw*dropu*v2*(v2-2.0*v+1.0)*dot - product;
          gradient.z += -dropu*dropv*w2*(w2-2.0*w+1.0)*dot - product;
          return;
        default:
          return;  // Should never get here
      }
  }

  /** Given a point (x,y,z) in 3D space, set v to the value of the vector
      noise function at that point. */

  public static void calcVector(Vec3 v, double x, double y, double z)
  {
    double xi = FastMath.floor(x), yi = FastMath.floor(y), zi = FastMath.floor(z);
    double u1 = x-xi, v1 = y-yi, w1 = z-zi;
    double u2 = 1.0-u1, v2 = 1.0-v1, w2 = 1.0-w1;
    int i = ((int) xi)&0xFF, j = ((int) yi)&0xFF, k = ((int) zi)&0xFF;

    v.set(0.0, 0.0, 0.0);
    addVectorWavelet(v, i, j, k, u1, v1, w1);
    addVectorWavelet(v, i+1, j, k, u2, v1, w1);
    addVectorWavelet(v, i, j+1, k, u1, v2, w1);
    addVectorWavelet(v, i+1, j+1, k, u2, v2, w1);
    addVectorWavelet(v, i, j, k+1, u1, v1, w2);
    addVectorWavelet(v, i+1, j, k+1, u2, v1, w2);
    addVectorWavelet(v, i, j+1, k+1, u1, v2, w2);
    addVectorWavelet(v, i+1, j+1, k+1, u2, v2, w2);
  }

  /** Evaluate the vector wavelet at node (i, j, k) for the point (u, v, w). */

  private static final void addVectorWavelet(Vec3 vec, int i, int j, int k, double u, double v, double w)
  {
    Vec3 g = vect[permute[permute[permute[i]+j]+k]];
    double u2 = u*u, v2 = v*v, w2 = w*w, drop;

    drop = 1.0 - u2*(10.0*u+(6.0*u-15.0)*u2);
    drop *= 1.0 - v2*(10.0*v+(6.0*v-15.0)*v2);
    drop *= 1.0 - w2*(10.0*w+(6.0*w-15.0)*w2);
    vec.x += drop*g.x;
    vec.y += drop*g.y;
    vec.z += drop*g.z;
  }
}