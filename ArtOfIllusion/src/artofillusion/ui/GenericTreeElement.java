/* This class is a generic TreeElement which can represent any object, but not does need
   any special behavior. */

/* Copyright (C) 2001 by Peter Eastman
   Changes copyright (C) 2017-2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.ui;

import java.util.*;

public class GenericTreeElement extends TreeElement
{
  Object obj;
  String label;

  public GenericTreeElement(String label,
		  Object obj,
		  TreeElement parent,
		  TreeList tree,
		  List<TreeElement> children)
  {
    this.label = label;
    this.obj = obj;
    this.parent = parent;
    this.tree = tree;
    this.children = children;
    if (null == children)
      this.children = new ArrayList<TreeElement>();
    else
      for(TreeElement item: children)
      {
          item.parent = this;
      }
  }

  /* Get the label to display for this element. */

  @Override
  public String getLabel()
  {
    return label;
  }

  /* Assume no other children can be added. */

  @Override
  public boolean canAcceptAsParent(TreeElement el)
  {
    return false;
  }

  /* Add another element as a child of this one. */

  @Override
  public void addChild(TreeElement el, int position)
  {
    children.add(position, el);
    el.parent = this;
  }

  /* Remove any elements corresponding to the given object from this element's list
     of children. */

  @Override
  public void removeChild(Object object)
  {
    TreeElement el = null;
    int pos;

    for (pos = 0; pos < children.size(); pos++)
      {
        el = children.get(pos);
        if (el.getObject() == object)
          break;
      }
    if (pos == children.size())
      {
        for (int i = 0; i < children.size(); i++)
          children.get(i).removeChild(object);
        return;
      }
    el.parent = null;
    children.remove(pos);
  }

  /* Get the object corresponding to this element. */

  @Override
  public Object getObject()
  {
    return obj;
  }

  /* Get whether this element should be drawn in gray (i.e. to indicate it is deactivated). */

  @Override
  public boolean isGray()
  {
    return false;
  }
}
