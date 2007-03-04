/* Copyright (C) 2001-2004 by Peter Eastman

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

/** The extrude tool creates new objects by extruding a curve or surface along a path. */

public class ExtrudeTool implements ModellingTool
{
  public ExtrudeTool()
  {
  }
  
  /* Get the text that appear as the menu item.*/

  public String getName()
  {
    return Translate.text("menu.extrude");
  }

  /* See whether an appropriate set of objects is selected and either display an error
     message, or bring up the extrude window. */
  
  public void commandSelected(LayoutWindow window)
  {
    Scene scene = window.getScene();
    int selection[] = scene.getSelection();
    
    for (int i = 0; i < selection.length; i++)
      {
	Object3D obj = scene.getObject(selection[i]).object;
	if (obj instanceof Curve || ((obj instanceof TriangleMesh || 
	    obj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT) && !obj.isClosed()))
	  {
	    new ExtrudeDialog(window);
	    return;
	  }
      }
    new BStandardDialog("", UIUtilities.breakString("You must select a curve or open surface to extrude and, optionally, a curve along which to extrude it."), BStandardDialog.INFORMATION).showMessageDialog(window.getFrame());
  }
}