/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.math.*;
import artofillusion.*;

import java.io.*;

/**
 * This abstract class represents an object whose surface is defined as an isosurface of a
 * 3D field function.  Subclasses define the function and isosurface value used to generate
 * a particular surface.  More precisely, the interior of the object is defined to consist of
 * all points which satisfy the following conditions:
 * <ol>
 * <li>The value of the field at that point is greater than the cutoff value.</li>
 * <li>The point is inside the bounding box returned by getBounds().</li>
 * </ol>
 * In general, an implicit surface can be rendered in two different ways: by triangulating the
 * surface, or by directly evaluating the field function (e.g. raymarching).  Some renderers do not
 * have the ability to visualize implicit surfaces directly, and therefore must use triangulation.
 * Conversely, some implicit surface implementations may not support triangulation (getRenderingMesh()
 * returns null).  In that case, they cannot be rendered by renderers that do not support
 * direct visualization.
 * <p>
 * When both methods are available, the object can specify which is the preferred one to use.
 * It does this by implementing getPreferDirectRendering().
 */

public abstract class ImplicitObject extends Object3D
{
  /**
   * Get the value of the field function at a point specified in object coordinates.
   *
   * @param x       the x coordinate of the location at which to evaluate the function
   * @param y       the y coordinate of the location at which to evaluate the function
   * @param z       the z coordinate of the location at which to evaluate the function
   * @param size    the width of the region over which to average the function for antialiasing
   * @param time    the time at which the function is being evaluated
   * @return the value of the field function at the specified location
   */

  public abstract double getFieldValue(double x, double y, double z, double size, double time);

  /**
   * Get the gradient of the field function at a point specified in object coordinates.
   * <p>
   * The default implementation of this method estimates the gradient by evaluating the
   * field function at several closely spaced points.  In many cases, it is possible to calculate
   * the gradient both more quickly and more accurately by analytical means.  Whenever possible,
   * this method should be overridden to calculate the gradient directly.
   *
   * @param x       the x coordinate of the location at which to evaluate the function
   * @param y       the y coordinate of the location at which to evaluate the function
   * @param z       the z coordinate of the location at which to evaluate the function
   * @param size    the width of the region over which to average the function for antialiasing
   * @param time    the time at which the function is being evaluated
   * @param grad    on exit, this should be set equal to the gradient of the field function at
   *                the specified location
   */

  public void getFieldGradient(double x, double y, double z, double size, double time, Vec3 grad)
  {
    final double delta = 0.5*size;
    final double invDelta = 1.0/delta;
    double val = getFieldValue(x, y, z, size, time);
    grad.x = (getFieldValue(x+delta, y, z, size, time)-val)*invDelta;
    grad.y = (getFieldValue(x, y+delta, z, size, time)-val)*invDelta;
    grad.z = (getFieldValue(x, y, z+delta, size, time)-val)*invDelta;
  }

  /**
   * Get the maximum value which can ever occur for the absolute value of the field gradient.
   * The default implementation returns Double.MAX_VALUE.  If you can guarantee that the
   * gradient will never be larger than some fixed value, overriding this method to return it
   * may allow faster rendering.
   */

  public double getMaxGradient()
  {
    return Double.MAX_VALUE;
  }

  /**
   * Get the cutoff value which defines the surface of the object.  Points for which
   * the field value is greater than the cutoff are inside the object.
   * <p>
   * The default implementation returns 1.0.  It may be overridden to return a different value.
   */

  public double getCutoff()
  {
    return 1.0;
  }

  /**
   * Get the preferred rendering method to use when this object is rendered by a renderer that
   * supports direct evaluation of the field.
   *
   * @return true if direct evaluation is preferred, false if it is preferable to triangulate the
   * surface by calling getRenderingMesh()
   */

  public abstract boolean getPreferDirectRendering();

  /**
   * The default constructor does nothing.
   */

  public ImplicitObject()
  {
  }

  /**
   * Subclasses should invoke this method in their own constructors for loading from a file.
   */

  public ImplicitObject(DataInputStream in, Scene theScene) throws IOException, InvalidObjectException
  {
    super(in, theScene);
  }

  /**
   * Subclasses should invoke this method in their own writeToFile() methods.
   */

  public void writeToFile(DataOutputStream out, Scene theScene) throws IOException
  {
    super.writeToFile(out, theScene);
  }

}
