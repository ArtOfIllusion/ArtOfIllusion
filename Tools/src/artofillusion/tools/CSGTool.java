/* Copyright (C) 2001-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.tools;

import artofillusion.*;
import artofillusion.animation.*;
import artofillusion.object.*;
import artofillusion.math.*;
import artofillusion.ui.*;
import buoy.widget.*;
import java.util.*;

/** The CSG tool creates Constructive Solid Geometry (CSG) objects. */

public class CSGTool implements ModellingTool
{
  private static int counter = 1;

  public CSGTool()
  {
  }
  
  /* Get the text that appear as the menu item.*/

  public String getName()
  {
    return Translate.text("menu.boolean");
  }

  /* See whether an appropriate set of objects is selected and either display an error
     message, or bring up the extrude window. */
  
  public void commandSelected(LayoutWindow window)
  {
    Scene scene = window.getScene();
    int selection[] = window.getSelectedIndices(), closedCount = 0;
    Vector<ObjectInfo> inputObj = new Vector<ObjectInfo>();
    
    for (int i = 0; i < selection.length; i++)
      {
        ObjectInfo obj = scene.getObject(selection[i]);
        if (obj.getObject().canSetTexture())
          if (obj.getObject() instanceof TriangleMesh || obj.getObject().canConvertToTriangleMesh() != Object3D.CANT_CONVERT)
            {
              inputObj.addElement(obj);
              if (obj.getObject().isClosed())
                closedCount++;
            }
      }
    if (inputObj.size() < 2 || closedCount < 1)
      {
        new BStandardDialog("", UIUtilities.breakString("You must select two objects for boolean modelling, at least one of which must be solid."), BStandardDialog.INFORMATION).showMessageDialog(window.getFrame());
        return;
      }
    CSGObject newobj = new CSGObject(inputObj.elementAt(0), inputObj.elementAt(1), CSGObject.UNION);
    Vec3 center = newobj.centerObjects();
    CSGDialog dial = new CSGDialog(window, newobj);
    if (!dial.clickedOk())
      return;
    ObjectInfo info = new ObjectInfo(newobj, new CoordinateSystem(center, Vec3.vz(), Vec3.vy()), "Boolean "+(counter++));
    info.addTrack(new PositionTrack(info), 0);
    info.addTrack(new RotationTrack(info), 1);
    window.addObject(info, null);
    window.setSelection(scene.getNumObjects()-1);
    window.setUndoRecord(new UndoRecord(window, false, UndoRecord.DELETE_OBJECT, new Object [] {new Integer(scene.getNumObjects()-1)}));
    window.updateImage();
  }
}