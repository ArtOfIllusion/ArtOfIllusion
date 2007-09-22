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
  protected CoordinateSystem coords;
  protected double ax, bx, cx, dx, ay, by, cy, dy, az, bz, cz, dz;
  protected double xscale, yscale, zscale;
  protected boolean scaleToObject;

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

  /** Get whether the material is scaled based on the size of the object. */

  public boolean isScaledToObject()
  {
    return scaleToObject;
  }

  /** Set whether the material is scaled based on the size of the object. */

  public void setScaledToObject(boolean scaled)
  {
    scaleToObject = scaled;
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
    if (scaleToObject)
    {
      double sizex, sizey, sizez;
      sizex = sizey = sizez = material.getStepSize();
      BoundingBox bounds = getObject().getBounds();
      if (bounds.maxx > bounds.minx)
        sizex /= bounds.maxx-bounds.minx;
      if (bounds.maxy > bounds.miny)
        sizey /= bounds.maxy-bounds.miny;
      if (bounds.maxz > bounds.minz)
        sizez /= bounds.maxz-bounds.minz;
      return Math.abs(Math.min(Math.min(length(ax*sizex, bx*sizey, cx*sizez),
          length(ay*sizex, by*sizey, cy*sizez)),
          length(az*sizex, bz*sizey, cz*sizez)));
    }
    return Math.abs(material.getStepSize()/Math.max(Math.max(xscale, yscale), zscale));
  }

  /* Methods from MaterialMapping. */

  public void getMaterialSpec(Vec3 pos, MaterialSpec spec, double size, double time)
  {
    double x = pos.x, y = pos.y, z = pos.z;
    double sizex = size, sizey = size, sizez = size;
    if (scaleToObject)
    {
      BoundingBox bounds = getObject().getBounds();
      if (bounds.maxx > bounds.minx)
      {
        double scale = 1.0/(bounds.maxx-bounds.minx);
        x *= scale;
        sizex = size*scale;
      }
      if (bounds.maxy > bounds.miny)
      {
        double scale = 1.0/(bounds.maxy-bounds.miny);
        y *= scale;
        sizey = size*scale;
      }
      if (bounds.maxz > bounds.minz)
      {
        double scale = 1.0/(bounds.maxz-bounds.minz);
        z *= scale;
        sizez = size*scale;
      }
    }
    ((Material3D) material).getMaterialSpec(spec, x*ax+y*bx+z*cx-dx, x*ay+y*by+z*cy-dy, x*az+y*bz+z*cz-dz,
        length(ax*sizex, bx*sizey, cx*sizez),
        length(ay*sizex, by*sizey, cy*sizez),
        length(az*sizex, bz*sizey, cz*sizez), time);
  }

  /**
   * Return the length of a vector defined by three components.
   */

  private double length(double x, double y, double z)
  {
    return Math.sqrt(x*x+y*y+z*z);
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
    map.scaleToObject = scaleToObject;
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
    scaleToObject = map.scaleToObject;
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
    if (version < 0 || version > 1)
      throw new InvalidObjectException("");
    coords = new CoordinateSystem(in);
    dx = in.readDouble();
    dy = in.readDouble();
    dz = in.readDouble();
    xscale = in.readDouble();
    yscale = in.readDouble();
    zscale = in.readDouble();
    scaleToObject = (version > 0 ? in.readBoolean() : false);
    findCoefficients();
  }
  
  public void writeToFile(DataOutputStream out) throws IOException
  {
    out.writeShort(1);
    coords.writeToFile(out);
    out.writeDouble(dx);
    out.writeDouble(dy);
    out.writeDouble(dz);
    out.writeDouble(xscale);
    out.writeDouble(yscale);
    out.writeDouble(zscale);
    out.writeBoolean(scaleToObject);
  }
  
  /** Editor is an inner class for editing the mapping. */

  private class Editor extends FormContainer
  {
    ValueField xrotField, yrotField, zrotField, xscaleField, yscaleField, zscaleField, xtransField, ytransField, ztransField;
    BCheckBox scaleToObjectBox;
    Object3D theObject;
    MaterialPreviewer preview;

    public Editor(Object3D obj, MaterialPreviewer preview)
    {
      super(6, 7);
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
      add(scaleToObjectBox = new BCheckBox(Translate.text("scaleMatToObject"), scaleToObject), 0, 6, 6, 1);
      xscaleField.addEventLink(ValueChangedEvent.class, this);
      yscaleField.addEventLink(ValueChangedEvent.class, this);
      zscaleField.addEventLink(ValueChangedEvent.class, this);
      xtransField.addEventLink(ValueChangedEvent.class, this);
      ytransField.addEventLink(ValueChangedEvent.class, this);
      ztransField.addEventLink(ValueChangedEvent.class, this);
      xrotField.addEventLink(ValueChangedEvent.class, this);
      yrotField.addEventLink(ValueChangedEvent.class, this);
      zrotField.addEventLink(ValueChangedEvent.class, this);
      scaleToObjectBox.addEventLink(ValueChangedEvent.class, this);
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
      scaleToObject = scaleToObjectBox.getState();
      findCoefficients();
      preview.render();
    }
  }
}