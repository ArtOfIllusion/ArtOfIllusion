/* Copyright (C) 2001-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.tools;

import artofillusion.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.util.*;

/** The skin tool creates new objects by placing a skin over a series of curves. */

public class SkinTool implements ModellingTool
{
  public SkinTool()
  {
  }
  
  /* Get the text that appear as the menu item.*/

  public String getName()
  {
    return Translate.text("menu.skin");
  }

  /* See whether an appropriate set of objects is selected and either display an error
     message, or bring up the extrude window. */
  
  public void commandSelected(LayoutWindow window)
  {
    Scene scene = window.getScene();
    int selection[] = window.getSelectedIndices();
    Vector<ObjectInfo> curves = new Vector<ObjectInfo>();
    
    for (int i = 0; i < selection.length; i++)
      {
        ObjectInfo obj = scene.getObject(selection[i]);
        if (obj.getObject() instanceof Curve)
          curves.addElement(obj);
      }
    if (curves.size() < 2)
      {
        new BStandardDialog("", UIUtilities.breakString("You must select two or more curves to create a skin across."), BStandardDialog.INFORMATION).showMessageDialog(window.getFrame());
        return;
      }
    Curve c = (Curve) curves.elementAt(0).getObject();
    for (int i = 1; i < curves.size(); i++)
      {
        Curve c2 = (Curve) curves.elementAt(i).getObject();
        if (c2.getVertices().length != c.getVertices().length)
          {
            new BStandardDialog("", UIUtilities.breakString("All the curves must have the same number of points."), BStandardDialog.INFORMATION).showMessageDialog(window.getFrame());
            return;
          }
        if (c2.isClosed() != c.isClosed())
          {
            new BStandardDialog("", UIUtilities.breakString("You cannot create a skin between a closed curve and an open one."), BStandardDialog.INFORMATION).showMessageDialog(window.getFrame());
            return;
          }
        if (c2.getSmoothingMethod() != c.getSmoothingMethod() && c.getSmoothingMethod() != Mesh.NO_SMOOTHING && c2.getSmoothingMethod() != Mesh.NO_SMOOTHING)
          {
            new BStandardDialog("", UIUtilities.breakString("You cannot create a skin between an interpolating curve and an approximating one."), BStandardDialog.INFORMATION).showMessageDialog(window.getFrame());
            return;
          }
      }
    new SkinDialog(window, curves);
  }
}