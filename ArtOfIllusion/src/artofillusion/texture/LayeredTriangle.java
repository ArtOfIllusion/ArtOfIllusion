/* Copyright (C) 2000-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.*;
import artofillusion.math.*;

/** LayeredTriangle is a subclass of RenderingTriangle, which represents a triangle whose
    properties are described by a LayeredMapping. */

public class LayeredTriangle extends RenderingTriangle
{
  private double x1, x2, x3, y1, y2, y3, z1, z2, z3;
  RenderingTriangle layerTriangle[];

  public LayeredTriangle(int v1, int v2, int v3, int n1, int n2, int n3, 
        double t1x, double t1y, double t1z, double t2x, double t2y, double t2z,
        double t3x, double t3y, double t3z, LayeredMapping theMapping,
        LayeredTexture theTexture, Vec3 vert[])
  {
    super(v1, v2, v3, n1, n2, n3);
    x1 = t1x;
    y1 = t1y;
    z1 = t1z;
    x2 = t2x;
    y2 = t2y;
    z2 = t2z;
    x3 = t3x;
    y3 = t3y;
    z3 = t3z;
    layerTriangle = new RenderingTriangle [theMapping.getNumLayers()];
    for (int i = 0; i < layerTriangle.length; i++)
      {
        RenderingTriangle tri = theMapping.getLayerMapping(i).mapTriangle(v1, v2, v3, n1, n2, n3, vert);
        if (!(tri instanceof UniformTriangle || tri instanceof Linear2DTriangle ||
            tri instanceof Linear3DTriangle))
          layerTriangle[i] = tri;
      }
  }

  public void getTextureSpec(TextureSpec spec, double angle, double u, double v, double w, double size, double time)
  {
    Vec3 pos = new Vec3(x1*u+x2*v+x3*w, y1*u+y2*v+y3*w, z1*u+z2*v+z3*w);
    LayeredMapping map = (LayeredMapping) theMesh.mapping;
    TextureSpec tempSpec = new TextureSpec();
    int numParams[] = map.numParams;
    int paramStartIndex[] = map.paramStartIndex;
    int fractParamIndex[] = map.fractParamIndex;
    int blendMode[] = map.blendMode;
    double paramTemp[] = new double [map.maxParams];
    double param[] = getParameters(u, v, w);
    TextureMapping mapping[] = map.mapping;
    float r, g, b, rt = 1.0f, gt = 1.0f, bt = 1.0f;
    double f, ft = 1.0;
    boolean front = (angle > 0.0);
    
    spec.diffuse.setRGB(0.0f, 0.0f, 0.0f);
    spec.specular.setRGB(0.0f, 0.0f, 0.0f);
    spec.hilight.setRGB(0.0f, 0.0f, 0.0f);
    spec.transparent.setRGB(1.0f, 1.0f, 1.0f);
    spec.emissive.setRGB(0.0f, 0.0f, 0.0f);
    spec.roughness = spec.cloudiness = 0.0;
    spec.bumpGrad.set(0.0, 0.0, 0.0);
    for (int i = 0; i < mapping.length; i++)
    {
      if (!mapping[i].appliesToFace(front))
        continue;
      f = param[fractParamIndex[i]];
      if (numParams[i] > 0)
        for (int j = 0; j < numParams[i]; j++)
          paramTemp[j] = param[paramStartIndex[i]+j];
      if (layerTriangle[i] == null)
        mapping[i].getTextureSpec(pos, tempSpec, angle, size, time, paramTemp);
      else
      {
        tempParamValues.set(paramTemp);
        layerTriangle[i].getTextureSpec(tempSpec, angle, u, v, w, size, time);
        tempParamValues.set(null);
      }
      r = rt*(float) f;
      g = gt*(float) f;
      b = bt*(float) f;
      spec.diffuse.add(r*tempSpec.diffuse.red, g*tempSpec.diffuse.green, b*tempSpec.diffuse.blue);
      spec.specular.add(r*tempSpec.specular.red, g*tempSpec.specular.green, b*tempSpec.specular.blue);
      spec.hilight.add(r*tempSpec.hilight.red, g*tempSpec.hilight.green, b*tempSpec.hilight.blue);
      spec.emissive.add(r*tempSpec.emissive.red, g*tempSpec.emissive.green, b*tempSpec.emissive.blue);
      if (blendMode[i] == LayeredMapping.BLEND)
      {
        spec.transparent.subtract(r*(1.0f-tempSpec.transparent.red), g*(1.0f-tempSpec.transparent.green), b*(1.0f-tempSpec.transparent.blue));
        f = ft*f;
      }
      else
      {
        r *= (1.0f-tempSpec.transparent.red);
        g *= (1.0f-tempSpec.transparent.green);
        b *= (1.0f-tempSpec.transparent.blue);
        spec.transparent.subtract(r, g, b);
        f = Math.max(Math.max(r, g), b);
      }
      spec.roughness += f*tempSpec.roughness;
      spec.cloudiness += f*tempSpec.cloudiness;
      if (blendMode[i] == LayeredMapping.OVERLAY_ADD_BUMPS)
        tempSpec.bumpGrad.scale(param[fractParamIndex[i]]);
      else
        tempSpec.bumpGrad.scale(f);
      spec.bumpGrad.add(tempSpec.bumpGrad);
      rt -= r;
      gt -= g;
      bt -= b;
      ft -= f;
      if (rt <= 0.0f && gt <= 0.0f && bt <= 0.0f)
        return;
    }
  }

  public void getTransparency(RGBColor trans, double angle, double u, double v, double w, double size, double time)
  {
    Vec3 pos = new Vec3(x1*u+x2*v+x3*w, y1*u+y2*v+y3*w, z1*u+z2*v+z3*w);
    LayeredMapping map = (LayeredMapping) theMesh.mapping;
    TextureSpec tempSpec = new TextureSpec();
    int numParams[] = map.numParams;
    int paramStartIndex[] = map.paramStartIndex;
    int fractParamIndex[] = map.fractParamIndex;
    int blendMode[] = map.blendMode;
    double paramTemp[] = new double [map.maxParams];
    double param[] = getParameters(u, v, w);
    TextureMapping mapping[] = map.mapping;
    float r, g, b, rt = 1.0f, gt = 1.0f, bt = 1.0f;
    double f;
    boolean front = (angle > 0.0);
    
    trans.setRGB(1.0f, 1.0f, 1.0f);
    for (int i = 0; i < mapping.length; i++)
      {
        if (!mapping[i].appliesToFace(front))
          continue;
        f = param[fractParamIndex[i]];
        if (numParams[i] > 0)
          for (int j = 0; j < numParams[i]; j++)
            paramTemp[j] = param[paramStartIndex[i]+j];
        if (layerTriangle[i] == null)
          mapping[i].getTransparency(pos, tempSpec.transparent, angle, size, time, paramTemp);
        else
        {
          tempParamValues.set(paramTemp);
          layerTriangle[i].getTransparency(tempSpec.transparent, angle, u, v, w, size, time);
          tempParamValues.set(null);
        }
        r = rt*(float) f;
        g = gt*(float) f;
        b = bt*(float) f;
        if (blendMode[i] == LayeredMapping.BLEND)
          trans.subtract(r*(1.0f-tempSpec.transparent.red), g*(1.0f-tempSpec.transparent.green), b*(1.0f-tempSpec.transparent.blue));
        else
          {
            r *= (1.0f-tempSpec.transparent.red);
            g *= (1.0f-tempSpec.transparent.green);
            b *= (1.0f-tempSpec.transparent.blue);
            trans.subtract(r, g, b);
          }
        rt -= r;
        gt -= g;
        bt -= b;
        if (rt <= 0.0f && gt <= 0.0f && bt <= 0.0f)
          return;
      }
  }

  public double getDisplacement(double u, double v, double w, double size, double time)
  {
    Vec3 pos = new Vec3(x1*u+x2*v+x3*w, y1*u+y2*v+y3*w, z1*u+z2*v+z3*w);
    LayeredMapping map = (LayeredMapping) theMesh.mapping;
    TextureSpec tempSpec = new TextureSpec();
    int numParams[] = map.numParams;
    int paramStartIndex[] = map.paramStartIndex;
    int fractParamIndex[] = map.fractParamIndex;
    int blendMode[] = map.blendMode;
    double paramTemp[] = new double [map.maxParams];
    double param[] = getParameters(u, v, w);
    TextureMapping mapping[] = map.mapping;
    double f, ft = 1.0, height = 0.0, tempHeight;
    
    for (int i = 0; i < mapping.length; i++)
      {
        f = param[fractParamIndex[i]];
        if (numParams[i] > 0)
          for (int j = 0; j < numParams[i]; j++)
            paramTemp[j] = param[paramStartIndex[i]+j];
        if (layerTriangle[i] == null)
          tempHeight = mapping[i].getDisplacement(pos, size, time, paramTemp);
        else
        {
          tempParamValues.set(paramTemp);
          tempHeight = layerTriangle[i].getDisplacement(u, v, w, size, time);
          tempParamValues.set(null);
        }
        f *= ft;
        if (blendMode[i] == LayeredMapping.OVERLAY_BLEND_BUMPS)
          {
            mapping[i].getTransparency(pos, tempSpec.transparent, 1.0, size, time, paramTemp);
            float min = tempSpec.transparent.red;
            if (min > tempSpec.transparent.green)
              min = tempSpec.transparent.green;
            if (min > tempSpec.transparent.blue)
              min = tempSpec.transparent.blue;
            f *= ft*(1.0f-min);
          }
        if (blendMode[i] != LayeredMapping.OVERLAY_ADD_BUMPS)
          ft -= f;
        height += tempHeight*f;
        if (ft <= 0.0)
          return height;
      }
    return height;
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
    LayeredMapping layered = (LayeredMapping) map;
    for (int i = 0; i < layerTriangle.length; i++)
      if (layerTriangle[i] != null)
        layerTriangle[i].setMesh(mesh, layered.mapping[i], index);
  }
}