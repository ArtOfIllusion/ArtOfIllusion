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

/** This is a VertexShader which renders a mesh in a solid color with flat shading. */

public class FlatVertexShader implements VertexShader
{
  private RGBColor meshColor;
  private RenderingMesh mesh;
  private Vec3 viewDir;
  
  /** Create a FlatVertexShader for a mesh.
      @param mesh      the mesh to render
      @param object    the object to which the mesh corresponds
      @param time      the current time
      @param viewDir   the direction from which it is being viewed
  */
  
  public FlatVertexShader(RenderingMesh mesh, Object3D object, double time, Vec3 viewDir)
  {
    this.mesh = mesh;
    this.viewDir = viewDir;
    TextureSpec spec = new TextureSpec();
    object.getTexture().getAverageSpec(spec, time, object.getAverageParameterValues());
    meshColor = new RGBColor(spec.diffuse.getRed()+0.5*spec.specular.getRed(), 
        spec.diffuse.getGreen()+0.5*spec.specular.getGreen(), 
        spec.diffuse.getBlue()+0.5*spec.specular.getBlue());
    meshColor.clip();
  }
  
  /** Create a FlatVertexShader for a mesh.
      @param mesh      the mesh to render
      @param color     the color in which to draw the mesh
      @param viewDir   the direction from which it is being viewed
  */
  
  public FlatVertexShader(RenderingMesh mesh, RGBColor color, Vec3 viewDir)
  {
    this.mesh = mesh;
    meshColor = color;
    this.viewDir = viewDir;
  }
  
  /** Select the color for a vertex.
      @param face     the index of the triangle being rendered
      @param vertex   the index of the vertex to color
      @param color    the vertex color will be returned in this object
  */
  
  public void getColor(int face, int vertex, RGBColor color)
  {
    color.copy(meshColor);
    color.scale(0.1f+0.8f*Math.abs((float) viewDir.dot(mesh.faceNorm[face])));
  }
  
  /** Get whether a particular face should be rendered with a single uniform color.
      @param face    the index of the triangle being rendered
  */
  
  public boolean isUniformFace(int face)
  {
    return true;
  }
  
  /** Get whether this shader represents a uniform texture.  If this returns true, all
      texture properties are uniform over the entire surface (although different parts
      may still be colored differently due to lighting).
   */
  
  public boolean isUniformTexture()
  {
    return true;
  }
  
  /** Get the texture properties of the surface.  This should only be called if isUniformTexture() returns true.
      @param spec     the surface properties will be returned in this object
   */

  public void getTextureSpec(TextureSpec spec)
  {
    spec.diffuse.copy(meshColor);
    spec.hilight.setRGB(0.0f, 0.0f, 0.0f);
    spec.emissive.setRGB(0.0f, 0.0f, 0.0f);    
  }
}
