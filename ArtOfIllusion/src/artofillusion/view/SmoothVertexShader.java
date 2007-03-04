/* Copyright (C) 2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.view;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;

/** This is a VertexShader which renders a uniform colored mesh with smooth shading. */

public class SmoothVertexShader implements VertexShader
{
  private RGBColor meshColor;
  private RenderingMesh mesh;
  private Vec3 viewDir;
  private float light[];
  
  /** Create a SmoothVertexShader for a mesh.
      @param mesh      the mesh to render
      @param object    the object to which the mesh corresponds
      @param time      the current time
      @param viewDir   the direction from which it is being viewed
  */
  
  public SmoothVertexShader(RenderingMesh mesh, Object3D object, double time, Vec3 viewDir)
  {
    this.mesh = mesh;
    this.viewDir = viewDir;
    TextureSpec spec = new TextureSpec();
    object.getTexture().getAverageSpec(spec, time, object.getAverageParameterValues());
    meshColor = new RGBColor(spec.diffuse.getRed()+0.5*spec.specular.getRed(), 
        spec.diffuse.getGreen()+0.5*spec.specular.getGreen(), 
        spec.diffuse.getBlue()+0.5*spec.specular.getBlue());
    meshColor.clip();
    light = new float [mesh.norm.length];
  }
  
  /** Create a SmoothVertexShader for a mesh.
      @param mesh      the mesh to render
      @param color     the color in which to draw the mesh
      @param viewDir   the direction from which it is being viewed
  */
  
  public SmoothVertexShader(RenderingMesh mesh, RGBColor color, Vec3 viewDir)
  {
    this.mesh = mesh;
    meshColor = color;
    this.viewDir = viewDir;
    light = new float [mesh.norm.length];
  }
  
  /** Select the color for a vertex.
      @param face     the index of the triangle being rendered
      @param vertex   the index of the vertex to color
      @param color    the vertex color will be returned in this object
  */
  
  public void getColor(int face, int vertex, RGBColor color)
  {
    int norm;
    switch (vertex)
    {
      case 0:
        norm = mesh.triangle[face].n1;
        break;
      case 1:
        norm = mesh.triangle[face].n2;
        break;
      default:
        norm = mesh.triangle[face].n3;
    }
    if (light[norm] == 0.0f)
    {
      float dot = (float) viewDir.dot(mesh.norm[norm]);
      light[norm] = 0.1f+0.8f*(dot > 0.0f ? dot : -dot);
    }
    color.copy(meshColor);
    color.scale(light[norm]);
  }
  
  /** Get whether a particular face should be rendered with a single uniform color.
      @param face    the index of the triangle being rendered
  */
  
  public boolean isUniformFace(int face)
  {
    RenderingTriangle tri = mesh.triangle[face];
    return (tri.n1 == tri.n2 && tri.n1 == tri.n3);
  }
  
  /** Get whether this shader represents a uniform texture.  If this returns true, all
      texture properties are uniform over the entire surface (although different parts
      may still be colored differently due to lighting).
   */
  
  public boolean isUniformTexture()
  {
    return true;
  }
  
  
  /** Get the color of the surface.  This should only be called if isUniformTexture() returns true.
      @param spec     the surface color will be returned in this object
   */

  public void getTextureSpec(TextureSpec spec)
  {
    spec.diffuse.copy(meshColor);
    spec.hilight.setRGB(0.0f, 0.0f, 0.0f);
    spec.emissive.setRGB(0.0f, 0.0f, 0.0f);    
  }
}
