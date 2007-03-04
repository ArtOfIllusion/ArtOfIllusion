/* Copyright (C) 2003-2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.texture;

import artofillusion.*;
import artofillusion.object.*;
import java.io.*;

/** This class defines a scalar parameter whose value is defined at each vertex of each face of a mesh. */

public class FaceVertexParameterValue implements ParameterValue
{
  private double values[];
  private int faceStartIndex[];

  /** This constructor is private.  It is used internally by duplicate(). */

  private FaceVertexParameterValue()
  {
  }
  
  /**
   * Create a new FaceVertexParameterValue object.  val is an array containing the parameter
   * value at every vertex of every face.  Specifically, val[i][j] is the value at the j'th
   * vertex of the i'th face.
   */
  
  public FaceVertexParameterValue(double val[][])
  {
    setValue(val);
  }
  
  /** Create a new FaceVertexParameterValue for a mesh, and initialize it to appropriate default values. */
  
  public FaceVertexParameterValue(FacetedMesh mesh, TextureParameter param)
  {
    faceStartIndex = new int[mesh.getFaceCount()+1];
    int totalValues = 0;
    for (int i = 0; i < mesh.getFaceCount(); i++)
    {
      totalValues += mesh.getFaceVertexCount(i);
      faceStartIndex[i+1] = totalValues;
    }

    MeshVertex vertex[] = mesh.getVertices();
    values = new double[totalValues];
    int index = 0;
    if (param.type == TextureParameter.NORMAL_PARAMETER)
      for (int face = 0; face < mesh.getFaceCount(); face++)
        for (int vert = 0; vert < mesh.getFaceVertexCount(face); vert++)
          values[index++] = param.defaultVal;
    if (param.type == TextureParameter.X_COORDINATE)
      for (int face = 0; face < mesh.getFaceCount(); face++)
        for (int vert = 0; vert < mesh.getFaceVertexCount(face); vert++)
          values[index++] = vertex[mesh.getFaceVertexIndex(face, vert)].r.x;
    if (param.type == TextureParameter.Y_COORDINATE)
      for (int face = 0; face < mesh.getFaceCount(); face++)
        for (int vert = 0; vert < mesh.getFaceVertexCount(face); vert++)
          values[index++] = vertex[mesh.getFaceVertexIndex(face, vert)].r.y;
    if (param.type == TextureParameter.Z_COORDINATE)
      for (int face = 0; face < mesh.getFaceCount(); face++)
        for (int vert = 0; vert < mesh.getFaceVertexCount(face); vert++)
          values[index++] = vertex[mesh.getFaceVertexIndex(face, vert)].r.z;
  }

  /**
   * Set the list of parameter values.  val is an array containing the parameter
   * value at every vertex of every face.  Specifically, val[i][j] is the value at the j'th
   * vertex of the i'th face.
   */

  public void setValue(double val[][])
  {
    faceStartIndex = new int[val.length+1];
    int totalValues = 0;
    for (int i = 0; i < val.length; i++)
    {
      totalValues += val[i].length;
      faceStartIndex[i+1] = totalValues;
    }
    values = new double[totalValues];
    int index = 0;
    for (int i = 0; i < val.length; i++)
      for (int j = 0; j < val[i].length; j++)
        values[index++] = val[i][j];
  }

  /**
   * Get the value of the parameter at a particular vertex in a particular face.
   *
   * @param faceIndex     the index of the face within the mesh
   * @param vertIndex     the index of the vertex within the face
   * @return the value at the specified face/vertex
   */

  public double getValue(int faceIndex, int vertIndex)
  {
    return values[faceStartIndex[faceIndex]+vertIndex];
  }

  /**
   * Set the value of the parameter at a particular vertex in a particular face.
   *
   * @param faceIndex     the index of the face within the mesh
   * @param vertIndex     the index of the vertex within the face
   * @param newValue      the value to set for the specified face/vertex
   */

  public void setValue(int faceIndex, int vertIndex, double newValue)
  {
    values[faceStartIndex[faceIndex]+vertIndex] = newValue;
  }

  /**
   * Get the value of the parameter at a particular point within the interior of a face.
   * This method assumes the face is triangular (which is always the case for a parameter
   * of a RenderingMesh).
   */
  
  public double getValue(int faceIndex, int v1, int v2, int v3, double u, double v, double w)
  {
    return u*getValue(faceIndex, 0)+v*getValue(faceIndex, 1)+w*getValue(faceIndex, 2);
  }

  /**
   * Get the number of faces for which the parameter has values.
   */

  public int getFaceCount()
  {
    return faceStartIndex.length-1;
  }

  /**
   * Get the number of vertices in a particular face.
   *
   * @param faceIndex     the index of the face within the mesh
   */

  public int getFaceVertexCount(int faceIndex)
  {
    return faceStartIndex[faceIndex+1]-faceStartIndex[faceIndex];
  }
  
  /** Get the average value of the parameter over the entire surface. */
  
  public double getAverageValue()
  {
    double avg = 0.0;

    for (int i = 0; i < values.length; i++)
      avg += values[i];
    return (avg/values.length);
  }
  
  /** Create a duplicate of this object. */
  
  public ParameterValue duplicate()
  {
    FaceVertexParameterValue copy = new FaceVertexParameterValue();
    copy.values = (double[]) values.clone();
    copy.faceStartIndex = (int[]) faceStartIndex.clone();
    return copy;
  }
  
  /** Determine whether this object represents the same set of values as another one. */
  
  public boolean equals(Object o)
  {
    if (!(o instanceof FaceVertexParameterValue))
      return false;
    FaceVertexParameterValue v = (FaceVertexParameterValue) o;
    if (v.values.length != values.length || v.faceStartIndex.length != faceStartIndex.length)
      return false;
    for (int i = 0; i < values.length; i++)
      if (v.values[i] != values[i])
        return false;
    for (int i = 0; i < faceStartIndex.length; i++)
      if (v.faceStartIndex[i] != faceStartIndex[i])
        return false;
    return true;
  }
  
  /** Write out a serialized representation of this object to a stream. */
  
  public void writeToStream(DataOutputStream out) throws IOException
  {
    out.writeInt(-1);
    out.writeInt(values.length);
    for (int i = 0; i < values.length; i++)
      out.writeDouble(values[i]);
    out.writeInt(faceStartIndex.length);
    for (int i = 1; i < faceStartIndex.length; i++)
      out.writeInt(faceStartIndex[i]);
  }
  
  /** Reconstruct a serialized object. */
  
  public FaceVertexParameterValue(DataInputStream in) throws IOException
  {
    int version = in.readInt();
    if (version >= 0)
    {
      // Old format from before a version number was being stored.

      int faces = version;
      values = new double[faces*3];
      faceStartIndex = new int[values.length+1];
      for (int i = 0; i < 3; i++)
        for (int j = 0; j < faces; j++)
          values[j*3+i] = in.readDouble();
      for (int i = 1; i <= faces; i++)
        faceStartIndex[i] = i*3;
    }
    else if (version != -1)
      throw new InvalidObjectException("");
    else
    {
      values = new double[in.readInt()];
      for (int i = 0; i < values.length; i++)
        values[i] = in.readDouble();
      faceStartIndex = new int[in.readInt()];
      for (int i = 1; i < faceStartIndex.length; i++)
        faceStartIndex[i] = in.readInt();
    }
  }
}
