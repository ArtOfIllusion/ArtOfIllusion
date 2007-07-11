/* Copyright (C) 2001-2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.material;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.io.*;

/** LinearMaterialMapping is a MaterialMapping which represents a linear mapping (this 
    includes rotations, translations, and scalings) of between material coordinates and 
    world coordinates. */

public class LinearMaterialMapping extends MaterialMapping
{
  CoordinateSystem coords;
  double ax, bx, cx, dx, ay, by, cy, dy, az, bz, cz, dz;
  double xscale, yscale, zscale, matScaleX, matScaleY, matScaleZ;

  public LinearMaterialMapping(Object3D theObject, Material3D theMaterial)
  {
    super(theObject, theMaterial);
    coords = new CoordinateSystem(new Vec3(), new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 1.0, 0.0));
    xscale = yscale = zscale = 1.0;
    dx = dy = dz = 0.0;
    findCoefficients();
  }

  public static String getName()
  {
    return "Linear";
  }

  public static boolean legalMapping(Object3D obj, Material mat)
  {
    return (mat instanceof Material3D);
  }

  /** Calculate the mapping coefficients. */
  
  private void findCoefficients()
  {
    Vec3 zdir = coords.getZDirection(), ydir = coords.getUpDirection();
    Vec3 xdir = ydir.cross(zdir);
    ax = xdir.x/xscale;
    bx = xdir.y/xscale;
    cx = xdir.z/xscale;
    ay = ydir.x/yscale;
    by = ydir.y/yscale;
    cy = ydir.z/yscale;
    az = zdir.x/zscale;
    bz = zdir.y/zscale;
    cz = zdir.z/zscale;
    matScaleX = 1.0/xscale;
    matScaleY = 1.0/yscale;
    matScaleZ = 1.0/zscale;
  }
  
  /** Get a vector whose components contain the center position for the mapping. */
  
  public Vec3 getCenter()
  {
    return new Vec3(dx, dy, dz);
  }
  
  /** Set the center position for the mapping. */
  
  public void setCenter(Vec3 center)
  {
    dx = center.x;
    dy = center.y;
    dz = center.z;
    findCoefficients();
  }
  
  /** Get a vector whose components contain the scale factors for the mapping. */
  
  public Vec3 getScale()
  {
    return new Vec3(xscale, yscale, zscale);
  }
  
  /** Set the scale factors for the mapping. */
  
  public void setScale(Vec3 scale)
  {
    xscale = scale.x;
    yscale = scale.y;
    zscale = scale.z;
    findCoefficients();
  }
  
  /** Get a vector whose components contain the rotation angles for the mapping. */
  
  public Vec3 getRotations()
  {
    double angles[] = coords.getRotationAngles();
    return new Vec3(angles[0], angles[1], angles[2]);
  }
  
  /** Set the rotation angles for the mapping. */
  
  public void setRotations(Vec3 angles)
  {
    coords.setOrientation(angles.x, angles.y, angles.z);
    findCoefficients();
  }

  /** Get the step size to use for integrating the material. */
  
  public double getStepSize()
  {
    return Math.abs(material.getStepSize()*Math.min(Math.min(matScaleX, matScaleY), matScaleZ));
  }

  /* Methods from MaterialMapping. */

  public void getMaterialSpec(Vec3 pos, MaterialSpec spec, double size, double time)
  {
    ((Material3D) material).getMaterialSpec(spec, pos.x*ax+pos.y*bx+pos.z*cx-dx,
	pos.x*ay+pos.y*by+pos.z*cy-dy, 
	pos.x*az+pos.y*bz+pos.z*cz-dz, 
	size*matScaleX, size*matScaleY, size*matScaleZ, time);
  }

  public MaterialMapping duplicate()
  {
    return duplicate(object, material);
  }
  
  public MaterialMapping duplicate(Object3D obj, Material mat)
  {
    LinearMaterialMapping map = new LinearMaterialMapping(obj, (Material3D) mat);
    
    map.coords = coords.duplicate();
    map.dx = dx;
    map.dy = dy;
    map.dz = dz;
    map.xscale = xscale;
    map.yscale = yscale;
    map.zscale = zscale;
    map.findCoefficients();
    return map;
  }
  
  public void copy(MaterialMapping mapping)
  {
    LinearMaterialMapping map = (LinearMaterialMapping) mapping; 
    
    coords = map.coords.duplicate();
    dx = map.dx;
    dy = map.dy;
    dz = map.dz;
    xscale = map.xscale;
    yscale = map.yscale;
    zscale = map.zscale;
    findCoefficients();
  }

  public Widget getEditingPanel(Object3D obj, MaterialPreviewer preview)
  {
    return new Editor(obj, preview);
  }
  
  public LinearMaterialMapping(DataInputStream in, Object3D theObject, Material theMaterial) throws IOException, InvalidObjectException
  {
    super(theObject, theMaterial);

    short version = in.readShort();
    if (version != 0)
      throw new InvalidObjectException("");
    coords = new CoordinateSystem(in);
    dx = in.readDouble();
    dy = in.readDouble();
    dz = in.readDouble();
    xscale = in.readDouble();
    yscale = in.readDouble();
    zscale = in.readDouble();
    findCoefficients();
  }
  
  public void writeToFile(DataOutputStream out) throws IOException
  {
    out.writeShort(0);
    coords.writeToFile(out);
    out.writeDouble(dx);
    out.writeDouble(dy);
    out.writeDouble(dz);
    out.writeDouble(xscale);
    out.writeDouble(yscale);
    out.writeDouble(zscale);
  }
  
  /** Editor is an inner class for editing the mapping. */

  private class Editor extends FormContainer
  {
    ValueField xrotField, yrotField, zrotField, xscaleField, yscaleField, zscaleField, xtransField, ytransField, ztransField;
    Object3D theObject;
    MaterialPreviewer preview;

    public Editor(Object3D obj, MaterialPreviewer preview)
    {
      super(6, 6);
      theObject = obj;
      this.preview = preview;
      
      // Add the various components to the container.
      
      setDefaultLayout(new LayoutInfo(LayoutInfo.CENTER, LayoutInfo.NONE, new Insets(0, 0, 0, 5), null));
      add(new BLabel(Translate.text("Scale")+":"), 0, 0, 3, 1);
      add(new BLabel("X"), 0, 1);
      add(xscaleField = new ValueField(xscale, ValueField.NONZERO, 5), 1, 1);
      add(new BLabel("Y"), 2, 1);
      add(yscaleField = new ValueField(yscale, ValueField.NONZERO, 5), 3, 1);
      add(new BLabel("Z"), 4, 1);
      add(zscaleField = new ValueField(zscale, ValueField.NONZERO, 5), 5, 1);
      add(new BLabel(Translate.text("Center")+":"), 0, 2, 3, 1);
      add(new BLabel("X"), 0, 3);
      add(xtransField = new ValueField(dx, ValueField.NONE, 5), 1, 3);
      add(new BLabel("Y"), 2, 3);
      add(ytransField = new ValueField(dy, ValueField.NONE, 5), 3, 3);
      add(new BLabel("Z"), 4, 3);
      add(ztransField = new ValueField(dz, ValueField.NONE, 5), 5, 3);
      double angles[] = coords.getRotationAngles();
      add(new BLabel(Translate.text("Rotation")+":"), 0, 4, 3, 1);
      add(new BLabel("X"), 0, 5);
      add(xrotField = new ValueField(angles[0], ValueField.NONE, 5), 1, 5);
      add(new BLabel("Y"), 2, 5);
      add(yrotField = new ValueField(angles[1], ValueField.NONE, 5), 3, 5);
      add(new BLabel("Z"), 4, 5);
      add(zrotField = new ValueField(angles[2], ValueField.NONE, 5), 5, 5);
      xscaleField.addEventLink(ValueChangedEvent.class, this);
      yscaleField.addEventLink(ValueChangedEvent.class, this);
      zscaleField.addEventLink(ValueChangedEvent.class, this);
      xtransField.addEventLink(ValueChangedEvent.class, this);
      ytransField.addEventLink(ValueChangedEvent.class, this);
      ztransField.addEventLink(ValueChangedEvent.class, this);
      xrotField.addEventLink(ValueChangedEvent.class, this);
      yrotField.addEventLink(ValueChangedEvent.class, this);
      zrotField.addEventLink(ValueChangedEvent.class, this);
    }

    private void processEvent()
    {
      xscale = xscaleField.getValue();
      yscale = yscaleField.getValue();
      zscale = zscaleField.getValue();
      dx = xtransField.getValue();
      dy = ytransField.getValue();
      dz = ztransField.getValue();
      coords.setOrientation(xrotField.getValue(), yrotField.getValue(), zrotField.getValue());
      findCoefficients();
      preview.render();
    }
  }
}