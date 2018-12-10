/* Copyright (C) 1999-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.*;
import artofillusion.texture.*;

/** A RenderingTriangle represents a triangle which is to be rendered to the screen.  This is
    an abstract class, whose subclasses provide details about how the triangle should be
    displayed.  Every RenderingTriangle is associated with a RenderingMesh.  The vertices of
    the triangle are specified as indices into the RenderingMesh's array of vertices. 
    Similarly, the normals are specified as indices into the array of normals. */

public abstract class RenderingTriangle implements Cloneable
{
  public int index, v1, v2, v3, n1, n2, n3;
  public RenderingMesh theMesh;

  private static double EMPTY_ARRAY[] = new double [0];
  protected static ThreadLocal tempParamValues = new ThreadLocal(); // Used when rendering layered textures

  public RenderingTriangle(int v1, int v2, int v3, int n1, int n2, int n3)
  {
    this.v1 = v1;
    this.v2 = v2;
    this.v3 = v3;
    this.n1 = n1;
    this.n2 = n2;
    this.n3 = n3;
  }

  /** Get the TextureMapping for this triangle. */
  
  public TextureMapping getTextureMapping()
  {
    return theMesh.mapping;
  }
  
  /** Given the barycentric coordinates for a point in the triangle, build a TextureSpec
      describing the properties of the triangle at that point.  The properties should be
      averaged over a region of width size. */
  
  public abstract void getTextureSpec(TextureSpec spec, double angle, double u, double v, double w, double size, double t);

  /** Same as above, except only return the transparent color.  This can save time in cases
      where only the transparency is required, for example, when tracing shadow rays. */

  public abstract void getTransparency(RGBColor trans, double angle, double u, double v, double w, double size, double t);
  
  /** Get the displacement for a point on the triangle. */

  public abstract double getDisplacement(double u, double v, double w, double size, double t);

  /** Set the mesh that this triangle is part of.  This is automatically called when the
      triangle is added to the mesh.
      @param mesh      the RenderingMesh this triangle belongs to
      @param map       the TextureMapping for this triangle
      @param index     the index of this triangle within the mesh
  */
  
  public void setMesh(RenderingMesh mesh, TextureMapping map, int index)
  {
    theMesh = mesh;
    this.index = index;
  }
  
  /** Given the barycentric coordinates for a point in the triangle, calculate the values
      of the texture parameters at that point.  If parameters have not been set for the
      rendering mesh this triangle belongs to, this returns null. */
  
  public double [] getParameters(double u, double v, double w)
  {
    ParameterValue param[] = theMesh.param;
    if (param.length == 0)
      return EMPTY_ARRAY; // Avoid creating unnecessary objects during rendering.
    double value[] = (double []) tempParamValues.get();
    if (value != null)
      return value;
    value = new double [param.length];
    for (int i = value.length-1; i >= 0; i--)
      value[i] = param[i].getValue(index, v1, v2, v3, u, v, w);
    return value;
  }

  @Override
  public RenderingTriangle clone()
  {
    try
    {
      return (RenderingTriangle) super.clone();
    }
    catch (CloneNotSupportedException ex)
    {
      // This should never happen.

      return null;
    }
  }
}