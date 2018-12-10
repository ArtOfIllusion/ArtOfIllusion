/* Copyright (C) 1999-2009 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.math.*;
import artofillusion.material.*;
import artofillusion.texture.*;

/** A RenderingMesh represents an object to be rendered to the screen.  It is described by
    an array of RenderingTriangles, and arrays describing the positions, normals, and 
    parameter values of the vertices.  If any normal vector is null, then each triangle 
    uses its own normal vector at the corresponding vertex. */

public class RenderingMesh implements Cloneable
{
  public Vec3 vert[], norm[], faceNorm[];
  public ParameterValue param[];
  public RenderingTriangle triangle[];
  public TextureMapping mapping;
  public MaterialMapping matMapping;
  private int indices[];
  private boolean hasCheckedForSmoothness;

  /** Construct a rendering mesh. */
  
  public RenderingMesh(Vec3 vert[], Vec3 norm[], RenderingTriangle triangle[], TextureMapping mapping, MaterialMapping matMapping)
  {
    int i, j;
    double length;
    
    this.vert = vert;
    this.norm = norm;
    this.triangle = triangle;
    this.mapping = mapping;
    this.matMapping = matMapping;
    faceNorm = new Vec3 [triangle.length];
    
    // Compute the normals for each face.

    Vec3 edge1 = new Vec3();
    Vec3 edge2 = new Vec3();
    for (i = 0; i < triangle.length; i++)
      {
        Vec3 v1 = vert[triangle[i].v1];
        Vec3 v2 = vert[triangle[i].v2];
        Vec3 v3 = vert[triangle[i].v3];
        edge1.set(v2.x-v1.x, v2.y-v1.y, v2.z-v1.z);
        edge2.set(v3.x-v1.x, v3.y-v1.y, v3.z-v1.z);
        faceNorm[i] = edge1.cross(edge2);
        length = faceNorm[i].length();
        if (length > 1e-12)
          faceNorm[i].scale(1.0/length);
        triangle[i].setMesh(this, mapping, i);
      }

    // If any triangles refer to null normal vectors, replace them by the triangle normals.
    
    for (i = 0, j = 0; i < triangle.length; i++)
      if (norm[triangle[i].n1] == null || norm[triangle[i].n2] == null || norm[triangle[i].n3] == null)
        j++;
    if (j == 0)
      return;
    Vec3 newNorm [] = new Vec3 [norm.length+j];
    for (i = 0; i < norm.length; i++)
      newNorm[i] = norm[i];
    for (i = 0, j = norm.length; i < triangle.length; i++)
      if (norm[triangle[i].n1] == null || norm[triangle[i].n2] == null || norm[triangle[i].n3] == null)
        {
          newNorm[j] = faceNorm[i];
          if (norm[triangle[i].n1] == null)
            triangle[i].n1 = j;
          if (norm[triangle[i].n2] == null)
            triangle[i].n2 = j;
          if (norm[triangle[i].n3] == null)
            triangle[i].n3 = j;
          j++;
        }
    this.norm = newNorm;
  }

  /** Create a clone of this mesh. */

  @Override
  public RenderingMesh clone()
  {
    try
    {
      RenderingMesh mesh = (RenderingMesh) super.clone();
      mesh.vert = vert.clone();
      mesh.norm = norm.clone();
      mesh.faceNorm = faceNorm.clone();
      mesh.triangle = new RenderingTriangle[triangle.length];
      for (int i = 0; i < triangle.length; i++)
      {
        mesh.triangle[i] = triangle[i].clone();
        mesh.triangle[i].setMesh(mesh, mapping, i);
      }
      return mesh;
    }
    catch (CloneNotSupportedException ex)
    {
      // This should never happen.

      return null;
    }
  }

  /** Set the texture parameters for the mesh. */

  public void setParameters(ParameterValue param[])
  {
    this.param = param;
    if (param == null)
      return;
    double val1[] = new double [param.length];
    double val2[] = new double [param.length];
    double val3[] = new double [param.length];
    for (int i = 0; i < triangle.length; i++)
    {
      RenderingTriangle tri = triangle[i];
      for (int j = param.length-1; j >= 0; j--)
      {
        val1[j] = param[j].getValue(i, tri.v1, tri.v2, tri.v3, 1.0, 0.0, 0.0);
        val2[j] = param[j].getValue(i, tri.v1, tri.v2, tri.v3, 0.0, 1.0, 0.0);
        val3[j] = param[j].getValue(i, tri.v1, tri.v2, tri.v3, 0.0, 0.0, 1.0);
      }
      mapping.setParameters(tri, val1, val2, val3, this);
    }
  }
  
  /** Apply a coordinate transformation to all of the vertices and normal vectors in this mesh. */
  
  public void transformMesh(Mat4 trans)
  {
    for (int i = 0; i < vert.length; i++)
      vert[i] = trans.times(vert[i]);
    for (int i = 0; i < norm.length; i++)
      if (norm[i] != null)
        norm[i] = trans.timesDirection(norm[i]);
    for (int i = 0; i < faceNorm.length; i++)
      faceNorm[i] = trans.timesDirection(faceNorm[i]);
  }
  
  /** This method is used to accelerate interactive rendering with OpenGL.  If this mesh is fully
      smoothed (there is exactly one normal for every vertex), it returns an array containing the
      vertex indices for all the faces in a form suitable for passing to glDrawElements().  Otherwise,
      it returns null. */
  
  public int [] getVertexIndices()
  {
    if (hasCheckedForSmoothness)
      return indices;
    hasCheckedForSmoothness = true;
    if (vert.length != norm.length)
      return null;
    for (int i = 0; i < triangle.length; i++)
    {
      RenderingTriangle tri = triangle[i];
      if (tri.v1 != tri.n1 || tri.v2 != tri.n2 || tri.v3 != tri.n3)
        return null;
    }
    indices = new int [triangle.length*3];
    for (int i = 0, j = 0; i < triangle.length; i++)
    {
      RenderingTriangle tri = triangle[i];
      indices[j++] = tri.v1;
      indices[j++] = tri.v2;
      indices[j++] = tri.v3;
    }
    return indices;
  }
}