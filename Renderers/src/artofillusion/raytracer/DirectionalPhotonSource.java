/* Copyright (C) 2003-2013 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.raytracer;

import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.util.*;

/** This is a PhotonSource corresponding to a directional light. */

public class DirectionalPhotonSource implements PhotonSource
{
  private CoordinateSystem coords;
  private RGBColor color;
  private float lightIntensity;
  private double minx, maxx, miny, maxy;
  private Vec3 center;

  /** Create a DirectionalPhotonSource. */
  
  public DirectionalPhotonSource(DirectionalLight light, CoordinateSystem coords, PhotonMap map)
  {
    this.coords = coords;
    BoundingBox bounds = map.getBounds().transformAndOutset(coords.toLocal());
    minx = bounds.minx;
    maxx = bounds.maxx;
    miny = bounds.miny;
    maxy = bounds.maxy;
    center = coords.fromLocal().times(new Vec3(0.5*(minx+maxx), 0.5*(miny+maxy), bounds.minz-1.0));
    color = new RGBColor();
    light.getLight(color, new Vec3());
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
  
  /**
   * Generate photons and add them to a map.
   *
   * @param map          the PhotonMap to add the Photons to
   * @param intensity    the PhotonSource should generate Photons whose total intensity is approximately equal to this
   * @param threads      a ThreadManager which may optionally be used to parallelize photon generation
   */
  
  public void generatePhotons(final PhotonMap map, double intensity, ThreadManager threads)
  {
    final Thread currentThread = Thread.currentThread();
    final Vec3 xdir = coords.fromLocal().timesDirection(Vec3.vx());
    final Vec3 ydir = coords.fromLocal().timesDirection(Vec3.vy());
    final double xsize = maxx-minx, ysize = maxy-miny;
    int num = (int) intensity;

    // Send out the photons.  To reduce noise, we use stratified sampling.  Repeatedly find the largest
    // NxN grid whose number of cells is smaller than the number of photons needed, and send out a photon
    // through a random point in each cell.

    while (num > 0)
      {
        final int n = Math.max((int) Math.sqrt(num), 1);
        final double dx = xsize/n, dy = ysize/n;
        threads.setNumIndices(n*n);
        threads.setTask(new ThreadManager.Task()
        {
          public void execute(int index)
          {
            if (map.getRenderer().renderThread != currentThread)
              return;
            int i = index/n;
            int j = index-(i*n);
            double basex = -0.5*xsize+i*dx;
            double basey = -0.5*ysize+j*dy;
            Ray r = new Ray(map.getWorkspace().context);
            Vec3 orig = r.getOrigin();
            r.getDirection().set(coords.getZDirection());
            double x = basex+map.random.nextDouble()*dx, y = basey+map.random.nextDouble()*dy;
            orig.set(center.x+x*xdir.x+y*ydir.x, center.y+x*xdir.y+y*ydir.y, center.z+x*xdir.z+y*ydir.z);
            r.newID();
            map.spawnPhoton(r, color, false);
          }
          public void cleanup()
          {
            map.getWorkspace().cleanup();
          }
        });
        threads.run();
        num -= n*n;
      }
  }
}