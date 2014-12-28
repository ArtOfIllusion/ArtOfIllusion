/* Copyright (C) 2004-2014 by Peter Eastman

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

import java.util.*;
import java.lang.ref.*;

/** This is a VertexShader which renders a textured mesh with smooth shading. */

public class TexturedVertexShader implements VertexShader
{
  private TextureSpec spec;
  private RenderingMesh mesh;
  private Vec3 viewDir;
  private double time, roughnessCache[];
  private RGBColor diffuseCache[], emissiveCache[], hilightCache[];
  private boolean cache;
  private int textureID;

  private static final WeakHashMap<RenderingMesh, SoftReference<TexturedVertexShader>> cachedShaderMap = new WeakHashMap<RenderingMesh, SoftReference<TexturedVertexShader>>();

  /** Create a TexturedVertexShader for a mesh.
      @param mesh      the mesh to render
      @param object    the object to which the mesh corresponds
      @param time      the current time
      @param viewDir   the direction from which it is being viewed
  */
  
  public TexturedVertexShader(RenderingMesh mesh, Object3D object, double time, Vec3 viewDir)
  {
    this.mesh = mesh;
    this.time = time;
    this.viewDir = viewDir;
    spec = new TextureSpec();
    textureID = mesh.mapping.getTexture().getID();

    // Determine whether we can cache the color components for each vertex.
    
    cache = true;
    ParameterValue value[] = object.getParameterValues();
    for (int i = 0; i < value.length; i++)
      if (!(value[i] instanceof ConstantParameterValue || value[i] instanceof VertexParameterValue))
      {
        cache = false;
        return;
      }
  }
  
  /** In some cases, a texture can be represented by a simpler shader.  In addition, it is
      sometimes possible to reuse shaders, thus avoiding having to repeat texture calculations.
      This method returns a shader which produces identical results to this one, but may or may
      not actually be the same object. */
  
  public VertexShader optimize()
  {
    if (mesh.mapping instanceof UniformMapping && !mesh.mapping.getTexture().hasComponent(Texture.HILIGHT_COLOR_COMPONENT))
    {
      // We can use a SmoothVertexShader instead.

      mesh.mapping.getTexture().getAverageSpec(spec, time, null);
      RGBColor color = new RGBColor(spec.diffuse.getRed()+spec.emissive.getRed()+0.5*spec.specular.getRed(), 
          spec.diffuse.getGreen()+spec.emissive.getGreen()+0.5*spec.specular.getGreen(), 
          spec.diffuse.getBlue()+spec.emissive.getBlue()+0.5*spec.specular.getBlue());
      color.clip();
      return new SmoothVertexShader(mesh, color, viewDir);
    }
    if (cache)
    {
      // See if we already have a cached shader for this mesh.  To ensure that we never tie up memory with this
      // cache, the key is stored behind a weak reference and the value behind a soft reference.

      SoftReference<TexturedVertexShader> ref = cachedShaderMap.get(mesh);
      if (ref != null)
      {
        TexturedVertexShader shader = ref.get();
        if (shader != null && shader.textureID == textureID)
        {
          shader.viewDir = viewDir;
          return shader;
        }
      }
      cachedShaderMap.put(mesh, new SoftReference<TexturedVertexShader>(this));
    }
    return this;
  }
  
  /** Select the color for a vertex.
      @param face     the index of the triangle being rendered
      @param vertex   the index of the vertex to color
      @param color    the vertex color will be returned in this object
  */
  
  public void getColor(int face, int vertex, RGBColor color)
  {
    RenderingTriangle tri = mesh.triangle[face];
    RGBColor diffuse, emissive, hilight;
    double dot, roughness;
    if (cache)
    {
      // Use cached texture information.
      
      if (diffuseCache == null)
      {
        diffuseCache = new RGBColor [mesh.vert.length];
        emissiveCache = new RGBColor [mesh.vert.length];
        hilightCache = new RGBColor [mesh.vert.length];
        roughnessCache = new double [mesh.vert.length];
      }
      int vert;
      switch (vertex)
      {
        case 0:
          vert = tri.v1;
          dot = viewDir.dot(mesh.norm[tri.n1]);
          if (diffuseCache[vert] == null)
          {
            tri.getTextureSpec(spec, -dot, 1.0, 0.0, 0.0, 0.1, time);
            diffuseCache[vert] = spec.diffuse.duplicate();
            emissiveCache[vert] = spec.emissive.duplicate();
            hilightCache[vert] = spec.hilight.duplicate();
            roughnessCache[vert] = spec.roughness;
          }
          break;
        case 1:
          vert = tri.v2;
          dot = viewDir.dot(mesh.norm[tri.n2]);
          if (diffuseCache[vert] == null)
          {
            tri.getTextureSpec(spec, -dot, 0.0, 1.0, 0.0, 0.1, time);
            diffuseCache[vert] = spec.diffuse.duplicate();
            emissiveCache[vert] = spec.emissive.duplicate();
            hilightCache[vert] = spec.hilight.duplicate();
            roughnessCache[vert] = spec.roughness;
          }
          break;
        default:
          vert = tri.v3;
          dot = viewDir.dot(mesh.norm[tri.n3]);
          if (diffuseCache[vert] == null)
          {
            tri.getTextureSpec(spec, -dot, 0.0, 0.0, 1.0, 0.1, time);
            diffuseCache[vert] = spec.diffuse.duplicate();
            emissiveCache[vert] = spec.emissive.duplicate();
            hilightCache[vert] = spec.hilight.duplicate();
            roughnessCache[vert] = spec.roughness;
          }
      }
      diffuse = diffuseCache[vert];
      emissive = emissiveCache[vert];
      hilight = hilightCache[vert];
      roughness = roughnessCache[vert];
    }
    else
    {
      // The texture needs to be recalculated for every face that uses a vertex.
      
      switch (vertex)
      {
        case 0:
          dot = viewDir.dot(mesh.norm[tri.n1]);
          tri.getTextureSpec(spec, -dot, 1.0, 0.0, 0.0, 0.01, time);
          break;
        case 1:
          dot = viewDir.dot(mesh.norm[tri.n2]);
          tri.getTextureSpec(spec, -dot, 0.0, 1.0, 0.0, 0.01, time);
          break;
        default:
          dot = viewDir.dot(mesh.norm[tri.n3]);
          tri.getTextureSpec(spec, -dot, 0.0, 0.0, 1.0, 0.01, time);
      }
      diffuse = spec.diffuse;
      emissive = spec.emissive;
      hilight = spec.hilight;
      roughness = spec.roughness;
    }
    
    // Select the color.
    
    double absDot = (dot > 0.0f ? dot : -dot);
    color.setRGB(diffuse.getRed()*absDot + emissive.getRed(),
        diffuse.getGreen()*absDot + emissive.getGreen(),
        diffuse.getBlue()*absDot + emissive.getBlue());
    if (hilight.getRed()+hilight.getGreen()+hilight.getBlue() > 0.0)
    {
      float intensity = (float) FastMath.pow(absDot, (int) ((1.0-roughness)*128.0)+1);
      color.add(hilight.getRed()*intensity, hilight.getGreen()*intensity, hilight.getBlue()*intensity);
    }
    color.clip();
  }
  
  /** Get whether a particular face should be rendered with a single uniform color.
      @param face    the index of the triangle being rendered
  */
  
  public boolean isUniformFace(int face)
  {
    return false;
  }
  
  /** Get whether this shader represents a uniform texture.  If this returns true, all
      texture properties are uniform over the entire surface (although different parts
      may still be colored differently due to lighting).
   */
  
  public boolean isUniformTexture()
  {
    return (mesh.mapping instanceof UniformMapping);
  }
  
  
  /** Get the color of the surface.  This should only be called if isUniformTexture() returns true.
      @param spec     the surface color will be returned in this object
   */

  public void getTextureSpec(TextureSpec spec)
  {
    mesh.mapping.getTexture().getAverageSpec(spec, time, null);
  }

  /**
   * Clear any cached information about a RenderingMesh.
   */
  public static void clearCachedShaders(RenderingMesh mesh)
  {
    cachedShaderMap.remove(mesh);
  }
}
