/* Copyright (C) 2007-2009 by Peter Eastman

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

/**
 * This class holds temporary information used during raster rendering.  One instance of it is
 * created for every worker thread.
 */

public class RasterContext
{
  public Vec3 tempVec[], lightPosition[], lightDirection[];
  public RGBColor tempColor[];
  public TextureSpec surfSpec, surfSpec2;
  public Camera camera;
  public Fragment fragment[];

  public RasterContext(Camera camera, int width)
  {
    this.camera = (camera == null ? null : camera.duplicate());
    surfSpec = new TextureSpec();
    surfSpec2 = new TextureSpec();
    tempColor = new RGBColor [4];
    for (int i = 0; i < tempColor.length; i++)
      tempColor[i] = new RGBColor(0.0f, 0.0f, 0.0f);
    tempVec = new Vec3 [4];
    for (int i = 0; i < tempVec.length; i++)
      tempVec[i] = new Vec3();
    fragment = new Fragment[width];
  }

  /**
   * This is called when rendering is finished.  It nulls out fields to help garbage collection.
   */

  public void cleanup()
  {
    tempVec = null;
    lightPosition = null;
    lightDirection = null;
    tempColor = null;
    surfSpec = null;
    surfSpec2 = null;
    camera = null;
  }
}
