/* Copyright (C) 2001-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.*;
import artofillusion.math.*;

/** Nonlinear2DTriangle is a subclass of RenderingTriangle, which represents a triangle whose
    properties are defined by a nonlinear mapping of a Texture2D.  It stores a 3D
    "intermediate texture coordinate" for each vertex.  To find the surface properties at
    a given point, it linearly interpolates to find the intermediate texture coordinate
    at that point, then calls the NonlinearMapping2D object to determine the final 2D
    texture coordinate and get the surface spec. */

public class Nonlinear2DTriangle extends RenderingTriangle
{
  Vec3 t1, t2, t3;
  private NonlinearMapping2D map;

  public Nonlinear2DTriangle(int v1, int v2, int v3, int n1, int n2, int n3,
          Vec3 t1, Vec3 t2, Vec3 t3)
  {
    super(v1, v2, v3, n1, n2, n3);
    this.t1 = t1;
    this.t2 = t2;
    this.t3 = t3;
  }

  public void setParameters(double p1[], double p2[], double p3[], RenderingMesh mesh)
  {
    if (!map.coordsFromParams || p1[map.numTextureParams] == Double.MAX_VALUE)
      return;
    t1 = new Vec3(p1[map.numTextureParams], p1[map.numTextureParams+1], p1[map.numTextureParams+2]);
    map.getPreTransform().transform(t1);
    t2 = new Vec3(p2[map.numTextureParams], p2[map.numTextureParams+1], p2[map.numTextureParams+2]);
    map.getPreTransform().transform(t2);
    t3 = new Vec3(p3[map.numTextureParams], p3[map.numTextureParams+1], p3[map.numTextureParams+2]);
    map.getPreTransform().transform(t3);
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
    this.map = (NonlinearMapping2D) map;
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
    map.getSpecIntermed(spec, t1.x*u+t2.x*v+t3.x*w, t1.y*u+t2.y*v+t3.y*w, t1.z*u+t2.z*v+t3.z*w, size, angle, t, getParameters(u, v, w));
  }

  public void getTransparency(RGBColor trans, double angle, double u, double v, double w, double size, double t)
  {
    if (!map.appliesToFace(angle > 0.0))
    {
      trans.setRGB(1.0f, 1.0f, 1.0f);
      return;
    }
    map.getTransIntermed(trans, t1.x*u+t2.x*v+t3.x*w, t1.y*u+t2.y*v+t3.y*w, t1.z*u+t2.z*v+t3.z*w, size, angle, t, getParameters(u, v, w));
  }

  public double getDisplacement(double u, double v, double w, double size, double t)
  {
    return map.getDisplaceIntermed(t1.x*u+t2.x*v+t3.x*w, t1.y*u+t2.y*v+t3.y*w, t1.z*u+t2.z*v+t3.z*w, size, t, getParameters(u, v, w));
  }
}