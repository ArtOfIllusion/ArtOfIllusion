/* Copyright (C) 2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.view;

import artofillusion.math.*;
import artofillusion.*;
import artofillusion.texture.*;

/**
 * This is a VertexShader which colors the surface based on the value of a TextureParameter.
 */

public class ParameterVertexShader implements VertexShader
{
  private ParameterValue param;
  private RGBColor lowColor, highColor;
  private double minValue, maxValue;
  private double rprimehigh, gprimehigh, bprimehigh;
  private double rprimelow, gprimelow, bprimelow;
  private RenderingMesh mesh;
  private Vec3 viewDir;
  private float light[];

  /** Create a ParameterVertexShader for a mesh.
      @param mesh      the mesh to render
      @param param     the ParameterValue by which to color the mesh
      @param lowColor  the color to display for low values of the parameter
      @param highColor the color to display for high values of the parameter
      @param minValue  the minimum value the parameter can have
      @param maxValue  the maximum value the parameter can have
      @param viewDir   the direction from which it is being viewed
  */

  public ParameterVertexShader(RenderingMesh mesh, ParameterValue param, RGBColor lowColor, RGBColor highColor, double minValue, double maxValue, Vec3 viewDir)
  {
    this.mesh = mesh;
    this.param = param;
    this.lowColor = lowColor;
    this.highColor = highColor;
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.viewDir = viewDir;
    light = new float [mesh.norm.length];
    rprimehigh = Math.pow(highColor.getRed(), 1.0/0.45);
    gprimehigh = Math.pow(highColor.getGreen(), 1.0/0.45);
    bprimehigh = Math.pow(highColor.getBlue(), 1.0/0.45);
    rprimelow = Math.pow(lowColor.getRed(), 1.0/0.45);
    gprimelow = Math.pow(lowColor.getGreen(), 1.0/0.45);
    bprimelow = Math.pow(lowColor.getBlue(), 1.0/0.45);
    if (minValue == -Double.MAX_VALUE || maxValue == Double.MAX_VALUE)
    {
      // Find the actual range of values that occur for the parameter.

      double min = Double.MAX_VALUE;
      double max = -Double.MAX_VALUE;
      for (int i = 0; i < mesh.triangle.length; i++)
      {
        RenderingTriangle tri = mesh.triangle[i];
        double value = param.getValue(i, tri.v1, tri.v2, tri.v3, 1.0, 0.0, 0.0);
        min = Math.min(min, value);
        max = Math.max(max, value);
        value = param.getValue(i, tri.v1, tri.v2, tri.v3, 0.0, 1.0, 0.0);
        min = Math.min(min, value);
        max = Math.max(max, value);
        value = param.getValue(i, tri.v1, tri.v2, tri.v3, 0.0, 0.0, 1.0);
        min = Math.min(min, value);
        max = Math.max(max, value);
      }
      if (minValue == -Double.MAX_VALUE)
        minValue = min;
      if (maxValue == Double.MAX_VALUE)
        maxValue = max;
    }
  }

  /** Select the color for a vertex.
      @param face     the index of the triangle being rendered
      @param vertex   the index of the vertex to color
      @param color    the vertex color will be returned in this object
  */

  public void getColor(int face, int vertex, RGBColor color)
  {
    int norm;
    double value;
    RenderingTriangle tri = mesh.triangle[face];
    switch (vertex)
    {
      case 0:
        norm = mesh.triangle[face].n1;
        value = param.getValue(face, tri.v1, tri.v2, tri.v3, 1.0, 0.0, 0.0);
        break;
      case 1:
        norm = mesh.triangle[face].n2;
        value = param.getValue(face, tri.v1, tri.v2, tri.v3, 0.0, 1.0, 0.0);
        break;
      default:
        norm = mesh.triangle[face].n3;
        value = param.getValue(face, tri.v1, tri.v2, tri.v3, 0.0, 0.0, 1.0);
    }
    if (light[norm] == 0.0f)
    {
      float dot = (float) viewDir.dot(mesh.norm[norm]);
      light[norm] = 0.1f+0.8f*(dot > 0.0f ? dot : -dot);
    }
    if (maxValue > minValue)
    {
      double fract1 = (value-minValue)/(maxValue-minValue);
      double fract2 = 1.0-fract1;
      color.setRGB(Math.pow(fract1*rprimehigh+fract2*rprimelow, 0.45),
          Math.pow(fract1*gprimehigh+fract2*gprimelow, 0.45),
          Math.pow(fract1*bprimehigh+fract2*bprimelow, 0.45));
    }
    else
      color.copy(lowColor);
    color.scale(light[norm]);
  }

  /** Get whether a particular face should be rendered with a single uniform color.
      @param face    the index of the triangle being rendered
  */

  public boolean isUniformFace(int face)
  {
    if (!(param instanceof ConstantParameterValue || param instanceof FaceParameterValue))
      return false;
    RenderingTriangle tri = mesh.triangle[face];
    return (tri.n1 == tri.n2 && tri.n1 == tri.n3);
  }

  /** Get whether this shader represents a uniform texture.  If this returns true, all
      texture properties are uniform over the entire surface (although different parts
      may still be colored differently due to lighting).
   */

  public boolean isUniformTexture()
  {
    return (param instanceof ConstantParameterValue);
  }


  /** Get the color of the surface.  This should only be called if isUniformTexture() returns true.
      @param spec     the surface color will be returned in this object
   */

  public void getTextureSpec(TextureSpec spec)
  {
    double value = param.getAverageValue();
    double fract1 = (value-minValue)/(maxValue-minValue);
    double fract2 = 1.0-fract1;
    spec.diffuse.setRGB(fract1*highColor.getRed()+fract2*lowColor.getRed(),
        fract1*highColor.getGreen()+fract2*lowColor.getGreen(),
        fract1*highColor.getBlue()+fract2*lowColor.getBlue());
    spec.hilight.setRGB(0.0f, 0.0f, 0.0f);
    spec.emissive.setRGB(0.0f, 0.0f, 0.0f);
  }
}
