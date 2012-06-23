/* Copyright (C) 1999-2012 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;

import java.io.*;

/** A Vec2 represents a 2 component vector. */

public class Vec2
{
  public double x, y;

  /** Create a new Vec2 whose x and y components are equal to 0.0. */

  public Vec2()
  {
  }

  /** Create a new Vec2 with the specified x and y components. */

  public Vec2(double xval, double yval)
  {
    x = xval;
    y = yval;
  }
  
  /** Create a new Vec2 identical to another one. */

  public Vec2(Vec2 v)
  {
    x = v.x;
    y = v.y;
  }
  
  /** Set the x and y components of this Vec2. */

  public final void set(double xval, double yval)
  {
    x = xval;
    y = yval;
  }
  
  /** Calculate the dot product of this vector with another one. */

  public final double dot(Vec2 v)
  {
    return x*v.x + y*v.y;
  }
  
  /** Calculate the cross product of this vector with another one.  Because the cross product of a
      pair of Vec2's always points along the Z axis, this returns only its Z component. */

  public final double cross(Vec2 v)
  {
    return x*v.y-y*v.x;
  }
  
  /** Calculate the sum of this vector and another one. */

  public final Vec2 plus(Vec2 v)
  {
    return new Vec2(x+v.x, y+v.y);
  }
  
  /** Calculate the difference between this vector and another one. */

  public final Vec2 minus(Vec2 v)
  {
    return new Vec2(x-v.x, y-v.y);
  }
  
  /** Create a new Vec2 by multiplying each component of this one by a constant. */
  
  public final Vec2 times(double d)
  {
    return new Vec2(x*d, y*d);
  }
  
  /** Determine whether two vectors are identical. */

  @Override
  public final boolean equals(Object o)
  {
    if (o instanceof Vec2) {
      Vec2 v = (Vec2) o;
      return (v.x == x && v.y == y);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return Float.floatToIntBits((float) x);
  }

  /** Calculate the length of this vector. */
  
  public final double length()
  {
    return Math.sqrt(x*x+y*y);
  }
  
  /** Calculate the square of the length of this vector. */

  public final double length2()
  {
    return x*x+y*y;
  }
  
  /** Add another Vec2 to this one. */
  
  public final void add(Vec2 v)
  {
    x += v.x;
    y += v.y;
  }

  /** Subtract another Vec2 from this one. */
  
  public final void subtract(Vec2 v)
  {
    x -= v.x;
    y -= v.y;
  }

  /** Multiply each component of this vector by a constant. */
  
  public final void scale(double d)
  {
    x *= d;
    y *= d;
  }

  /** Calculate the Euclidean distance between this vector and another one. */
  
  public final double distance(Vec2 v)
  {
    return Math.sqrt((v.x-x)*(v.x-x)+(v.y-y)*(v.y-y));
  }

  /** Calculate the square of the Euclidean distance between this vector and another one. */
  
  public final double distance2(Vec2 v)
  {
    return (v.x-x)*(v.x-x)+(v.y-y)*(v.y-y);
  }

  /** Scale each component of this vector so that it has a length of 1.  If this vector has a length
      of 0, this method has no effect. */

  public final void normalize()
  {
    double len = Math.sqrt(x*x+y*y);
    
    if (len > 0.0)
      {
        x /= len;
        y /= len;
      }
  }
  
  public String toString()
  {
    return "Vec2: " + x + ", " + y;
  }
  
  /** Create a unit vector which points in the X direction. */
  
  public static Vec2 vx()
  {
    return new Vec2(1.0, 0.0);
  }

  /** Create a unit vector which points in the Y direction. */
  
  public static Vec2 vy()
  {
    return new Vec2(0.0, 1.0);
  }
  
  /** Create a Vec2 by reading in information that was written by writeToFile(). */
  
  public Vec2(DataInputStream in) throws IOException
  {
    x = in.readDouble();
    y = in.readDouble();
  }
  
  /** Write out a serialized representation of this object. */

  public void writeToFile(DataOutputStream out) throws IOException
  {
    out.writeDouble(x);
    out.writeDouble(y);
  }
}