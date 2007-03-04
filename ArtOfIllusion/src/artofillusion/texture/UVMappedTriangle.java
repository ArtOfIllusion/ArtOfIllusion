/* Copyright (C) 2002-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.*;
import artofillusion.math.*;

/** UVMappedTriangle is a subclass of RenderingTriangle, which represents a triangle whose
    2D texture coordinates are explicitly specified at each vertex. */

public class UVMappedTriangle extends RenderingTriangle
{
  float s1, s2, s3, t1, t2, t3;
  float dsdx, dsdy, dsdz, dtdx, dtdy, dtdz;
  float texScaleS, texScaleT;
  boolean bumpMapped;
  private TextureMapping map;

  /** Create a new UVMappedTriangle.  The triangle will not be fully initialized
      and should not be used until setParameters() has been called on it. */
  
  public UVMappedTriangle(int v1, int v2, int v3, int n1, int n2, int n3)
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
    this.map = map;
    bumpMapped = map.getTexture().hasComponent(Texture.BUMP_COMPONENT);
  }
  
  /** Set the texture coordinates for this triangle and update various internal parameters. */

  public void setTextureCoordinates(float s1, float t1, float s2, float t2,
        float s3, float t3, Vec3 vert1, Vec3 vert2, Vec3 vert3)
  {
    this.s1 = s1;
    this.t1 = t1;
    this.s2 = s2;
    this.t2 = t2;
    this.s3 = s3;
    this.t3 = t3;
    
    // Construct a set of orthonormal vectors in the plane of the triangle,
    // and find the gradients of S and T.
    
    Vec3 a = vert2.minus(vert1);
    Vec3 b = vert3.minus(vert1);
    double inva = 1.0/a.length();
    double invb = 1.0/b.length();
    a.scale(inva);
    b.scale(invb);
    double adotb = a.dot(b);
    a.set(a.x-adotb*b.x, a.y-adotb*b.y, a.z-adotb*b.z);
    double aprimeinv2 = 1.0/(1.0-adotb*adotb);
    double scaleaa = aprimeinv2*inva;
    double scaleab = aprimeinv2*adotb*invb;
    double scalea = scaleaa*(s2-s1) - scaleab*(s3-s1);
    double scaleb = invb*(s3-s1);
    dsdx = (float) (scalea*a.x + scaleb*b.x);
    dsdy = (float) (scalea*a.y + scaleb*b.y);
    dsdz = (float) (scalea*a.z + scaleb*b.z);
    scalea = scaleaa*(t2-t1) - scaleab*(t3-t1);
    scaleb = invb*(t3-t1);
    dtdx = (float) (scalea*a.x + scaleb*b.x);
    dtdy = (float) (scalea*a.y + scaleb*b.y);
    dtdz = (float) (scalea*a.z + scaleb*b.z);
    texScaleS = (float) Math.sqrt(dsdx*dsdx + dsdy*dsdy + dsdz*dsdz);
    texScaleT = (float) Math.sqrt(dtdx*dtdx + dtdy*dtdy + dtdz*dtdz);
  }

  public void getTextureSpec(TextureSpec spec, double angle, double u, double v, double w, double size, double time)
  {
    double s, t;

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
    ((Texture2D) map.getTexture()).getTextureSpec(spec, s1*u+s2*v+s3*w, t1*u+t2*v+t3*w, size*texScaleS, size*texScaleT, angle, time, getParameters(u, v, w));
    if (bumpMapped)
    {
      s = spec.bumpGrad.x;
      t = spec.bumpGrad.y;
      spec.bumpGrad.set(s*dsdx+t*dtdx, s*dsdy+t*dtdy, s*dsdz+t*dtdz);
    }
  }

  public void getTransparency(RGBColor trans, double angle, double u, double v, double w, double size, double time)
  {
    if (!map.appliesToFace(angle > 0.0))
    {
      trans.setRGB(1.0f, 1.0f, 1.0f);
      return;
    }
    ((Texture2D) map.getTexture()).getTransparency(trans, s1*u+s2*v+s3*w, t1*u+t2*v+t3*w, size*texScaleS, size*texScaleT, angle, time, getParameters(u, v, w));
  }

  public double getDisplacement(double u, double v, double w, double size, double time)
  {
    return ((Texture2D) map.getTexture()).getDisplacement(s1*u+s2*v+s3*w, t1*u+t2*v+t3*w, size*texScaleS, size*texScaleT, time, getParameters(u, v, w));
  }
}