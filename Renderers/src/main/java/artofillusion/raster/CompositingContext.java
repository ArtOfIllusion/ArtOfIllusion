/* Copyright (C) 2007-2008 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raster;

import artofillusion.math.*;
import artofillusion.texture.*;
import artofillusion.material.*;
import artofillusion.*;

import java.util.*;

/**
 * This class holds temporary information used while compositing fragments to form the final
 * image.  One instance of it is created for every worker thread.
 */

public class CompositingContext
{
  public Vec3 tempVec[];
  public TextureSpec surfSpec;
  public MaterialSpec matSpec;
  public Camera camera;
  public RGBColor addColor, multColor, subpixelMult;
  public RGBColor subpixelColor, totalColor, totalTransparency;
  public ArrayList<ObjectMaterialInfo> materialStack;

  public CompositingContext(Camera camera)
  {
    this.camera = (camera == null ? null : camera.duplicate());
    surfSpec = new TextureSpec();
    matSpec = new MaterialSpec();
    tempVec = new Vec3 [4];
    for (int i = 0; i < tempVec.length; i++)
      tempVec[i] = new Vec3();
    addColor = new RGBColor();
    multColor = new RGBColor();
    subpixelMult = new RGBColor();
    subpixelColor = new RGBColor();
    totalColor = new RGBColor();
    totalTransparency = new RGBColor();
    materialStack = new ArrayList<ObjectMaterialInfo>();
  }

  /**
   * This is called when rendering is finished.  It nulls out fields to help garbage collection.
   */

  public void cleanup()
  {
    tempVec = null;
    surfSpec = null;
    matSpec = null;
    camera = null;
    addColor = null;
    multColor = null;
    subpixelMult = null;
    subpixelColor = null;
    totalColor = null;
    totalTransparency = null;
    materialStack = null;
  }
}
