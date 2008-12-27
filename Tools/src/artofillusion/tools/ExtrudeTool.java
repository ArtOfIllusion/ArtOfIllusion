/* Copyright (C) 2001-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.tools;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.widget.*;

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
    int selection[] = window.getSelectedIndices();
    
    for (int i = 0; i < selection.length; i++)
      {
        Object3D obj = scene.getObject(selection[i]).getObject();
        if (obj instanceof Curve || ((obj instanceof TriangleMesh ||
            obj.canConvertToTriangleMesh() != Object3D.CANT_CONVERT) && !obj.isClosed()))
          {
            new ExtrudeDialog(window);
            return;
          }
      }
    new BStandardDialog("", UIUtilities.breakString("You must select a curve or open surface to extrude and, optionally, a curve along which to extrude it."), BStandardDialog.INFORMATION).showMessageDialog(window.getFrame());
  }

  /** Extrude a curve into a spline mesh.

      @param profile     the curve to extrude
      @param profCoords  the coordinate system of the profile
      @param dir         the direction and distance along which to extrude it
      @param segments    the number of segments to create
      @param angle       the twist angle (in radians)
      @param orient      if true, the orientation of the profile will follow the curve
      @return the extruded object
  */

  public static Object3D extrudeCurve(Curve profile, CoordinateSystem profCoords, Vec3 dir, int segments, double angle, boolean orient)
  {
    return ExtrudeDialog.extrudeCurve(profile, profCoords, dir, segments, angle, orient);
  }

  /** Extrude a curve into a spline mesh.

      @param profile     the curve to extrude
      @param path        the path along which to extrude it
      @param profCoords  the coordinate system of the profile
      @param pathCoords  the coordinate system of the path
      @param angle       the twist angle (in radians)
      @param orient      if true, the orientation of the profile will follow the curve
      @return the extruded object
  */

  public static Object3D extrudeCurve(Curve profile, Curve path, CoordinateSystem profCoords, CoordinateSystem pathCoords, double angle, boolean orient)
  {
    return ExtrudeDialog.extrudeCurve(profile, path, profCoords, pathCoords, angle, orient);
  }

  /** Extrude a triangle mesh into a solid object.

      @param profile     the TriangleMesh to extrude
      @param profCoords  the coordinate system of the profile
      @param dir         the direction and distance along which to extrude it
      @param segments    the number of segments to create
      @param angle       the twist angle (in radians)
      @param orient      if true, the orientation of the profile will follow the curve
      @return the extruded object
  */

  public static Object3D extrudeMesh(TriangleMesh profile, CoordinateSystem profCoords, Vec3 dir, int segments, double angle, boolean orient)
  {
    return ExtrudeDialog.extrudeMesh(profile, profCoords, dir, segments, angle, orient);
  }

  /** Extrude a triangle mesh into a solid object.

      @param profile     the TriangleMesh to extrude
      @param path        the path along which to extrude it
      @param profCoords  the coordinate system of the profile
      @param pathCoords  the coordinate system of the path
      @param angle       the twist angle (in radians)
      @param orient      if true, the orientation of the profile will follow the curve
      @return the extruded object
  */

  public static Object3D extrudeMesh(TriangleMesh profile, Curve path, CoordinateSystem profCoords, CoordinateSystem pathCoords, double angle, boolean orient)
  {
    return ExtrudeDialog.extrudeMesh(profile, path, profCoords, pathCoords, angle, orient);
  }
}