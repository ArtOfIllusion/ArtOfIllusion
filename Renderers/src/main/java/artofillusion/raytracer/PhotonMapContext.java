/* Copyright (C) 2005 by Peter Eastman

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

/**
 * This class holds temporary information which is used while evaluating a photon map.
 * One instance of it is created for each photon map for each worker thread.
 */

public class PhotonMapContext
{
  private PhotonMap map;
  public PhotonList nearbyPhotons;
  public RGBColor tempColor, tempColor2;
  public Vec3 tempVec, lastPos;
  public float lastCutoff2;

  /**
   * Create a new PhotonMapContext for a PhotonMap.
   */

  public PhotonMapContext(PhotonMap map)
  {
    this.map = map;
    tempColor = new RGBColor();
    tempColor2 = new RGBColor();
    tempVec = new Vec3();
    lastPos = new Vec3();
    nearbyPhotons = new PhotonList(map.getNumToEstimate());
    lastCutoff2 = 99999.0f;
  }

  /** Determine the surface lighting at a point due to the photons in the map.
      @param pos      the position near which to locate photons
      @param spec     the surface properties at the point being evaluated
      @param normal   the surface normal at the point being evaluated
      @param viewDir  the direction from which the surface is being viewed
      @param front    true if the surface is being viewed from the front
      @param light    the total lighting contribution will be stored in this
  */

  public void getLight(Vec3 pos, TextureSpec spec, Vec3 normal, Vec3 viewDir, boolean front, RGBColor light)
  {
    map.getLight(pos, spec, normal, viewDir, front, light, this);
  }

  /** Determine the volume lighting at a point due to the photons in this map.
      @param pos      the position near which to locate photons
      @param spec     the material properties at the point being evaluated
      @param viewDir  the direction from which the material is being viewed
      @param light    the total lighting contribution will be stored in this
  */

  public void getVolumeLight(Vec3 pos, MaterialSpec spec, Vec3 viewDir, RGBColor light)
  {
    map.getVolumeLight(pos, spec, viewDir, light, this);
  }
}
