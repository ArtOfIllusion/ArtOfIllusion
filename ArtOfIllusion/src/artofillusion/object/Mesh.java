/* Copyright (C) 1999-2004 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.object;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.math.*;
import artofillusion.texture.*;
import artofillusion.ui.MeshEditController;
import buoy.widget.*;

/** The Mesh interface represents an object which is defined by a set of control vertices. */

public interface Mesh
{
  public static final int NO_SMOOTHING = 0;
  public static final int SMOOTH_SHADING = 1;
  public static final int INTERPOLATING = 2;
  public static final int APPROXIMATING = 3;

  /** Get the list of vertices which define the mesh. */

  public MeshVertex[] getVertices();
  
  /** Get a list of the positions of all vertices which define the mesh. */
  
  public Vec3 [] getVertexPositions();

  /** Set the positions for all the vertices of the mesh. */

  public void setVertexPositions(Vec3 v[]);
      
  /** Get a bounding box for the mesh. */

  public BoundingBox getBounds();

  /** Get an array of normal vectors, one for each vertex. */
     
  public Vec3 [] getNormals();
  
  /** Get an array of TextureParameters which are defined on this mesh. */
  
  public TextureParameter [] getParameters();
  
  /** Get the values of the TextureParameters which are defined on this mesh. */
  
  public ParameterValue [] getParameterValues();

  /** Create a new object which is an exact duplicate of this one. */
  
  public abstract Object3D duplicate();

  /** Copy all the properties of another object, to make this one identical to it.  If the
      two objects are of different classes, this will throw a ClassCastException. */
  
  public abstract void copyObject(Object3D obj);

  /** Get the skeleton for the object.  If it does not have one, this should return null. */
  
  public Skeleton getSkeleton();
  
  /** Set the skeleton for the object.  If it cannot have a skeleton, this should do nothing. */

  public void setSkeleton(Skeleton s);
  
  /** Get a MeshViewer which can be used for viewing this mesh. */
  
  public MeshViewer createMeshViewer(MeshEditController controller, RowContainer options);
}