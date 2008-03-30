/* Copyright (C) 2001-2002 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.animation;

import artofillusion.object.*;
import artofillusion.ui.*;
import java.util.*;

/** This class represents an ObjectRef in a the TreeList. */

public class ObjectRefTreeElement extends TreeElement
{
  ObjectRef ref;
  boolean disabled;
  
  public ObjectRefTreeElement(ObjectRef obj, TreeElement parent, TreeList tree, ObjectInfo exclude)
  {
    ref = obj;
    this.parent = parent;
    this.tree = tree;
    children = new Vector();
    ObjectInfo info = ref.getObject();
    setEnabled(info != exclude);
    if (ref.getJoint() == null)
      {
	Skeleton s = info.getSkeleton();
	if (s != null)
	  {
	    Joint j[] = s.getJoints();
	    for (int i = 0; i < j.length; i++)
	      children.addElement(new ObjectRefTreeElement(new ObjectRef(info, j[i]), this, tree, exclude));
	  }
	for (int i = 0; i < info.getChildren().length; i++)
	  children.addElement(new ObjectRefTreeElement(new ObjectRef(info.getChildren()[i]), this, tree, exclude));
      }
  }
  
  /* Get the label to display for this element. */
  
  public String getLabel()
  {
    Joint j = ref.getJoint();
    if (j != null)
      return j.name;
    return ref.getObject().getName();
  }
  
  /* Determine whether this element can be added as a child of another one  If el is null,
     return whether this element can be added at the root level of the tree. */
  
  public boolean canAcceptAsParent(TreeElement el)
  {
    return false;
  }
  
  /* Add another element as a child of this one. */
  
  public void addChild(TreeElement el, int position)
  {
    children.insertElementAt(el, position);
    ((ObjectRefTreeElement) el).parent = this;
  }
  
  /* Remove any elements corresponding to the given object from this element's list 
     of children. */
  
  public void removeChild(Object object)
  {
  }
  
  /* Get the object corresponding to this element. */
  
  public Object getObject()
  {
    return ref;
  }
  
  /* Set whether this element is enabled so that it can be selected. */
  
  public void setEnabled(boolean enable)
  {
    disabled = !enable;
    setSelectable(enable);
  }
  
  /* Get whether this element should be drawn in gray (i.e. to indicate it is deactivated). */
  
  public boolean isGray()
  {
    return disabled;
  }
}