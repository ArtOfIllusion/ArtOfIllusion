/* Copyright (C) 2005-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.math.*;
import artofillusion.texture.*;
import artofillusion.material.*;
import artofillusion.raytracer.RaytracerRenderer.*;

import java.util.*;

/**
 * This class holds temporary information used during {@link RaytracerRenderer} during rendering.  One instance of it is
 * created for every worker thread.
 */

public class RenderWorkspace
{
  public RaytracerRenderer rt;
  public RaytracerContext context;
  public RTObject firstObjectHit, materialAtCamera;
  public boolean materialAtCameraIsFixed;
  public Ray ray[];
  public RGBColor color[], rayIntensity[], tempColor, tempColor2;
  public Vec3 pos[], normal[], trueNormal[];
  public double transparency[];
  public MaterialIntersection matChange[];
  public TextureSpec surfSpec[];
  public MaterialSpec matSpec;
  public PixelInfo tempPixel;
  public PhotonMapContext globalMap, causticsMap, volumeMap;

  public RenderWorkspace(RaytracerRenderer rt, RaytracerContext context)
  {
    this.rt = rt;
    this.context = context;
    int maxRayDepth = rt.maxRayDepth;
    ray = new Ray [maxRayDepth+1];
    color = new RGBColor [maxRayDepth+1];
    transparency = new double [maxRayDepth+1];
    rayIntensity = new RGBColor [maxRayDepth+1];
    surfSpec = new TextureSpec [maxRayDepth+1];
    pos = new Vec3 [maxRayDepth+1];
    normal = new Vec3 [maxRayDepth+1];
    trueNormal = new Vec3 [maxRayDepth+1];
    for (int i = 0; i < maxRayDepth+1; i++)
    {
      ray[i] = new Ray(context);
      color[i] = new RGBColor(0.0f, 0.0f, 0.0f);
      rayIntensity[i] = new RGBColor(0.0f, 0.0f, 0.0f);
      surfSpec[i] = new TextureSpec();
      pos[i] = new Vec3();
      normal[i] = new Vec3();
      trueNormal[i] = new Vec3();
    }
    matSpec = new MaterialSpec();
    tempColor = new RGBColor(0.0f, 0.0f, 0.0f);
    tempColor2 = new RGBColor(0.0f, 0.0f, 0.0f);
    matChange = new MaterialIntersection [16];
    for (int i = 0; i < matChange.length; i++)
      matChange[i] = new MaterialIntersection();
    tempPixel = new PixelInfo();
    if (rt.globalMap != null)
      globalMap = new PhotonMapContext(rt.globalMap);
    if (rt.causticsMap != null)
      causticsMap = new PhotonMapContext(rt.causticsMap);
    if (rt.volumeMap != null)
      volumeMap = new PhotonMapContext(rt.volumeMap);
  }

  /**
   * Get the RaytracerContext for this thread.
   */

  public RaytracerContext getContext()
  {
    return context;
  }

  /**
   * Increase the length of the matChange array.
   */

  public void increaseMaterialChangeLength()
  {
    MaterialIntersection newMatChange[] = new MaterialIntersection [matChange.length*2];
    System.arraycopy(matChange, 0, newMatChange, 0, matChange.length);
    for (int i = matChange.length; i < newMatChange.length; i++)
      newMatChange[i] = new MaterialIntersection();
    matChange = newMatChange;
  }

  /**
   * This is called when rendering is finished.  It nulls out fields to help garbage collection.
   */

  public void cleanup()
  {
    matChange = null;
    tempPixel = null;
    globalMap = null;
    causticsMap = null;
    volumeMap = null;
  }
}
