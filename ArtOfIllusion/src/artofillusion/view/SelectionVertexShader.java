/* Copyright (C) 2005 by Peter Eastman

This program is free software; you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY 
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.view;

import artofillusion.math.*;
import artofillusion.texture.TextureSpec;

/** This is a VertexShader which highlights selected faces.  For unselected faces, it delegates
    to another shader to select the color. */

public class SelectionVertexShader implements VertexShader
{
  private RGBColor selectionColor;
  private VertexShader shader;
  private int faceIndex[];
  private boolean selected[];
  
  /** Create a FlatVertexShader for a mesh.
      @param selectionColor  the color to use for selected faces
      @param shader          the shader to use for unselected faces
      @param faceIndex       the index of the control mesh face for each face of the rendering mesh
      @param selected        specifies which faces of the control mesh are selected
  */
  
  public SelectionVertexShader(RGBColor selectionColor, VertexShader shader, int faceIndex[], boolean selected[])
  {
    this.selectionColor = selectionColor;
    this.shader = shader;
    this.faceIndex = faceIndex;
    this.selected = selected;
  }
  
  /** Select the color for a vertex.
      @param face     the index of the triangle being rendered
      @param vertex   the index of the vertex to color
      @param color    the vertex color will be returned in this object
  */
  
  public void getColor(int face, int vertex, RGBColor color)
  {
    if (selected[faceIndex[face]])
      color.copy(selectionColor);
    else
      shader.getColor(face, vertex, color);
  }
  
  /** Get whether a particular face should be rendered with a single uniform color.
     @param face    the index of the triangle being rendered
  */
  
  public boolean isUniformFace(int face)
  {
    if (selected[faceIndex[face]])
      return true;
    return shader.isUniformFace(face);
  }
  
  /** Get whether this shader represents a uniform texture.  If this returns true, all
      texture properties are uniform over the entire surface (although different parts
      may still be colored differently due to lighting).
   */
  
  public boolean isUniformTexture()
  {
    return false;
  }
  
  /** Get the texture properties of the surface.  This should only be called if isUniformTexture() returns true.
      @param spec     the surface properties will be returned in this object
   */

  public void getTextureSpec(TextureSpec spec)
  {
  }
}
