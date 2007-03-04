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

/** UVWMappedTriangle is a subclass of RenderingTriangle, which represents a triangle whose
    3D texture coordinates are explicitly specified at each vertex. */

public class UVWMappedTriangle extends RenderingTriangle
{
  float r1, r2, r3, s1, s2, s3, t1, t2, t3;
  float drdx, drdy, drdz, dsdx, dsdy, dsdz, dtdx, dtdy, dtdz;
  float texScaleR, texScaleS, texScaleT;
  boolean bumpMapped;
  private TextureMapping map;

  /** Create a new UVMappedTriangle.  The triangle will not be fully initialized
      and should not be used until setParameters() has been called on it. */
  
  public UVWMappedTriangle(int v1, int v2, int v3, int n1, int n2, int n3)
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

  public void setTextureCoordinates(float r1, float s1, float t1, float r2, float s2, float t2,
        float r3, float s3, float t3, Vec3 vert1, Vec3 vert2, Vec3 vert3)
  {
    this.r1 = r1;
    this.s1 = s1;
    this.t1 = t1;
    this.r2 = r2;
    this.s2 = s2;
    this.t2 = t2;
    this.r3 = r3;
    this.s3 = s3;
    this.t3 = t3;
    
    // Construct a set of orthonormal vectors in the plane of the triangle,
    // and find the gradients of R, S, and T.
    
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
    double scalea = scaleaa*(r2-r1) - scaleab*(r3-r1);
    double scaleb = invb*(r3-r1);
    drdx = (float) (scalea*a.x + scaleb*b.x);
    drdy = (float) (scalea*a.y + scaleb*b.y);
    drdz = (float) (scalea*a.z + scaleb*b.z);
    scalea = scaleaa*(s2-s1) - scaleab*(s3-s1);
    scaleb = invb*(s3-s1);
    dsdx = (float) (scalea*a.x + scaleb*b.x);
    dsdy = (float) (scalea*a.y + scaleb*b.y);
    dsdz = (float) (scalea*a.z + scaleb*b.z);
    scalea = scaleaa*(t2-t1) - scaleab*(t3-t1);
    scaleb = invb*(t3-t1);
    dtdx = (float) (scalea*a.x + scaleb*b.x);
    dtdy = (float) (scalea*a.y + scaleb*b.y);
    dtdz = (float) (scalea*a.z + scaleb*b.z);
    texScaleR = (float) Math.sqrt(drdx*drdx + drdy*drdy + drdz*drdz);
    texScaleS = (float) Math.sqrt(dsdx*dsdx + dsdy*dsdy + dsdz*dsdz);
    texScaleT = (float) Math.sqrt(dtdx*dtdx + dtdy*dtdy + dtdz*dtdz);
  }

  public void getTextureSpec(TextureSpec spec, double angle, double u, double v, double w, double size, double time)
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
    ((Texture3D) map.getTexture()).getTextureSpec(spec, r1*u+r2*v+r3*w, s1*u+s2*v+s3*w, t1*u+t2*v+t3*w, size*texScaleR, size*texScaleS, size*texScaleT, angle, time, getParameters(u, v, w));
    if (bumpMapped)
    {
      double r = spec.bumpGrad.x;
      double s = spec.bumpGrad.y;
      double t = spec.bumpGrad.z;
      spec.bumpGrad.set(r*drdx+s*dsdx+t*dtdx, r*drdy+s*dsdy+t*dtdy, r*drdz+s*dsdz+t*dtdz);
    }
  }

  public void getTransparency(RGBColor trans, double angle, double u, double v, double w, double size, double time)
  {
    if (!map.appliesToFace(angle > 0.0))
    {
      trans.setRGB(1.0f, 1.0f, 1.0f);
      return;
    }
    ((Texture3D) map.getTexture()).getTransparency(trans, r1*u+r2*v+r3*w, s1*u+s2*v+s3*w, t1*u+t2*v+t3*w, size*texScaleR, size*texScaleS, size*texScaleT, angle, time, getParameters(u, v, w));
  }

  public double getDisplacement(double u, double v, double w, double size, double time)
  {
    return ((Texture3D) map.getTexture()).getDisplacement(r1*u+r2*v+r3*w, s1*u+s2*v+s3*w, t1*u+t2*v+t3*w, size*texScaleR, size*texScaleS, size*texScaleT, time, getParameters(u, v, w));
  }
}