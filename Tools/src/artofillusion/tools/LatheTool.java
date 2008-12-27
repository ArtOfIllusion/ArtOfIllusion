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

/** The lathe tool creates new objects by rotating a curve around an axis. */

public class LatheTool implements ModellingTool
{
  public static final int X_AXIS = 0;
  public static final int Y_AXIS = 1;
  public static final int Z_AXIS = 2;
  public static final int AXIS_THROUGH_ENDS = 3;

  public LatheTool()
  {
  }
  
  /* Get the text that appear as the menu item.*/

  public String getName()
  {
    return Translate.text("menu.lathe");
  }

  /* See whether an appropriate object is selected and either display an error
     message, or bring up the extrude window. */
  
  public void commandSelected(LayoutWindow window)
  {
    Scene scene = window.getScene();
    int selection[] = window.getSelectedIndices();
    
    if (selection.length == 1)
      {
        ObjectInfo obj = scene.getObject(selection[0]);
        if (obj.getObject() instanceof Curve)
          {
            new LatheDialog(window, obj);
            return;
          }
      }
    new BStandardDialog("", "You must select a single curve to lathe.", BStandardDialog.INFORMATION).showMessageDialog(window.getFrame());
  }

  /**
   * Create a mesh by rotating a curve around an axis.
   *
   * @param curve      the Curve to lathe
   * @param axis       the axis around which to rotate the curve.  This should be one of the constants
   *                   defined by this class: X_AXIS, Y_AXIS, Z_AXIS, or AXIS_THROUGH_ENDS.
   * @param segments   the number of segments the lathed mesh should include.  The larger this number,
   *                   the higher the resolution of the resulting mesh.
   * @param angle      the total angle by which to rotate the curve, in degrees.
   * @param radius     the radius by which to offset the rotation axis from the curve before performing
   *                   the lathe operation.
   * @return the mesh created by lathing the curve
   */

  public static Mesh latheCurve(Curve curve, int axis, int segments, double angle, double radius)
  {
    return LatheDialog.latheCurve(curve, axis, segments, angle, radius);
  }
}