/* Copyright (C) 1999-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import artofillusion.animation.*;
import artofillusion.object.*;
import artofillusion.ui.MeshEditController;
import buoy.widget.*;
import java.util.*;

/** MeshViewer is an abstract subclass of ViewerCanvas used for displaying Mesh objects. */

public abstract class MeshViewer extends ObjectViewer
{
  public static final int HANDLE_SIZE = 5;
  protected boolean showMesh, showSurface, showSkeleton;
  private int selectedJoint;
  private boolean detachSkeleton;
  private Vector lockedJoints;

  public MeshViewer(MeshEditController controller, RowContainer p)
  {
    super(controller, p);
    lockedJoints = new Vector();
    Skeleton s = controller.getObject().object.getSkeleton();
  }
  
  /** Get the ID of the selected joint. */
  
  public int getSelectedJoint()
  {
    return selectedJoint;
  }
  
  /** Set the selected joint. */
  
  public void setSelectedJoint(int id)
  {
    selectedJoint = id;
  }
  
  /** Get an array of size [# joints in skeleton] specifyiing which ones are locked. */
  
  public boolean [] getLockedJoints()
  {
    Skeleton s = ((Mesh) getController().getObject().object).getSkeleton();
    if (s == null)
      return new boolean [0];
    boolean b[] = new boolean [s.getNumJoints()];
    for (int i = 0; i < lockedJoints.size(); i++)
    {
      int index = s.findJointIndex(((Integer) lockedJoints.elementAt(i)).intValue());
      if (index > -1 && index < b.length)
        b[index] = true;
    }
    return b;
  }
  
  /** Determine whether a particular joint is locked. */
  
  public boolean isJointLocked(int id)
  {
    for (int i = 0; i < lockedJoints.size(); i++)
      if (((Integer) lockedJoints.elementAt(i)).intValue() == id)
        return true;
    return false;
  }
  
  /** Lock the joint with the specified ID. */
  
  public void lockJoint(int id)
  {
    Integer i = new Integer(id);
    if (lockedJoints.indexOf(i) == -1)
      lockedJoints.addElement(i);
  }
  
  /** Unlock the joint with the specified ID. */
  
  public void unlockJoint(int id)
  {
    lockedJoints.removeElement(new Integer(id));
  }

  /** Get whether the control mesh is visible. */
  
  public boolean getMeshVisible()
  {
    return showMesh;
  }

  /** Set whether the control mesh is visible. */
  
  public void setMeshVisible(boolean visible)
  {
    showMesh = visible;
  }
  
  /** Get whether the surface is visible. */
  
  public boolean getSurfaceVisible()
  {
    return showSurface;
  }

  /** Set whether the surface is visible. */
  
  public void setSurfaceVisible(boolean visible)
  {
    showSurface = visible;
  }
  
  /** Get whether the skeleton is visible. */
  
  public boolean getSkeletonVisible()
  {
    return showSkeleton;
  }

  /** Set whether the skeleton is visible. */
  
  public void setSkeletonVisible(boolean visible)
  {
    showSkeleton = visible;
  }
  
  /** Get whether the mesh is detached from the skeleton. */
  
  public boolean getSkeletonDetached()
  {
    return detachSkeleton;
  }

  /** Set whether the mesh is detached from the skeleton. */

  public void setSkeletonDetached(boolean detached)
  {
    detachSkeleton = detached;
  }
}