/* Copyright (C) 1999-2012 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.math;

import java.io.*;

/** A Vec3 represents a 3 component vector. */

public class Vec3
{
  public double x, y, z;

  /** Create a new Vec3 whose x, y, and z components are all equal to 0.0. */

  public Vec3()
  {
  }

  /** Create a new Vec3 with the specified x, y, and z components. */

  public Vec3(double xval, double yval, double zval)
  {
    x = xval;
    y = yval;
    z = zval;
  }
  
  /** Create a new Vec3 identical to another one. */

  public Vec3(Vec3 v)
  {
    x = v.x;
    y = v.y;
    z = v.z;
  }
  
  /** Set the x, y, and z components of this Vec3. */

  public final void set(double xval, double yval, double zval)
  {
    x = xval;
    y = yval;
    z = zval;
  }
  
  /** Set this Vec3 to be identical to another one. */
  
  public final void set(Vec3 v)
  {
    x = v.x;
    y = v.y;
    z = v.z;
  }
  
  /** Calculate the dot product of this vector with another one. */

  public final double dot(Vec3 v)
  {
    return x*v.x + y*v.y + z*v.z;
  }
  
  /** Calculate the cross product of this vector with another one. */

  public final Vec3 cross(Vec3 v)
  {
    return new Vec3(y*v.z-z*v.y, z*v.x-x*v.z, x*v.y-y*v.x);
  }
  
  /** Calculate the sum of this vector and another one. */

  public final Vec3 plus(Vec3 v)
  {
    return new Vec3(x+v.x, y+v.y, z+v.z);
  }
  
  /** Calculate the difference between this vector and another one. */

  public final Vec3 minus(Vec3 v)
  {
    return new Vec3(x-v.x, y-v.y, z-v.z);
  }
  
  /** Create a new Vec3 by multiplying each component of this one by a constant. */
  
  public final Vec3 times(double d)
  {
    return new Vec3(x*d, y*d, z*d);
  }


  /** Determine whether two vectors are identical. */

  @Override
  public final boolean equals(Object o)
  {
    if (o instanceof Vec3) {
      Vec3 v = (Vec3) o;
      return (v.x == x && v.y == y && v.z == z);
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
    return Math.sqrt(x*x+y*y+z*z);
  }
  
  /** Calculate the square of the length of this vector. */

  public final double length2()
  {
    return x*x+y*y+z*z;
  }
  
  /** Add another Vec3 to this one. */
  
  public final void add(Vec3 v)
  {
    x += v.x;
    y += v.y;
    z += v.z;
  }

  /** Subtract another Vec3 from this one. */
  
  public final void subtract(Vec3 v)
  {
    x -= v.x;
    y -= v.y;
    z -= v.z;
  }

  /** Multiply each component of this vector by the corresponding component of another vector. */
  
  public final void multiply(Vec3 v)
  {
    x *= v.x;
    y *= v.y;
    z *= v.z;
  }

  /** Multiply each component of this vector by a constant. */
  
  public final void scale(double d)
  {
    x *= d;
    y *= d;
    z *= d;
  }

  /** Scale each component of this vector so that it has a length of 1.  If this vector has a length
      of 0, this method has no effect. */
  
  public final void normalize()
  {
    double len = Math.sqrt(x*x+y*y+z*z);
    
    if (len > 0.0)
      {
        x /= len;
        y /= len;
        z /= len;
      }
  }
  
  /** Calculate the Euclidean distance between this vector and another one. */
  
  public final double distance(Vec3 v)
  {
    return Math.sqrt((v.x-x)*(v.x-x)+(v.y-y)*(v.y-y)+(v.z-z)*(v.z-z));
  }

  /** Calculate the square of the Euclidean distance between this vector and another one. */
  
  public final double distance2(Vec3 v)
  {
    return (v.x-x)*(v.x-x)+(v.y-y)*(v.y-y)+(v.z-z)*(v.z-z);
  }

  /** Create a 2 component vector by removing one axis of this one.
      @param which     the axis to drop (0=X, 1=Y, 2=Z)
  */

  public final Vec2 dropAxis(int which)
  {
    if (which == 0)
      return new Vec2(y, z);
    else if (which == 1)
      return new Vec2(x, z);
    else
      return new Vec2(x, y);
  }
    
  public String toString()
  {
    return "Vec3: " + x + ", " + y + ", " + z;
  }
  
  /** Create a unit vector which points in the X direction. */
  
  public static Vec3 vx()
  {
    return new Vec3(1.0, 0.0, 0.0);
  }

  /** Create a unit vector which points in the Y direction. */
  
  public static Vec3 vy()
  {
    return new Vec3(0.0, 1.0, 0.0);
  }

  /** Create a unit vector which points in the Z direction. */
  
  public static Vec3 vz()
  {
    return new Vec3(0.0, 0.0, 1.0);
  }

  /** Create a Vec3 by reading in information that was written by writeToFile(). */
  
  public Vec3(DataInputStream in) throws IOException
  {
    x = in.readDouble();
    y = in.readDouble();
    z = in.readDouble();
  }
  
  /** Write out a serialized representation of this object. */

  public void writeToFile(DataOutputStream out) throws IOException
  {
    out.writeDouble(x);
    out.writeDouble(y);
    out.writeDouble(z);
  }
}