/* Copyright (C) 2003-2005 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.math.*;
import artofillusion.object.*;

/** This is a PhotonSource corresponding to a directional light. */

public class DirectionalPhotonSource implements PhotonSource
{
  private CoordinateSystem coords;
  private RGBColor color;
  private float lightIntensity;
  private double minx, maxx, miny, maxy;
  private Vec3 center;

  /** Create a DirectionalPhotonSource. */
  
  public DirectionalPhotonSource(DirectionalLight light, CoordinateSystem coords, BoundingBox sceneBounds)
  {
    this.coords = coords;
    BoundingBox bounds = sceneBounds.transformAndOutset(coords.toLocal());
    minx = bounds.minx;
    maxx = bounds.maxx;
    miny = bounds.miny;
    maxy = bounds.maxy;
    center = coords.fromLocal().times(new Vec3(0.5*(minx+maxx), 0.5*(miny+maxy), bounds.minz-1.0));
    color = new RGBColor();
    light.getLight(color, 0.0f);
    lightIntensity = color.getRed()+color.getGreen()+color.getBlue();
    if (lightIntensity == 0.0f)
      return;
    color.scale(1.0f/lightIntensity);
    lightIntensity *= (float) ((maxx-minx)*(maxy-miny));
  }

  /** Get the total intensity of light which this object sends into the scene. */

  public double getTotalIntensity()
  {
    return lightIntensity;
  }
  
  /** Generate photons and add them to a map.
      @param map          the PhotonMap to add the Photons to
      @param intensity    the PhotonSource should generate Photons whose total intensity is approximately equal to this
  */
  
  public void generatePhotons(PhotonMap map, double intensity)
  {
    Thread currentThread = Thread.currentThread();
    Vec3 xdir = coords.fromLocal().timesDirection(Vec3.vx());
    Vec3 ydir = coords.fromLocal().timesDirection(Vec3.vy());
    double xsize = maxx-minx, ysize = maxy-miny;
    Ray r = new Ray(map.getContext());
    Vec3 orig = r.getOrigin();
    r.getDirection().set(coords.getZDirection());
    int num = (int) intensity;

    // Send out the photons.  To reduce noise, we use stratified sampling.  Repeatedly find the largest
    // NxN grid whose number of cells is smaller than the number of photons needed, and send out a photon
    // through a random point in each cell.

    while (num > 0)
      {
        int n = (int) Math.sqrt(num);
        if (n == 0)
          n = 1;
        double dx = xsize/n, dy = ysize/n;
        double basex = -0.5*xsize;
        for (int i = 0; i < n; i++)
          {
            double basey = -0.5*ysize;
            for (int j = 0; j < n; j++)
              {
                if (map.getRaytracer().renderThread != currentThread)
                  return;
                double x = basex+map.random.nextDouble()*dx, y = basey+map.random.nextDouble()*dy;
                orig.set(center.x+x*xdir.x+y*ydir.x, center.y+x*xdir.y+y*ydir.y, center.z+x*xdir.z+y*ydir.z);
                r.newID();
                map.spawnPhoton(r, color, false);
                basey += dy;
              }
            basex += dx;
          }
        num -= n*n;
      }
  }
}