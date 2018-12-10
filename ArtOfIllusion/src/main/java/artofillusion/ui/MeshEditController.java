/* Copyright (C) 2005 by Peter Eastman

This program is free software; you can redistribute it and/or modify it under the
terms of the GNU General Public License as published by the Free Software
Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY 
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import artofillusion.object.*;

/** This interface represents an object which coordinates the editing of a mesh. */

public interface MeshEditController
{
  public static final int POINT_MODE = 0;
  public static final int EDGE_MODE = 1;
  public static final int FACE_MODE = 2;

  /** Get the object being edited. */

  public ObjectInfo getObject();
  
  /** Set the mesh being edited. */
  
  public void setMesh(Mesh mesh);
  
  /** This should be called whenever the object has changed. */
  
  public void objectChanged();
  
  /** Get the current selection mode.  This will be POINT_MODE, EDGE_MODE, or FACE_MODE. */
  
  public int getSelectionMode();
  
  /** Set the selection mode.  The allowed values are POINT_MODE, EDGE_MODE, and FACE_MODE,
      although some modes may not be permitted for some controllers. */
  
  public void setSelectionMode(int mode);
  
  /** Get an array of flags specifying which parts of the object are selected.  Depending on the selection mode
      and type of object, this may correspond to vertices, faces, edges, etc. */
  
  public boolean[] getSelection();
  
  /** Set an array of flags specifying which parts of the object are selected.  Depending on the selection mode
      and type of object, this may correspond to vertices, faces, edges, etc. */
  
  public void setSelection(boolean selected[]);

  /** Get the distance of each vertex from the selected part of the object.  This is 0 for a selected
      vertex, 1 for a vertex adjacent to a selected one, etc., up to a specified maximum
      distance.  For vertices more than the maximum distance for a selected one, it is -1. */

  public int[] getSelectionDistance();

  /** Get the mesh tension level. */

  public double getMeshTension();

  /** Get the distance over which mesh tension applies. */

  public int getTensionDistance();
}
