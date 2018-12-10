/* Copyright (C) 2004-2017 by Peter Eastman

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
  private RGBColor diffuse, emissive, hilight;
  private double time, roughnessCache[];
  private float diffuseCache[], emissiveCache[], hilightCache[];
  private boolean cachePerFace;
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
    diffuse = new RGBColor();
    emissive = new RGBColor();
    hilight = new RGBColor();
    textureID = mesh.mapping.getTexture().getID();

    // Determine whether we can cache the color components for each vertex.

    ParameterValue value[] = object.getParameterValues();
    for (int i = 0; i < value.length; i++)
      if (!(value[i] instanceof ConstantParameterValue || value[i] instanceof VertexParameterValue))
      {
        cachePerFace = true;
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
    return this;
  }

  /** Select the color for a vertex.
      @param face     the index of the triangle being rendered
      @param vertex   the index of the vertex to color
      @param color    the vertex color will be returned in this object
  */

  @Override
  public void getColor(int face, int vertex, RGBColor color)
  {
    double dot, roughness;
    if (cachePerFace)
    {
      if (diffuseCache == null)
      {
        diffuseCache = new float[9*mesh.triangle.length];
        emissiveCache = new float[9*mesh.triangle.length];
        hilightCache = new float[9*mesh.triangle.length];
        roughnessCache = new double[9*mesh.triangle.length];
        Arrays.fill(roughnessCache, -1.0);
      }
      RenderingTriangle tri = mesh.triangle[face];
      switch (vertex)
      {
        case 0:
          dot = viewDir.dot(mesh.norm[tri.n1]);
          if (roughnessCache[3*face] == -1.0)
            tri.getTextureSpec(spec, -dot, 1.0, 0.0, 0.0, 0.1, time);
          break;
        case 1:
          dot = viewDir.dot(mesh.norm[tri.n2]);
          if (roughnessCache[3*face+1] == -1.0)
            tri.getTextureSpec(spec, -dot, 0.0, 1.0, 0.0, 0.1, time);
          break;
        default:
          dot = viewDir.dot(mesh.norm[tri.n3]);
          if (roughnessCache[3*face+2] == -1.0)
            tri.getTextureSpec(spec, -dot, 0.0, 0.0, 1.0, 0.1, time);
      }
      int cacheIndex = 9*face+3*vertex;
      if (roughnessCache[3*face+vertex] == -1.0)
      {
        diffuseCache[cacheIndex] = spec.diffuse.getRed();
        diffuseCache[cacheIndex+1] = spec.diffuse.getGreen();
        diffuseCache[cacheIndex+2] = spec.diffuse.getBlue();
        emissiveCache[cacheIndex] = spec.emissive.getRed();
        emissiveCache[cacheIndex+1] = spec.emissive.getGreen();
        emissiveCache[cacheIndex+2] = spec.emissive.getBlue();
        hilightCache[cacheIndex] = spec.hilight.getRed();
        hilightCache[cacheIndex+1] = spec.hilight.getGreen();
        hilightCache[cacheIndex+2] = spec.hilight.getBlue();
        roughnessCache[3*face+vertex] = spec.roughness;
      }
      diffuse.setRGB(diffuseCache[cacheIndex], diffuseCache[cacheIndex+1], diffuseCache[cacheIndex+2]);
      emissive.setRGB(emissiveCache[cacheIndex], emissiveCache[cacheIndex+1], emissiveCache[cacheIndex+2]);
      hilight.setRGB(hilightCache[cacheIndex], hilightCache[cacheIndex+1], hilightCache[cacheIndex+2]);
      roughness = roughnessCache[3*face+vertex];
    }
    else
    {
      if (diffuseCache == null)
      {
        diffuseCache = new float[3*mesh.vert.length];
        emissiveCache = new float[3*mesh.vert.length];
        hilightCache = new float[3*mesh.vert.length];
        roughnessCache = new double[mesh.vert.length];
        Arrays.fill(roughnessCache, -1.0);
      }
      RenderingTriangle tri = mesh.triangle[face];
      int vert;
      switch (vertex)
      {
        case 0:
          vert = tri.v1;
          dot = viewDir.dot(mesh.norm[tri.n1]);
          if (roughnessCache[vert] == -1.0)
            tri.getTextureSpec(spec, -dot, 1.0, 0.0, 0.0, 0.1, time);
          break;
        case 1:
          vert = tri.v2;
          dot = viewDir.dot(mesh.norm[tri.n2]);
          if (roughnessCache[vert] == -1.0)
            tri.getTextureSpec(spec, -dot, 0.0, 1.0, 0.0, 0.1, time);
          break;
        default:
          vert = tri.v3;
          dot = viewDir.dot(mesh.norm[tri.n3]);
          if (roughnessCache[vert] == -1.0)
            tri.getTextureSpec(spec, -dot, 0.0, 0.0, 1.0, 0.1, time);
      }
      if (roughnessCache[vert] == -1.0)
      {
        diffuseCache[3*vert] = spec.diffuse.getRed();
        diffuseCache[3*vert+1] = spec.diffuse.getGreen();
        diffuseCache[3*vert+2] = spec.diffuse.getBlue();
        emissiveCache[3*vert] = spec.emissive.getRed();
        emissiveCache[3*vert+1] = spec.emissive.getGreen();
        emissiveCache[3*vert+2] = spec.emissive.getBlue();
        hilightCache[3*vert] = spec.hilight.getRed();
        hilightCache[3*vert+1] = spec.hilight.getGreen();
        hilightCache[3*vert+2] = spec.hilight.getBlue();
        roughnessCache[vert] = spec.roughness;
      }
      diffuse.setRGB(diffuseCache[3*vert], diffuseCache[3*vert+1], diffuseCache[3*vert+2]);
      emissive.setRGB(emissiveCache[3*vert], emissiveCache[3*vert+1], emissiveCache[3*vert+2]);
      hilight.setRGB(hilightCache[3*vert], hilightCache[3*vert+1], hilightCache[3*vert+2]);
      roughness = roughnessCache[vert];
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

  @Override
  public boolean isUniformFace(int face)
  {
    return false;
  }

  /** Get whether this shader represents a uniform texture.  If this returns true, all
      texture properties are uniform over the entire surface (although different parts
      may still be colored differently due to lighting).
   */

  @Override
  public boolean isUniformTexture()
  {
    return (mesh.mapping instanceof UniformMapping);
  }


  /** Get the color of the surface.  This should only be called if isUniformTexture() returns true.
      @param spec     the surface color will be returned in this object
   */

  @Override
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
