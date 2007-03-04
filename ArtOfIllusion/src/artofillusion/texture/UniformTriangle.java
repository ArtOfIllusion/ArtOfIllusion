/* Copyright (C) 1999-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.*;
import artofillusion.math.*;

/** UniformTriangle is a subclass of RenderingTriangle, which represents a triangle whose
    properties are uniform over the entire triangle. */

public class UniformTriangle extends RenderingTriangle
{
  private UniformMapping map;

  public UniformTriangle(int v1, int v2, int v3, int n1, int n2, int n3)
  {
    super(v1, v2, v3, n1, n2, n3);
  }

  /** Set the mesh that this triangle is part of.  This is automatically called when the
      triangle is added to the mesh.
      @param mesh      the RenderingMesh this triangle belongs to
      @param map       the TextureMapping for this triangle
      @param index     the index of this triangle within the mesh
  */

  public void setMesh(RenderingMesh mesh, TextureMapping map, int index)
  {
    super.setMesh(mesh, map, index);
    this.map = (UniformMapping) map;
  }

  public void getTextureSpec(TextureSpec spec, double angle, double u, double v, double w, double size, double t)
  {
    if (!map.appliesToFace(angle > 0.0))
    {
      spec.diffuse.setRGB(0.0f, 0.0f, 0.0f);
      spec.specular.setRGB(0.0f, 0.0f, 0.0f);
      spec.transparent.setRGB(1.0f, 1.0f, 1.0f);
      spec.emissive.setRGB(0.0f, 0.0f, 0.0f);
      spec.roughness = spec.cloudiness = 0.0;
      spec.bumpGrad.set(0.0, 0.0, 0.0);
      return;
    }
    ((UniformTexture) map.getTexture()).getTextureSpec(spec);
  }

  public  void getTransparency(RGBColor trans, double angle, double u, double v, double w, double size, double t)
  {
    if (!map.appliesToFace(angle > 0.0))
    {
      trans.setRGB(1.0f, 1.0f, 1.0f);
      return;
    }
    ((UniformTexture) map.getTexture()).getTransparency(trans);
  }

  public double getDisplacement(double u, double v, double w, double size, double t)
  {
    return 0.0;
  }
}