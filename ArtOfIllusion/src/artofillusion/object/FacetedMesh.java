package artofillusion.object;

/* Copyright (C) 2006 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

/**
 * A FacetedMesh represents an object which is defined by a set of polygonal faces.
 * Each face is defined by an ordered set of mesh vertices.
 */

public interface FacetedMesh extends Mesh
{
  /**
   * Get the number of faces in this mesh.
   */

  int getFaceCount();

  /**
   * Get the number of vertices in a particular face.
   *
   * @param face    the index of the face
   */

  int getFaceVertexCount(int face);

  /**
   * Get the index of a particular vertex in a particular face.
   *
   * @param face    the index of the face
   * @param vertex  the index of the vertex within the face
   *                (between 0 and getFaceVertexCount(face)-1 inclusive)
   * @return the index of the corresponding vertex in the list returned by getVertices()
   */

  int getFaceVertexIndex(int face, int vertex);
}
